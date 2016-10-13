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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collection;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ProfilerResult {

	public final Integer instrumented_methods;
	public final Collection<MethodResult> methods;

	public ProfilerResult() {
		instrumented_methods = null;
		methods = null;
	}

	ProfilerResult(final int instrumentedMethods, final Collection<MethodResult> methods) {
		this.instrumented_methods = instrumentedMethods;
		this.methods = methods;
	}

}
