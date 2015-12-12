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
package com.qwazr.webapps.exception;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

public class WebappRedirectException extends AbstractWebappException {

	/**
	 *
	 */
	private static final long serialVersionUID = -9197281733923358011L;

	private final String location;

	public WebappRedirectException(String location) {
		this.location = location;
	}

	@Override
	public void sendQuietly(HttpServletResponse response) {
		try {
			response.sendRedirect(location);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}