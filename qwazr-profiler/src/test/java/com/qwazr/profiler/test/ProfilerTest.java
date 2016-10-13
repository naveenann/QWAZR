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

import com.qwazr.profiler.*;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProfilerTest {

	final private static Logger LOGGER = LoggerFactory.getLogger(ProfilerTest.class);

	final private static Map<String, Long> EXPECTED = new HashMap<>();

	static {
		EXPECTED.put("com/qwazr/profiler/test/ProfiledClass:<clinit>@()V", 1L);
		EXPECTED.put("com/qwazr/profiler/test/ProfiledClass:<init>@()V", 8L);
		EXPECTED.put("com/qwazr/profiler/test/ProfiledClass:test@()V", 80L);
		EXPECTED.put("com/qwazr/profiler/test/ProfiledClass:test@(I)V", 80L);
		EXPECTED.put("com/qwazr/profiler/test/ProfiledClass:testEx@()V", 80L);
		EXPECTED.put(
				"com/qwazr/profiler/test/ProfiledClass:wait@(Ljava/util/concurrent/atomic/AtomicInteger;Ljava/util/concurrent/atomic/AtomicLong;I)V",
				240L);
		EXPECTED.put("com/qwazr/profiler/test/ProfiledClass$InnerClass:<clinit>@()V", 1L);
		EXPECTED.put("com/qwazr/profiler/test/ProfiledClass$InnerClass:<init>@()V", 8L);
		EXPECTED.put("com/qwazr/profiler/test/ProfiledClass$InnerClass:test@()V", 8L);
	}

	@Test
	public void test000loadManager() {
		ProfilerManager.load(null);
		Assert.assertTrue(ProfilerManager.isInitialized());
	}

	private MethodResult findMethod(final String method, Collection<MethodResult> results) {
		if (CollectionUtils.isEmpty(results))
			return null;
		for (MethodResult result : results)
			if (method.equals(result.method))
				return result;
		return null;
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
				new ProfiledClass.InnerClass().test();
			});
		}
		for (Future<?> future : futures)
			future.get();
		executorService.shutdown();

		Assert.assertEquals(8, ProfiledClass.constructorCount.get());
		Assert.assertEquals(80, ProfiledClass.testCount.get());
		Assert.assertEquals(80, ProfiledClass.testParamCount.get());
		Assert.assertEquals(80, ProfiledClass.testExCount.get());
		Assert.assertEquals(8, ProfiledClass.InnerClass.testCount.get());

		final ProfilerServiceInterface service = new ProfilerServiceImpl();
		service.getPrefix(ProfilerServiceInterface.Parameters.of("com/qwazr/profiler/test/ProfiledClass").build());
		Assert.assertTrue(service.get(null).methods.size() >= EXPECTED.size());
		final ProfilerResult result =
				service.getPrefix(
						ProfilerServiceInterface.Parameters.of("com/qwazr/profiler/test/ProfiledClass").build());
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.methods);
		Assert.assertTrue(result.methods.size() >= EXPECTED.size());
		EXPECTED.forEach(
				(method, count) -> {
					MethodResult methodResult = findMethod(method, result.methods);
					Assert.assertNotNull("Check " + method, methodResult);
					Assert.assertEquals("Check " + method, (long) count, methodResult.invocations);
				});
		Assert.assertEquals(1,
				service.getPrefix(
						ProfilerServiceInterface.Parameters.of("com/qwazr/profiler/test/ProfiledClass").start(2).rows(1)
								.build()).methods.size());
	}

	@Test
	public void test120empty() {
		final ProfilerServiceInterface service = new ProfilerServiceImpl();
		Assert.assertTrue(CollectionUtils
				.isEmpty(service.get(ProfilerServiceInterface.Parameters.of().start(0).rows(0).build()).methods));
		Assert.assertTrue(CollectionUtils
				.isEmpty(service.get(
						ProfilerServiceInterface.Parameters.of().start(Integer.MAX_VALUE).rows(0).build()).methods));
		Assert.assertTrue(CollectionUtils
				.isEmpty(service.getPrefix(ProfilerServiceInterface.Parameters.of("/com/dummy").build()).methods));
	}

	private final void checkSorting(final ProfilerServiceInterface.Parameters.Builder paramsBuilder,
			final Comparator<MethodResult> comparator) {
		final ProfilerServiceInterface service = new ProfilerServiceImpl();
		ProfilerResult results = service.get(paramsBuilder == null ? null : paramsBuilder.build());
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.methods);
		Assert.assertTrue(results.methods.size() >= EXPECTED.size());
		MethodResult last = null;
		for (MethodResult method : results.methods) {
			if (last != null)
				Assert.assertTrue(comparator.compare(method, last) >= 0);
			last = method;
		}
	}

	@Test
	public void test150sort() {
		checkSorting(ProfilerServiceInterface.Parameters.of().sort(ProfilerServiceInterface.SortBy.invocations),
				(o1, o2) -> Integer.compare(o2.invocations, o1.invocations));
		checkSorting(ProfilerServiceInterface.Parameters.of().sort(ProfilerServiceInterface.SortBy.mean_time),
				(o1, o2) -> Long.compare(o2.mean_time, o1.mean_time));
		checkSorting(ProfilerServiceInterface.Parameters.of().sort(ProfilerServiceInterface.SortBy.total_time),
				(o1, o2) -> Long.compare(o2.total_time, o1.total_time));
		checkSorting(ProfilerServiceInterface.Parameters.of().sort(ProfilerServiceInterface.SortBy.method),
				(o1, o2) -> o1.method.compareTo(o2.method));
		checkSorting(null,
				(o1, o2) -> o1.method.compareTo(o2.method));
	}

	private final void checkFiltering(final ProfilerServiceInterface.Parameters.Builder paramsBuilder,
			final Function<MethodResult, Boolean> checker) {
		final ProfilerServiceInterface service = new ProfilerServiceImpl();
		ProfilerResult results = service.get(paramsBuilder.build());
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.methods);
		Assert.assertTrue(results.methods.size() >= 0);
		for (MethodResult method : results.methods)
			Assert.assertTrue(method.toString(), checker.apply(method));
	}

	@Test
	public void test180filter() {
		checkFiltering(ProfilerServiceInterface.Parameters.of().invocations(80), res -> res.invocations >= 80);
		checkFiltering(ProfilerServiceInterface.Parameters.of().meanTime(10L), res -> res.mean_time >= 10);
		checkFiltering(ProfilerServiceInterface.Parameters.of().totalTime(1000L), res -> res.total_time >= 1000);
	}

	@Test
	public void test200dumpSize() {
		Assert.assertTrue(ProfilerManager.dump() >= EXPECTED.size());
		Assert.assertTrue(ProfilerManager.size() >= EXPECTED.size());
	}

}
