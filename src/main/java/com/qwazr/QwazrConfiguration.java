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

import com.qwazr.utils.StringUtils;
import com.qwazr.utils.server.ServerConfiguration;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileFilter;
import java.net.SocketException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class QwazrConfiguration extends ServerConfiguration implements QwazrConfigurationProperties {

	final static Logger LOGGER = LoggerFactory.getLogger(QwazrConfiguration.class);

	public enum ServiceEnum {

		webcrawler,

		extractor,

		scripts,

		schedulers,

		webapps,

		search,

		graph,

		table,

		store,

		compiler,

		profiler;

		/**
		 * @param serverConfiguration
		 * @return true if the service is present
		 */
		public boolean isActive(final QwazrConfiguration serverConfiguration) {
			if (serverConfiguration == null)
				return true;
			if (serverConfiguration.services == null)
				return true;
			return serverConfiguration.services.contains(this);
		}
	}

	public final Set<ServiceEnum> services;
	public final int scheduler_max_threads;

	public QwazrConfiguration(final String... args) {
		super(args);


		this.services = buildServices(configProperties.qwazrServices());
		this.scheduler_max_threads = configProperties.qwazrSchedulerMaxThreads();
	}


	private static Set<ServiceEnum> buildServices(final String servicesString) {
		if (servicesString == null)
			return null;
		final String[] services_array = StringUtils.split(servicesString, ',');
		if (services_array == null || services_array.length == 0)
			return null;
		final Set<ServiceEnum> services = new HashSet<>();
		for (String service : services_array) {
			try {
				services.add(ServiceEnum.valueOf(service.trim()));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Unknown service in QWAZR_SERVICES: " + service);
			}
		}
		return services;
	}

	private static Set<String> splitValue(String value, char separator) {
		if (StringUtils.isEmpty(value))
			return null;
		final String[] valueArray = StringUtils.split(value, separator);
		if (valueArray == null || valueArray.length == 0)
			return null;
		final Set<String> values = new HashSet<>();
		for (String v : valueArray)
			values.add(v.trim());
		return values;
	}

}
