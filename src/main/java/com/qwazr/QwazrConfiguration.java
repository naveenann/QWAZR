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
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.FileFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class QwazrConfiguration extends ServerConfiguration {

	public enum VariablesEnum {

		QWAZR_MASTERS,

		QWAZR_SERVICES,

		QWAZR_GROUPS,

		QWAZR_ETC,

		QWAZR_SCHEDULER_MAX_THREADS,

		QWAZR_PROFILERS

	}

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
		public boolean isActive(QwazrConfiguration serverConfiguration) {
			if (serverConfiguration == null)
				return true;
			if (serverConfiguration.services == null)
				return true;
			return serverConfiguration.services.contains(this);
		}
	}

	public final Set<String> masters;
	public final Set<ServiceEnum> services;
	public final Set<String> groups;
	public final FileFilter etcFileFilter;
	public final Integer scheduler_max_threads;

	public QwazrConfiguration(final Collection<String> etcFilters, final Collection<String> masters,
			final Collection<ServiceEnum> services, final Collection<String> groups, Integer schedulerMaxThreads) {
		this.etcFileFilter = buildEtcFileFilter(etcFilters);
		this.services = buildServices(services);
		this.groups = buildStringCollection(groups);
		this.masters = buildStringCollection(masters);
		this.scheduler_max_threads = buildSchedulerMaxThreads(schedulerMaxThreads);
	}

	QwazrConfiguration() {
		this.etcFileFilter = buildEtcFileFilter(getPropertyOrEnv(VariablesEnum.QWAZR_ETC));
		this.masters = buildCommaSeparated(getPropertyOrEnv(VariablesEnum.QWAZR_MASTERS));
		this.services = buildServices(getPropertyOrEnv(VariablesEnum.QWAZR_SERVICES));
		this.groups = buildCommaSeparated(getPropertyOrEnv(VariablesEnum.QWAZR_GROUPS));
		this.scheduler_max_threads =
				buildSchedulerMaxThreads(getPropertyOrEnv(VariablesEnum.QWAZR_SCHEDULER_MAX_THREADS));
	}

	private static FileFilter buildEtcFileFilter(final String etcFilter) {
		if (StringUtils.isEmpty(etcFilter))
			return FileFileFilter.FILE;
		final String[] array = StringUtils.split(etcFilter, ',');
		if (array == null || array.length == 0)
			return FileFileFilter.FILE;
		return new AndFileFilter(FileFileFilter.FILE, new WildcardFileFilter(array));
	}

	private static FileFilter buildEtcFileFilter(final Collection<String> etcFilters) {
		if (etcFilters == null || etcFilters.isEmpty())
			return FileFileFilter.FILE;
		return new AndFileFilter(FileFileFilter.FILE,
				new WildcardFileFilter(etcFilters.toArray(new String[etcFilters.size()])));
	}

	private static Set<ServiceEnum> buildServices(final Collection<ServiceEnum> serviceCollection) {
		if (serviceCollection == null)
			return null;
		final Set<ServiceEnum> services = new HashSet<>();
		services.addAll(serviceCollection);
		return services;
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

	private static Set<String> buildCommaSeparated(String commaSeparated) {
		return splitValue(commaSeparated, ',');
	}

	private static Set<String> buildStringCollection(Collection<String> groupCollection) {
		if (groupCollection == null || groupCollection.isEmpty())
			return null;
		final Set<String> groups = new LinkedHashSet<>();
		groupCollection.forEach((g) -> groups.add(g.trim()));
		return groups;
	}

	/**
	 * @return the number of allowed threads. The default value is 100.
	 */
	private static int buildSchedulerMaxThreads(String value) {
		if (value == null)
			return buildSchedulerMaxThreads((Integer) null);
		return buildSchedulerMaxThreads(Integer.parseInt(value));
	}

	private static int buildSchedulerMaxThreads(Integer value) {
		return value == null ? 100 : value;
	}

}
