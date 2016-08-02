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

public class MethodResult {

	final public int invocations;
	final public long total_time;
	final public long mean_time;

	public MethodResult() {
		invocations = 0;
		total_time = 0;
		mean_time = 0;
	}

	public MethodResult(int invocations, long totalTime) {
		this.invocations = invocations;
		this.total_time = totalTime;
		this.mean_time = totalTime == 0 || invocations == 0 ? 0 : totalTime / invocations;
	}
}
