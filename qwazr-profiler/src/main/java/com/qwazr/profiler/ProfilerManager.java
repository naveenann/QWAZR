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
package com.qwazr.profiler;

import com.ea.agentloader.AgentLoader;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.WildcardMatcher;
import com.qwazr.utils.server.ServerBuilder;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class ProfilerManager {

	final private static Logger LOGGER = LoggerFactory.getLogger(ProfilerManager.class);

	final private static PatriciaTrie<Integer> classMethodMap = new PatriciaTrie<>();

	private static int idSequence = 0;
	private static int[] callCountArray = null;
	private static long[] totalTimeArray = null;

	private static volatile boolean initialized = false;

	private static final long zeroNano = System.nanoTime();

	final public static String VARIABLE_NAME = "QWAZR_PROFILERS";

	private static synchronized void main(String agentArgs, final Instrumentation inst) {
		if (agentArgs == null || agentArgs.isEmpty())
			agentArgs = System.getenv(VARIABLE_NAME);
		if (agentArgs == null || agentArgs.isEmpty())
			agentArgs = System.getProperty(VARIABLE_NAME);
		if (agentArgs == null || agentArgs.isEmpty()) {
			LOGGER.info("Profiler not initialized. No argument provided.");
			return;
		}
		initialized = true;
		LOGGER.info("Profiler initialization: " + agentArgs);
		final String[] matchers = StringUtils.split(agentArgs, ';');
		final WildcardMatcher[] wildcardMatchers = new WildcardMatcher[matchers.length];
		int i = 0;
		for (String matcher : matchers)
			wildcardMatchers[i++] = new WildcardMatcher(matcher);

		inst.addTransformer(new ProfilerTransformer(wildcardMatchers));
		Runtime.getRuntime().addShutdownHook(new Thread(ProfilerManager::dump));
	}

	public static void premain(final String agentArgs, final Instrumentation inst) {
		main(agentArgs, inst);
	}

	public static void agentmain(final String agentArgs, final Instrumentation inst) {
		main(agentArgs, inst);
	}

	static private void checkArrays(int size) {
		if (callCountArray == null || callCountArray.length < size) {
			callCountArray = new int[size + 999];
			if (LOGGER.isInfoEnabled())
				LOGGER.info("Profiler method buffer size: " + callCountArray.length);
		}
		if (totalTimeArray == null || totalTimeArray.length < size)
			totalTimeArray = new long[size + 999];
	}

	public static void load(final ServerBuilder serverBuilder) {
		if (!isInitialized())
			AgentLoader.loadAgentClass(ProfilerManager.class.getName(), null);

		if (serverBuilder != null)
			serverBuilder.registerWebService(ProfilerServiceImpl.class);
	}

	final static public boolean isInitialized() {
		return initialized;
	}

	final synchronized static int register(final String name) {
		synchronized (classMethodMap) {
			Integer id = classMethodMap.get(name);
			if (id != null)
				return id;
			id = idSequence++;
			classMethodMap.put(name, id);
			checkArrays(idSequence);
			if (LOGGER.isTraceEnabled())
				LOGGER.trace("Profiler register method: " + name);
			return id;
		}
	}

	final static public void methodEnter(final String key, final int methodId) {
		final long t = System.nanoTime() - zeroNano;
		synchronized (key) {
			totalTimeArray[methodId] -= t;
		}
	}

	final static public void methodExit(final String key, final int methodId) {
		final long t = System.nanoTime() - zeroNano;
		synchronized (key) {
			callCountArray[methodId]++;
			totalTimeArray[methodId] += t;
		}
	}

	final static public int dump() {
		final AtomicInteger count = new AtomicInteger();
		synchronized (classMethodMap) {
			classMethodMap.forEach((methodKey, methodId) -> {
				if (callCountArray[methodId] == 0)
					return;
				count.incrementAndGet();
				if (LOGGER.isInfoEnabled())
					LOGGER.info(
							methodKey + " => " + callCountArray[methodId] + " - " +
									(totalTimeArray[methodId]) / 1000000 + " - " +
									(totalTimeArray[methodId] / callCountArray[methodId]) / 1000000);
			});
		}
		return count.get();
	}

	static private ProfilerResult getMethods(final Set<String> keys,
			final ProfilerServiceInterface.Parameters params) {

		final List<MethodResult> results = new ArrayList<>();

		// Filtering
		final List<Function<MethodResult, Boolean>> checkers = new ArrayList<>();
		if (params != null) {
			if (params.invocations != null)
				checkers.add(result -> result.invocations >= params.invocations);
			if (params.total_time != null)
				checkers.add(result -> result.total_time >= params.total_time);
			if (params.mean_time != null)
				checkers.add(result -> result.mean_time >= params.mean_time);
		}

		// Extract counters
		int count = 0;
		for (String key : keys) {
			final Integer pos = classMethodMap.get(key);
			synchronized (pos) {
				final MethodResult methodResult = new MethodResult(key, callCountArray[pos], totalTimeArray[pos]);
				int checked = 0;
				for (Function<MethodResult, Boolean> checker : checkers)
					if (checker.apply(methodResult))
						checked++;
				if (checkers.size() == checked) {
					results.add(methodResult);
					count++;
				}
			}
		}

		// Sorting
		final ProfilerServiceInterface.SortBy sort =
				params == null || params.sort == null ? ProfilerServiceInterface.SortBy.method : params.sort;
		switch (sort)

		{
			case invocations:
				results.sort((o1, o2) -> Integer.compare(o2.invocations, o1.invocations));
				break;
			case mean_time:
				results.sort((o1, o2) -> Long.compare(o2.mean_time, o1.mean_time));
				break;
			case total_time:
				results.sort((o1, o2) -> Long.compare(o2.total_time, o1.total_time));
				break;
			default:
			case method:
				results.sort((o1, o2) -> o1.method.compareTo(o2.method));
				break;
		}

		// Paging
		int start = params == null || params.start == null ? 0 : params.start;
		if (start >= count)
			return new ProfilerResult(count, null);
		int rows = params == null || params.rows == null ? 100 : params.rows;
		if (rows > count - start)
			rows = count - start;
		if (rows <= 0)
			return new ProfilerResult(count, null);

		return new ProfilerResult(count, results.subList(start, start + rows));
	}

	final static public ProfilerResult getMethods(final ProfilerServiceInterface.Parameters params) {
		synchronized (classMethodMap) {
			final Set<String> keySet = params == null || StringUtils.isEmpty(params.prefix) ? classMethodMap.keySet() :
					classMethodMap.prefixMap(params.prefix).keySet();
			return getMethods(keySet, params);
		}
	}

	final public static int size() {
		synchronized (classMethodMap) {
			return classMethodMap.size();
		}
	}
}
