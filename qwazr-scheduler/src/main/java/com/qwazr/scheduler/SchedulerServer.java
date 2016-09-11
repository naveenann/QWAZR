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
package com.qwazr.scheduler;

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.scripts.ScriptManager;
import com.qwazr.utils.file.TrackedInterface;
import com.qwazr.utils.server.GenericServer;
import com.qwazr.utils.server.ServerBuilder;
import com.qwazr.utils.server.ServerConfiguration;

public class SchedulerServer {

	public static GenericServer start() throws Exception {
		final ServerBuilder builder =
				new ServerBuilder(new ServerConfiguration(System.getProperties(), System.getenv()));
		final TrackedInterface etcTracker =
				TrackedInterface.build(builder.getServerConfiguration().etcDirectories, null);
		ClusterManager.load(builder, null, null);
		ScriptManager.load(builder);
		SchedulerManager.load(builder, etcTracker, 10);
		return builder.build().start(true);
	}

	public static void main(String[] args) throws Exception {
		SchedulerServer.start();
	}

}