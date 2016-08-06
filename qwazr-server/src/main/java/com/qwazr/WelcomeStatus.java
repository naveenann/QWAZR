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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qwazr.cluster.manager.ClusterManager;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WelcomeStatus {

	public final TitleVendorVersion implementation;
	public final TitleVendorVersion specification;
	public final List<String> endpoints;
	public final SortedMap<String, Object> properties;
	public final SortedMap<String, String> env;

	WelcomeStatus() {
		endpoints = new ArrayList<>();
		final Collection<String> servicePaths = Qwazr.qwazr.getServicePaths();
		if (servicePaths != null)
			servicePaths.forEach(path -> endpoints.add(ClusterManager.INSTANCE.me.httpAddressKey + path));
		final Package pkg = getClass().getPackage();
		implementation = new TitleVendorVersion(pkg.getImplementationTitle(), pkg.getImplementationVendor(),
				pkg.getImplementationVersion());
		specification = new TitleVendorVersion(pkg.getSpecificationTitle(), pkg.getSpecificationVendor(),
				pkg.getSpecificationVersion());
		properties = new TreeMap<>();
		System.getProperties().forEach((key, value) -> properties.put(key.toString(), value));
		env = new TreeMap<>(System.getenv());
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public class TitleVendorVersion {

		public final String title;
		public final String vendor;
		public final String version;

		TitleVendorVersion(final String title, final String vendor, final String version) {
			this.title = title;
			this.vendor = vendor;
			this.version = version;
		}
	}

}
