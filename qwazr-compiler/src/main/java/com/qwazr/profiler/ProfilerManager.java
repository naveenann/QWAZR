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

import com.qwazr.utils.StringUtils;
import com.qwazr.utils.WildcardMatcher;
import com.qwazr.utils.server.ServerBuilder;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public class ProfilerManager {

	final private static Logger LOGGER = LoggerFactory.getLogger(ProfilerManager.class);

	final private static PatriciaTrie<Integer> classMethodMap = new PatriciaTrie<>();

	private static int idSequence = 0;
	private static int[] callCountArray = null;
	private static long[] totalTimeArray = null;

	private static volatile boolean initialized = false;

	public static void premain(final String agentArgs, final Instrumentation inst) {

		initialized = true;

		final String[] matchers = StringUtils.split(agentArgs, ';');
		final WildcardMatcher[] wildcardMatchers = new WildcardMatcher[matchers.length];
		int i = 0;
		for (String matcher : matchers)
			wildcardMatchers[i++] = new WildcardMatcher(matcher);

		inst.addTransformer(new ProfilerTransformer(wildcardMatchers));
		Runtime.getRuntime().addShutdownHook(new Thread(ProfilerManager::dump));
	}

	static private void checkArrays(int size) {
		if (callCountArray == null || callCountArray.length < size) {
			callCountArray = new int[size + 1000];
			if (LOGGER.isInfoEnabled())
				LOGGER.info("Profiler method buffer size: " + callCountArray.length);
		}
		if (totalTimeArray == null || totalTimeArray.length < size)
			totalTimeArray = new long[size + 1000];
	}

	public static void load(final ServerBuilder serverBuilder) {
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
			return id;
		}
	}

	final static public void methodCalled(final String key, final int methodId, final long startTime) {
		final long t = (System.nanoTime() - startTime) / 1000000;
		synchronized (key) {
			callCountArray[methodId]++;
			totalTimeArray[methodId] += t;
		}
	}

	final static public void dump() {
		synchronized (classMethodMap) {
			classMethodMap.forEach((methodKey, methodId) -> {
				if (callCountArray[methodId] == 0)
					return;
				System.out.println(
						methodKey + " => " + callCountArray[methodId] + " - " + totalTimeArray[methodId] + " - "
								+ totalTimeArray[methodId] / callCountArray[methodId]);
			});
		}
	}

	final static public Map<String, MethodResult> getMethods(final String prefixKey, Integer start, Integer rows) {
		synchronized (classMethodMap) {
			final SortedMap<String, Integer> prefixMap = classMethodMap.prefixMap(prefixKey);
			final Map<String, MethodResult> results = new LinkedHashMap();

			if (prefixMap.size() == 0)
				return results;

			if (start == null)
				start = 0;
			if (rows == null)
				rows = 100;

			final Set<String> keySet = prefixMap.keySet();
			final String[] keys = keySet.toArray(new String[keySet.size()]);
			for (String key : keys) {
				if (start > 0) {
					start--;
					continue;
				}
				if (rows <= 0)
					break;
				rows--;
				final Integer pos = classMethodMap.get(key);
				synchronized (pos) {
					final int count = callCountArray[pos];
					if (count > 0)
						results.put(key, new MethodResult(count, totalTimeArray[pos]));
				}
			}
			return results;
		}
	}

	public final static int size() {
		return classMethodMap.size();
	}
}
