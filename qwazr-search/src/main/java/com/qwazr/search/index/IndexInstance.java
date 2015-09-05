/**
 * Copyright 2015 Emmanuel Keller / QWAZR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.search.index;

import com.datastax.driver.core.utils.UUIDs;
import com.qwazr.search.SearchServer;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.json.JsonMapper;
import com.qwazr.utils.server.ServerException;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexInstance implements Closeable {

	private static final Logger logger = LoggerFactory
			.getLogger(IndexInstance.class);

	private final static String FIELD_ID_NAME = "_id";
	private final static String INDEX_DATA = "data";
	private final static String FIELDS_FILE = "fields.json";

	private final File indexDirectory;
	private final File dataDirectory;
	private final File fieldMapFile;
	private final Directory luceneDirectory;
	private final IndexWriterConfig indexWriterConfig;
	private final IndexWriter indexWriter;
	private final SearcherManager searcherManager;
	private volatile IndexSearcher indexSearcher;
	private volatile PerFieldAnalyzerWrapper perFieldAnalyzer;
	private volatile Map<String, FieldDefinition> fieldMap;

	/**
	 * Create an index directory
	 *
	 * @param indexDirectory the root location of the directory
	 * @throws IOException
	 * @throws ServerException
	 */
	IndexInstance(File indexDirectory)
			throws IOException, ServerException {

		this.indexDirectory = indexDirectory;
		dataDirectory = new File(indexDirectory, INDEX_DATA);
		SearchServer.checkDirectoryExists(indexDirectory);
		luceneDirectory = FSDirectory.open(dataDirectory.toPath());

		fieldMapFile = new File(indexDirectory, FIELDS_FILE);
		fieldMap = fieldMapFile.exists() ?
				JsonMapper.MAPPER.readValue(fieldMapFile, IndexSingleClient.MapStringFieldTypeRef) : null;
		perFieldAnalyzer = buildFieldAnalyzer(fieldMap);
		indexWriterConfig = new IndexWriterConfig(perFieldAnalyzer);
		indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		indexWriter = new IndexWriter(luceneDirectory, indexWriterConfig);
		searcherManager = new SearcherManager(indexWriter, true, null);
	}

	@Override
	public void close() {
		IOUtils.close(searcherManager);
		if (indexWriter.isOpen())
			IOUtils.close(indexWriter);
		IOUtils.close(luceneDirectory);
	}

	/**
	 * Delete the index. The directory is deleted from the local file system.
	 */
	void delete() {
		close();
		if (indexDirectory.exists())
			FileUtils.deleteQuietly(indexDirectory);
	}

	IndexStatus getStatus() throws IOException {
		indexSearcher = searcherManager.acquire();
		return new IndexStatus(indexSearcher.getIndexReader(), fieldMap);
	}


	private Class<?> findAnalyzer(String analyzer) throws ClassNotFoundException {
		try {
			return Class.forName(analyzer);
		} catch (ClassNotFoundException e1) {
			try {
				return Class.forName("org.apache.lucene.analysis." + analyzer);
			} catch (ClassNotFoundException e2) {
				throw e1;
			}
		}
	}

	private PerFieldAnalyzerWrapper buildFieldAnalyzer(Map<String, FieldDefinition> fields)
			throws ServerException {
		if (fields == null || fields.size() == 0)
			return new PerFieldAnalyzerWrapper(new StandardAnalyzer(CharArraySet.EMPTY_SET));
		Map<String, Analyzer> analyzerMap = new HashMap<String, Analyzer>();
		for (Map.Entry<String, FieldDefinition> field : fields.entrySet()) {
			String fieldName = field.getKey();
			FieldDefinition fieldDef = field.getValue();
			try {
				if (!StringUtils.isEmpty(fieldDef.analyzer))
					analyzerMap.put(field.getKey(), (Analyzer) findAnalyzer(fieldDef.analyzer).newInstance());
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				throw new ServerException(Response.Status.NOT_ACCEPTABLE,
						"Class " + fieldDef.analyzer + " not known for the field " + fieldName);
			}
		}
		return new PerFieldAnalyzerWrapper(new StandardAnalyzer(CharArraySet.EMPTY_SET), analyzerMap);
	}

	public synchronized void setFields(Map<String, FieldDefinition> fields) throws ServerException, IOException {
		perFieldAnalyzer = buildFieldAnalyzer(fields);
		JsonMapper.MAPPER.writeValue(fieldMapFile, fields);
		fieldMap = fields;
	}

	private BytesRef objectToBytesRef(Object object) throws IOException {
		if (object instanceof String)
			return new BytesRef((String) object);
		BytesRefBuilder bytesBuilder = new BytesRefBuilder();
		if (object instanceof Integer)
			NumericUtils.longToPrefixCodedBytes(((Integer) object).longValue(), 0, bytesBuilder);
		else if (object instanceof Double)
			NumericUtils.longToPrefixCodedBytes(NumericUtils.doubleToSortableLong((Double) object), 0, bytesBuilder);
		else throw new IOException("Type not supported: " + object.getClass());
		return bytesBuilder.get();
	}

	private void addValues(Document doc, String fieldName, Object object, Field.Store store) {
		if (object instanceof String) {
			doc.add(new StringField(fieldName, (String) object, store));
		} else if (object instanceof Integer) {
			doc.add(new LongField(fieldName, ((Integer) object).intValue(), store));
		} else if (object instanceof Double) {
			doc.add(new DoubleField(fieldName, ((Double) object).doubleValue(), store));
		} else if (object instanceof List) {
			List<Object> list = (List<Object>) object;
			for (Object o : list)
				addValues(doc, fieldName, o, store);
		}
	}

	private void addDocValues(Document doc, String fieldName, Object object, Field.Store store) {
		if (object instanceof String) {
			doc.add(new SortedDocValuesField(fieldName, new BytesRef((String) object)));
		} else if (object instanceof Integer) {
			doc.add(new NumericDocValuesField(fieldName, ((Integer) object).longValue()));
		} else if (object instanceof Double) {
			doc.add(new DoubleDocValuesField(fieldName, ((Double) object).doubleValue()));
		} else if (object instanceof List) {
			List<Object> list = (List<Object>) object;
			for (Object o : list) {
				doc.add(new SortedSetDocValuesField(fieldName, new BytesRef(o.toString())));
				if (store == Field.Store.YES)
					doc.add(new StoredField(fieldName, (String) object));
			}
		}
	}

	private void addAnalyzedValue(Document doc, String fieldName, Object object, Field.Store store) {
		if (object instanceof List) {
			List<Object> list = (List<Object>) object;
			for (Object o : list)
				doc.add(new TextField(fieldName, (String) o, store));
		} else {
			doc.add(new TextField(fieldName, object.toString(), store));
		}
	}

	private void addField(Document doc, String fieldName, FieldDefinition fieldDef, Object object) {
		if (object == null)
			return;
		boolean analyzer = fieldDef != null && fieldDef.analyzer != null;
		boolean stored = fieldDef != null && fieldDef.stored != null && fieldDef.stored;
		Field.Store store = stored ? Field.Store.YES : Field.Store.NO;
		boolean docValues = fieldDef != null && fieldDef.doc_values != null && fieldDef.doc_values;
		if (analyzer)
			addAnalyzedValue(doc, fieldName, object, store);
		else if (docValues)
			addDocValues(doc, fieldName, object, store);
		else
			addValues(doc, fieldName, object, store);
	}

	private Object addNewLuceneDocument(Map<String, Object> document) throws IOException {
		Document doc = new Document();

		Object id = document.get(FIELD_ID_NAME);
		Term termId;
		if (id == null) { // New UUID means document creation
			FieldDefinition fieldDef = fieldMap == null ? null : fieldMap.get(FIELD_ID_NAME);
			addField(doc, FIELD_ID_NAME, fieldDef, UUIDs.timeBased());
			termId = null;
		} else {
			BytesRef ref = objectToBytesRef(id);
			termId = new Term(FIELD_ID_NAME, ref);
		}

		for (Map.Entry<String, Object> field : document.entrySet()) {
			String fieldName = field.getKey();
			FieldDefinition fieldDef = fieldMap == null ? null : fieldMap.get(fieldName);
			addField(doc, fieldName, fieldDef, field.getValue());
		}

		if (termId == null)
			indexWriter.addDocument(doc);
		else
			indexWriter.updateDocument(termId, doc);
		return id;
	}

	public Object postDocument(Map<String, Object> document) throws IOException {
		Object id = addNewLuceneDocument(document);
		indexWriter.commit();
		searcherManager.maybeRefresh();
		return id;
	}

	public List<Object> postDocuments(List<Map<String, Object>> documents) throws IOException {
		if (documents == null) return null;
		List<Object> ids = new ArrayList<Object>(documents.size());
		for (Map<String, Object> document : documents)
			ids.add(addNewLuceneDocument(document));
		indexWriter.commit();
		searcherManager.maybeRefresh();
		return ids;
	}
}
