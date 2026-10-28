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
package org.springframework.samples.petclinic.dashboard;

import java.lang.reflect.Constructor;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.aop.support.AopUtils;
import org.springframework.boot.SpringBootVersion;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.audit.Audited;
import org.springframework.samples.petclinic.dashboard.BenchmarkDemo.BenchmarkResult;
import org.springframework.samples.petclinic.dashboard.DashboardLogAppender.LogLine;
import org.springframework.samples.petclinic.dashboard.GrpcDemoClient.GrpcResult;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.pricing.VisitPriceService;
import org.springframework.samples.petclinic.pricing.VisitPriceStrategy;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Case study dashboard: the {@code /painel} page and the JSON API it calls. It only reads
 * what the other packages do; none of them depends on it.
 */
@Controller
class DashboardController {

	/** Appended to the JS/CSS URLs so each deploy bypasses the 12h static cache. */
	private static final long ASSETS_VERSION = System.currentTimeMillis();

	private final DataSource dataSource;

	private final Environment environment;

	private final ApplicationContext context;

	private final OwnerRepository owners;

	private final VisitPriceService visitPriceService;

	private final GrpcDemoClient grpcDemoClient;

	private final BenchmarkDemo benchmarkDemo;

	private final DashboardLogAppender logs;

	DashboardController(DataSource dataSource, Environment environment, ApplicationContext context,
			OwnerRepository owners, VisitPriceService visitPriceService, GrpcDemoClient grpcDemoClient,
			BenchmarkDemo benchmarkDemo, DashboardLogAppender logs) {
		this.dataSource = dataSource;
		this.environment = environment;
		this.context = context;
		this.owners = owners;
		this.visitPriceService = visitPriceService;
		this.grpcDemoClient = grpcDemoClient;
		this.benchmarkDemo = benchmarkDemo;
		this.logs = logs;
	}

	@GetMapping("/painel")
	String dashboard(Model model) {
		model.addAttribute("version", ASSETS_VERSION);
		return "dashboard/dashboard";
	}

	record RuntimeInfo(String java, String springBoot, String database, String databaseVersion, String databaseUrl,
			List<String> profiles, boolean virtualThreads) {
	}

	@GetMapping("/api/dashboard/environment")
	@ResponseBody
	RuntimeInfo environment() throws SQLException {
		try (Connection connection = this.dataSource.getConnection()) {
			DatabaseMetaData meta = connection.getMetaData();
			return new RuntimeInfo(Runtime.version().toString(), SpringBootVersion.getVersion(),
					meta.getDatabaseProductName(), meta.getDatabaseProductVersion(), withoutParameters(meta.getURL()),
					Arrays.asList(this.environment.getActiveProfiles()),
					this.environment.getProperty("spring.threads.virtual.enabled", Boolean.class, false));
		}
	}

	/** Drops URL parameters, which could hold credentials. */
	private static String withoutParameters(String jdbcUrl) {
		int query = jdbcUrl.indexOf('?');
		String base = (query < 0) ? jdbcUrl : jdbcUrl.substring(0, query);
		int semicolon = base.indexOf(';');
		return (semicolon < 0) ? base : base.substring(0, semicolon);
	}

	@GetMapping("/api/dashboard/grpc")
	@ResponseBody
	GrpcResult grpc() {
		return this.grpcDemoClient.listVets();
	}

	@GetMapping("/api/dashboard/benchmark")
	@ResponseBody
	ResponseEntity<BenchmarkResult> benchmark(@RequestParam(defaultValue = "1000") int tasks) {
		BenchmarkResult result = this.benchmarkDemo.run(tasks);
		return (result == null) ? ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build()
				: ResponseEntity.ok(result);
	}

	record PetPrice(int ownerId, String owner, int petId, String pet, String type, int age, String strategy,
			BigDecimal price) {
	}

