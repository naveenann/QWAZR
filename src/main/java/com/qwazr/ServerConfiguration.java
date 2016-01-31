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
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.FileFilter;
import java.util.HashSet;
import java.util.Set;

public class ServerConfiguration {

	public enum ServiceEnum {

		webcrawler,

		extractor,

		scripts,

		schedulers,

		semaphores,

		webapps,

		search,

		graph,

		table,

		compiler;

		/**
		 * @param serverConfiguration
		 * @return true if the service is present
		 */
		public boolean isActive(ServerConfiguration serverConfiguration) {
			if (serverConfiguration == null)
				return true;
			if (serverConfiguration.services == null)
				return true;
			return serverConfiguration.services.contains(this);
		}
	}

	public final Set<ServiceEnum> services;
	public final Set<String> groups;
	public final FileFilter etcFileFilter;

	public final Integer scheduler_max_threads;

	ServerConfiguration() {

		final String etc_env = System.getenv("QWAZR_ETC");
		if (StringUtils.isEmpty(etc_env)) {
			etcFileFilter = FileFileFilter.FILE;
		} else {
			String[] array = StringUtils.split(etc_env, ',');
			if (array != null && array.length > 0)
				etcFileFilter = new AndFileFilter(FileFileFilter.FILE, new WildcardFileFilter(array));
			else
				etcFileFilter = FileFileFilter.FILE;
		}

		final String services_env = System.getenv("QWAZR_SERVICES");
		if (StringUtils.isEmpty(services_env)) {
			services = null;
		} else {
			services = new HashSet<ServiceEnum>();
			String[] services_array = StringUtils.split(services_env, ',');
			for (String service : services_array) {
				try {
					services.add(ServiceEnum.valueOf(service.trim()));
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("Unknown service in QWAZR_SERVICES: " + service);
				}
			}
		}
		final String groups_env = System.getenv("QWAZR_GROUPS");
		if (StringUtils.isEmpty(groups_env)) {
			groups = null;
		} else {
			groups = new HashSet<String>();
			String[] groups_array = StringUtils.split(groups_env, ',');
			for (String group : groups_array)
				groups.add(group);
		}
		String s = System.getenv("QWAZR_SCHEDULER_MAX_THREADS");
		scheduler_max_threads = StringUtils.isEmpty(s) ? 100 : Integer.parseInt(s);
	}

	/**
	 * @return the number of allowed threads. The default value is 1000.
	 */
	int getSchedulerMaxThreads() {
		return scheduler_max_threads == null ? 100 : scheduler_max_threads;
	}

}