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

import com.fasterxml.jackson.core.type.TypeReference;
import com.qwazr.utils.UBuilder;
import com.qwazr.utils.http.HttpRequest;
import com.qwazr.utils.json.client.JsonClientAbstract;
import com.qwazr.utils.server.RemoteService;
import com.qwazr.utils.server.ServiceInterface;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.TreeSet;

public class StoreSingleClient extends JsonClientAbstract implements StoreServiceInterface {

	private final static String PATH = "/" + StoreServiceInterface.SERVICE_NAME;
	private final static String PATH_SLASH = PATH + "/";

	public StoreSingleClient(final RemoteService remote) throws URISyntaxException {
		super(remote);
	}

	@Override
	public StoreFileResult getDirectory(final String schemaName, final String path) {
		final UBuilder uBuilder = RemoteService.getNewUBuilder(remote, PATH_SLASH, schemaName, "/", path);
		final HttpRequest request =
				HttpRequest.Get(uBuilder.buildNoEx()).addHeader("Accept", ServiceInterface.APPLICATION_JSON_UTF8);
		return executeJson(request, null, null, StoreFileResult.class, valid200Json);
	}

	@Override
	public StreamingOutput getFile(final String schemaName, final String path) {
		final UBuilder uBuilder = RemoteService.getNewUBuilder(remote, PATH_SLASH, schemaName, "/", path);
		final HttpRequest request = HttpRequest.Get(uBuilder.buildNoEx());
		request.addHeader("Accept", MediaType.APPLICATION_OCTET_STREAM);
		return executeStream(request, null, null, valid200Stream);
	}

	@Override
	public Response headFile(final String schemaName, final String path) {
		final UBuilder uBuilder = RemoteService.getNewUBuilder(remote, PATH_SLASH, schemaName, "/", path);
		final URI uri = uBuilder.buildNoEx();
		final HttpRequest request = HttpRequest.Head(uri);
		final HttpResponse response = execute(request, null, null, valid200);
		final ResponseBuilder builder = Response.ok();
		StoreFileResult.buildHeaders(response, uri, builder);
		return builder.build();
	}

	@Override
	final public StoreFileResult getDirectory(final String schemaName) {
		return getDirectory(schemaName, StringUtils.EMPTY);
	}

	@Override
	final public StreamingOutput getFile(final String schemaName) {
		return getFile(schemaName, StringUtils.EMPTY);
	}

	@Override
	final public Response headFile(final String schemaName) {
		return headFile(schemaName, StringUtils.EMPTY);
	}

	@Override
	final public Response putFile(final String schemaName, final String path, final InputStream inputStream,
			final Long lastModified) {
		final UBuilder uBuilder = RemoteService.getNewUBuilder(remote, PATH_SLASH, schemaName, "/", path);
		uBuilder.setParameterObject("last_modified", lastModified);
		final HttpRequest request = HttpRequest.Put(uBuilder.buildNoEx());
		final HttpResponse response = execute(request, inputStream, null, valid200);
		final ResponseBuilder builder = Response.ok("File created: " + path, MediaType.TEXT_PLAIN);
		StoreFileResult.buildHeaders(response, null, builder);
		return builder.build();
	}

	@Override
	final public Response deleteFile(final String schemaName, final String path) {
		final UBuilder uBuilder = RemoteService.getNewUBuilder(remote, PATH_SLASH, schemaName, "/", path);
		final HttpRequest request = HttpRequest.Delete(uBuilder.buildNoEx());
		final HttpResponse response = execute(request, null, null, valid200);
		final ResponseBuilder builder = Response.ok(response.getStatusLine().getReasonPhrase(), MediaType.TEXT_PLAIN);
		StoreFileResult.buildHeaders(response, null, builder);
		return builder.build();
	}

	@Override
	final public Response createSchema(final String schemaName) {
		final UBuilder uBuilder = RemoteService.getNewUBuilder(remote, PATH_SLASH, schemaName);
		final HttpRequest request = HttpRequest.Post(uBuilder.buildNoEx());
		execute(request, null, null, valid200);
		final ResponseBuilder builder = Response.ok("Schema created: " + schemaName, MediaType.TEXT_PLAIN);
		return builder.build();
	}

	@Override
	final public Response deleteSchema(final String schemaName) {
		final UBuilder uBuilder = RemoteService.getNewUBuilder(remote, PATH_SLASH, schemaName);
		final HttpRequest request = HttpRequest.Delete(uBuilder.buildNoEx());
		execute(request, null, null, valid200);
		final ResponseBuilder builder = Response.ok("Schema deleted: " + schemaName, MediaType.TEXT_PLAIN);
		return builder.build();
	}

	public final static TypeReference<TreeSet<String>> SetStringTypeRef = new TypeReference<TreeSet<String>>() {
	};

	@Override
	final public Set<String> getSchemas() {
		final UBuilder uBuilder = RemoteService.getNewUBuilder(remote, PATH);
		final HttpRequest request = HttpRequest.Get(uBuilder.buildNoEx());
		return executeJson(request, null, null, SetStringTypeRef, valid200Json);
	}
}
