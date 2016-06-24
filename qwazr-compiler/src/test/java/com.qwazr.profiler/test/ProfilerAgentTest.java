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
package com.qwazr.profiler.test;

import com.qwazr.profiler.ProfilerAgent;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class ProfilerAgentTest {

	public class ProfiledClass {

		public void test() throws InterruptedException {
			Thread.sleep(1000 + new Random().nextInt(1000));
		}
	}

	@Test
	public void profile() throws InterruptedException {
		new ProfiledClass().test();
		ConcurrentHashMap.KeySetView<String, SummaryStatistics> keys = ProfilerAgent.getKeys();
		Assert.assertNotNull(keys);
		Assert.assertFalse(keys.isEmpty());
	}

}
