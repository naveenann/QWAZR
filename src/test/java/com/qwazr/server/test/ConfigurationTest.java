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

import com.qwazr.QwazrConfiguration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConfigurationTest {

	private static final File dataDir = new File("src/test/resources").getAbsoluteFile();
	private static final File etcDir = new File(dataDir, "etc").getAbsoluteFile();
	private static final File confDir = new File(dataDir, "conf").getAbsoluteFile();

	private static final Properties properties = new Properties();

	@BeforeClass
	public static void before() throws IOException {
		properties.put("QWAZR_MASTERS", "localhost:9191,localhost:9291");
		properties.put("WEBAPP_PORT", Integer.toString(9190));
		properties.put("WEBSERVICE_PORT", Integer.toString(9191));
		properties.put("QWAZR_DATA", dataDir.getAbsolutePath());
		properties.put("QWAZR_ETC_DIR", confDir.getAbsolutePath() + File.pathSeparator + etcDir.getAbsolutePath());

	}

	public QwazrConfiguration getConfig() throws IOException {
		File propFile = Files.createTempFile("qwazr-test", ".properties").toFile();
		try (final FileWriter writer = new FileWriter(propFile)) {
			properties.store(writer, null);
		}
		return new QwazrConfiguration("--QWAZR_PROPERTIES=" + propFile.getAbsolutePath());
	}

	@Test
	public void test005WebPort() throws IOException {
		final QwazrConfiguration configuration = getConfig();
		Assert.assertEquals(configuration.webAppConnector.port, 9190);
		Assert.assertEquals(configuration.webServiceConnector.port, 9191);
		Assert.assertTrue(configuration.masters.contains("localhost:9191"));
		Assert.assertTrue(configuration.masters.contains("localhost:9291"));
	}

	@Test
	public void test010NoFilter() throws IOException {
		final QwazrConfiguration configuration = getConfig();
		Assert.assertTrue(configuration.etcFileFilter.accept(new File(confDir, "conf_include.json")));
		Assert.assertTrue(configuration.etcFileFilter.accept(new File(etcDir, "conf_exclude.json")));
	}

	@Test
	public void test020WithEnvProperties() throws IOException {
		properties.put("QWAZR_ETC", "!*_exclude.json");
		final QwazrConfiguration configuration = getConfig();
		Assert.assertEquals(dataDir, configuration.dataDirectory);
		Assert.assertEquals(2, configuration.etcDirectories.size());
		Assert.assertTrue(configuration.etcDirectories.contains(etcDir));
		Assert.assertTrue(configuration.etcDirectories.contains(confDir));
	}

	@Test
	public void test030ExplicitInclusion() throws IOException {
		properties.put("QWAZR_ETC", "*_include.json");
		final QwazrConfiguration configuration = getConfig();
		Assert.assertTrue(configuration.etcFileFilter.accept(new File(confDir, "conf_include.json")));
		Assert.assertFalse(configuration.etcFileFilter.accept(new File(etcDir, "conf_exclude.json")));
	}

	@Test
	public void test040ExplicitExclusion() throws IOException {
		properties.put("QWAZR_ETC", "!*_exclude.json");
		final QwazrConfiguration configuration = getConfig();
		Assert.assertTrue(configuration.etcFileFilter.accept(new File(confDir, "conf_include.json")));
		Assert.assertFalse(configuration.etcFileFilter.accept(new File(etcDir, "conf_exclude.json")));
	}

	@Test
	public void test050BothInclusionExclusion() throws IOException {
		properties.put("QWAZR_ETC", "*_include.json,!*_exclude.json");
		final QwazrConfiguration configuration = getConfig();
		Assert.assertTrue(configuration.etcFileFilter.accept(new File(confDir, "conf_include.json")));
		Assert.assertFalse(configuration.etcFileFilter.accept(new File(etcDir, "conf_exclude.json")));
	}
}
