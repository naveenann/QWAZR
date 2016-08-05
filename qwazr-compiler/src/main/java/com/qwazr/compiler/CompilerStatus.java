/**
 * Copyright 2016 Emmanuel Keller / QWAZR
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
 **/

package com.qwazr.compiler;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.tools.Diagnostic;
import java.net.URI;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CompilerStatus {

	public final SortedMap<URI, Date> compilables;
	public final SortedMap<URI, DiagnosticStatus> diagnostics;
	public final SortedSet<String> classPath;

	public CompilerStatus() {
		compilables = null;
		diagnostics = null;
		classPath = null;
	}

	public CompilerStatus(final Map<URI, Date> compilables, final Map<URI, DiagnosticStatus> diagnostics,
			final LinkedHashSet<String> classPath) {
		this.compilables = new TreeMap<>(compilables);
		this.diagnostics = new TreeMap<>(diagnostics);
		this.classPath = classPath == null ? null : new TreeSet<>(classPath);
	}

	public static class DiagnosticStatus {

		public final Date date;
		public final String code;
		public final Diagnostic.Kind kind;
		public final Long lineNumber;
		public final Long columnNumber;
		public final String message;

		public DiagnosticStatus() {
			date = null;
			code = null;
			kind = null;
			lineNumber = null;
			columnNumber = null;
			message = null;
		}

		public DiagnosticStatus(final Date date, final Diagnostic<?> diagnostic) {
			this.date = date;
			this.code = diagnostic.getCode();
			this.kind = diagnostic.getKind();
			this.lineNumber = diagnostic.getLineNumber();
			this.columnNumber = diagnostic.getColumnNumber();
			this.message = diagnostic.getMessage(Locale.getDefault());
		}

	}
}
