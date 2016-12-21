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
package com.qwazr.server.test;

import com.qwazr.Qwazr;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServerTest {

	@Test
	public void test100start() throws IOException {
		Qwazr.main(new String[] { "start" });
		Assert.assertNotNull(Qwazr.getInstance().getClassLoaderManager());
		Assert.assertNotNull(Qwazr.getInstance().getClusterService());
		Assert.assertNotNull(Qwazr.getInstance().getCompilerService());
		Assert.assertNotNull(Qwazr.getInstance().getExtractorService());
		Assert.assertNotNull(Qwazr.getInstance().getGraphService());
		Assert.assertNotNull(Qwazr.getInstance().getIndexService());
		Assert.assertNotNull(Qwazr.getInstance().getLibraryManager());
		Assert.assertNotNull(Qwazr.getInstance().getSchedulerService());
		Assert.assertNotNull(Qwazr.getInstance().getScriptService());
		Assert.assertNotNull(Qwazr.getInstance().getStoreService());
		Assert.assertNotNull(Qwazr.getInstance().getTableService());
		Assert.assertNotNull(Qwazr.getInstance().getWebCrawlerService());
		Assert.assertNotNull(Qwazr.getInstance().getWebappService());
	}

	@Test
	public void test900stop() throws IOException {
		// First stop
		Qwazr.shutdown();
		// Second one
		Qwazr.shutdown();
	}
}