	@GetMapping("/api/dashboard/pets")
	@ResponseBody
	List<PetPrice> pets() {
		List<PetPrice> result = new ArrayList<>();
		for (Owner owner : this.owners.findByLastNameStartingWith("", PageRequest.of(0, 100))) {
			for (Pet pet : owner.getPets()) {
				VisitPriceStrategy strategy = this.visitPriceService.strategyFor(pet);
				int age = (pet.getBirthDate() == null) ? 0
						: Period.between(pet.getBirthDate(), LocalDate.now()).getYears();
				result.add(new PetPrice(owner.getId(), owner.getFirstName() + " " + owner.getLastName(), pet.getId(),
						pet.getName(), (pet.getType() == null) ? "?" : pet.getType().getName(), age,
						ClassUtils.getUserClass(strategy).getSimpleName(), strategy.calculatePrice(pet)));
			}
		}
		result.sort(Comparator.comparing(PetPrice::petId));
		return result;
	}

	record ControllerProxy(String bean, String runtimeClass, boolean proxy, List<String> auditedMethods) {
	}

	@GetMapping("/api/dashboard/aop")
	@ResponseBody
	List<ControllerProxy> aop() {
		List<ControllerProxy> result = new ArrayList<>();
		for (String name : this.context.getBeanNamesForAnnotation(Controller.class)) {
			Object bean = this.context.getBean(name);
			Class<?> targetClass = AopUtils.getTargetClass(bean);
			if (!targetClass.getPackageName().startsWith("org.springframework.samples.petclinic")
					|| targetClass.getPackageName().endsWith(".dashboard")) {
				continue;
			}
			List<String> audited = Arrays.stream(targetClass.getDeclaredMethods())
				.filter(m -> AnnotatedElementUtils.hasAnnotation(m, Audited.class))
				.map(m -> m.getName() + "()")
				.sorted()
				.toList();
			result.add(new ControllerProxy(name, bean.getClass().getSimpleName(), AopUtils.isAopProxy(bean), audited));
		}
		result.sort(Comparator.comparing(ControllerProxy::proxy).reversed().thenComparing(ControllerProxy::bean));
		return result;
	}

	record GenericsInfo(String stringListClass, String integerListClass, boolean sameClass, List<String> repositories,
			String genericInjection, int injectedStrategies, String vetRepositoryRuntimeClass) {
	}

	/**
	 * Type erasure: {@code List<String>} and {@code List<Integer>} share a runtime class,
	 * but the type arguments stay in signatures, where Spring reads them via reflection.
	 */
	@GetMapping("/api/dashboard/generics")
	@ResponseBody
	GenericsInfo generics() {
		List<String> strings = new ArrayList<>();
		List<Integer> integers = new ArrayList<>();

		List<String> repositories = Stream.of(OwnerRepository.class, PetTypeRepository.class, VetRepository.class)
			.map(repository -> repository.getSimpleName() + " extends "
					+ shortName(ResolvableType.forClass(repository).getInterfaces()[0]))
			.toList();

		Constructor<?> constructor = VisitPriceService.class.getConstructors()[0];
		String injection = "VisitPriceService(" + shortName(ResolvableType.forConstructorParameter(constructor, 0))
				+ ")";

		return new GenericsInfo(strings.getClass().getName(), integers.getClass().getName(),
				strings.getClass() == integers.getClass(), repositories, injection,
				this.context.getBeansOfType(VisitPriceStrategy.class).size(),
				this.context.getBean(VetRepository.class).getClass().getName());
	}

	private static String shortName(ResolvableType type) {
		String name = type.toClass().getSimpleName();
		if (!type.hasGenerics()) {
			return name;
		}
		return name + Arrays.stream(type.getGenerics())
			.map(DashboardController::shortName)
			.collect(Collectors.joining(", ", "<", ">"));
	}

	@GetMapping("/api/dashboard/logs")
	@ResponseBody
	List<LogLine> logs(@RequestParam(defaultValue = "0") long after) {
		return this.logs.linesAfter(after);
	}

}
