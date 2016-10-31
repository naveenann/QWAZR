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

import com.qwazr.profiler.ProfilerManager;

public class ProfiledClassTemplate {

	public double[] test() throws InterruptedException {
		ProfilerManager.methodEnter("methodKey", 5678);
		final double[] d = new double[]{1, 2, 3};
		ProfilerManager.methodExit("methodKey", 5678);
		return d;
	}
}
