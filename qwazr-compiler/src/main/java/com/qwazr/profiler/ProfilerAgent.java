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

import com.qwazr.utils.WildcardMatcher;
import javassist.NotFoundException;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedSummaryStatistics;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.ConcurrentHashMap;

public class ProfilerAgent {

	final private static ConcurrentHashMap<String, SummaryStatistics> classMap = new ConcurrentHashMap<>();

	public static void premain(final String agentArgs, final Instrumentation inst) {
		try {
			inst.addTransformer(new TimeCollectorTransformer(new WildcardMatcher("com/qwazr/*")));
		} catch (NotFoundException e) {
			throw new IllegalStateException(e);
		}
	}


	final public static class TimeCollector {

		private final long start;
		private final SummaryStatistics stats;

		private TimeCollector(final String classNameMethod) {
			start = System.currentTimeMillis();
			stats = classMap.computeIfAbsent(classNameMethod, s -> new SynchronizedSummaryStatistics());
		}

		final public void exit() {
			stats.addValue(System.currentTimeMillis() - start);
		}

	}

	final static public TimeCollector enter(final String classMethod) {
		return new TimeCollector(classMethod);
	}

	final static public ConcurrentHashMap.KeySetView<String, SummaryStatistics> getKeys() {
		return classMap.keySet();

	}

}
