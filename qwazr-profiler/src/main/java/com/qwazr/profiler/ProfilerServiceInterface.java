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
package com.qwazr.profiler;

import com.qwazr.utils.server.ServiceInterface;
import com.qwazr.utils.server.ServiceName;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;

@RolesAllowed(ProfilerServiceInterface.SERVICE_NAME)
@Path("/profiler")
@ServiceName(ProfilerServiceInterface.SERVICE_NAME)
public interface ProfilerServiceInterface extends ServiceInterface {

	enum SortBy {
		invocations, total_time, mean_time, method;
	}

	String SERVICE_NAME = "profiler";

	@GET
	@Path("/")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	ProfilerResult get(@BeanParam Parameters params);

	@GET
	@Path("/{prefix : .+}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	ProfilerResult getPrefix(@BeanParam Parameters params);

	class Parameters {

		@PathParam("prefix")
		String prefix;

		@QueryParam("start")
		Integer start;

		@QueryParam("rows")
		Integer rows;

		@QueryParam("sort")
		SortBy sort;

		@QueryParam("invocations")
		Integer invocations;

		@QueryParam("invocations")
		Long total_time;

		@QueryParam("invocations")
		Long mean_time;


		public Parameters() {
			prefix = null;
			start = null;
			rows = null;
			sort = null;
			invocations = null;
			total_time = null;
			mean_time = null;
		}

		Parameters(final Builder builder) {
			prefix = builder.prefix;
			start = builder.start;
			rows = builder.rows;
			sort = builder.sort;
			invocations = builder.invocations;
			total_time = builder.totalTime;
			mean_time = builder.meanTime;
		}

		static public Builder of() {
			return new Builder();
		}

		static public Builder of(final String prefix) {
			return new Builder().prefix(prefix);
		}

		static public class Builder {

			String prefix;
			Integer start;
			Integer rows;
			SortBy sort;
			Integer invocations;
			Long totalTime;
			Long meanTime;

			public Parameters build() {
				return new Parameters(this);
			}

			public Builder prefix(final String prefix) {
				this.prefix = prefix;
				return this;
			}

			public Builder start(final Integer start) {
				this.start = start;
				return this;
			}

			public Builder rows(final Integer rows) {
				this.rows = rows;
				return this;
			}

			public Builder sort(final SortBy sort) {
				this.sort = sort;
				return this;
			}

			public Builder invocations(final Integer invocations) {
				this.invocations = invocations;
				return this;
			}

			public Builder totalTime(final Long totalTime) {
				this.totalTime = totalTime;
				return this;
			}

			public Builder meanTime(final Long meanTime) {
				this.meanTime = meanTime;
				return this;
			}

		}
	}
}
