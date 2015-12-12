/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.job.script;

import com.qwazr.job.JobServer;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Set;

@RolesAllowed(JobServer.SERVICE_NAME_SCRIPT)
@Path("/scripts")
public interface ScriptServiceInterface {

	@GET
	@Path("/run/{script_path : .+}")
	@Produces(MediaType.APPLICATION_JSON)
	public ScriptRunStatus runScript(@PathParam("script_path") String scriptPath);

	@POST
	@Path("/run/{script_path : .+}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ScriptRunStatus runScriptVariables(
			@PathParam("script_path") String scriptPath,
			Map<String, String> variables);

	@GET
	@Path("/status")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, ScriptRunStatus> getRunsStatus(
			@QueryParam("local") Boolean local,
			@QueryParam("timeout") Integer msTimeout);

	@GET
	@Path("/status/{run_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public ScriptRunStatus getRunStatus(@PathParam("run_id") String run_id);

	@GET
	@Path("/status/{run_id}/out")
	@Produces(MediaType.TEXT_PLAIN)
	public String getRunOut(@PathParam("run_id") String run_id);

	@GET
	@Path("/status/{run_id}/err")
	@Produces(MediaType.TEXT_PLAIN)
	public String getRunErr(@PathParam("run_id") String run_id);

	@GET
	@Path("/semaphores")
	@Produces(MediaType.APPLICATION_JSON)
	public Set<String> getSemaphores(@QueryParam("local") Boolean local,
			@QueryParam("timeout") Integer msTimeout);

	@GET
	@Path("/semaphores/{semaphore_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Set<String> getSemaphoreOwners(
			@PathParam("semaphore_id") String semaphore_id,
			@QueryParam("local") Boolean local,
			@QueryParam("timeout") Integer msTimeout);
}