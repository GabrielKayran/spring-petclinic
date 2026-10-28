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
import org.springframework.samples.petclinic.preco.PrecoConsultaService;
import org.springframework.samples.petclinic.preco.PrecoConsultaStrategy;
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
 * Relatórios (tópico 4.4 do plano de ensino): gera um PDF com JasperReports.
 * <p>
 * O fluxo clássico do Jasper tem três passos:
 * <ol>
 * <li><b>compilar</b> o layout ({@code .jrxml}, desenhado no iReport/Jaspersoft Studio)
 * em um {@link JasperReport}. Fazemos isso uma vez só e guardamos o resultado;</li>
 * <li><b>preencher</b> o layout com os dados ({@link JasperPrint});</li>
 * <li><b>exportar</b> para o formato final (PDF).</li>
 * </ol>
 * Os dados vêm das mesmas classes do resto do sistema: o repositório de donos e o
 * {@link PrecoConsultaService} (padrão Strategy).
 */
@Service
public class RelatorioService {

	private static final Logger log = LoggerFactory.getLogger(RelatorioService.class);

	private static final String LAYOUT = "relatorios/pets-e-precos.jrxml";

	private static final Locale PT_BR = Locale.of("pt", "BR");

	private final OwnerRepository owners;

	private final PrecoConsultaService precoConsultaService;

	private volatile JasperReport layoutCompilado;

	public RelatorioService(OwnerRepository owners, PrecoConsultaService precoConsultaService) {
		this.owners = owners;
		this.precoConsultaService = precoConsultaService;
	}

	/**
	 * Compila o layout em segundo plano assim que a aplicação fica pronta. No Render, com
	 * pouca CPU, a primeira compilação levava cerca de 20 s; assim o primeiro relatório
	 * pedido durante a apresentação já sai na hora.
	 */
	@EventListener(ApplicationReadyEvent.class)
	void compilarLayoutEmSegundoPlano() {
		Thread.ofVirtual().name("compila-relatorio").start(() -> {
			try {
				layout();
			}
			catch (JRException | IOException | RuntimeException ex) {
				log.warn("Não foi possível pré-compilar o relatório: {}", ex.getMessage());
			}
		});
	}

	/** Gera o PDF do relatório "Pets e preço da consulta". */
	public byte[] gerarPdfPetsEPrecos(String banco) throws JRException, IOException {
		Map<String, Object> parametros = new HashMap<>();
		parametros.put("GERADO_EM", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
		parametros.put("BANCO", banco);
		// Moeda e números no formato brasileiro (R$ 90,00)
		parametros.put(JRParameter.REPORT_LOCALE, PT_BR);

		JasperPrint preenchido = JasperFillManager.fillReport(layout(), parametros,
				new JRBeanCollectionDataSource(linhas()));
		return JasperExportManager.exportReportToPdf(preenchido);
	}

	/** Uma linha por pet, ordenada por tipo, porque o .jrxml agrupa por tipo. */
	List<LinhaRelatorio> linhas() {
		List<LinhaRelatorio> linhas = new ArrayList<>();
		for (Owner owner : this.owners.findByLastNameStartingWith("", PageRequest.of(0, 100))) {
			for (Pet pet : owner.getPets()) {
				PrecoConsultaStrategy estrategia = this.precoConsultaService.estrategiaPara(pet);
				int idade = (pet.getBirthDate() == null) ? 0
						: Period.between(pet.getBirthDate(), LocalDate.now()).getYears();
				linhas.add(new LinhaRelatorio((pet.getType() == null) ? "?" : pet.getType().getName(),
						ClassUtils.getUserClass(estrategia).getSimpleName(), pet.getName(),
						owner.getFirstName() + " " + owner.getLastName(), idade, pet.getVisits().size(),
						estrategia.calcularPreco(pet)));
			}
		}
		linhas.sort(Comparator.comparing(LinhaRelatorio::getTipo).thenComparing(LinhaRelatorio::getPet));
		return linhas;
	}

	/** Compila o .jrxml só na primeira vez (leva cerca de 1 s) e reaproveita depois. */
	private JasperReport layout() throws JRException, IOException {
		if (this.layoutCompilado == null) {
			synchronized (this) {
				if (this.layoutCompilado == null) {
					try (InputStream jrxml = new ClassPathResource(LAYOUT).getInputStream()) {
						this.layoutCompilado = JasperCompileManager.compileReport(jrxml);
					}
				}
			}
		}
		return this.layoutCompilado;
	}

}
