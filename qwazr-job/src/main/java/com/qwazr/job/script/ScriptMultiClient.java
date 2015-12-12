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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.utils.json.client.JsonMultiClientAbstract;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.threads.ThreadUtils;
import com.qwazr.utils.threads.ThreadUtils.ProcedureExceptionCatcher;

public class ScriptMultiClient extends
		JsonMultiClientAbstract<String, ScriptSingleClient> implements
		ScriptServiceInterface {

	private static final Logger logger = LoggerFactory
			.getLogger(ScriptMultiClient.class);

	public ScriptMultiClient(ExecutorService executor, String[] urls,
			Integer msTimeout) throws URISyntaxException {
		super(executor, new ScriptSingleClient[urls.length], urls, msTimeout);
	}

	@Override
	protected ScriptSingleClient newClient(String url, Integer msTimeOut)
			throws URISyntaxException {
		return new ScriptSingleClient(url, msTimeOut);
	}

	@Override
	public ScriptRunStatus runScript(String scriptPath) {
		WebAppExceptionHolder exceptionHolder = new WebAppExceptionHolder(
				logger);
		for (ScriptSingleClient client : this) {
			try {
				return client.runScript(scriptPath);
			} catch (WebApplicationException e) {
				exceptionHolder.switchAndWarn(e);
			}
		}
		if (exceptionHolder.getException() != null)
			throw exceptionHolder.getException();
		return null;
	}

	@Override
	public ScriptRunStatus runScriptVariables(String scriptPath,
			Map<String, String> variables) {
		WebAppExceptionHolder exceptionHolder = new WebAppExceptionHolder(
				logger);
		for (ScriptSingleClient client : this) {
			try {
				return client.runScriptVariables(scriptPath, variables);
			} catch (WebApplicationException e) {
				exceptionHolder.switchAndWarn(e);
			}
		}
		if (exceptionHolder.getException() != null)
			throw exceptionHolder.getException();
		return null;
	}

	public ScriptRunStatus runScript(String scriptPath, String... variables) {
		if (variables == null || variables.length == 0)
			return runScript(scriptPath);
		HashMap<String, String> variablesMap = new HashMap<String, String>();
		int l = variables.length / 2;
		for (int i = 0; i < l; i++)
			variablesMap.put(variables[i * 2], variables[i * 2 + 1]);
		return runScriptVariables(scriptPath, variablesMap);
	}

	@Override
	public Map<String, ScriptRunStatus> getRunsStatus(Boolean local,
			Integer msTimeout) {
		if (local != null && local)
			return getClientByUrl(ClusterManager.INSTANCE.myAddress)
					.getRunsStatus(true, msTimeout);
		TreeMap<String, ScriptRunStatus> results = new TreeMap<String, ScriptRunStatus>();
		for (ScriptSingleClient client : this) {
			try {
				results.putAll(client.getRunsStatus(true, msTimeout));
			} catch (WebApplicationException e) {
				if (e.getResponse().getStatus() != 404)
					throw e;
			}
		}
		return results;
	}

	@Override
	public ScriptRunStatus getRunStatus(String run_id) {
		for (ScriptSingleClient client : this) {
			try {
				return client.getRunStatus(run_id);
			} catch (WebApplicationException e) {
				throw e;
			}
		}
		return null;
	}

	@Override
	public String getRunOut(String run_id) {
		for (ScriptSingleClient client : this) {
			try {
				return client.getRunOut(run_id);
			} catch (WebApplicationException e) {
				throw e;
			}
		}
		return null;
	}

	@Override
	public String getRunErr(String run_id) {
		for (ScriptSingleClient client : this) {
			try {
				return client.getRunErr(run_id);
			} catch (WebApplicationException e) {
				throw e;
			}
		}
		return null;
	}

	@Override
	public Set<String> getSemaphores(Boolean local, Integer msTimeout) {

		try {

			final TreeSet<String> semaphores = new TreeSet<String>();
			List<ProcedureExceptionCatcher> threads = new ArrayList<>(size());
			for (ScriptServiceInterface client : this) {
				threads.add(new ProcedureExceptionCatcher() {
					@Override
					public void execute() throws Exception {
						try {
							synchronized (this) {
								semaphores.addAll(client.getSemaphores(true,
										msTimeout));
							}
						} catch (WebApplicationException e) {
							switch (e.getResponse().getStatus()) {
							case 404:
								break;
							default:
								throw e;
							}
						}
					}
				});
			}
			ThreadUtils.invokeAndJoin(executor, threads);
			return semaphores;

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	public Set<String> getSemaphoreOwners(String semaphore_id) {
		return getSemaphoreOwners(semaphore_id, null, null);
	}

	@Override
	public Set<String> getSemaphoreOwners(String semaphore_id, Boolean local,
			Integer msTimeout) {

		try {

			final TreeSet<String> ownerSet = new TreeSet<String>();
			List<ProcedureExceptionCatcher> threads = new ArrayList<>(size());
			for (ScriptServiceInterface client : this) {
				threads.add(new ProcedureExceptionCatcher() {
					@Override
					public void execute() throws Exception {
						try {
							synchronized (this) {
								ownerSet.addAll(client.getSemaphoreOwners(
										semaphore_id, true, msTimeout));
							}
						} catch (WebApplicationException e) {
							switch (e.getResponse().getStatus()) {
							case 404:
								break;
							default:
								throw e;
							}
						}
					}
				});
			}
			ThreadUtils.invokeAndJoin(executor, threads);
			return ownerSet;

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

}