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

import com.qwazr.utils.server.RemoteService;
import com.qwazr.utils.server.ServiceInterface;
import com.qwazr.utils.server.ServiceName;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Set;

@RolesAllowed(StoreServiceInterface.SERVICE_NAME)
@ServiceName(StoreServiceInterface.SERVICE_NAME)
@Path("/" + StoreServiceInterface.SERVICE_NAME)
public interface StoreServiceInterface extends ServiceInterface {

	String SERVICE_NAME = "store";

	@GET
	@Path("/{schema_name}/{path : .+}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	Response getFile(@PathParam("schema_name") String schemaName, @PathParam("path") String path);

	@GET
	@Path("/{schema_name}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	Response getFile(@PathParam("schema_name") String schemaName);

	@GET
	@Path("/{schema_name}/{path : .+}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	StoreFileResult getDirectory(@PathParam("schema_name") String schemaName, @PathParam("path") String path);

	@GET
	@Path("/{schema_name}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	StoreFileResult getDirectory(@PathParam("schema_name") String schemaName);

	@HEAD
	@Path("/{schema_name}/{path : .+}")
	Response headFile(@PathParam("schema_name") String schemaName, @PathParam("path") String path);

	@HEAD
	@Path("/{schema_name}")
	Response headFile(@PathParam("schema_name") String schemaName);

	@PUT
	@Path("/{schema_name}/{path : .+}")
	@Produces(MediaType.TEXT_PLAIN)
	Response putFile(@PathParam("schema_name") String schemaName, @PathParam("path") String path,
			InputStream inputStream, @QueryParam("last_modified") Long lastModified);

	@DELETE
	@Path("/{schema_name}/{path : .+}")
	@Produces(MediaType.TEXT_PLAIN)
	Response deleteFile(@PathParam("schema_name") String schemaName, @PathParam("path") String path);

	@POST
	@Path("/{schema_name}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	Response createSchema(@PathParam("schema_name") String schemaName);

	@DELETE
	@Path("/{schema_name}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	Response deleteSchema(@PathParam("schema_name") String schemaName);

	@GET
	@Path("/")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	Set<String> getSchemas();

	static StoreServiceInterface getClient(final RemoteService... remote) throws URISyntaxException {
		if (remote == null || remote.length == 0)
			return StoreServiceImpl.getInstance();
		if (remote.length == 1)
			return new StoreSingleClient(remote[0]);
		throw new NotSupportedException();
	}
}
