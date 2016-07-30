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
package com.qwazr;

import org.aeonbits.owner.Config;

public interface QwazrConfigurationProperties extends Config {

	String QWAZR_PROPERTIES = "QWAZR_PROPERTIES";

	@Key("QWAZR_MASTERS")
	String qwazrMasters();

	@Key("QWAZR_SERVICES")
	String qwazrServices();

	@Key("QWAZR_GROUPS")
	String qwazrGroups();

	@Key("QWAZR_ETC")
	String qwazrEtc();

	@Key("QWAZR_SCHEDULER_MAX_THREADS")
	@DefaultValue("100")
	int qwazrSchedulerMaxThreads();

	@Config.Key("QWAZR_PROFILERS")
	String qwazrProfilers();
}
