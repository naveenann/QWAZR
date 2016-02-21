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
package com.qwazr.search.field;

import com.qwazr.search.index.QueryDefinition;
import org.apache.lucene.document.LongField;
import org.apache.lucene.search.SortField;

import java.util.Collection;

class LongFieldType extends StorableFieldType {

	LongFieldType(FieldDefinition fieldDefinition) {
		super(fieldDefinition);
	}

	@Override
	final public void fill(final String fieldName, final Object value, FieldConsumer consumer) {
		if (value instanceof Collection)
			fillCollection(fieldName, (Collection) value, consumer);
		else if (value instanceof Number)
			consumer.accept(new LongField(fieldName, (long) value, store));
		else
			consumer.accept(new LongField(fieldName, Long.parseLong(value.toString()), store));
	}

	@Override
	public final SortField getSortField(String fieldName, QueryDefinition.SortEnum sortEnum) {
		final SortField sortField = new SortField(fieldName, SortField.Type.LONG, FieldUtils.sortReverse(sortEnum));
		FieldUtils.sortLongMissingValue(sortEnum, sortField);
		return sortField;
	}

}
