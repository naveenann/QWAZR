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
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.LinkedHashMap;

public class ProfilerManager {

	final private static Logger LOGGER = LoggerFactory.getLogger(ProfilerManager.class);

	final private static PatriciaTrie<Integer> classMethodMap = new PatriciaTrie<>();

	private static int idSequence = 0;
	private static int[] callCountArray;
	private static long[] totalTimeArray;

	public static void premain(final String agentArgs, final Instrumentation inst) {

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

	final synchronized static int register(final String name) {
		Integer id = classMethodMap.get(name);
		if (id != null)
			return id;
		id = idSequence++;
		classMethodMap.put(name, id);
		checkArrays(idSequence);
		return id;
	}

	final static public void methodCalled(final String key, final int methodId, final long startTime) {
		final long t = (System.nanoTime() - startTime) / 1000000;
		synchronized (key) {
			callCountArray[methodId]++;
			totalTimeArray[methodId] += t;
		}
	}

	final static public void dump() {
		classMethodMap.forEach((methodKey, methodId) -> {
			if (callCountArray[methodId] == 0)
				return;
			System.out.println(methodKey + " => " + callCountArray[methodId] + " - " + totalTimeArray[methodId] + " - "
					+ totalTimeArray[methodId] / callCountArray[methodId]);
		});
	}

	final static public Map<String, MethodResult> getMethods(final String prefixKey, Integer start, Integer rows) {
		final SortedMap<String, Integer> subMap = classMethodMap.prefixMap(prefixKey);
		final Iterator<Map.Entry<String, Integer>> iterator = subMap.entrySet().iterator();
		if (start != null)
			while (start-- > 0 && iterator.hasNext())
				iterator.next();
		if (rows == null)
			rows = 100;
		final Map<String, MethodResult> results = new LinkedHashMap();
		while (rows-- > 0 && iterator.hasNext()) {
			final Map.Entry<String, Integer> entry = iterator.next();
			final int pos = entry.getValue();
			final int count = callCountArray[pos];
			if (count > 0)
				results.put(entry.getKey(), new MethodResult(count, totalTimeArray[pos]));
		}
		return results;
	}

}
