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
package org.springframework.samples.petclinic.preco;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Extensão 4: abre a tela de nova visita da aplicação completa e confere o preço exibido
 * para pets de tipos diferentes (dados do data.sql).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PrecoConsultaTelaTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void gatoSamanthaMostraPrecoDeGato() throws Exception {
		// Dono 6 (Jean Coleman), pet 7 (Samantha, cat)
		this.mockMvc.perform(get("/owners/6/pets/7/visits/new"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("precoConsulta", comparesEqualTo(new BigDecimal("90.00"))))
			.andExpect(content().string(containsString("R$ 90,00")));
	}

	@Test
	void cachorroRosyMostraPrecoConformeIdade() throws Exception {
		// Dono 3 (Eduardo Rodriquez), pet 3 (Rosy, dog, nascida em 2011-04-17)
		int idade = Period.between(LocalDate.of(2011, 4, 17), LocalDate.now()).getYears();
		BigDecimal esperado = new BigDecimal("100.00").add(new BigDecimal("5.00").multiply(BigDecimal.valueOf(idade)));

		this.mockMvc.perform(get("/owners/3/pets/3/visits/new"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("precoConsulta", comparesEqualTo(esperado)));
	}

	@Test
	void lagartoIggyUsaPrecoPadrao() throws Exception {
		// Dono 4 (Harold Davis), pet 5 (Iggy, lizard)
		this.mockMvc.perform(get("/owners/4/pets/5/visits/new"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("R$ 70,00")));
	}

}
