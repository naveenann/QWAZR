/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.connectors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import com.qwazr.utils.StringUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MongoDbConnector extends AbstractConnector {

	private MongoClient mongoClient = null;

	public static class MongoDbCredential {
		public String username;
		public String password;
		public String database;
	}

	public List<MongoDbCredential> credentials;
	public String hostname;
	public Integer port;

	@Override
	public void load(String contextId) {
		ServerAddress serverAddress = null;
		if (!StringUtils.isEmpty(hostname)) {
			if (port == null)
				serverAddress = new ServerAddress(hostname);
			else
				serverAddress = new ServerAddress(hostname, port);
		} else
			serverAddress = new ServerAddress();
		if (credentials == null || credentials.isEmpty()) {
			mongoClient = new MongoClient(serverAddress);
		} else {
			List<MongoCredential> mongoCredentials = new ArrayList<MongoCredential>(
					credentials.size());
			for (MongoDbCredential credential : credentials)
				mongoCredentials.add(MongoCredential.createMongoCRCredential(
						credential.username, credential.database,
						credential.password.toCharArray()));
			mongoClient = new MongoClient(serverAddress, mongoCredentials);
		}

	}

	@Override
	public void unload(String contextId) {
		if (mongoClient != null) {
			mongoClient.close();
			mongoClient = null;
		}
	}

	/**
	 * Return a Mongo DB instance
	 * 
	 * @param databaseName
	 *            the name of the database
	 * @return a MongoDatabase object
	 * @throws IOException
	 *             if any I/O error occurs
	 */
	public MongoDatabase getDatabase(String databaseName) throws IOException {
		if (StringUtils.isEmpty(databaseName))
			throw new IOException("No database name.");
		return mongoClient.getDatabase(databaseName);
	}

	/**
	 * Returns a DB collection instance
	 * 
	 * @param db
	 *            a Mongo DB object
	 * @param collectionName
	 *            the name of the collection
	 * @return a DBCollection object
	 * @throws IOException
	 *             if any I/O error occurs
	 */
	public DBCollection getCollection(DB db, String collectionName)
			throws IOException {
		if (StringUtils.isEmpty(collectionName))
			throw new IOException("No collection name.");
		return db.getCollection(collectionName);
	}

	/**
	 * Build a DBObject from a JSON string
	 * 
	 * @param criteria
	 *            the JSON string
	 * @return a DBObject or NULL if criteria is empty
	 */
	public DBObject getDBObject(String criteria) {
		if (StringUtils.isEmpty(criteria))
			return null;
		return (DBObject) JSON.parse(criteria);
	}

}