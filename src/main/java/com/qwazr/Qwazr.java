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
import com.qwazr.library.LibraryServiceImpl;
import com.qwazr.profiler.ProfilerManager;
import com.qwazr.scheduler.SchedulerManager;
import com.qwazr.scripts.ScriptManager;
import com.qwazr.search.index.IndexManager;
import com.qwazr.store.StoreManager;
import com.qwazr.utils.file.TrackedInterface;
import com.qwazr.utils.server.GenericServer;
import com.qwazr.utils.server.ServerBuilder;
import com.qwazr.webapps.WebappManager;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class QwazrServer extends GenericServer {

	static final Logger LOGGER = LoggerFactory.getLogger(QwazrServer.class);

	private static final String ACCESS_LOG_LOGGER_NAME = "com.qwazr.rest.accessLogger";
	private static final Logger ACCESS_REST_LOGGER = LoggerFactory.getLogger(ACCESS_LOG_LOGGER_NAME);

	private QwazrServer(final QwazrConfiguration config) throws IOException {
		super(config);

		final ServerBuilder builder = getBuilder();
		final TrackedInterface etcTracker = getEtcTracker();

		builder.setRestAccessLogger(ACCESS_REST_LOGGER);

		final File currentTempDir = new File(config.dataDirectory, "tmp");

		ClassLoaderManager.load(config.dataDirectory, null);

		if (QwazrConfiguration.ServiceEnum.compiler.isActive(config))
			CompilerManager.load(builder);

		builder.registerWebService(WelcomeShutdownService.class);

		ClusterManager.load(builder);

		LibraryManager.load(builder);
		builder.setIdentityManagerProvider(LibraryManager.getInstance());

		if (QwazrConfiguration.ServiceEnum.profiler.isActive(config))
			ProfilerManager.load(builder);

		if (QwazrConfiguration.ServiceEnum.extractor.isActive(config))
			ExtractorManager.load(builder);

		if (QwazrConfiguration.ServiceEnum.scripts.isActive(config))
			ScriptManager.load(builder);

		if (QwazrConfiguration.ServiceEnum.webcrawler.isActive(config))
			WebCrawlerManager.load(builder);

		if (QwazrConfiguration.ServiceEnum.search.isActive(config))
			IndexManager.load(builder);

		if (QwazrConfiguration.ServiceEnum.graph.isActive(config))
			GraphManager.load(builder);

		if (QwazrConfiguration.ServiceEnum.table.isActive(config))
			TableManager.load(builder);

		if (QwazrConfiguration.ServiceEnum.store.isActive(config))
			StoreManager.load(builder);

		if (QwazrConfiguration.ServiceEnum.webapps.isActive(config))
			WebappManager.load(builder, etcTracker, currentTempDir);

		// Scheduler is last, because it may immediatly execute a scripts
		if (QwazrConfiguration.ServiceEnum.schedulers.isActive(config))
			SchedulerManager.load(builder, etcTracker, config.scheduler_max_threads);

		etcTracker.check();
	}

	static GenericServer qwazr = null;

	/**
	 * Start the server
	 *
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws ServletException
	 * @throws IllegalAccessException
	 * @throws ParseException
	 */
	public static synchronized void startWithConf(final QwazrConfiguration configuration) throws Exception {
		if (qwazr != null)
			throw new IllegalAccessException("QWAZR is already started");
		qwazr = newServer(configuration);
		qwazr.start(true);
	}

	/**
	 * Start the server. The prototype is based on Procrun specs
	 *
	 * @param args For procrun compatbility, currently ignored
	 */
	public static synchronized void start(final String[] args) {

		if (LOGGER.isInfoEnabled())
			LOGGER.info("QWAZR is starting...");
		try {

			// Load qwazr properties
			final Properties properties = new Properties();
			final String propertyFile = System.getProperty(QwazrConfigurationProperties.QWAZR_PROPERTIES,
					System.getenv(QwazrConfigurationProperties.QWAZR_PROPERTIES));
			if (propertyFile != null) {
				try (final FileReader reader = new FileReader(new File(propertyFile))) {
					properties.load(reader);
				}
			}

			startWithConf(new QwazrConfiguration(properties, System.getProperties(), System.getenv()));
			if (LOGGER.isInfoEnabled())
				LOGGER.info("QWAZR started successfully.");
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
		if (LOGGER.isInfoEnabled())
			LOGGER.info("QWAZR is stopping...");
		if (qwazr != null)
			qwazr.stopAll();
		LOGGER.info("QWAZR stopped.");
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
