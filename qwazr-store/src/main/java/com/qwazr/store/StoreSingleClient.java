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
import com.qwazr.utils.http.HttpResponseEntityException;
import com.qwazr.utils.http.HttpUtils;
import com.qwazr.utils.json.client.JsonClientAbstract;
import com.qwazr.utils.server.RemoteService;
import com.qwazr.utils.server.ServiceInterface;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
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
	public StoreFileResult getDirectory(String schemaName, String path) {
		UBuilder uBuilder = new UBuilder(PATH_SLASH, schemaName, "/", path);
		Request request = Request.Get(uBuilder.build()).addHeader("Accept", ServiceInterface.APPLICATION_JSON_UTF8);
		return commonServiceRequest(request, null, null, StoreFileResult.class, 200);
	}

	@Override
	public Response getFile(String schemaName, String path) {
		try {
			UBuilder uBuilder = new UBuilder(PATH_SLASH, schemaName, "/", path);
			Request request = Request.Get(uBuilder.build()).addHeader("Accept", MediaType.APPLICATION_OCTET_STREAM);
			HttpResponse response = execute(request, null, null);
			HttpUtils.checkStatusCodes(response, 200);
			ResponseBuilder builder = Response.ok();
			StoreFileResult.buildHeaders(response, null, builder);
			builder.type(response.getEntity().getContentType().getValue());
			builder.entity(response.getEntity().getContent());
			return builder.build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e, Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Response headFile(String schemaName, String path) {
		try {
			UBuilder uBuilder = new UBuilder(PATH_SLASH, schemaName, "/", path);
			URI uri = uBuilder.build();
			Request request = Request.Head(uri);
			HttpResponse response = execute(request, null, null);
			HttpUtils.checkStatusCodes(response, 200);
			ResponseBuilder builder = Response.ok();
			StoreFileResult.buildHeaders(response, uri, builder);
			return builder.build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e, Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	final public StoreFileResult getDirectory(String schemaName) {
		return getDirectory(schemaName, StringUtils.EMPTY);
	}

	@Override
	final public Response getFile(final String schemaName) {
		return getFile(schemaName, StringUtils.EMPTY);
	}

	@Override
	final public Response headFile(final String schemaName) {
		return headFile(schemaName, StringUtils.EMPTY);
	}

	@Override
	final public Response putFile(final String schemaName, final String path, final InputStream inputStream,
			final Long lastModified) {
		try {
			UBuilder uBuilder = new UBuilder(PATH_SLASH, schemaName, "/", path);
			uBuilder.setParameterObject("last_modified", lastModified);
			Request request = Request.Put(uBuilder.build());
			HttpResponse response = execute(request, inputStream, null);
			HttpUtils.checkStatusCodes(response, 200);
			ResponseBuilder builder = Response.ok("File created: " + path, MediaType.TEXT_PLAIN);
			StoreFileResult.buildHeaders(response, null, builder);
			return builder.build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e, Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	final public Response deleteFile(final String schemaName, final String path) {
		try {
			UBuilder uBuilder = new UBuilder(PATH_SLASH, schemaName, "/", path);
			Request request = Request.Delete(uBuilder.build());
			HttpResponse response = execute(request, null, null);
			HttpUtils.checkStatusCodes(response, 200);
			ResponseBuilder builder = Response.ok(response.getStatusLine().getReasonPhrase(), MediaType.TEXT_PLAIN);
			StoreFileResult.buildHeaders(response, null, builder);
			return builder.build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e, Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	final public Response createSchema(final String schemaName) {
		try {
			UBuilder uBuilder = new UBuilder(PATH_SLASH, schemaName);
			Request request = Request.Post(uBuilder.build());
			HttpResponse response = execute(request, null, null);
			HttpUtils.checkStatusCodes(response, 200);
			ResponseBuilder builder = Response.ok("Schema created: " + schemaName, MediaType.TEXT_PLAIN);
			return builder.build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e, Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	final public Response deleteSchema(final String schemaName) {
		try {
			UBuilder uBuilder = new UBuilder(PATH_SLASH, schemaName);
			Request request = Request.Delete(uBuilder.build());
			HttpResponse response = execute(request, null, null);
			HttpUtils.checkStatusCodes(response, 200);
			ResponseBuilder builder = Response.ok("Schema deleted: " + schemaName, MediaType.TEXT_PLAIN);
			return builder.build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e, Status.INTERNAL_SERVER_ERROR);
		}
	}

	public final static TypeReference<TreeSet<String>> SetStringTypeRef = new TypeReference<TreeSet<String>>() {
	};

	@Override
	final public Set<String> getSchemas() {
		UBuilder uBuilder = new UBuilder(PATH);
		Request request = Request.Get(uBuilder.build());
		return commonServiceRequest(request, null, null, SetStringTypeRef, 200);
	}
}
