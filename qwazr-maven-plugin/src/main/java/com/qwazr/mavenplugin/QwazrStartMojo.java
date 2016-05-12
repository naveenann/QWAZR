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

import com.qwazr.Qwazr;
import com.qwazr.QwazrConfiguration;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.server.ServerConfiguration;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Starts a QWAZR application
 */
@Mojo(name = "start", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class QwazrStartMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;

	/**
	 * The location of the directory which contains the application. The current directory by default.
	 */
	@Parameter(property = "qwazrData")
	private String dataDirectory;

	/**
	 * The local address the server will bind to for TCP connections (HTTP and WebService).
	 */
	@Parameter(property = "qwazr.listenAddr")
	private String listenAddr;

	/**
	 * The public address the server can be reach with.
	 */
	@Parameter(property = "qwazr.publicAddr")
	private String publicAddr;


	/**
	 * The local address the server will bind to for UDP connections.
	 */
	@Parameter(property = "qwazr.udpAddress")
	private String udpAddress;

	/**
	 * The port the server will bind to for HTTP connections. Default is 9090.
	 */
	@Parameter(property = "qwazr.webappPort")
	private Integer webappPort;

	/**
	 * The port the server will bind to for REST web service connections. Default is 9091.
	 */
	@Parameter(property = "qwazr.webservicePort")
	private Integer webservicePort;

	/**
	 * The port the server will bind to for UDP connections. Default is 9091.
	 */
	@Parameter(property = "qwazr.udpPort")
	private Integer udpPort;

	/**
	 * The library item which will handle the Basic authentication for the HTTP connections.
	 */
	@Parameter(property = "qwazr.webappRealm")
	private String webappRealm;

	/**
	 * The library item which will handle the Basic authentication for the REST web service connections.
	 */
	@Parameter(property = "qwazr.webserviceRealm")
	private String webserviceRealm;

	/**
	 * A list of directories which contains the configuration files.
	 */
	@Parameter(property = "qwazr.etcDirs")
	private List<String> etcDirectories;

	/**
	 * A list of wildcard filters applied to the configuration files.
	 */
	@Parameter(property = "qwazr.etcFilters")
	private List<String> etcFilters;

	/**
	 * A list of services to activate.
	 */
	@Parameter(property = "qwazr.services")
	private List<QwazrConfiguration.ServiceEnum> services;

	/**
	 * The size of the thread pool used by the scheduler.
	 */
	@Parameter(property = "qwazr.schedulerMaxThreads")
	private Integer schedulerMaxThreads;

	/**
	 * The groups the application will be registered in.
	 */
	@Parameter(property = "qwazr.groups")
	private List<String> groups;

	/**
	 * Pass true to start the QWAZR application as a daemon.
	 */
	@Parameter(property = "qwazr.daemon")
	private Boolean daemon;

	public void execute() throws MojoExecutionException, MojoFailureException {
		final Log log = getLog();
		log.info("Starting QWAZR");
		final Launcher launcher = new Launcher();

		try {
			if (daemon == null || !daemon)
				launcher.startEmbedded(log);
			else
				launcher.startAsDaemon(log);
		} catch (Exception e) {
			throw new MojoFailureException("Cannot start QWAZR", e);
		}
	}

	private class Launcher {

		private final Map<String, String> parameters = new HashMap<>();

		private Launcher() {
			setParameter(ServerConfiguration.VariablesEnum.QWAZR_DATA, dataDirectory);
			setParameter(ServerConfiguration.VariablesEnum.LISTEN_ADDR, listenAddr);
			setParameter(ServerConfiguration.VariablesEnum.PUBLIC_ADDR, publicAddr);
			if (etcDirectories != null && !etcDirectories.isEmpty())
				setParameter(ServerConfiguration.VariablesEnum.QWAZR_ETC_DIR,
						StringUtils.join(etcDirectories, File.pathSeparatorChar));
			setParameter("WEBAPP_PORT", webappPort);
			setParameter("WEBSERVICE_PORT", webservicePort);
			setParameter("WEBAPP_REALM", webappRealm);
			setParameter("WEBSERVICE_REALM", webserviceRealm);
			setParameter("UDP_ADDRESS", udpAddress);
			setParameter("UDP_PORT", udpPort);
		}

		private void setParameter(Enum<?> key, Object value) {
			setParameter(key.name(), value);
		}

		private void setParameter(String key, Object value) {
			if (value == null)
				return;
			String str = value.toString();
			if (StringUtils.isEmpty(str))
				return;
			System.setProperty(key, str);
			parameters.put(key, str);
		}

		private String buildClassPass() throws DependencyResolutionRequiredException {
			final StringBuilder sb = new StringBuilder();

			// Build the runtime classpath
			final List<String> classPathList = project.getRuntimeClasspathElements();
			classPathList.forEach(path -> {
				sb.append(path);
				sb.append(File.pathSeparatorChar);
			});

			// Build the artifacts classpath
			final Set<Artifact> artifacts = project.getArtifacts();
			if (artifacts != null)
				artifacts.forEach(artifact -> {
					sb.append(artifact.getFile().getPath());
					sb.append(File.pathSeparatorChar);
				});
			return sb.toString();
		}

		private void startAsDaemon(final Log log)
				throws MojoFailureException, IOException, InterruptedException, DependencyResolutionRequiredException {

			final File javaBinFile = new File(System.getProperty("java.home"),
					File.separator + "bin" + File.separator + (SystemUtils.IS_OS_WINDOWS ? "java.exe" : "java"));
			if (!javaBinFile.exists())
				throw new MojoFailureException("Cannot find JAVA: " + javaBinFile);

			final String classpath = buildClassPass();
			parameters.put("CLASSPATH", classpath);

			if (etcFilters != null && !etcFilters.isEmpty())
				parameters
						.put(ServerConfiguration.VariablesEnum.QWAZR_ETC.name(), StringUtils.join(etcFilters, ","));

			if (groups != null && !groups.isEmpty())
				parameters.put(QwazrConfiguration.VariablesEnum.QWAZR_GROUPS.name(), StringUtils.join(groups, ","));

			if (services != null && !services.isEmpty())
				parameters.put(QwazrConfiguration.VariablesEnum.QWAZR_SERVICES.name(), StringUtils.join(services, ","));

			final String className = Qwazr.class.getCanonicalName();
			final ProcessBuilder builder =
					new ProcessBuilder(javaBinFile.getCanonicalPath(), "-Dfile.encoding=UTF-8", className);

			builder.environment().putAll(parameters);
			builder.inheritIO();
			Process process = builder.start();
			log.info("QWAZR started (Daemon)");
			process.waitFor(5000, TimeUnit.MILLISECONDS);
		}

		private void startEmbedded(final Log log)
				throws ParseException, InstantiationException, IllegalAccessException, ServletException, IOException {
			Qwazr.start(new QwazrConfiguration(etcFilters, services, groups, schedulerMaxThreads));
			log.info("QWAZR started (Embedded)");
			try {
				for (; ; )
					Thread.sleep(30000);
			} catch (InterruptedException e) {
				log.info("QWAZR interrupted");
			}
			log.info("Stopping QWAZR");
			Qwazr.stop();
		}
	}
}
