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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DatabaseDemoControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PetTypeRepository petTypes;

	@Test
	void countsRowsOfEachTable() throws Exception {
		this.mockMvc.perform(get("/api/dashboard/database"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.table == 'types')].rows").value(hasItem(6)));
	}

	@Test
	void showsTheSqlOfADerivedQuery() throws Exception {
		this.mockMvc.perform(get("/api/dashboard/sql").param("demo", "findByLastName"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.objects").value(2))
			.andExpect(jsonPath("$.sql[0]").value(containsString("like")))
			.andExpect(jsonPath("$.result[0]").value(containsString("Davis")));
	}

	@Test
	void rollbackDemoLeavesNoRow() throws Exception {
		long before = this.petTypes.count();

		this.mockMvc.perform(get("/api/dashboard/sql").param("demo", "rollback"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sql[0]").value(containsString("insert")))
			.andExpect(jsonPath("$.millis").value(greaterThan(-1)));

		assertThat(this.petTypes.count()).isEqualTo(before);
	}

	@Test
	void unknownDemoIsRejected() throws Exception {
		this.mockMvc.perform(get("/api/dashboard/sql").param("demo", "drop")).andExpect(status().isBadRequest());
	}

}
