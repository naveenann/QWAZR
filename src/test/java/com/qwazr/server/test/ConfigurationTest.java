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
 **/
package com.qwazr.server.test;

import com.qwazr.QwazrConfiguration;
import com.qwazr.profiler.ProfilerManager;
import com.qwazr.scheduler.SchedulerManager;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConfigurationTest {

	@Test
	public void builder() throws IOException {
		QwazrConfiguration config = QwazrConfiguration.of()
				.service(QwazrConfiguration.ServiceEnum.search)
				.maxSchedulerThreads(123)
				.profiler("com.qwazr.*")
				.profiler("org.apache.*")
				.build();
		Assert.assertNotNull(config.services);
		Assert.assertEquals(1, config.services.size());
		Assert.assertTrue(config.services.contains(QwazrConfiguration.ServiceEnum.search));
		Assert.assertEquals(Integer.valueOf(123),
				config.getIntegerProperty(SchedulerManager.QWAZR_SCHEDULER_MAX_THREADS, null));
		Assert.assertEquals("com.qwazr.*;org.apache.*",
				config.getStringProperty(ProfilerManager.QWAZR_PROFILERS, null));
	}
}
