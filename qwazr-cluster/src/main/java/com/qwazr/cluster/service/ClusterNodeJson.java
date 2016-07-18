/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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
 */
package com.qwazr.cluster.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@JsonInclude(Include.NON_NULL)
public class ClusterNodeJson {

	public final String address;
	public final String nodeLiveId;
	public final Integer timeToLive;
	public final Set<String> services;
	public final Set<String> groups;

	public ClusterNodeJson() {
		address = null;
		nodeLiveId = null;
		timeToLive = null;
		services = null;
		groups = null;
	}

	public ClusterNodeJson(final String address, final UUID nodeLiveId, final Integer timeToLive,
			final Collection<String> groups, final Collection<String> services) {
		this.address = address;
		this.nodeLiveId = nodeLiveId == null ? null : nodeLiveId.toString();
		this.timeToLive = timeToLive;
		this.groups = groups == null ? null : new TreeSet<>(groups);
		this.services = services == null ? null : new TreeSet<>(services);
	}

}
