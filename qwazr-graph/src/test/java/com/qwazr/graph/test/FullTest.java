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
package com.qwazr.graph.test;

import com.qwazr.graph.model.GraphDefinition;
import com.qwazr.graph.model.GraphDefinition.PropertyTypeEnum;
import com.qwazr.graph.model.GraphNode;
import com.qwazr.utils.CharsetUtils;
import com.qwazr.utils.http.HttpClients;
import com.qwazr.utils.http.HttpRequest;
import com.qwazr.utils.json.JsonMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.hamcrest.core.AnyOf;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FullTest {

	private static volatile boolean started;

	public static final String BASE_URL = "http://localhost:9091/graph";
	public static final String TEST_BASE = "graph-test";
	public static final int PRODUCT_NUMBER = 1000;
	public static final int VISIT_NUMBER = 1000;

	@Test
	public void test000startGraphServer() throws Exception {
		TestServer.startServer();
		Assert.assertTrue(TestServer.serverStarted);
	}

	final private HttpClientContext getContext() {
		final HttpClientContext context = HttpClientContext.create();
		final RequestConfig.Builder requestConfig = RequestConfig.custom();
		requestConfig.setSocketTimeout(60000).setConnectTimeout(60000).setConnectionRequestTimeout(60000);
		context.setAttribute(HttpClientContext.REQUEST_CONFIG, requestConfig.build());
		return context;
	}

	@Test
	public void test050CreateDatabase() throws IOException {

		HashMap<String, PropertyTypeEnum> node_properties = new HashMap<>();
		node_properties.put("type", PropertyTypeEnum.indexed);
		node_properties.put("date", PropertyTypeEnum.indexed);
		node_properties.put("name", PropertyTypeEnum.stored);
		node_properties.put("user", PropertyTypeEnum.stored);
		HashSet<String> edge_types = new HashSet<String>();
		edge_types.add("see");
		edge_types.add("buy");
		GraphDefinition graphDef = new GraphDefinition(node_properties, edge_types);

		try (CloseableHttpResponse response = HttpRequest.Post(BASE_URL + '/' + TEST_BASE)
				.bodyString(JsonMapper.MAPPER.writeValueAsString(graphDef), ContentType.APPLICATION_JSON)
				.execute(getContext())) {
			Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		}
	}

	@Test
	public void test100PutProductNodes() throws IOException {
		for (int i = 0; i < PRODUCT_NUMBER; i++) {
			GraphNode node = new GraphNode();
			node.properties = new HashMap<>();
			node.properties.put("type", "product");
			node.properties.put("name", "product" + i);
			try (CloseableHttpResponse response = HttpRequest.Post(BASE_URL + '/' + TEST_BASE + "/node/p" + i)
					.bodyString(JsonMapper.MAPPER.writeValueAsString(node), ContentType.APPLICATION_JSON)
					.execute(getContext())) {
				Assert.assertEquals(200, response.getStatusLine().getStatusCode());
			}
		}
	}

	@Test
	public void test110PutVisitNodes() throws IOException {
		for (int i = 0; i < VISIT_NUMBER; i += 100) {
			Map<String, GraphNode> nodeMap = new LinkedHashMap<>();
			for (int k = 0; k < 100; k++) {
				GraphNode node = new GraphNode();
				node.properties = new HashMap<>();
				node.properties.put("type", "visit");
				node.properties.put("user", "user" + RandomUtils.nextInt(0, 100));
				node.properties.put("date", "201501" + RandomUtils.nextInt(10, 31));
				node.edges = new HashMap<String, Set<Object>>();
				int seePages = RandomUtils.nextInt(3, 12);
				Set<Object> set = new TreeSet<Object>();
				for (int j = 0; j < seePages; j++)
					set.add("p" + RandomUtils.nextInt(0, PRODUCT_NUMBER / 2));
				node.edges.put("see", set);
				if (RandomUtils.nextInt(0, 10) == 0) {
					int buyItems = RandomUtils.nextInt(1, 5);
					set = new TreeSet<>();
					for (int j = 0; j < buyItems; j++)
						set.add("p" + RandomUtils.nextInt(0, PRODUCT_NUMBER / 2));
					node.edges.put("buy", set);
				}
				nodeMap.put("v" + (i + k), node);
			}
			try (final CloseableHttpResponse response = HttpRequest.Post(BASE_URL + '/' + TEST_BASE + "/node")
					.bodyString(JsonMapper.MAPPER.writeValueAsString(nodeMap), ContentType.APPLICATION_JSON)
					.execute(getContext())) {
				Assert.assertEquals(200, response.getStatusLine().getStatusCode());
			}
		}

	}

	private boolean nodeExists(int visiteNodeId) throws IOException {
		try (CloseableHttpResponse response = HttpRequest.Get(BASE_URL + '/' + TEST_BASE + "/node/v" + visiteNodeId)
				.execute(getContext())) {
			Assert.assertThat(response.getStatusLine().getStatusCode(), AnyOf.anyOf(Is.is(200), Is.is(404)));
			Assert.assertEquals(ContentType.parse(response.getEntity().getContentType().getValue()).toString(),
					ContentType.APPLICATION_JSON.toString());
			return response.getStatusLine().getStatusCode() == 200;
		}
	}

	@Test
	public void test200PutRandomEdges() throws IOException {
		for (int i = 0; i < VISIT_NUMBER / 100; i++) {
			int visitNodeId = RandomUtils.nextInt(VISIT_NUMBER / 2, VISIT_NUMBER);
			if (!nodeExists(visitNodeId))
				continue;
			int productNodeId = RandomUtils.nextInt(PRODUCT_NUMBER / 2, PRODUCT_NUMBER);
			try (CloseableHttpResponse response = HttpRequest.Post(
					BASE_URL + '/' + TEST_BASE + "/node/v" + visitNodeId + "/edge/see/p" + productNodeId)
					.execute(getContext())) {
				if (response.getStatusLine().getStatusCode() == 500)
					System.out.println(IOUtils.toString(response.getEntity().getContent(), CharsetUtils.CharsetUTF8));
				Assert.assertThat(response.getStatusLine().getStatusCode(), AnyOf.anyOf(Is.is(200), Is.is(404)));
				Assert.assertEquals(ContentType.parse(response.getEntity().getContentType().getValue()).toString(),
						ContentType.APPLICATION_JSON.toString());
			}
		}
	}

	@Test
	public void test210DeleteRandomEdges() throws IOException {
		for (int i = 0; i < VISIT_NUMBER / 100; i++) {
			int visiteNodeId = RandomUtils.nextInt(0, VISIT_NUMBER / 2);
			if (!nodeExists(visiteNodeId))
				continue;
			int productNodeId = RandomUtils.nextInt(0, PRODUCT_NUMBER / 2);
			try (CloseableHttpResponse response = HttpRequest.Delete(
					BASE_URL + '/' + TEST_BASE + "/node/v" + visiteNodeId + "/edge/see/p" + productNodeId)
					.execute(getContext())) {
				Assert.assertThat(response.getStatusLine().getStatusCode(), AnyOf.anyOf(Is.is(200), Is.is(404)));
			}
		}
	}

	@Test
	public void test900DeleteDatabase() throws IOException {
		try (CloseableHttpResponse response = HttpRequest.Delete(BASE_URL + '/' + TEST_BASE).execute(getContext())) {
			Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		}
	}

	@Test
	public void test999httpClient() {
		Assert.assertEquals(0, HttpClients.CNX_MANAGER.getTotalStats().getLeased());
		Assert.assertEquals(0, HttpClients.CNX_MANAGER.getTotalStats().getPending());
		Assert.assertTrue(HttpClients.CNX_MANAGER.getTotalStats().getAvailable() > 0);
	}
}
