/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.store;

import com.qwazr.utils.server.ServerException;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;

class StoreServiceImpl implements StoreServiceInterface {

	private File getExistingFile(String schemaName, String path) throws ServerException {
		File file = StoreManager.INSTANCE.get(schemaName).getFile(path);
		if (!file.exists())
			throw new ServerException(Status.NOT_FOUND, "Error. file not found: " + path);
		return file;
	}

	@Override
	public Response getFile(String schemaName, String path) {
		StoreFileResult storeFile = null;
		try {
			File file = getExistingFile(schemaName, path);
			storeFile = new StoreFileResult(file, file.isDirectory());
			ResponseBuilder builder = Response.ok();
			storeFile.buildHeader(builder);
			storeFile.buildEntity(builder);
			return builder.build();
		} catch (ServerException | IOException e) {
			if (storeFile != null)
				storeFile.free();
			return ServerException.getTextException(e).getResponse();
		}
	}

	@Override
	public StoreFileResult getDirectory(String schemaName, String path) {
		try {
			File file = getExistingFile(schemaName, path);
			return new StoreFileResult(file, file.isDirectory());
		} catch (ServerException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Set<String> getSchemas() {
		return new TreeSet<>(StoreManager.INSTANCE.nameSet());
	}

	@Override
	public Response headFile(String schemaName, String path) {
		try {
			File file = getExistingFile(schemaName, path);
			StoreFileResult storeFile = new StoreFileResult(file, false);
			ResponseBuilder builder = Response.ok();
			storeFile.buildHeader(builder);
			return builder.build();
		} catch (ServerException e) {
			return ServerException.getTextException(e).getResponse();
		}
	}

	@Override
	public Response putFile(String schemaName, String path, InputStream inputStream, Long lastModified) {
		try {
			File file = StoreManager.INSTANCE.get(schemaName).putFile(path, inputStream, lastModified);
			StoreFileResult storeFile = new StoreFileResult(file, false);
			ResponseBuilder builder = Response.ok("File created: " + path, MediaType.TEXT_PLAIN);
			storeFile.buildHeader(builder);
			return builder.build();
		} catch (ServerException | IOException e) {
			throw ServerException.getTextException(e);
		}
	}

	@Override
	public Response deleteFile(String schemaName, String path) {
		try {
			StoreManager.INSTANCE.get(schemaName).deleteFile(path);
			return Response.ok("File deleted: " + path, MediaType.TEXT_PLAIN).build();
		} catch (ServerException e) {
			throw ServerException.getTextException(e);
		}
	}

	@Override
	final public StoreFileResult getDirectory(String schemaName) {
		return getDirectory(schemaName, StringUtils.EMPTY);
	}

	@Override
	final public Response getFile(String schemaName) {
		return getFile(schemaName, StringUtils.EMPTY);
	}

	@Override
	final public Response headFile(String schemaName) {
		return headFile(schemaName, StringUtils.EMPTY);
	}

	@Override
	public Response createSchema(String schemaName) {
		try {
			StoreManager.INSTANCE.createUpdate(schemaName);
			return Response.ok("Schema created: " + schemaName).build();
		} catch (IOException e) {
			throw new ServerException(e).getJsonException();
		}
	}

	@Override
	public Response deleteSchema(String schemaName) {
		try {
			StoreManager.INSTANCE.delete(schemaName);
			return Response.ok("Schema deleted: " + schemaName).build();
		} catch (IOException e) {
			throw new ServerException(e).getJsonException();
		}
	}
}
