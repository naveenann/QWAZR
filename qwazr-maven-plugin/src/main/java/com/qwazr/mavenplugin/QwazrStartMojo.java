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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
	 * The local address the server will bind to for UDP connections (may be a multicast group)
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
	 * A list of masters.
	 */
	@Parameter(property = "qwazr.masters")
	private List<String> masters;

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
	 * The profiled classes (wildcards are allowed).
	 */
	@Parameter(property = "qwazr.profilers")
	private List<String> profilers;

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
			setParameter("QWAZR_DATA", dataDirectory);
			setParameter("LISTEN_ADDR", listenAddr);
			setParameter("PUBLIC_ADDR", publicAddr);
			if (etcDirectories != null && !etcDirectories.isEmpty())
				setParameter("QWAZR_ETC_DIR", StringUtils.join(etcDirectories, File.pathSeparatorChar));
			setParameter("WEBAPP_PORT", webappPort);
			setParameter("WEBSERVICE_PORT", webservicePort);
			setParameter("WEBAPP_REALM", webappRealm);
			setParameter("WEBSERVICE_REALM", webserviceRealm);
			setParameter("UDP_ADDRESS", udpAddress);
			setParameter("UDP_PORT", udpPort);

			if (etcFilters != null && !etcFilters.isEmpty())
				setParameter("QWAZR_ETC", StringUtils.join(etcFilters, ','));

			if (groups != null && !groups.isEmpty())
				setParameter("QWAZR_GROUPS", StringUtils.join(groups, ','));

			if (profilers != null && !profilers.isEmpty())
				setParameter("QWAZR_PROFILERS", StringUtils.join(profilers, ';'));

			if (services != null && !services.isEmpty())
				setParameter("QWAZR_SERVICES", StringUtils.join(services, ','));
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

		private String buildClassPath() throws DependencyResolutionRequiredException {
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
					final File artifactFile = artifact.getFile();
					sb.append(artifactFile.getPath());
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

			final String classpath = buildClassPath();
			parameters.put("CLASSPATH", classpath);

			final String className = Qwazr.class.getCanonicalName();
			final ProcessBuilder builder =
					new ProcessBuilder(javaBinFile.getCanonicalPath(), "-Dfile.encoding=UTF-8", className);

			builder.environment().putAll(parameters);
			builder.inheritIO();
			Process process = builder.start();
			log.info("QWAZR started (Daemon)");
			process.waitFor(5000, TimeUnit.MILLISECONDS);
		}

		private void startEmbedded(final Log log) throws Exception {
			final String oldClassPath = System.getProperty("java.class.path");
			try {
				final String classpath = buildClassPath();
				if (classpath != null && !classpath.isEmpty())
					System.setProperty("java.class.path", classpath);

				Qwazr.startWithConf(new QwazrConfiguration(parameters));
				log.info("QWAZR started (Embedded)");
				try {
					for (; ; )
						Thread.sleep(30000);
				} catch (InterruptedException e) {
					log.info("QWAZR interrupted");
				}
				log.info("Stopping QWAZR");
				Qwazr.stop(null);
			} finally {
				if (oldClassPath != null)
					System.setProperty("java.class.path", oldClassPath);
			}
		}
	}
}
