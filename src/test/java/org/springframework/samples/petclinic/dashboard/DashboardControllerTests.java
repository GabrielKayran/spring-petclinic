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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void dashboardPage() throws Exception {
		this.mockMvc.perform(get("/painel"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("id=\"dashboard\"")))
			.andExpect(content().string(containsString("/resources/js/dashboard.js?v=")));
	}

	@Test
	void environmentShowsJavaAndDatabase() throws Exception {
		this.mockMvc.perform(get("/api/dashboard/environment"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.java").value(containsString("21")))
			.andExpect(jsonPath("$.database").value("H2"))
			.andExpect(jsonPath("$.virtualThreads").value(true));
	}

	@Test
	void grpcCallReturnsVetsAndProtobufIsSmallerThanJson() throws Exception {
		this.mockMvc.perform(get("/api/dashboard/grpc"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.vets", hasSize(6)))
			.andExpect(jsonPath("$.protobufBytes", greaterThan(0)))
			.andExpect(jsonPath("$.protobufBytes", lessThan(200)));
	}

	@Test
	void petsShowTheChosenStrategy() throws Exception {
		// Pet 7 (Samantha) is a cat in the H2 seed data
		this.mockMvc.perform(get("/api/dashboard/pets"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.petId == 7)].strategy").value(hasItem("CatPriceStrategy")))
			.andExpect(jsonPath("$[?(@.petId == 7)].price").value(hasItem(90.0)));
	}

	@Test
	void aopListsProxiedControllers() throws Exception {
		this.mockMvc.perform(get("/api/dashboard/aop"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.bean == 'vetController')].proxy").value(hasItem(true)))
			.andExpect(jsonPath("$[?(@.bean == 'welcomeController')].proxy").value(hasItem(false)));
	}

	@Test
	void genericsShowTypeErasureAndSignatureTypes() throws Exception {
		this.mockMvc.perform(get("/api/dashboard/generics"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sameClass").value(true))
			.andExpect(jsonPath("$.repositories").value(hasItem("VetRepository extends Repository<Vet, Integer>")))
			.andExpect(jsonPath("$.genericInjection").value("VisitPriceService(Map<String, VisitPriceStrategy>)"))
			.andExpect(jsonPath("$.injectedStrategies").value(3));
	}

	@Test
	void umlDiagramsAreServed() throws Exception {
		this.mockMvc.perform(get("/uml/05_classes_strategy.svg")).andExpect(status().isOk());
	}

	@Test
	void benchmarkWithFewTasks() throws Exception {
		this.mockMvc.perform(get("/api/dashboard/benchmark").param("tasks", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tasks").value(10))
			.andExpect(jsonPath("$.virtualMs", greaterThan(900)));
	}

	@Test
	void liveConsoleReceivesAuditLogs() throws Exception {
		this.mockMvc.perform(get("/vets")).andExpect(status().isOk());

		this.mockMvc.perform(get("/api/dashboard/logs").param("after", "0"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("[AUDITORIA] VetController.showResourcesVetList()")));
	}

}
