/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.dashboard;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AsyncDemoControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void throughTheProxyReturnsBeforeTheReminder() throws Exception {
		this.mockMvc.perform(get("/api/dashboard/async").param("proxy", "true"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.calledClass").value(containsString("SpringCGLIB")))
			.andExpect(jsonPath("$.millis").value(lessThan(1000)));
	}

	@Test
	void withoutTheProxyWaitsForTheReminder() throws Exception {
		this.mockMvc.perform(get("/api/dashboard/async").param("proxy", "false"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.calledClass").value("VisitReminderListener"))
			.andExpect(jsonPath("$.millis").value(greaterThanOrEqualTo(2000)));
	}

}
