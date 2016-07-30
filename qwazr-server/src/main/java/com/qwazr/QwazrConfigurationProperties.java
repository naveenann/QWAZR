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

import com.qwazr.utils.server.ConfigurationProperties;
import org.aeonbits.owner.Config;

public interface QwazrConfigurationProperties extends ConfigurationProperties {
	
	@Config.Key("QWAZR_MASTERS")
	String qwazrMasters();

	@Config.Key("QWAZR_SERVICES")
	String qwazrServices();

	@Config.Key("QWAZR_GROUPS")
	String qwazrGroups();

	@Config.Key("QWAZR_ETC")
	String qwazrEtc();

	@Config.Key("QWAZR_SCHEDULER_MAX_THREADS")
	@Config.DefaultValue("100")
	int qwazrSchedulerMaxThreads();

	@Config.Key("QWAZR_PROFILERS")
	String qwazrProfilers();
}
