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

import com.datastax.driver.core.utils.UUIDs;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.LockUtils;
import com.qwazr.utils.server.ServerException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.Response;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

class StoreSchemaInstance implements Closeable {

	private final LockUtils.ReadWriteLock rwl = new LockUtils.ReadWriteLock();

	final static String DATA_DIRECTORY = "data";
	final static String TEMP_DIRECTORY = "tmp";

	private final File schemaDirectory;
	private final File dataDirectory;
	private final File tempDirectory;

	StoreSchemaInstance(final File schemaDirectory) throws IOException {
		this.schemaDirectory = schemaDirectory;
		dataDirectory = new File(schemaDirectory, DATA_DIRECTORY);
		FileUtils.forceMkdir(dataDirectory);
		tempDirectory = new File(schemaDirectory, TEMP_DIRECTORY);
		FileUtils.forceMkdir(tempDirectory);
	}

	@Override
	final public void close() throws IOException {
		rwl.readEx(() -> {
			FileUtils.cleanDirectory(tempDirectory);
		});
	}

	final void delete() throws IOException {
		rwl.writeEx(() -> {
			if (!schemaDirectory.exists())
				return;
			FileUtils.deleteQuietly(schemaDirectory);
		});
	}

	/**
	 * Get a File with a path relative to the schema directory. This method also
	 * checks that the resolved path is a child of the schema directory
	 *
	 * @param relativePath a relative path
	 * @return a file instance
	 * @throws ServerException if the schema does not exists, or if there is a permission
	 *                         issue
	 */
	final File getFile(final String relativePath) throws ServerException {
		return rwl.readEx(() -> {
			if (StringUtils.isEmpty(relativePath) || relativePath.equals("/"))
				return dataDirectory;
			final File finalFile = new File(dataDirectory, relativePath);
			File file = finalFile;
			while (file != null) {
				if (file.equals(dataDirectory))
					return finalFile;
				file = file.getParentFile();
			}
			throw new ServerException(Response.Status.FORBIDDEN, "Permission denied.");
		});
	}

	/**
	 * Upload a file to a patch relative to the schema directory.
	 *
	 * @param relativePath
	 * @param inputStream
	 * @param lastModified
	 * @return a file instance
	 * @throws ServerException
	 * @throws IOException
	 */
	final File putFile(final String relativePath, final InputStream inputStream, final Long lastModified)
			throws ServerException, IOException {
		return rwl.readEx(() -> {
			final File file = getFile(relativePath);
			if (file.exists() && file.isDirectory())
				throw new ServerException(Response.Status.CONFLICT,
						"Error. A directory already exists: " + relativePath);
			File tmpFile = null;
			try {
				tmpFile = new File(tempDirectory, UUIDs.timeBased().toString());
				if (lastModified != null)
					tmpFile.setLastModified(lastModified);
				File parent = file.getParentFile();
				if (!parent.exists())
					parent.mkdir();
				IOUtils.copy(inputStream, tmpFile);
				tmpFile.renameTo(file);
				tmpFile = null;
				return file;
			} finally {
				if (tmpFile != null)
					tmpFile.delete();
			}
		});
	}

	/**
	 * Delete the file, and prune the parent directory if empty.
	 *
	 * @param relativePath the path of the file, relative to the schema
	 * @return the file instance of the deleted file
	 * @throws ServerException is thrown is the file does not exists or if deleting the file
	 *                         was not possible
	 */
	final File deleteFile(final String relativePath) throws ServerException {
		return rwl.readEx(() -> {
			final File file = getFile(relativePath);
			if (!file.exists())
				throw new ServerException(Response.Status.NOT_FOUND, "File not found: " + relativePath);
			if (file.isDirectory()) {
				String[] files = file.list();
				if (files != null && files.length > 0)
					throw new ServerException(Response.Status.NOT_ACCEPTABLE, "The directory is not empty");
			}
			file.delete();
			if (file.exists())
				throw new ServerException(Response.Status.INTERNAL_SERVER_ERROR,
						"Unable to delete the file: " + relativePath);
			final File parent = file.getParentFile();
			if (parent.equals(dataDirectory))
				return file;
			if (parent.list().length == 0)
				parent.delete();
			return file;
		});
	}

}
