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
package com.qwazr.connectors.test;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.qwazr.connectors.ConnectorManager;

public class ConnectorsTestContext {

	static synchronized ConnectorManager getConnectorManager()
			throws JsonParseException, JsonMappingException, IOException {
		if (ConnectorManager.INSTANCE != null)
			return ConnectorManager.INSTANCE;
		ConnectorManager.load(new File("src/test/resources"), null);
		return ConnectorManager.INSTANCE;
	}

}