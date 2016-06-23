/**
 * Copyright 2016 Emmanuel Keller / QWAZR
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
package com.qwazr.scheduler.test;

import com.qwazr.scheduler.SchedulerServiceImpl;
import com.qwazr.scheduler.SchedulerServiceInterface;
import com.qwazr.scheduler.SchedulerStatus;
import com.qwazr.scripts.ScriptRunStatus;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.servlet.ServletException;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FullTest {

	@Test
	public void test000StartServer()
			throws Exception {
		TestServer.startServer();
		Assert.assertTrue(TestServer.serverStarted);
	}

	private final static String[] TASK_NAME =
			{"TaskRunnable", "TaskScript", "TaskJS", "TaskRunnableError", "TaskScriptError"};
	private final static String[] SCRIPT_PATHS =
			{"com.qwazr.scheduler.test.TaskRunnable", "com.qwazr.scheduler.test.TaskScript", "js/task.js",
					"com.qwazr.scheduler.test.TaskRunnableError", "com.qwazr.scheduler.test.TaskScript"};

	private final static String DUMMY_TASK_NAME = "DUMMY_TASK";

	@Test
	public void test100getSchedulers() {
		TreeMap<String, String> list = new SchedulerServiceImpl().list();
		Assert.assertNotNull(list);
		Assert.assertEquals(TASK_NAME.length, list.size());
		for (String task : TASK_NAME)
			Assert.assertTrue(list.containsKey(task));
	}

	private void checkErrorStatusCode(Runnable runnable, int expectedStatusCode) {
		try {
			runnable.run();
			Assert.fail("WebApplicationException was not thrown");
		} catch (WebApplicationException e) {
			Assert.assertEquals(expectedStatusCode, e.getResponse().getStatus());
		}
	}

	@Test
	public void test200getScheduler() {
		final SchedulerServiceInterface client = new SchedulerServiceImpl();
		checkErrorStatusCode(() -> client.get(DUMMY_TASK_NAME, null), 404);
		int i = 0;
		for (String task : TASK_NAME) {
			final SchedulerStatus status = client.get(task, null);
			Assert.assertNotNull(status);
			Assert.assertEquals(SCRIPT_PATHS[i++], status.script_path);
		}
	}

	@Test
	public void test300waitExecute() throws InterruptedException {
		final SchedulerServiceInterface client = new SchedulerServiceImpl();
		final long timeOut = System.currentTimeMillis() + 1000 * 120;
		final Set<String> found = new HashSet<>();
		while (System.currentTimeMillis() < timeOut && found.size() < TASK_NAME.length) {
			Thread.sleep(1000);
			for (String task : TASK_NAME) {
				final SchedulerStatus schedulerStatus = client.get(task, null);
				if (schedulerStatus.script_status != null && !schedulerStatus.script_status.isEmpty())
					for (ScriptRunStatus status : schedulerStatus.script_status)
						found.add(task);
			}
		}
		Assert.assertTrue(TaskRunnable.EXECUTION_COUNT.get() > 0);
		Assert.assertTrue(TaskScript.EXECUTION_COUNT.get() > 0);
		Assert.assertEquals(TASK_NAME.length, found.size());
	}
}
