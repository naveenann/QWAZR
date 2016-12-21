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
import com.qwazr.cluster.service.ClusterServiceInterface;
import com.qwazr.compiler.CompilerManager;
import com.qwazr.compiler.CompilerServiceInterface;
import com.qwazr.crawler.web.manager.WebCrawlerManager;
import com.qwazr.crawler.web.service.WebCrawlerServiceInterface;
import com.qwazr.database.TableManager;
import com.qwazr.database.TableServiceInterface;
import com.qwazr.extractor.ExtractorManager;
import com.qwazr.extractor.ExtractorServiceInterface;
import com.qwazr.graph.GraphManager;
import com.qwazr.graph.GraphServiceInterface;
import com.qwazr.library.LibraryManager;
import com.qwazr.profiler.ProfilerManager;
import com.qwazr.scheduler.SchedulerManager;
import com.qwazr.scheduler.SchedulerServiceInterface;
import com.qwazr.scripts.ScriptManager;
import com.qwazr.scripts.ScriptServiceInterface;
import com.qwazr.search.index.IndexManager;
import com.qwazr.search.index.IndexServiceInterface;
import com.qwazr.server.BaseServer;
import com.qwazr.server.GenericServer;
import com.qwazr.server.WelcomeShutdownService;
import com.qwazr.store.StoreManager;
import com.qwazr.store.StoreServiceInterface;
import com.qwazr.webapps.WebappManager;
import com.qwazr.webapps.WebappServiceInterface;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Qwazr implements BaseServer {

	static final Logger LOGGER = LoggerFactory.getLogger(Qwazr.class);

	private static final String ACCESS_LOG_LOGGER_NAME = "com.qwazr.rest.accessLogger";
	private static final Logger ACCESS_REST_LOGGER = LoggerFactory.getLogger(ACCESS_LOG_LOGGER_NAME);

	private final GenericServer server;

	private final ClassLoaderManager classLoaderManager;
	private final ClusterManager clusterManager;
	private final CompilerManager compilerManager;
	private final TableManager tableManager;
	private final LibraryManager libraryManager;
	private final ExtractorManager extractorManager;
	private final ScriptManager scriptManager;
	private final WebCrawlerManager webCrawlerManager;
	private final IndexManager indexManager;
	private final GraphManager graphManager;
	private final StoreManager storeManager;
	private final WebappManager webappManager;
	private final SchedulerManager schedulerManager;

	public Qwazr(final QwazrConfiguration configuration)
			throws IOException, URISyntaxException, ReflectiveOperationException, SchedulerException {
		final ExecutorService executorService = Executors.newCachedThreadPool();
		final GenericServer.Builder builder = GenericServer.of(configuration, executorService);

		builder.restAccessLogger(ACCESS_REST_LOGGER);

		classLoaderManager = new ClassLoaderManager(builder, Thread.currentThread());
		clusterManager = new ClusterManager(builder);

		compilerManager = QwazrConfiguration.ServiceEnum.compiler.isActive(configuration) ?
				new CompilerManager(executorService, classLoaderManager, builder) :
				null;

		builder.webService(WelcomeShutdownService.class);

		tableManager = QwazrConfiguration.ServiceEnum.table.isActive(configuration) ?
				TableManager.getNewInstance(builder) :
				null;

		libraryManager = new LibraryManager(classLoaderManager, tableManager == null ? null : tableManager.getService(),
				builder);
		builder.identityManagerProvider(libraryManager);

		if (QwazrConfiguration.ServiceEnum.profiler.isActive(configuration))
			ProfilerManager.load(builder);

		extractorManager =
				QwazrConfiguration.ServiceEnum.extractor.isActive(configuration) ? new ExtractorManager(builder) : null;

		scriptManager = QwazrConfiguration.ServiceEnum.scripts.isActive(configuration) ?
				new ScriptManager(executorService, classLoaderManager, clusterManager, libraryManager, builder) :
				null;

		webCrawlerManager = QwazrConfiguration.ServiceEnum.webcrawler.isActive(configuration) ?
				new WebCrawlerManager(clusterManager, scriptManager, executorService, builder) :
				null;

		indexManager = QwazrConfiguration.ServiceEnum.search.isActive(configuration) ?
				new IndexManager(classLoaderManager, builder, executorService) :
				null;

		graphManager = QwazrConfiguration.ServiceEnum.graph.isActive(configuration) ? new GraphManager(builder) : null;

		storeManager = QwazrConfiguration.ServiceEnum.store.isActive(configuration) ? new StoreManager(builder) : null;

		webappManager = QwazrConfiguration.ServiceEnum.webapps.isActive(configuration) ?
				new WebappManager(libraryManager, builder) :
				null;

		// Scheduler is last, because it may immediatly execute a scripts
		schedulerManager = QwazrConfiguration.ServiceEnum.schedulers.isActive(configuration) ?
				new SchedulerManager(clusterManager, scriptManager, builder) :
				null;

		server = builder.build();
	}

	@Override
	public GenericServer getServer() {
		return server;
	}

	public ClassLoaderManager getClassLoaderManager() {
		return classLoaderManager;
	}

	public ClusterServiceInterface getClusterService() {
		return clusterManager == null ? null : clusterManager.getService();
	}

	public CompilerServiceInterface getCompilerService() {
		return compilerManager == null ? null : compilerManager.getService();
	}

	public TableServiceInterface getTableService() {
		return tableManager == null ? null : tableManager.getService();
	}

	public LibraryManager getLibraryManager() {
		return libraryManager;
	}

	public ExtractorServiceInterface getExtractorService() {
		return extractorManager == null ? null : extractorManager.getService();
	}

	public ScriptServiceInterface getScriptService() {
		return scriptManager == null ? null : scriptManager.getService();
	}

	public WebCrawlerServiceInterface getWebCrawlerService() {
		return webCrawlerManager == null ? null : webCrawlerManager.getService();
	}

	public IndexServiceInterface getIndexService() {
		return indexManager == null ? null : indexManager.getService();
	}

	public GraphServiceInterface getGraphService() {
		return graphManager == null ? null : graphManager.getService();
	}

	public StoreServiceInterface getStoreService() {
		return storeManager == null ? null : storeManager.getService();
	}

	public WebappServiceInterface getWebappService() {
		return webappManager == null ? null : webappManager.getService();
	}

	public SchedulerServiceInterface getSchedulerService() {
		return schedulerManager == null ? null : schedulerManager.getService();
	}

	private static volatile Qwazr INSTANCE = null;

	public static Qwazr getInstance() {
		Objects.requireNonNull(INSTANCE, "QWAZR is not started");
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
	 * Stop the server..
	 */
	public static synchronized void shutdown() {
		if (INSTANCE == null)
			return;
		INSTANCE.stop();
	}

	/**
	 * Stop the server. The prototype is based on Procrun specs.
	 *
	 * @param args For procrun compatbility, currently ignored
	 */
	public static synchronized void stop(final String[] args) {
		shutdown();
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
