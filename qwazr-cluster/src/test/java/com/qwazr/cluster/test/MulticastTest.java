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
 */
package com.qwazr.cluster.test;

import com.qwazr.utils.server.UdpServerThread;

public class MulticastTest extends AbstractMultiTests {

	@Override
	protected void startServers() throws Exception {
		final String multicastGroup = UdpServerThread.DEFAULT_MULTICAST;

		master1 = new TestServer(null, 9091, multicastGroup, GROUP_MASTER);
		master2 = new TestServer(null, 9092, multicastGroup, GROUP_MASTER);
		front1 = new TestServer(null, 9093, multicastGroup, GROUP_FRONT);
		front2 = new TestServer(null, 9094, multicastGroup, GROUP_FRONT);
		front3 = new TestServer(null, 9095, multicastGroup, GROUP_FRONT);
	}


}
