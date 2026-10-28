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
package org.springframework.samples.petclinic.report;

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

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ReportService reportService;

	@Test
	void generatesAPdf() throws Exception {
		byte[] pdf = this.mockMvc.perform(get("/relatorios/pets-e-precos.pdf"))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_PDF))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();

		assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
		assertThat(pdf.length).isGreaterThan(5_000);
	}

	@Test
	void rowsAreSortedByTypeAndUseTheStrategyPrice() {
		List<ReportRow> rows = this.reportService.rows();

		assertThat(rows).hasSize(13);
		assertThat(rows).extracting(ReportRow::getType).isSorted();
		assertThat(rows).filteredOn(row -> row.getType().equals("cat"))
			.extracting(ReportRow::getStrategy)
			.containsOnly("CatPriceStrategy");
	}

}
