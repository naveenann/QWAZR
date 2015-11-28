/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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
package com.qwazr.connectors;

import com.qwazr.utils.server.ServerException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConnectorsServiceImpl implements ConnectorsServiceInterface {

	public Map<String, String> list() {
		Map<String, String> tools = new LinkedHashMap<String, String>();
		ConnectorManagerImpl.getInstance()
						.forEach((s, abstractTool) -> tools.put(s, abstractTool.getClass().getName()));
		return tools;
	}

	public AbstractConnector get(String connectorName) {
		try {
			return ConnectorManagerImpl.getInstance().get(connectorName);
		} catch (IOException e) {
			throw ServerException.getJsonException(e);
		}
	}

}
