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
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.pricing.VisitPriceService;
import org.springframework.samples.petclinic.pricing.VisitPriceStrategy;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * Builds the "pets and visit prices" PDF with JasperReports: compile the {@code .jrxml}
 * once, fill it with the data, export to PDF.
 */
@Service
public class ReportService {

	private static final Logger log = LoggerFactory.getLogger(ReportService.class);

	private static final String LAYOUT = "reports/pets-and-prices.jrxml";

	private static final Locale PT_BR = Locale.of("pt", "BR");

	private final OwnerRepository owners;

	private final VisitPriceService visitPriceService;

	private volatile JasperReport compiledLayout;

	public ReportService(OwnerRepository owners, VisitPriceService visitPriceService) {
		this.owners = owners;
		this.visitPriceService = visitPriceService;
	}

	/**
	 * Compiles the layout in the background once the app is ready. On Render's small CPU
	 * the first compilation took about 20 seconds.
	 */
	@EventListener(ApplicationReadyEvent.class)
	void precompileLayout() {
		Thread.ofVirtual().name("report-compiler").start(() -> {
			try {
				layout();
			}
			catch (JRException | IOException | RuntimeException ex) {
				log.warn("Could not precompile the report layout: {}", ex.getMessage());
			}
		});
	}

	public byte[] generatePetPricesPdf(String database) throws JRException, IOException {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("GENERATED_AT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
		parameters.put("DATABASE", database);
		parameters.put(JRParameter.REPORT_LOCALE, PT_BR);

		JasperPrint filled = JasperFillManager.fillReport(layout(), parameters, new JRBeanCollectionDataSource(rows()));
		return JasperExportManager.exportReportToPdf(filled);
	}

	/** One row per pet, sorted by type because the layout groups by type. */
	List<ReportRow> rows() {
		List<ReportRow> rows = new ArrayList<>();
		for (Owner owner : this.owners.findByLastNameStartingWith("", PageRequest.of(0, 100))) {
			for (Pet pet : owner.getPets()) {
				VisitPriceStrategy strategy = this.visitPriceService.strategyFor(pet);
				int age = (pet.getBirthDate() == null) ? 0
						: Period.between(pet.getBirthDate(), LocalDate.now()).getYears();
				rows.add(new ReportRow((pet.getType() == null) ? "?" : pet.getType().getName(),
						ClassUtils.getUserClass(strategy).getSimpleName(), pet.getName(),
						owner.getFirstName() + " " + owner.getLastName(), age, pet.getVisits().size(),
						strategy.calculatePrice(pet)));
			}
		}
		rows.sort(Comparator.comparing(ReportRow::getType).thenComparing(ReportRow::getPet));
		return rows;
	}

	private JasperReport layout() throws JRException, IOException {
		if (this.compiledLayout == null) {
			synchronized (this) {
				if (this.compiledLayout == null) {
					try (InputStream jrxml = new ClassPathResource(LAYOUT).getInputStream()) {
						this.compiledLayout = JasperCompileManager.compileReport(jrxml);
					}
				}
			}
		}
		return this.compiledLayout;
	}

}
