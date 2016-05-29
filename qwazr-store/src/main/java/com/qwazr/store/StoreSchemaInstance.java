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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

class StoreSchemaInstance implements Closeable {

	private final LockUtils.ReadWriteLock rwl = new LockUtils.ReadWriteLock();

	final static String DATA_DIRECTORY = "data";
	final static String TEMP_DIRECTORY = "tmp";

	private final Path schemaDirectory;
	private final Path dataDirectory;
	private final Path tempDirectory;

	StoreSchemaInstance(final Path schemaDirectory) throws IOException {
		this.schemaDirectory = schemaDirectory;
		dataDirectory = schemaDirectory.resolve(DATA_DIRECTORY);
		Files.createDirectories(dataDirectory);
		tempDirectory = schemaDirectory.resolve(TEMP_DIRECTORY);
		Files.createDirectories(tempDirectory);
	}

	@Override
	final public void close() throws IOException {
		rwl.readEx(() -> {
			FileUtils.cleanDirectory(tempDirectory.toFile());
		});
	}

	final void delete() throws IOException {
		rwl.writeEx(() -> {
			FileUtils.deleteDirectory(schemaDirectory.toFile());
		});
	}

	/**
	 * Get a Path given a path relative to the schema directory. This method also
	 * checks that the resolved path is a child of the schema directory
	 *
	 * @param relativePath a relative path
	 * @return a file instance
	 * @throws ServerException if the schema does not exists, or if there is a permission
	 *                         issue
	 */
	final Path getPath(final String relativePath) throws IOException {
		return rwl.readEx(() -> {
			if (StringUtils.isEmpty(relativePath) || relativePath.equals("/"))
				return dataDirectory;

			final Path finalPath = dataDirectory.resolve(relativePath);
			Path path = finalPath;
			while (path != null) {
				if (Files.exists(path) && Files.isSameFile(path, dataDirectory))
					return finalPath;
				path = path.getParent();
			}
			throw new ServerException(Response.Status.FORBIDDEN, "Permission denied.");
		});
	}

	/**
	 * Upload a file to a path relative to the schema directory.
	 *
	 * @param relativePath
	 * @param inputStream
	 * @param lastModified
	 * @return a file instance
	 * @throws ServerException
	 * @throws IOException
	 */
	final Path putPath(final String relativePath, final InputStream inputStream, final Long lastModified)
			throws ServerException, IOException {
		return rwl.readEx(() -> {
			final Path path = getPath(relativePath);
			if (Files.exists(path) && Files.isDirectory(path))
				throw new ServerException(Response.Status.CONFLICT,
						"Error. A directory already exists: " + relativePath);
			Path tmpPath = null;
			try {
				tmpPath = tempDirectory.resolve(UUIDs.timeBased().toString());
				if (lastModified != null)
					Files.setLastModifiedTime(tmpPath, FileTime.fromMillis(lastModified));
				Path parent = path.getParent();
				if (parent == null || !Files.exists(parent))
					Files.createDirectories(parent);
				IOUtils.copy(inputStream, tmpPath.toFile());
				Files.move(tmpPath, path);
				tmpPath = null;
				return path;
			} finally {
				if (tmpPath != null)
					Files.deleteIfExists(tmpPath);
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
	final Path deletePath(final String relativePath) throws ServerException, IOException {
		return rwl.readEx(() -> {
			final Path path = getPath(relativePath);
			if (!Files.exists(path))
				throw new ServerException(Response.Status.NOT_FOUND, "File not found: " + relativePath);
			if (Files.isDirectory(path)) {
				String[] files = path.toFile().list();
				if (files != null && files.length > 0)
					throw new ServerException(Response.Status.NOT_ACCEPTABLE, "The directory is not empty");
			}
			Files.deleteIfExists(path);
			if (Files.exists(path))
				throw new ServerException(Response.Status.INTERNAL_SERVER_ERROR,
						"Unable to delete the file: " + relativePath);
			final Path parent = path.getParent();
			if (Files.isSameFile(parent, dataDirectory))
				return path;
			final String[] parentFiles = parent.toFile().list();
			if (parentFiles == null || parentFiles.length == 0)
				Files.deleteIfExists(parent);
			return path;
		});
	}

}
