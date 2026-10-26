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
package org.springframework.samples.petclinic.lembrete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extensão 3: registra uma visita pelo formulário e verifica que (1) o
 * {@link VisitaRegistradaEvent} foi publicado e (2) o listener assíncrono rodou em outra
 * thread.
 * <p>
 * {@code @Transactional} desfaz a visita gravada no fim do teste, para não afetar outros
 * testes que usam o mesmo banco H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class VisitaRegistradaEventTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApplicationEvents eventos;

	@Test
	void registrarVisitaDevePublicarEventoEDispararLembreteAssincrono(CapturedOutput saida) throws Exception {
		LocalDate amanha = LocalDate.now().plusDays(1);

		// Dono 1 = George Franklin, pet 1 = Leo (dados do data.sql)
		this.mockMvc
			.perform(post("/owners/{ownerId}/pets/{petId}/visits/new", 1, 1).param("date", amanha.toString())
				.param("description", "Vacina anual"))
			.andExpect(status().is3xxRedirection());

		// (1) O controller publicou exatamente um evento, com os dados da visita
		List<VisitaRegistradaEvent> publicados = this.eventos.stream(VisitaRegistradaEvent.class).toList();
		assertThat(publicados).hasSize(1);
		VisitaRegistradaEvent evento = publicados.get(0);
		assertThat(evento.getNomeDono()).isEqualTo("George Franklin");
		assertThat(evento.getNomePet()).isEqualTo("Leo");
		assertThat(evento.getDataVisita()).isEqualTo(amanha);
		assertThat(evento.getDescricao()).isEqualTo("Vacina anual");

		// (2) O lembrete é processado depois, em uma thread do executor ("task-N"),
		// e não na thread do teste. Por isso esperamos o log aparecer.
		await().atMost(Duration.ofSeconds(10))
			.untilAsserted(() -> assertThat(saida.getOut()).contains("lembrete enviado para George Franklin"));
		assertThat(saida.getOut()).contains("[Observer] Thread 'task-");
		assertThat(saida.getOut()).doesNotContain("[Observer] Thread '" + Thread.currentThread().getName() + "'");
	}

}
