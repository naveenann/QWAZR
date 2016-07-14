/**
 * Copyright 2016 Emmanuel Keller / QWAZR
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
package com.qwazr.mavenplugin;

import com.qwazr.utils.IOUtils;
import com.qwazr.utils.http.HttpClients;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Stops a QWAZR application
 */
@Mojo(name = "stop")
public class QwazrStopMojo extends AbstractMojo {

	/**
	 * The public address of the QWAZR application. Default value: localhost.
	 */
	@Parameter(property = "qwazr.publicAddr")
	private String publicAddr;

	/**
	 * The port of the REST Web service. Default value: 9091.
	 */
	@Parameter(property = "qwazr.webservicePort")
	private Integer webservicePort;

	/**
	 * The time out in milliseconds. Default value: 5000ms.
	 */
	@Parameter(property = "qwazr.waitMs")
	private Integer waitMs;

	/**
	 * Be fault tolerant, or generate an error if some wrong happens. Default value: true.
	 */
	@Parameter(property = "qwazr.faultTolerant")
	private Boolean faultTolerant;

	static String getPropertyOrEnv(String currentValue, String env, String defaultValue) {
		if (currentValue != null)
			return currentValue;
		if (env == null)
			return defaultValue;
		currentValue = System.getProperty(env);
		if (currentValue != null)
			return currentValue;
		currentValue = System.getenv().get(env);
		return currentValue == null ? defaultValue : currentValue;
	}

	static Integer getPropertyOrEnv(Integer currentValue, String env, Integer defaultValue) {
		if (currentValue != null)
			return currentValue;
		if (env == null)
			return defaultValue;
		String value = System.getProperty(env);
		if (value != null)
			return Integer.parseInt(value);
		value = System.getenv().get(env);
		return value == null ? defaultValue : Integer.parseInt(value);
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		final Log log = getLog();
		log.info("Stopping QWAZR");

		publicAddr = getPropertyOrEnv(publicAddr, "PUBLIC_ADDR", "localhost");
		webservicePort = getPropertyOrEnv(webservicePort, "WEBSERVICE_PORT", 9091);
		waitMs = getPropertyOrEnv(waitMs, null, 5000);

		CloseableHttpResponse response = null;
		try {
			URI uri = new URI("http", null, publicAddr, webservicePort, "/shutdown", null, null);
			log.info("Post HTTP Delete on: " + uri);
			response = HttpClients.UNSECURE_HTTP_CLIENT.execute(new HttpDelete(uri));
			log.info("HTTP Status Code: " + response.getStatusLine().getStatusCode());
		} catch (IOException | URISyntaxException e) {
			if (faultTolerant == null || faultTolerant)
				log.warn(e);
			else
				throw new MojoExecutionException(e.getMessage(), e);
		} finally {
			IOUtils.close(response);
		}
		try {
			Thread.sleep(waitMs);
		} catch (InterruptedException e) {
			log.warn(e);
		}
	}
}
