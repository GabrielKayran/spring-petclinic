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
package org.springframework.samples.petclinic.relatorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Relatórios: o JasperReports compila o .jrxml, preenche com os dados do banco e exporta
 * um PDF válido.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RelatorioControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RelatorioService relatorioService;

	@Test
	void geraPdfDoRelatorio() throws Exception {
		byte[] pdf = this.mockMvc.perform(get("/relatorios/pets-e-precos.pdf"))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_PDF))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();

		// Todo PDF começa com "%PDF-"
		assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
		assertThat(pdf.length).isGreaterThan(5_000);
	}

	@Test
	void linhasVemOrdenadasPorTipoComAEstrategiaDoStrategy() {
		List<LinhaRelatorio> linhas = this.relatorioService.linhas();

		// O data.sql do H2 tem 13 pets
		assertThat(linhas).hasSize(13);
		assertThat(linhas).extracting(LinhaRelatorio::getTipo).isSorted();
		assertThat(linhas).filteredOn(l -> l.getTipo().equals("cat"))
			.extracting(LinhaRelatorio::getEstrategia)
			.containsOnly("PrecoConsultaGato");
	}

}
