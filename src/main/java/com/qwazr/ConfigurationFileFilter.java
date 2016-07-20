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
 **/
package com.qwazr;

import com.qwazr.utils.WildcardMatcher;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationFileFilter implements IOFileFilter {

	private final List<WildcardMatcher> positives;
	private final List<WildcardMatcher> negatives;

	ConfigurationFileFilter(final String[] patternArray) {
		positives = new ArrayList<>();
		negatives = new ArrayList<>();
		if (patternArray == null)
			return;
		for (final String pattern : patternArray) {
			if (pattern.startsWith("~"))
				negatives.add(new WildcardMatcher(pattern.substring(1)));
			else
				positives.add(new WildcardMatcher(pattern));
		}
	}

	@Override
	final public boolean accept(final File pathname) {
		return accept(pathname.getParentFile(), pathname.getName());
	}

	@Override
	final public boolean accept(final File dir, final String name) {
		if (positives.isEmpty() && negatives.isEmpty())
			return false;
		for (WildcardMatcher matcher : negatives)
			if (matcher.match(name))
				return false;
		if (positives.isEmpty())
			return true;
		for (WildcardMatcher matcher : positives)
			if (matcher.match(name))
				return true;
		return false;
	}
}
