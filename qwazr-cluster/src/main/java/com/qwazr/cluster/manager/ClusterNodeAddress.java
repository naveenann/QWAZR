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
package com.qwazr.cluster.manager;

import com.qwazr.utils.StringUtils;
import com.qwazr.utils.server.ServerException;

import javax.ws.rs.core.Response;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

public class ClusterNodeAddress {

	public final URI uri;
	public final InetSocketAddress address;
	public final String httpAddressKey;

	/**
	 * @param httpAddress the address of the node: {scheme}://{host}:{port}
	 */
	ClusterNodeAddress(String httpAddress) {
		try {
			if (!httpAddress.contains("//"))
				httpAddress = "http://" + httpAddress;
			final URI u = new URI(httpAddress);
			uri = new URI(StringUtils.isEmpty(u.getScheme()) ? "http" : u.getScheme(), null, u.getHost(), u.getPort(),
					null,
					null, null);
			address = new InetSocketAddress(u.getHost(), u.getPort());
			httpAddressKey = uri.toString().intern();
		} catch (URISyntaxException e) {
			throw new ServerException(Response.Status.NOT_ACCEPTABLE,
					"Cannot create node. Wrong address syntax: " + httpAddress, e);
		}
	}

}
