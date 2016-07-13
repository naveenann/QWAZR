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

import com.fasterxml.jackson.core.type.TypeReference;
import com.qwazr.graph.model.*;
import com.qwazr.utils.UBuilder;
import com.qwazr.utils.http.HttpRequest;
import com.qwazr.utils.json.client.JsonClientAbstract;
import com.qwazr.utils.server.RemoteService;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class GraphSingleClient extends JsonClientAbstract implements GraphServiceInterface {

	private final static String GRAPH_PREFIX = "/graph/";

	GraphSingleClient(RemoteService remote) {
		super(remote);
	}

	public final static TypeReference<TreeSet<String>> SetStringTypeRef = new TypeReference<TreeSet<String>>() {
	};

	@Override
	public Set<String> list() {
		final UBuilder uBuilder = RemoteService.getNewUBuilder(remote, GRAPH_PREFIX);
		final HttpRequest request = HttpRequest.Get(uBuilder.buildNoEx());
		return commonServiceRequest(request, null, null, SetStringTypeRef, 200);
	}

	@Override
	public GraphResult createUpdateGraph(final String graphName, final GraphDefinition graphDef) {
		final UBuilder uBuilder = RemoteService.getNewUBuilder(remote, GRAPH_PREFIX, graphName);
		final HttpRequest request = HttpRequest.Post(uBuilder.buildNoEx());
		return commonServiceRequest(request, graphDef, null, GraphResult.class, 200);
	}

	@Override
	public GraphResult getGraph(final String graphName) {
		final UBuilder uBuilder = RemoteService.getNewUBuilder(remote, GRAPH_PREFIX, graphName);
		final HttpRequest request = HttpRequest.Get(uBuilder.buildNoEx());
		return commonServiceRequest(request, null, null, GraphResult.class, 200);
	}

	@Override
	public GraphResult deleteGraph(final String graphName) {
		final UBuilder uBuilder = RemoteService.getNewUBuilder(remote, GRAPH_PREFIX, graphName);
		final HttpRequest request = HttpRequest.Delete(uBuilder.buildNoEx());
		return commonServiceRequest(request, null, null, GraphResult.class, 200);
	}

	@Override
	public Set<String> createUpdateNodes(final String db_name, final LinkedHashMap<String, GraphNode> nodes,
			final Boolean upsert) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long createUpdateNodes(final String db_name, final Boolean upsert, final InputStream inpustStream) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphNode createUpdateNode(final String db_name, final String node_id, final GraphNode node,
			final Boolean upsert) {
		// TODO Auto-generated method stub

		return null;
	}

	@Override
	public GraphNode getNode(final String db_name, final String node_id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphNode deleteNode(final String db_name, final String node_id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphNode createEdge(final String db_name, final String node_id, final String edge_type,
			final String to_node_id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphNode deleteEdge(final String db_name, final String node_id, final String edge_type,
			final String to_node_id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<GraphNodeResult> requestNodes(final String db_name, final GraphRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

}
