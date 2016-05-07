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
import com.qwazr.scheduler.SchedulerManager;
import com.qwazr.scripts.ScriptManager;
import com.qwazr.search.index.IndexManager;
import com.qwazr.utils.AnnotationsUtils;
import com.qwazr.utils.file.TrackedDirectory;
import com.qwazr.utils.server.GenericServer;
import com.qwazr.utils.server.ServerBuilder;
import com.qwazr.webapps.transaction.WebappManager;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

public class Qwazr {

	static final Logger logger = LoggerFactory.getLogger(Qwazr.class);

	private final static synchronized GenericServer newServer(final QwazrConfiguration config) throws IOException {

		final ServerBuilder builder = new ServerBuilder(config);

		File currentTempDir = new File(config.dataDirectory, "tmp");
		TrackedDirectory etcTracker = new TrackedDirectory(config.etcDirectory, config.etcFileFilter);
		if (config.etcDirectory.exists()) {
			File log4jFile = new File(config.etcDirectory, "log4j.properties");
			if (log4jFile.exists() && log4jFile.isFile())
				PropertyConfigurator.configureAndWatch(log4jFile.getAbsolutePath(), 60000);
		}

		ClassLoaderManager.load(config.dataDirectory, Thread.currentThread());

		builder.registerWebService(WelcomeServiceImpl.class);

		ClusterManager.load(builder, config.groups);

		if (QwazrConfiguration.ServiceEnum.compiler.isActive(config))
			CompilerManager.load(builder);

		if (QwazrConfiguration.ServiceEnum.extractor.isActive(config))
			ExtractorManager.load(builder);

		if (QwazrConfiguration.ServiceEnum.webapps.isActive(config))
			WebappManager.load(builder, etcTracker, currentTempDir);

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

		LibraryManager.load(config.dataDirectory, etcTracker);
		builder.registerWebService(LibraryServiceImpl.class);
		builder.setIdentityManagerProvider(LibraryManager.getInstance());

		// Scheduler is last, because it may immediatly execute a scripts
		if (QwazrConfiguration.ServiceEnum.schedulers.isActive(config))
			SchedulerManager.load(builder, etcTracker, config.scheduler_max_threads);

		etcTracker.check();

		return new GenericServer(builder);
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
	public static synchronized void start(QwazrConfiguration configuration)
			throws IOException, InstantiationException, ServletException, IllegalAccessException, ParseException {
		if (qwazr != null)
			throw new IllegalAccessException("QWAZR is already started");
		qwazr = newServer(configuration);
		qwazr.start(true);
	}

	/**
	 * Stop the server
	 */
	public static synchronized void stop() {
		if (qwazr == null)
			return;
		qwazr.stopAll();
	}

	public static void main(String[] args) {
		try {
			start(new QwazrConfiguration());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			System.exit(1);
		}
	}

	final public static void inject(final Object object) {
		if (object == null)
			return;
		AnnotationsUtils.browseFieldsRecursive(object.getClass(), new QwazrInjector(object));
	}

}
