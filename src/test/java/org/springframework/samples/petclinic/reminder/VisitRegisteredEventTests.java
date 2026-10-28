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
package org.springframework.samples.petclinic.reminder;

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

/** {@code @Transactional} rolls the saved visit back so other tests see the seed data. */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class VisitRegisteredEventTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApplicationEvents events;

	@Test
	void savingAVisitPublishesTheEventAndSendsTheReminderAsynchronously(CapturedOutput output) throws Exception {
		LocalDate tomorrow = LocalDate.now().plusDays(1);

		// Owner 1 = George Franklin, pet 1 = Leo
		this.mockMvc
			.perform(post("/owners/{ownerId}/pets/{petId}/visits/new", 1, 1).param("date", tomorrow.toString())
				.param("description", "Vacina anual"))
			.andExpect(status().is3xxRedirection());

		List<VisitRegisteredEvent> published = this.events.stream(VisitRegisteredEvent.class).toList();
		assertThat(published).hasSize(1);
		VisitRegisteredEvent event = published.get(0);
		assertThat(event.getOwnerName()).isEqualTo("George Franklin");
		assertThat(event.getPetName()).isEqualTo("Leo");
		assertThat(event.getVisitDate()).isEqualTo(tomorrow);
		assertThat(event.getDescription()).isEqualTo("Vacina anual");

		await().atMost(Duration.ofSeconds(10))
			.untilAsserted(() -> assertThat(output.getOut()).contains("lembrete enviado para George Franklin"));
		assertThat(output.getOut()).contains("[Observer] Thread 'task-");
		assertThat(output.getOut()).doesNotContain("[Observer] Thread '" + Thread.currentThread().getName() + "'");
	}

}
