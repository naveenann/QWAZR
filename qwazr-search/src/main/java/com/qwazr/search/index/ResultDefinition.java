/**
 * Copyright 2015 Emmanuel Keller / QWAZR
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
package com.qwazr.search.index;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.TimeTracker;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;
import java.util.*;

@JsonInclude(Include.NON_EMPTY)
public class ResultDefinition {

	final public Map<String, Long> timer;
	final public Long total_hits;
	final public Float max_score;
	final public List<ResultDocument> documents;
	final public Map<String, Map<String, Number>> facets;
	final public String query;
	final public List<Function> functions;

	public static class Function extends QueryDefinition.Function {

		final public Object value;

		public Function() {
			value = null;
		}

		Function(FunctionCollector functionCollector) {
			super(functionCollector.function);
			this.value = functionCollector.getValue();
		}
	}

	public ResultDefinition() {
		this.timer = null;
		this.total_hits = null;
		this.documents = null;
		this.facets = null;
		this.functions = null;
		this.max_score = null;
		this.query = null;
	}

	private static String getQuery(Boolean queryDebug, String defaultField, Query query) {
		if (queryDebug == null || query == null)
			return null;
		if (!queryDebug && query != null)
			return null;
		return query.toString(defaultField == null ? StringUtils.EMPTY : defaultField);
	}

	ResultDefinition(Map<String, FieldDefinition> fieldMap, TimeTracker timeTracker, IndexSearcher searcher,
					int totalHits, TopDocs topDocs, QueryDefinition queryDef, Facets facets,
					Map<String, String[]> postingsHighlightsMap, Collection<FunctionCollector> functionsCollector,
					Query query) throws IOException {
		this.query = getQuery(queryDef.query_debug, queryDef.default_field, query);
		this.total_hits = (long) totalHits;
		max_score = topDocs != null ? topDocs.getMaxScore() : null;
		int pos = queryDef.start == null ? 0 : queryDef.start;
		int end = queryDef.getEnd();
		documents = new ArrayList<ResultDocument>();
		ScoreDoc[] docs = topDocs != null ? topDocs.scoreDocs : null;
		if (docs != null) {
			Map<String, DocValueUtils.DVConverter> docValuesSources = ResultUtils
							.extractDocValuesFields(fieldMap, searcher.getIndexReader(), queryDef.returned_fields);
			while (pos < total_hits && pos < end) {
				final ScoreDoc scoreDoc = docs[pos];
				final Document document = searcher.doc(scoreDoc.doc, queryDef.returned_fields);
				documents.add(new ResultDocument(pos, scoreDoc, document, queryDef.postings_highlighter,
								postingsHighlightsMap, docValuesSources));
				pos++;
			}
			timeTracker.next("returned_fields");
		}
		this.facets = facets != null && queryDef != null ? ResultUtils.buildFacets(queryDef.facets, facets) : null;
		this.functions = functionsCollector != null ? ResultUtils.buildFunctions(functionsCollector) : null;
		timeTracker.next("facet_fields");
		this.timer = timeTracker == null ? null : timeTracker.getMap();
	}

	ResultDefinition(Map<String, FieldDefinition> fieldMap, TimeTracker timeTracker, IndexSearcher searcher,
					TopDocs topDocs, MltQueryDefinition mltQueryDef, Query query) throws IOException {
		this.query = getQuery(mltQueryDef.query_debug, null, query);
		total_hits = (long) topDocs.totalHits;
		max_score = topDocs.getMaxScore();
		int pos = mltQueryDef.start == null ? 0 : mltQueryDef.start;
		int end = mltQueryDef.getEnd();
		documents = new ArrayList<ResultDocument>();
		ScoreDoc[] docs = topDocs.scoreDocs;
		Map<String, DocValueUtils.DVConverter> docValuesSources = ResultUtils
						.extractDocValuesFields(fieldMap, searcher.getIndexReader(), mltQueryDef.returned_fields);
		while (pos < total_hits && pos < end) {
			final ScoreDoc scoreDoc = docs[pos];
			final Document document = searcher.doc(scoreDoc.doc, mltQueryDef.returned_fields);
			documents.add(new ResultDocument(pos, scoreDoc, document, null, null, docValuesSources));
			pos++;
		}
		timeTracker.next("returned_fields");
		this.facets = null;
		this.functions = null;
		this.timer = timeTracker == null ? null : timeTracker.getMap();
	}

	ResultDefinition(TimeTracker timeTracker) {
		query = null;
		total_hits = 0L;
		documents = Collections.emptyList();
		facets = null;
		functions = null;
		max_score = null;
		this.timer = timeTracker == null ? null : timeTracker.getMap();
	}

	ResultDefinition(long total_hits) {
		query = null;
		this.total_hits = total_hits;
		documents = Collections.emptyList();
		facets = null;
		functions = null;
		max_score = null;
		this.timer = null;
	}

	public Long getTotal_hits() {
		return total_hits;
	}

	public Float getMax_score() {
		return max_score;
	}

	public List<ResultDocument> getDocuments() {
		return documents;
	}

	public Map<String, Map<String, Number>> getFacets() {
		return facets;
	}

	public Map<String, Long> getTimer() {
		return timer;
	}

	public String getQuery() {
		return query;
	}
}
