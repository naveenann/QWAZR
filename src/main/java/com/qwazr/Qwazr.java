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
import com.qwazr.cluster.ClusterManager;
import com.qwazr.cluster.ClusterServiceBuilder;
import com.qwazr.compiler.CompilerManager;
import com.qwazr.compiler.CompilerServiceInterface;
import com.qwazr.crawler.web.WebCrawlerManager;
import com.qwazr.crawler.web.WebCrawlerServiceBuilder;
import com.qwazr.database.TableManager;
import com.qwazr.database.TableServiceBuilder;
import com.qwazr.extractor.ExtractorManager;
import com.qwazr.extractor.ExtractorServiceInterface;
import com.qwazr.graph.GraphManager;
import com.qwazr.graph.GraphServiceInterface;
import com.qwazr.library.LibraryManager;
import com.qwazr.profiler.ProfilerManager;
import com.qwazr.scheduler.SchedulerManager;
import com.qwazr.scheduler.SchedulerServiceInterface;
import com.qwazr.scripts.ScriptManager;
import com.qwazr.scripts.ScriptServiceBuilder;
import com.qwazr.search.index.IndexManager;
import com.qwazr.search.index.IndexServiceBuilder;
import com.qwazr.server.BaseServer;
import com.qwazr.server.GenericServer;
import com.qwazr.server.WelcomeShutdownService;
import com.qwazr.store.StoreManager;
import com.qwazr.store.StoreServiceBuilder;
import com.qwazr.webapps.WebappManager;
import com.qwazr.webapps.WebappServiceInterface;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanException;
import javax.management.OperationsException;
import javax.servlet.ServletException;
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
	private final TableServiceBuilder tableServiceBuilder;
	private final LibraryManager libraryManager;
	private final ExtractorManager extractorManager;
	private final ScriptServiceBuilder scriptServiceBuilder;
	private final WebCrawlerServiceBuilder webCrawlerServiceBuilder;
	private final IndexServiceBuilder indexServiceBuilder;
	private final GraphManager graphManager;
	private final StoreServiceBuilder storeServiceBuilder;
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

		final TableManager tableManager = QwazrConfiguration.ServiceEnum.table.isActive(configuration) ?
				TableManager.getNewInstance(builder) :
				null;
		tableServiceBuilder = new TableServiceBuilder(clusterManager, tableManager);

		libraryManager = new LibraryManager(classLoaderManager, tableManager == null ? null : tableManager.getService(),
				builder);
		builder.identityManagerProvider(libraryManager);

		if (QwazrConfiguration.ServiceEnum.profiler.isActive(configuration))
			ProfilerManager.load(builder);

		extractorManager =
				QwazrConfiguration.ServiceEnum.extractor.isActive(configuration) ? new ExtractorManager(builder) : null;

		final ScriptManager scriptManager = QwazrConfiguration.ServiceEnum.scripts.isActive(configuration) ?
				new ScriptManager(executorService, classLoaderManager, clusterManager, libraryManager, builder) :
				null;
		scriptServiceBuilder = new ScriptServiceBuilder(clusterManager, scriptManager);

		final WebCrawlerManager webCrawlerManager = QwazrConfiguration.ServiceEnum.webcrawler.isActive(configuration) ?
				new WebCrawlerManager(clusterManager, scriptManager, executorService, builder) :
				null;
		webCrawlerServiceBuilder = new WebCrawlerServiceBuilder(clusterManager, webCrawlerManager);

		final IndexManager indexManager = QwazrConfiguration.ServiceEnum.search.isActive(configuration) ?
				new IndexManager(classLoaderManager, builder, executorService) :
				null;
		indexServiceBuilder = new IndexServiceBuilder(clusterManager, indexManager);

		graphManager = QwazrConfiguration.ServiceEnum.graph.isActive(configuration) ? new GraphManager(builder) : null;

		final StoreManager storeManager =
				QwazrConfiguration.ServiceEnum.store.isActive(configuration) ? new StoreManager(builder) : null;
		storeServiceBuilder = new StoreServiceBuilder(clusterManager, storeManager);

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

	public ClusterServiceBuilder getClusterService() {
		return clusterManager == null ? null : clusterManager.getServiceBuilder();
	}

	public CompilerServiceInterface getCompilerService() {
		return compilerManager == null ? null : compilerManager.getService();
	}

	public TableServiceBuilder getTableService() {
		return tableServiceBuilder;
	}

	public LibraryManager getLibraryManager() {
		return libraryManager;
	}

	public ExtractorServiceInterface getExtractorService() {
		return extractorManager == null ? null : extractorManager.getService();
	}

	public ScriptServiceBuilder getScriptService() {
		return scriptServiceBuilder;
	}

	public WebCrawlerServiceBuilder getWebCrawlerService() {
		return webCrawlerServiceBuilder;
	}

	public IndexServiceBuilder getIndexService() {
		return indexServiceBuilder;
	}

	public GraphServiceInterface getGraphService() {
		return graphManager == null ? null : graphManager.getService();
	}

	public StoreServiceBuilder getStoreService() {
		return storeServiceBuilder;
	}

	public WebappServiceInterface getWebappService() {
		return webappManager == null ? null : webappManager.getService();
	}

	public SchedulerServiceInterface getSchedulerService() {
		return schedulerManager == null ? null : schedulerManager.getService();
	}

	private static volatile Qwazr INSTANCE = null;

	public static Qwazr getInstance() {
		return INSTANCE;
	}

	/**
	 * Stop the server..
	 */
	public static synchronized void start(final QwazrConfiguration config)
			throws IOException, ReflectiveOperationException, OperationsException, ServletException, MBeanException,
			URISyntaxException, SchedulerException {
		shutdown();
		INSTANCE = new Qwazr(config);
		INSTANCE.start();
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
	 * Start the server. The prototype is based on Procrun specs
	 *
	 * @param args For procrun compatbility, currently ignored
	 */
	public static synchronized void start(final String[] args) {
		try {
			start(new QwazrConfiguration(args));
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
		shutdown();
		System.exit(0);
	}

	/**
	 * Apply available injections from the LibraryManager
	 *
	 * @param object
	 */
	public static void inject(final Object object) {
		if (INSTANCE == null)
			return;
		Objects.requireNonNull(INSTANCE).getLibraryManager().inject(object);
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
