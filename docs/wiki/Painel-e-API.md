# Painel e API

[← Início](Home.md)

## O painel `/painel`

Uma página para a apresentação: cada card mostra uma extensão com o código real, o diagrama e um botão que executa a demonstração no servidor.

| Card | Tema | O que o botão faz |
|---|---|---|
| 0 | Mapa da disciplina e genéricos | inspeciona tipos genéricos em tempo de execução |
| 1 | RMI → gRPC | chama o `VetGrpcService` e compara bytes Protobuf × JSON |
| 2 | Virtual threads | roda o benchmark (pool de 200 × virtual threads) |
| 3 | Observer + `@Async` | registra uma visita e mostra o lembrete em outra thread |
| 4 | Strategy | tabela de pets com a estratégia e o preço de cada um |
| 5 | AOP | lista os controllers, indica quais são proxies e chama um método auditado |
| 6 | Relatórios | gera o PDF do JasperReports na própria página |
| 7 | Banco de dados | contagem por tabela e o SQL que o Hibernate gera |
| U | UML | galeria com os 11 diagramas |

O console ao vivo no rodapé lê `/api/dashboard/logs` a cada segundo e mostra as linhas dos pacotes `audit`, `reminder` e `grpc`.

Arquivos: [DashboardController.java](../../src/main/java/org/springframework/samples/petclinic/dashboard/DashboardController.java), [dashboard.html](../../src/main/resources/templates/dashboard/dashboard.html), [dashboard.js](../../src/main/resources/static/resources/js/dashboard.js), [dashboard.css](../../src/main/resources/static/resources/css/dashboard.css).

## Console ao vivo

[DashboardLogAppender.java](../../src/main/java/org/springframework/samples/petclinic/dashboard/DashboardLogAppender.java#L41-L61) é um appender do Logback registrado só nos pacotes que interessam:

```java
private static final List<String> WATCHED_PACKAGES = List.of("org.springframework.samples.petclinic.audit",
		"org.springframework.samples.petclinic.reminder", "org.springframework.samples.petclinic.grpc");

@Override
public void afterPropertiesSet() {
	if (LoggerFactory.getILoggerFactory() instanceof LoggerContext context) {
		setContext(context);
		setName("dashboard");
		start();
		WATCHED_PACKAGES.forEach(name -> ((Logger) LoggerFactory.getLogger(name)).addAppender(this));
	}
}
```

Ele guarda as últimas 200 linhas, e o navegador pede só as que têm id maior que a última recebida (`?after=`).

## Genéricos em tempo de execução

[DashboardController.java](../../src/main/java/org/springframework/samples/petclinic/dashboard/DashboardController.java#L199-L218)

```java
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
```

Mostra três coisas:

1. **Apagamento de tipo:** `List<String>` e `List<Integer>` são a mesma classe (`sameClass = true`).
2. Mas o tipo genérico **continua nas assinaturas**: `OwnerRepository extends JpaRepository<Owner, Integer>`.
3. E o Spring usa essa informação para injetar `Map<String, VisitPriceStrategy>` no [Strategy](Strategy-preco-da-consulta.md).

## Endpoints

### Páginas (Thymeleaf)

| Método | Caminho | Controller |
|---|---|---|
| GET | `/` | `WelcomeController` |
| GET | `/owners/find`, `/owners`, `/owners/{id}` | `OwnerController` |
| GET/POST | `/owners/new`, `/owners/{id}/edit` | `OwnerController` |
| GET/POST | `/owners/{id}/pets/new`, `/owners/{id}/pets/{petId}/edit` | `PetController` |
| GET/POST | `/owners/{id}/pets/{petId}/visits/new` | `VisitController` (auditado, publica evento) |
| GET | `/vets.html` | `VetController` |
| GET | `/painel` | `DashboardController` |
| GET | `/oups` | `CrashController` (erro proposital) |

### JSON e arquivos

| Caminho | Retorno |
|---|---|
| `/health` | `{"status":"UP"}`, 200 ou 503 |
| `/thread` | thread que atendeu a requisição |
| `/vets` | veterinários em JSON |
| `/relatorios/pets-e-precos.pdf` | PDF do JasperReports |
| `/api/dashboard/environment` | versão do Java, do Spring Boot, banco e perfis |
| `/api/dashboard/grpc` | resposta do gRPC + bytes Protobuf × JSON |
| `/api/dashboard/benchmark?tasks=1000` | tempos do benchmark (429 se já houver um rodando) |
| `/api/dashboard/pets` | pets com estratégia e preço |
| `/api/dashboard/aop` | controllers, proxies e métodos auditados |
| `/api/dashboard/generics` | demonstração de genéricos |
| `/api/dashboard/logs?after=0` | linhas do console ao vivo |
| `/api/dashboard/database` | número de linhas de cada tabela |
| `/api/dashboard/sql?demo=findById\|findByLastName\|query\|rollback` | SQL gerado, tempo e resultado |
| `/actuator/*` | Actuator (em produção, só `health` e `info`) |

### gRPC (porta 9090)

| Serviço | Método |
|---|---|
| `petclinic.VetService` | `ListVets(ListVetsRequest) returns (ListVetsResponse)` |

## Health

[HealthController.java](../../src/main/java/org/springframework/samples/petclinic/system/HealthController.java#L33-L50)

```java
@RestController
class HealthController {

	private final ObjectProvider<HealthEndpoint> healthEndpoint;

	@GetMapping("/health")
	ResponseEntity<Map<String, String>> health() {
		HealthEndpoint endpoint = this.healthEndpoint.getIfAvailable();
		Status status = (endpoint != null) ? endpoint.health().getStatus() : Status.UP;
		HttpStatus httpStatus = Status.UP.equals(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
		return ResponseEntity.status(httpStatus).body(Map.of("status", status.getCode()));
	}

}
```

Reaproveita o health do Actuator, que inclui a conexão com o banco. Se o Neon cair, a resposta vira 503 e o Render percebe pelo `healthCheckPath`.
