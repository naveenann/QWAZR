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
 **/
package com.qwazr;

import com.qwazr.ServerConfiguration.ServiceEnum;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.cluster.service.ClusterNodeJson;
import com.qwazr.cluster.service.ClusterServiceImpl;
import com.qwazr.compiler.CompilerManager;
import com.qwazr.crawler.web.manager.WebCrawlerManager;
import com.qwazr.database.TableManager;
import com.qwazr.extractor.ParserManager;
import com.qwazr.graph.GraphManager;
import com.qwazr.library.AbstractLibrary;
import com.qwazr.library.LibraryManager;
import com.qwazr.library.LibraryServiceImpl;
import com.qwazr.scheduler.SchedulerManager;
import com.qwazr.scripts.ScriptManager;
import com.qwazr.search.index.IndexManager;
import com.qwazr.semaphores.SemaphoresManager;
import com.qwazr.utils.file.TrackedDirectory;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.ServiceInterface;
import com.qwazr.utils.server.ServiceName;
import com.qwazr.utils.server.ServletApplication;
import com.qwazr.webapps.transaction.WebappManager;
import io.undertow.security.idm.IdentityManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.exec.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;

public class Qwazr extends AbstractServer {

	static final Logger logger = LoggerFactory.getLogger(Qwazr.class);

	private final static ServerDefinition serverDefinition = new ServerDefinition();

	static {
		serverDefinition.defaultWebApplicationTcpPort = 9090;
		serverDefinition.defaultWebServiceTcpPort = 9091;
		serverDefinition.mainJarPath = "qwazr.jar";
		serverDefinition.defaultDataDirName = "qwazr";
	}

	private final Collection<Class<? extends ServiceInterface>> services = new ArrayList<>();

	private ServerConfiguration serverConfiguration = null;

	private Qwazr() {
		super(serverDefinition, Executors.newCachedThreadPool());
	}

	@Path("/")
	@ServiceName("welcome")
	public class WelcomeServiceImpl implements ServiceInterface {

		@GET
		@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
		public WelcomeStatus welcome() {
			return new WelcomeStatus(services);
		}

	}

	public final static Option THREADS_OPTION = new Option("t", "maxthreads", true, "The maximum of threads");

	@Override
	public void defineOptions(Options options) {
		super.defineOptions(options);
		options.addOption(THREADS_OPTION);
	}

	@Override
	public void commandLine(CommandLine cmd) throws IOException {
		// Load the configuration
		serverConfiguration = new ServerConfiguration();
	}

	@Override
	public ServletApplication load(Collection<Class<? extends ServiceInterface>> classes) throws IOException {

		final ServletApplication servletApplication;

		File currentDataDir = getCurrentDataDir();
		File currentTempDir = new File(currentDataDir, "tmp");
		File currentEtcDir = new File(currentDataDir, "etc");
		TrackedDirectory etcTracker = new TrackedDirectory(currentEtcDir, serverConfiguration.etcFileFilter);

		ClusterManager.load(executorService, getWebServicePublicAddress(), serverConfiguration.groups);

		if (ServiceEnum.compiler.isActive(serverConfiguration))
			services.add(CompilerManager.load(executorService, currentDataDir));

		services.add(WelcomeServiceImpl.class);
		services.add(ClusterServiceImpl.class);

		if (ServiceEnum.extractor.isActive(serverConfiguration))
			services.add(ParserManager.load());

		if (ServiceEnum.webapps.isActive(serverConfiguration)) {
			services.add(WebappManager.load(currentDataDir, etcTracker, currentTempDir));
			servletApplication = WebappManager.getInstance().getServletApplication();
		} else
			servletApplication = null;

		if (ServiceEnum.semaphores.isActive(serverConfiguration))
			services.add(SemaphoresManager.load(executorService));

		if (ServiceEnum.scripts.isActive(serverConfiguration))
			services.add(ScriptManager.load(executorService, currentDataDir));

		if (ServiceEnum.webcrawler.isActive(serverConfiguration))
			services.add(WebCrawlerManager.load(executorService));

		if (ServiceEnum.search.isActive(serverConfiguration))
			services.add(IndexManager.load(executorService, currentDataDir));

		if (ServiceEnum.graph.isActive(serverConfiguration))
			services.add(GraphManager.load(executorService, currentDataDir));

		if (ServiceEnum.table.isActive(serverConfiguration))
			services.add(TableManager.load(executorService, currentDataDir));

		LibraryManager.load(currentDataDir, etcTracker);
		services.add(LibraryServiceImpl.class);

		// Scheduler is last, because it may immediatly execute a scripts
		if (ServiceEnum.schedulers.isActive(serverConfiguration))
			services.add(SchedulerManager.load(etcTracker, serverConfiguration.getSchedulerMaxThreads()));

		etcTracker.check();

		classes.addAll(services);

		return servletApplication;
	}

	@Override
	protected IdentityManager getIdentityManager(String realm) throws IOException {
		AbstractLibrary library = LibraryManager.getInstance().get(realm);
		if (library == null)
			throw new IOException("No realm connector with this name: " + realm);
		if (!(library instanceof IdentityManager))
			throw new IOException("This is a not a realm connector: " + realm);
		return (IdentityManager) library;
	}

	private void startAll(String[] args)
			throws InstantiationException, ServletException, IllegalAccessException, ParseException, IOException {
		super.start(args, true);
		// Register the services
		ClusterManager.INSTANCE.registerMe(
				new ClusterNodeJson(ClusterManager.INSTANCE.myAddress, services, serverConfiguration.groups));

	}

	private static Qwazr qwazr = null;

	public static synchronized void start(String[] args)
			throws IOException, InstantiationException, ServletException, IllegalAccessException, ParseException {
		if (qwazr != null)
			throw new ExecuteException("QWAZR is already started", -1);
		qwazr = new Qwazr();
		qwazr.startAll(args);
	}

	public static synchronized void stop() {
		if (qwazr == null)
			return;
		qwazr.stopAll();
	}

	public static void main(String[] args) {
		// Start the server
		try {
			start(args);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			System.exit(1);
		}

	}
}