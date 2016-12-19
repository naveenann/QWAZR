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

import com.qwazr.classloader.ClassLoaderManager;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.compiler.CompilerManager;
import com.qwazr.crawler.web.manager.WebCrawlerManager;
import com.qwazr.database.TableManager;
import com.qwazr.extractor.ExtractorManager;
import com.qwazr.graph.GraphManager;
import com.qwazr.library.LibraryManager;
import com.qwazr.profiler.ProfilerManager;
import com.qwazr.scheduler.SchedulerManager;
import com.qwazr.scripts.ScriptManager;
import com.qwazr.search.index.IndexManager;
import com.qwazr.server.BaseServer;
import com.qwazr.server.GenericServer;
import com.qwazr.server.WelcomeShutdownService;
import com.qwazr.store.StoreManager;
import com.qwazr.webapps.WebappManager;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Qwazr implements BaseServer {

	static final Logger LOGGER = LoggerFactory.getLogger(Qwazr.class);

	private static final String ACCESS_LOG_LOGGER_NAME = "com.qwazr.rest.accessLogger";
	private static final Logger ACCESS_REST_LOGGER = LoggerFactory.getLogger(ACCESS_LOG_LOGGER_NAME);

	private final GenericServer server;

	public Qwazr(final QwazrConfiguration configuration)
			throws IOException, URISyntaxException, ReflectiveOperationException, SchedulerException {
		final ExecutorService executorService = Executors.newCachedThreadPool();
		final GenericServer.Builder builder = GenericServer.of(configuration, executorService);

		builder.restAccessLogger(ACCESS_REST_LOGGER);

		final ClassLoaderManager classLoaderManager = new ClassLoaderManager(builder, Thread.currentThread());
		final ClusterManager clusterManager = new ClusterManager(builder);

		if (QwazrConfiguration.ServiceEnum.compiler.isActive(configuration))
			new CompilerManager(executorService, classLoaderManager, builder);

		builder.webService(WelcomeShutdownService.class);

		final TableManager tableManager = QwazrConfiguration.ServiceEnum.table.isActive(configuration) ?
				TableManager.getNewInstance(builder) :
				null;

		final LibraryManager libraryManager =
				new LibraryManager(classLoaderManager, tableManager == null ? null : tableManager.getService(),
						builder);
		builder.identityManagerProvider(libraryManager);

		if (QwazrConfiguration.ServiceEnum.profiler.isActive(configuration))
			ProfilerManager.load(builder);

		if (QwazrConfiguration.ServiceEnum.extractor.isActive(configuration))
			new ExtractorManager(builder);

		final ScriptManager scriptManager = QwazrConfiguration.ServiceEnum.scripts.isActive(configuration) ?
				new ScriptManager(executorService, classLoaderManager, clusterManager, libraryManager, builder) :
				null;

		if (QwazrConfiguration.ServiceEnum.webcrawler.isActive(configuration))
			new WebCrawlerManager(clusterManager, scriptManager, executorService, builder);

		if (QwazrConfiguration.ServiceEnum.search.isActive(configuration))
			new IndexManager(classLoaderManager, builder, executorService);

		if (QwazrConfiguration.ServiceEnum.graph.isActive(configuration))
			new GraphManager(builder);

		if (QwazrConfiguration.ServiceEnum.store.isActive(configuration))
			new StoreManager(builder);

		if (QwazrConfiguration.ServiceEnum.webapps.isActive(configuration))
			new WebappManager(libraryManager, builder);

		// Scheduler is last, because it may immediatly execute a scripts
		if (QwazrConfiguration.ServiceEnum.schedulers.isActive(configuration))
			new SchedulerManager(clusterManager, scriptManager, builder);

		server = builder.build();
	}

	@Override
	public GenericServer getServer() {
		return server;
	}

	private static volatile Qwazr INSTANCE = null;

	public static Qwazr getInstance() {
		return INSTANCE;
	}

	/**
	 * Start the server. The prototype is based on Procrun specs
	 *
	 * @param args For procrun compatbility, currently ignored
	 */
	public static synchronized void start(final String[] args) {
		try {
			INSTANCE = new Qwazr(new QwazrConfiguration(args));
			INSTANCE.start();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			System.exit(1);
		}
	}

	/**
	 * Stop the server. The prototype is based on Procrun specs.
	 *
	 * @param args For procrun compatbility, currently ignored
	 */
	public static synchronized void stop(final String[] args) {
		if (INSTANCE != null) {
			INSTANCE.stop();
			INSTANCE = null;
		}
		System.exit(0);
	}

	/**
	 * Main with Procrun compatibility
	 *
	 * @param args One argument: "start" or "stop"
	 */
	public static void main(final String[] args) {
		if (args != null && args.length > 0) {
			switch (args[0]) {
			case "start":
				start(args);
				return;
			case "stop":
				stop(args);
				return;
			}
		}
		start(args);
	}

}
