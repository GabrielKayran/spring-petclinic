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
package org.springframework.samples.petclinic.painel;

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

/**
 * Painel: garante que a página e cada endpoint da API respondem com os dados das
 * extensões.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PainelControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void paginaDoPainel() throws Exception {
		this.mockMvc.perform(get("/painel"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("id=\"painel\"")))
			.andExpect(content().string(containsString("/resources/js/painel.js?v=")));
	}

	@Test
	void ambienteMostraJavaEBancoSemCredenciais() throws Exception {
		this.mockMvc.perform(get("/api/painel/ambiente"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.java").value(containsString("21")))
			.andExpect(jsonPath("$.banco").value("H2"))
			.andExpect(jsonPath("$.virtualThreads").value(true));
	}

	@Test
	void grpcFazChamadaRealEProtobufEhMenorQueJson() throws Exception {
		this.mockMvc.perform(get("/api/painel/grpc"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.veterinarios", hasSize(6)))
			.andExpect(jsonPath("$.bytesProtobuf", greaterThan(0)))
			.andExpect(jsonPath("$.bytesProtobuf", lessThan(200)));
	}

	@Test
	void petsMostramAEstrategiaEscolhida() throws Exception {
		// Pet 7 (Samantha) é um gato no data.sql do H2
		this.mockMvc.perform(get("/api/painel/pets"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.petId == 7)].estrategia").value(hasItem("PrecoConsultaGato")))
			.andExpect(jsonPath("$[?(@.petId == 7)].preco").value(hasItem(90.0)));
	}

	@Test
	void aopListaControllersQueViraramProxies() throws Exception {
		this.mockMvc.perform(get("/api/painel/aop"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.bean == 'vetController')].proxy").value(hasItem(true)))
			.andExpect(jsonPath("$[?(@.bean == 'welcomeController')].proxy").value(hasItem(false)));
	}

	@Test
	void benchmarkComPoucasTarefas() throws Exception {
		this.mockMvc.perform(get("/api/painel/benchmark").param("tarefas", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tarefas").value(10))
			.andExpect(jsonPath("$.virtualMs", greaterThan(900)));
	}

	@Test
	void genericosMostramTypeErasureETiposNasAssinaturas() throws Exception {
		this.mockMvc.perform(get("/api/painel/genericos"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.mesmaClasse").value(true))
			.andExpect(jsonPath("$.repositorios").value(hasItem("VetRepository extends Repository<Vet, Integer>")))
			.andExpect(jsonPath("$.injecaoPorTipoGenerico")
				.value("PrecoConsultaService(Map<String, PrecoConsultaStrategy>)"))
			.andExpect(jsonPath("$.estrategiasInjetadas").value(3));
	}

	@Test
	void diagramasUmlSaoServidosParaOPainel() throws Exception {
		this.mockMvc.perform(get("/uml/05_classes_strategy.svg")).andExpect(status().isOk());
	}

	@Test
	void consoleRecebeLogDoAspecto() throws Exception {
		this.mockMvc.perform(get("/vets")).andExpect(status().isOk());

		this.mockMvc.perform(get("/api/painel/logs").param("depoisDe", "0"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("[AUDITORIA] VetController.showResourcesVetList()")));
	}

}
