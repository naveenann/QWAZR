/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.store;

import com.qwazr.utils.IOUtils;
import com.qwazr.utils.server.ServerBuilder;
import com.qwazr.utils.server.ServerException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StoreManager {

	public static volatile StoreManager INSTANCE = null;

	private static final Logger logger = LoggerFactory.getLogger(StoreManager.class);

	public static void load(final ServerBuilder builder) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new StoreManager(builder);
	}

	private final static String STORE_DIRECTORY = "store";

	private final ConcurrentHashMap<String, StoreSchemaInstance> schemaMap;

	private final File storeDirectory;

	private StoreManager(final ServerBuilder builder) throws IOException {
		storeDirectory = new File(builder.getServerConfiguration().dataDirectory, STORE_DIRECTORY);
		FileUtils.forceMkdir(storeDirectory);
		builder.registerWebService(StoreServiceImpl.class);
		builder.registerShutdownListener(server -> shutdown());
		schemaMap = new ConcurrentHashMap<>();
		File[] directories = storeDirectory.listFiles((FileFilter) DirectoryFileFilter.INSTANCE);
		if (directories == null)
			return;
		for (File schemaDirectory : directories) {
			try {
				schemaMap.put(schemaDirectory.getName(), new StoreSchemaInstance(schemaDirectory));
			} catch (ServerException | IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	private void shutdown() {
		synchronized (schemaMap) {
			schemaMap.values().forEach(IOUtils::closeQuietly);
		}
	}

	void createUpdate(String schemaName) throws IOException {
		synchronized (schemaMap) {
			StoreSchemaInstance schemaInstance = schemaMap.get(schemaName);
			if (schemaInstance == null) {
				schemaInstance = new StoreSchemaInstance(new File(storeDirectory, schemaName));
				schemaMap.put(schemaName, schemaInstance);
			}
		}
	}

	StoreSchemaInstance get(String schemaName) {
		StoreSchemaInstance schemaInstance = schemaMap.get(schemaName);
		if (schemaInstance == null)
			throw new ServerException(Status.NOT_FOUND, "Schema not found: " + schemaName);
		return schemaInstance;
	}

	void delete(String schemaName) throws IOException {
		synchronized (schemaMap) {
			StoreSchemaInstance schemaInstance = get(schemaName);
			schemaInstance.delete();
			schemaMap.remove(schemaName);
		}
	}

	Set<String> nameSet() {
		return schemaMap.keySet();
	}

	/**
	 * Get a File with a path including the schema name
	 *
	 * @param schemaAndPath a full path schema_name/path
	 * @return a file instance
	 * @throws ServerException if the schema does not exists, or if there is a permission
	 *                         issue
	 */
	final File getFile(final String schemaAndPath) throws ServerException {
		final int idx = schemaAndPath.indexOf('/');
		switch (idx) {
		case -1:
			return get(schemaAndPath).getFile(StringUtils.EMPTY);
		case 0:
			return get(schemaAndPath.substring(1)).getFile(StringUtils.EMPTY);
		default:
			return get(schemaAndPath.substring(0, idx)).getFile(schemaAndPath.substring(idx));
		}
	}

}
