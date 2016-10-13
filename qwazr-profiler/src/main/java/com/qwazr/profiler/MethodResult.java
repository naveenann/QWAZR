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

	final public String method;
	final public int invocations;
	final public long total_time;
	final public long mean_time;

	public MethodResult() {
		method = null;
		invocations = 0;
		total_time = 0;
		mean_time = 0;
	}

	MethodResult(final String method, final int invocations, final long totalTime) {
		this.method = method;
		this.invocations = invocations;
		this.total_time = totalTime;
		this.mean_time = totalTime == 0 || invocations == 0 ? 0 : totalTime / invocations;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("method: ");
		sb.append(method);
		sb.append(" - invocations: ");
		sb.append(invocations);
		sb.append(" - total time: ");
		sb.append(total_time);
		sb.append(" - mean time: ");
		sb.append(mean_time);
		return sb.toString();
	}
}
