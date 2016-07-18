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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WelcomeStatus {

	public final ImplementationStatus implementation;
	public final List<String> endpoints;

	WelcomeStatus() {
		this.implementation = new ImplementationStatus();
		endpoints = new ArrayList<>();
		final Collection<String> servicePaths = Qwazr.qwazr.getServicePaths();
		if (servicePaths != null)
			servicePaths.forEach(path -> endpoints.add(ClusterManager.INSTANCE.me.httpAddressKey + path));
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public class ImplementationStatus {

		public final String title;
		public final String vendor;
		public final String version;

		ImplementationStatus() {
			final Package pkg = getClass().getPackage();
			title = pkg.getImplementationTitle();
			vendor = pkg.getImplementationVendor();
			version = pkg.getImplementationVersion();
		}
	}

}
