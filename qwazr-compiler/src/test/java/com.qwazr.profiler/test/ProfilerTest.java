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

import com.qwazr.profiler.MethodResult;
import com.qwazr.profiler.ProfilerManager;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProfilerTest {

	final private static Logger LOGGER = LoggerFactory.getLogger(ProfilerTest.class);

	final private static Map<String, Long> EXPECTED = new HashMap<>();

	static {
		EXPECTED.put("com/qwazr/profiler/test/ProfiledClass:<init>@()V", 8L);
		EXPECTED.put("com/qwazr/profiler/test/ProfiledClass:test@()V", 80L);
		EXPECTED.put("com/qwazr/profiler/test/ProfiledClass:test@(I)V", 80L);
		EXPECTED.put("com/qwazr/profiler/test/ProfiledClass:testEx@()V", 80L);
		EXPECTED.put(
				"com/qwazr/profiler/test/ProfiledClass:wait@(Ljava/util/concurrent/atomic/AtomicInteger;Ljava/util/concurrent/atomic/AtomicLong;I)V",
				240L);
	}
	
	@Test
	public void test100profile() throws InterruptedException, ExecutionException {
		if (!ProfilerManager.isInitialized()) {
			LOGGER.info("ProfilerTest skipped");
			return;
		}
		final ExecutorService executorService = Executors.newCachedThreadPool();
		final Future<?>[] futures = new Future<?>[8];
		for (int i = 0; i < futures.length; i++) {
			futures[i] = executorService.submit(() -> {
				ProfiledClass profiledClass = new ProfiledClass();
				for (int j = 0; j < 10; j++) {
					try {
						profiledClass.test();
						profiledClass.test(250);
						profiledClass.testEx();
					} catch (Exception e) {
					}
				}
			});
		}
		for (Future<?> future : futures)
			future.get();
		executorService.shutdown();

		Assert.assertEquals(8, ProfiledClass.constructorCount.get());
		Assert.assertEquals(80, ProfiledClass.testCount.get());
		Assert.assertEquals(80, ProfiledClass.testParamCount.get());
		Assert.assertEquals(80, ProfiledClass.testExCount.get());

		Map<String, MethodResult> results =
				ProfilerManager.getMethods("com/qwazr/profiler/test/ProfiledClass", null, null);
		Assert.assertNotNull(results);
		Assert.assertTrue(results.size() >= EXPECTED.size());

		EXPECTED.forEach((key, count) -> Assert.assertEquals((long) count, results.get(key).invocations));

	}

}
