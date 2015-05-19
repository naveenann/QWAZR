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
package com.qwazr.crawler.web.service;

import java.net.URISyntaxException;
import java.util.TreeMap;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.qwazr.crawler.web.manager.WebCrawlerManager;
import com.qwazr.utils.server.ServerException;

public class WebCrawlerServiceImpl implements WebCrawlerServiceInterface {

	@Override
	public TreeMap<String, WebCrawlStatus> getSessions(Boolean local) {

		// Read the sessions in the local node
		if (local != null && local)
			return WebCrawlerManager.INSTANCE.getSessions();

		// Read the sessions present in the remote nodes
		try {
			TreeMap<String, WebCrawlStatus> globalSessions = new TreeMap<String, WebCrawlStatus>();
			globalSessions.putAll(WebCrawlerManager.getClient().getSessions(
					false));
			return globalSessions;
		} catch (URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public WebCrawlStatus getSession(String session_name, Boolean local) {
		try {
			if (local != null && local) {
				WebCrawlStatus status = WebCrawlerManager.INSTANCE
						.getSession(session_name);
				if (status != null)
					return status;
				throw new ServerException(Status.NOT_FOUND, "Session not found");
			}
			return WebCrawlerManager.getClient()
					.getSession(session_name, false);
		} catch (URISyntaxException | ServerException e) {
			throw ServerException.getJsonException(e);
		}

	}

	@Override
	public Response abortSession(String session_name, Boolean local) {
		try {
			if (local != null && local) {
				WebCrawlerManager.INSTANCE.abortSession(session_name);
				return Response.accepted().build();
			}
			return WebCrawlerManager.getClient().abortSession(session_name,
					false);
		} catch (ServerException | URISyntaxException e) {
			throw ServerException.getTextException(e);
		}
	}

	@Override
	public WebCrawlStatus runSession(String session_name,
			WebCrawlDefinition crawlDefinition) {
		try {
			return WebCrawlerManager.INSTANCE.runSession(session_name,
					crawlDefinition);
		} catch (ServerException e) {
			throw ServerException.getTextException(e);
		}
	}
}