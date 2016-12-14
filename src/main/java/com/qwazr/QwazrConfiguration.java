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

import com.qwazr.profiler.ProfilerManager;
import com.qwazr.scheduler.SchedulerManager;
import com.qwazr.server.configuration.ServerConfiguration;
import com.qwazr.utils.StringUtils;

import java.io.IOException;
import java.util.*;

public class QwazrConfiguration extends ServerConfiguration {

	public final static String QWAZR_SERVICES = "QWAZR_SERVICES";

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

	public QwazrConfiguration(final String... args) throws IOException {
		this(System.getenv(), System.getProperties(), argsToMap(args));
	}

	protected QwazrConfiguration(final Map<?, ?>... propertiesMaps) throws IOException {
		super(propertiesMaps);
		this.services = buildServices(getStringProperty(QWAZR_SERVICES, null));
	}

	private static Set<ServiceEnum> buildServices(final String servicesString) {
		if (servicesString == null)
			return null;
		final Set<ServiceEnum> services = new HashSet<>();
		fillStringListProperty(servicesString, ",; ", true, service -> {
			try {
				services.add(ServiceEnum.valueOf(service.trim()));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Unknown service in QWAZR_SERVICES: " + service);
			}
		});
		return services.isEmpty() ? null : Collections.unmodifiableSet(services);
	}

	public static Builder of() {
		return new Builder();
	}

	public static class Builder extends ServerConfiguration.Builder {

		private final Set<String> services;
		private final Set<String> profilers;

		protected Builder() {
			this.services = new LinkedHashSet<>();
			this.profilers = new LinkedHashSet<>();

		}

		public Builder service(final ServiceEnum service) {
			if (service != null)
				services.add(service.name());
			return this;
		}

		public Builder profiler(final String profiler) {
			if (profiler != null)
				profilers.add(profiler);
			return this;
		}

		public Builder maxSchedulerThreads(final Integer maxThreads) {
			if (maxThreads != null)
				map.put(SchedulerManager.QWAZR_SCHEDULER_MAX_THREADS, maxThreads.toString());
			return this;
		}

		@Override
		public Map<String, String> finalMap() {
			super.finalMap();
			if (!services.isEmpty())
				map.put(QWAZR_SERVICES, StringUtils.join(services, ','));
			if (!profilers.isEmpty())
				map.put(ProfilerManager.QWAZR_PROFILERS, StringUtils.join(profilers, ';'));
			return map;
		}

		@Override
		public QwazrConfiguration build() throws IOException {
			return new QwazrConfiguration(finalMap());
		}

	}

}
