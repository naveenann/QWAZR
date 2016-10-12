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

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ProfiledClass {

	final public static AtomicInteger constructorCount = new AtomicInteger();
	final public static AtomicInteger testCount = new AtomicInteger();
	final public static AtomicLong testTime = new AtomicLong();
	final public static AtomicInteger testParamCount = new AtomicInteger();
	final public static AtomicLong testParamTime = new AtomicLong();
	final public static AtomicInteger testExCount = new AtomicInteger();
	final public static AtomicLong testExTime = new AtomicLong();

	public ProfiledClass() {
		constructorCount.incrementAndGet();
	}

	private static void wait(final AtomicInteger count, final AtomicLong time, final int waitBase)
			throws InterruptedException {
		count.incrementAndGet();
		long ms = System.currentTimeMillis();
		Thread.sleep(waitBase + new Random().nextInt(waitBase));
		time.addAndGet(System.currentTimeMillis() - ms);
	}

	public void test() throws InterruptedException {
		wait(testCount, testTime, 250);
	}

	public void test(int waitBase) throws InterruptedException {
		wait(testParamCount, testParamTime, waitBase);
	}

	public void testEx() throws InterruptedException {
		wait(testExCount, testExTime, 250);
		throw new InterruptedException();
	}

	public static class InnerClass {

		final public static AtomicInteger testCount = new AtomicInteger();

		public void test() {
			testCount.incrementAndGet();
		}
	}
}
