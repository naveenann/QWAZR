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
package com.qwazr.graph;

import com.qwazr.graph.model.*;
import com.qwazr.utils.ExceptionUtils;
import com.qwazr.utils.concurrent.ThreadUtils;
import com.qwazr.utils.json.client.JsonMultiClientAbstract;
import com.qwazr.utils.server.RemoteService;
import com.qwazr.utils.server.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class GraphMultiClient extends JsonMultiClientAbstract<GraphSingleClient> implements GraphServiceInterface {

	private static final Logger logger = LoggerFactory.getLogger(GraphMultiClient.class);

	GraphMultiClient(final RemoteService... remote) throws URISyntaxException {
		super(new GraphSingleClient[remote.length], remote);
	}

	@Override
	public Set<String> list() {

		try {

			// We merge the result of all the nodes
			final TreeSet<String> globalSet = new TreeSet<>();

			final List<ThreadUtils.ParallelRunnable> threads = new ArrayList<>(size());
			this.forEach(client -> {
				threads.add(() -> {
					Set<String> set = client.list();
					synchronized (globalSet) {
						if (set != null)
							globalSet.addAll(set);
					}
				});
			});

			ThreadUtils.parallel(threads);
			return globalSet;

		} catch (Exception e) {
			throw ServerException.getJsonException(logger, e);
		}
	}

	@Override
	public GraphDefinition createUpdateGraph(final String graphName, final GraphDefinition graphDef) {

		try {
			final AtomicReference<GraphDefinition> resultRef = new AtomicReference<>();
			final List<ThreadUtils.ParallelRunnable> threads = new ArrayList<>(size());
			this.forEach(client -> {
				threads.add(() -> {
					resultRef.compareAndSet(null, client.createUpdateGraph(graphName, graphDef));
				});
			});

			ThreadUtils.parallel(threads);
			return resultRef.get();

		} catch (Exception e) {
			throw ServerException.getJsonException(logger, e);
		}
	}

	@Override
	public GraphResult getGraph(String graphName) {
		ExceptionUtils.Holder exceptionHolder = new ExceptionUtils.Holder(logger);

		for (GraphSingleClient client : this) {
			try {
				return client.getGraph(graphName);
			} catch (WebApplicationException e) {
				if (e.getResponse().getStatus() == 404)
					logger.warn(e.getMessage(), e);
				else
					exceptionHolder.switchAndWarn(e);
			}
		}
		exceptionHolder.thrownIfAny();
		return null;
	}

	@Override
	public GraphDefinition deleteGraph(String graphName) {

		try {

			final AtomicReference<GraphDefinition> resultRef = new AtomicReference<>();
			final List<ThreadUtils.ParallelRunnable> threads = new ArrayList<>(size());
			this.forEach(client -> {
				threads.add(() -> {
					try {
						resultRef.compareAndSet(null, client.deleteGraph(graphName));
					} catch (WebApplicationException e) {
						if (e.getResponse().getStatus() != 404) {
							logger.warn(e.getMessage(), e);
							throw e;
						}
					}
				});
			});

			ThreadUtils.parallel(threads);
			GraphDefinition graphDef = resultRef.get();
			if (graphDef == null)
				throw new ServerException(Status.NOT_FOUND, "Graph not found: " + graphName);
			return graphDef;
		} catch (Exception e) {
			throw ServerException.getJsonException(logger, e);
		}
	}

	@Override
	public Set<String> createUpdateNodes(String db_name, LinkedHashMap<String, GraphNode> nodes, Boolean upsert) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long createUpdateNodes(String db_name, Boolean upsert, InputStream inpustStream) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphNode createUpdateNode(String db_name, String node_id, GraphNode node, Boolean upsert) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphNode getNode(String db_name, String node_id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphNode deleteNode(String db_name, String node_id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphNode createEdge(String db_name, String node_id, String edge_type, String to_node_id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphNode deleteEdge(String db_name, String node_id, String edge_type, String to_node_id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<GraphNodeResult> requestNodes(String db_name, GraphRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected GraphSingleClient newClient(RemoteService remote) {
		return new GraphSingleClient(remote);
	}

}
