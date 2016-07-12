/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.store.test;

import com.qwazr.store.StoreFileResult;
import com.qwazr.store.StoreServiceInterface;
import org.apache.commons.io.input.CountingInputStream;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class StoreAbstractTest {

	protected abstract StoreServiceInterface getClient() throws URISyntaxException;

	private final List<String> SCHEMAS =
			Arrays.asList("schema1", "schema2", "schema3", "schema4", "schema5", "schema6");

	private Response checkResponse(Response response, int code) {
		Assert.assertNotNull(response);
		Assert.assertEquals(code, response.getStatus());
		return response;
	}

	private StoreFileResult checkFileResult(StoreFileResult result, StoreFileResult.Type type) {
		Assert.assertNotNull(result);
		if (type != null) {
			Assert.assertNotNull(result.type);
			Assert.assertEquals(type, result.type);
		}
		return result;
	}

	private Set<String> checkSchemas(Set<String> schemas, String schema, boolean assertTrue) {
		Assert.assertNotNull(schemas);
		if (schema != null) {
			if (assertTrue)
				Assert.assertTrue(schemas.contains(schema));
			else
				Assert.assertFalse(schemas.contains(schema));
		}
		return schemas;
	}

	private StoreFileResult checkDirectory(StoreFileResult result, String directory) {
		Assert.assertNotNull(result.directories);
		Assert.assertTrue(result.directories.containsKey(directory));
		return result;
	}

	private void checkFile(StoreFileResult dirResult, String file) {
		Assert.assertNotNull(dirResult.files);
		final StoreFileResult fileResult = dirResult.files.get(file);
		Assert.assertNotNull(fileResult);
		Assert.assertNotNull(fileResult.size);
		Assert.assertEquals(FILE_SIZES.get(file), fileResult.size);
	}

	private void checkNoFile(StoreFileResult dirResult, String file) {
		if (dirResult.files == null)
			return;
		Assert.assertFalse(dirResult.files.containsKey(file));
	}

	@Test
	public void test000StartSearchServer() throws Exception {
		TestServer.startServer();
		Assert.assertTrue(TestServer.serverStarted);
	}

	@Test
	public void test050createDeleteSchema() throws URISyntaxException {
		final StoreServiceInterface client = getClient();
		SCHEMAS.parallelStream().forEach(schema -> {
			checkResponse(client.createSchema(schema), 200);
			checkFileResult(client.getDirectory(schema), StoreFileResult.Type.DIRECTORY);
			checkSchemas(client.getSchemas(), schema, true);
			checkResponse(client.deleteSchema(schema), 200);
			checkSchemas(client.getSchemas(), schema, false);
		});
		final Set<String> schemas = client.getSchemas();
		Assert.assertNotNull(schemas);
		Assert.assertTrue(schemas.isEmpty());
	}

	private final static String SCHEMA = "schema";

	@Test
	public void test100createSchema() throws URISyntaxException {
		final StoreServiceInterface client = getClient();
		client.createSchema(SCHEMA);
		checkFileResult(client.getDirectory(SCHEMA), StoreFileResult.Type.DIRECTORY);
	}

	@Test
	public void test200headEmptyRootDirectory() throws URISyntaxException {
		final StoreServiceInterface client = getClient();
		final Response response = checkResponse(client.headFile(SCHEMA), 200);
		Assert.assertNotNull(response.getHeaderString("Last-Modified"));
		Assert.assertEquals("DIRECTORY", response.getHeaderString("X-QWAZR-Store-Type"));
	}

	private final static String QWAZR_JPG = "Qwazr_BAT.jpg";
	private final static String QWAZR_PDF = "Qwazr_BAT.pdf";

	private final static List<String> FILES = Arrays.asList(QWAZR_JPG, QWAZR_PDF);
	private final static Map<String, Long> FILE_SIZES = new ConcurrentHashMap<>();

	private final static String DIRECTORY = "testdir";

	@Test
	public void test300putFile() throws URISyntaxException {
		final StoreServiceInterface client = getClient();
		FILES.parallelStream().forEach(file -> {
			try (final CountingInputStream is = new CountingInputStream(getClass().getResourceAsStream(file))) {
				checkResponse(client.putFile(SCHEMA, DIRECTORY + "/" + file, is, null), 200);
				FILE_SIZES.put(file, is.getByteCount());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	public void test400getDirectory() throws URISyntaxException {
		final StoreServiceInterface client = getClient();
		final StoreFileResult result = checkFileResult(client.getDirectory(SCHEMA), StoreFileResult.Type.DIRECTORY);
		checkDirectory(result, DIRECTORY);
	}

	@Test
	public void test410getFiles() throws URISyntaxException {
		final StoreServiceInterface client = getClient();
		final StoreFileResult result =
				checkFileResult(client.getDirectory(SCHEMA, DIRECTORY), StoreFileResult.Type.DIRECTORY);
		FILES.parallelStream().forEach(file -> checkFile(result, file));
	}

	@Test
	public void test500deleteFilesCheckPrune() throws URISyntaxException {
		final StoreServiceInterface client = getClient();
		checkFileResult(client.getDirectory(SCHEMA, DIRECTORY), StoreFileResult.Type.DIRECTORY);
		FILES.parallelStream().forEach(file -> checkResponse(client.deleteFile(SCHEMA, DIRECTORY + "/" + file), 200));
		try {
			client.getDirectory(SCHEMA, DIRECTORY);
			Assert.fail("Directory not pruned");
		} catch (WebApplicationException e) {
			Assert.assertEquals(404, e.getResponse().getStatus());
		}
	}

	@Test
	public void test900deleteSchema() throws URISyntaxException {
		final StoreServiceInterface client = getClient();
		checkResponse(client.deleteSchema(SCHEMA), 200);
		checkSchemas(client.getSchemas(), SCHEMA, false);
	}
}
