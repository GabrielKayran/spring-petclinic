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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.aop.support.AopUtils;
import org.springframework.boot.SpringBootVersion;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.auditoria.Auditado;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.painel.BenchmarkDemo.ResultadoBenchmark;
import org.springframework.samples.petclinic.painel.GrpcDemoCliente.ResultadoGrpc;
import org.springframework.samples.petclinic.painel.PainelLogAppender.LinhaDeLog;
import org.springframework.samples.petclinic.preco.PrecoConsultaService;
import org.springframework.samples.petclinic.preco.PrecoConsultaStrategy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Painel do estudo de caso: a página {@code /painel} e a API JSON que ela usa.
 * <p>
 * Este pacote é só a "vitrine" das cinco extensões: ele lê o que elas fazem, mas nenhuma
 * extensão depende dele. Os botões da página chamam os endpoints abaixo e também os
 * endpoints reais do PetClinic (formulário de visita, {@code /owners/1}, {@code /vets},
 * {@code /thread}).
 */
@Controller
class PainelController {

	private final DataSource dataSource;

	private final Environment environment;

	private final ApplicationContext contexto;

	private final OwnerRepository owners;

	private final PrecoConsultaService precoConsultaService;

	private final GrpcDemoCliente grpcDemoCliente;

	private final BenchmarkDemo benchmarkDemo;

	private final PainelLogAppender logs;

	PainelController(DataSource dataSource, Environment environment, ApplicationContext contexto,
			OwnerRepository owners, PrecoConsultaService precoConsultaService, GrpcDemoCliente grpcDemoCliente,
			BenchmarkDemo benchmarkDemo, PainelLogAppender logs) {
		this.dataSource = dataSource;
		this.environment = environment;
		this.contexto = contexto;
		this.owners = owners;
		this.precoConsultaService = precoConsultaService;
		this.grpcDemoCliente = grpcDemoCliente;
		this.benchmarkDemo = benchmarkDemo;
		this.logs = logs;
	}

	/**
	 * Instante em que a aplicação subiu. Vai na URL do JS/CSS ({@code painel.js?v=...}),
	 * então cada novo deploy fura o cache de 12h dos arquivos estáticos.
	 */
	private static final long VERSAO_DOS_ARQUIVOS = System.currentTimeMillis();

	@GetMapping("/painel")
	String painel(Model model) {
		model.addAttribute("versao", VERSAO_DOS_ARQUIVOS);
		return "painel/painel";
	}

	// ---------- Ambiente: Java, Spring Boot e banco de dados em uso ----------

	record Ambiente(String java, String springBoot, String banco, String bancoVersao, String bancoHost,
			List<String> profiles, boolean virtualThreads) {
	}

	@GetMapping("/api/painel/ambiente")
	@ResponseBody
	Ambiente ambiente() throws SQLException {
		try (Connection conexao = this.dataSource.getConnection()) {
			DatabaseMetaData meta = conexao.getMetaData();
			return new Ambiente(Runtime.version().toString(), SpringBootVersion.getVersion(),
					meta.getDatabaseProductName(), meta.getDatabaseProductVersion(), semCredenciais(meta.getURL()),
					Arrays.asList(this.environment.getActiveProfiles()),
					this.environment.getProperty("spring.threads.virtual.enabled", Boolean.class, false));
		}
	}

	/** Mostra só o "endereço" do banco: corta parâmetros, onde poderia haver senha. */
	private static String semCredenciais(String jdbcUrl) {
		int interrogacao = jdbcUrl.indexOf('?');
		String semParametros = (interrogacao < 0) ? jdbcUrl : jdbcUrl.substring(0, interrogacao);
		int ponto = semParametros.indexOf(';');
		return (ponto < 0) ? semParametros : semParametros.substring(0, ponto);
	}

	// ---------- Extensão 1: gRPC ----------

	@GetMapping("/api/painel/grpc")
	@ResponseBody
	ResultadoGrpc grpc() {
		return this.grpcDemoCliente.listarVeterinarios();
	}

	// ---------- Extensão 2: benchmark de threads ----------

	@GetMapping("/api/painel/benchmark")
	@ResponseBody
	ResponseEntity<ResultadoBenchmark> benchmark(@RequestParam(defaultValue = "1000") int tarefas) {
		ResultadoBenchmark resultado = this.benchmarkDemo.executar(tarefas);
		return (resultado == null) ? ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build()
				: ResponseEntity.ok(resultado);
	}

	// ---------- Extensões 3 e 4: pets, preço (Strategy) e registro de visita ----------

	record PetComPreco(int donoId, String dono, int petId, String pet, String tipo, int idade, String estrategia,
			BigDecimal preco) {
	}

	@GetMapping("/api/painel/pets")
	@ResponseBody
	List<PetComPreco> pets() {
		List<PetComPreco> resultado = new ArrayList<>();
		for (Owner owner : this.owners.findByLastNameStartingWith("", PageRequest.of(0, 100))) {
			for (Pet pet : owner.getPets()) {
				PrecoConsultaStrategy estrategia = this.precoConsultaService.estrategiaPara(pet);
				int idade = (pet.getBirthDate() == null) ? 0
						: Period.between(pet.getBirthDate(), LocalDate.now()).getYears();
				resultado.add(new PetComPreco(owner.getId(), owner.getFirstName() + " " + owner.getLastName(),
						pet.getId(), pet.getName(), (pet.getType() == null) ? "?" : pet.getType().getName(), idade,
						ClassUtils.getUserClass(estrategia).getSimpleName(), estrategia.calcularPreco(pet)));
			}
		}
		resultado.sort(Comparator.comparing(PetComPreco::petId));
		return resultado;
	}

	// ---------- Extensão 5: quais controllers viraram proxies AOP ----------

	record ControllerAop(String bean, String classeEmTempoDeExecucao, boolean proxy, List<String> metodosAuditados) {
	}

	@GetMapping("/api/painel/aop")
	@ResponseBody
	List<ControllerAop> aop() {
		List<ControllerAop> resultado = new ArrayList<>();
		for (String nome : this.contexto.getBeanNamesForAnnotation(Controller.class)) {
			Object bean = this.contexto.getBean(nome);
			Class<?> classeOriginal = AopUtils.getTargetClass(bean);
			if (!classeOriginal.getPackageName().startsWith("org.springframework.samples.petclinic")
					|| classeOriginal.getPackageName().endsWith(".painel")) {
				continue;
			}
			List<String> auditados = Arrays.stream(classeOriginal.getDeclaredMethods())
				.filter(m -> AnnotatedElementUtils.hasAnnotation(m, Auditado.class))
				.map(m -> m.getName() + "()")
				.sorted()
				.toList();
			resultado
				.add(new ControllerAop(nome, bean.getClass().getSimpleName(), AopUtils.isAopProxy(bean), auditados));
		}
		resultado.sort(Comparator.comparing(ControllerAop::proxy).reversed().thenComparing(ControllerAop::bean));
		return resultado;
	}

	// ---------- Console ao vivo ----------

	@GetMapping("/api/painel/logs")
	@ResponseBody
	List<LinhaDeLog> logs(@RequestParam(defaultValue = "0") long depoisDe) {
		return this.logs.linhasDepoisDe(depoisDe);
	}

}
