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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.ws.rs.core.Response;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StoreTest {

	@BeforeClass
	public static void startSearchServer() throws Exception {
		TestServer.startServer();
	}

	private final List<String> SCHEMAS =
			Arrays.asList("schema1", "schema2", "schema3", "schema4", "schema5", "schema6");

	@Test
	public void test001createDeleteSchema() throws URISyntaxException {
		final StoreServiceInterface client = TestServer.getClient();
		SCHEMAS.parallelStream().forEach(schema -> {
			client.createSchema(schema);
			final StoreFileResult result = client.getDirectory(schema);
			Assert.assertNotNull(result);
			Assert.assertNotNull(result.type);
			Assert.assertEquals(result.type, StoreFileResult.Type.DIRECTORY);
			Set<String> schemas = client.getSchemas();
			Assert.assertNotNull(schemas);
			Assert.assertTrue(schemas.contains(schema));
			final Response response = client.deleteSchema(schema);
			Assert.assertNotNull(response);
			Assert.assertEquals(response.getStatus(), 200);
			schemas = client.getSchemas();
			Assert.assertNotNull(schemas);
			Assert.assertFalse(schemas.contains(schema));
		});
		final Set<String> schemas = client.getSchemas();
		Assert.assertNotNull(schemas);
		Assert.assertTrue(schemas.isEmpty());
	}
}
