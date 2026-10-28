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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.audit.Audited;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import net.sf.jasperreports.engine.JRException;

@RestController
class ReportController {

	private final ReportService reportService;

	private final DataSource dataSource;

	ReportController(ReportService reportService, DataSource dataSource) {
		this.reportService = reportService;
		this.dataSource = dataSource;
	}

	@Audited("Gerar relatório PDF (JasperReports)")
	@GetMapping("/relatorios/pets-e-precos.pdf")
	ResponseEntity<byte[]> petPrices() throws JRException, IOException, SQLException {
		byte[] pdf = this.reportService.generatePetPricesPdf(databaseName());
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_PDF)
			.header(HttpHeaders.CONTENT_DISPOSITION,
					ContentDisposition.inline().filename("pets-e-precos.pdf").build().toString())
			.body(pdf);
	}

	private String databaseName() throws SQLException {
		try (Connection connection = this.dataSource.getConnection()) {
			return connection.getMetaData().getDatabaseProductName();
		}
	}

}
