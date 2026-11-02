# Arquitetura e pacotes

[← Início](Home.md)

## Camadas

O sistema segue o MVC do Spring com três camadas:

```
Navegador ──HTTP──▶ Controller (Spring MVC) ──▶ Repository (Spring Data JPA) ──▶ Hibernate ──JDBC──▶ H2 / PostgreSQL
                          │
                          └──▶ View Thymeleaf (templates/*.html)
```

- **Controller** recebe a requisição, valida o formulário e decide qual página mostrar.
- **Repository** é só uma interface. O Spring Data gera a implementação em tempo de execução.
- **Entity** (`Owner`, `Pet`, `Visit`, `Vet`) é uma classe Java mapeada para uma tabela.
- **View** é um HTML com atributos `th:*`, preenchido no servidor.

As extensões do grupo entram ao lado dessa base sem alterar o fluxo: um aspecto envolve os controllers, um evento sai do `VisitController`, um interceptor injeta o preço na tela de visita e um servidor gRPC roda na porta 9090.

Diagrama completo: [UML 03, componentes da arquitetura](../uml/svg/03_componentes_arquitetura.svg).

## Pacotes

Todos ficam em `src/main/java/org/springframework/samples/petclinic/`.

| Pacote | Origem | Responsabilidade |
|---|---|---|
| `model` | base | `BaseEntity`, `NamedEntity`, `Person`: superclasses das entidades |
| `owner` | base + grupo | Tutores, pets, visitas, controllers e repositórios. O grupo alterou o `VisitController` |
| `vet` | base | Veterinários e especialidades |
| `system` | base + grupo | Configuração web, idioma, cache, página de erro e `/health` |
| `audit` | grupo | `@Audited` e `AuditAspect` (AOP) |
| `reminder` | grupo | `VisitRegisteredEvent`, `VisitReminderListener`, `@EnableAsync` (Observer) |
| `pricing` | grupo | `VisitPriceStrategy` e implementações (Strategy) |
| `grpc` | grupo | `VetGrpcService` e o cliente de linha de comando |
| `virtualthreads` | grupo | Endpoint `/thread` que mostra a thread atual |
| `report` | grupo | Relatório PDF com JasperReports |
| `dashboard` | grupo | Painel `/painel`, benchmark, console de logs, SQL do Hibernate |

Fora de `src/`:

| Pasta | Conteúdo |
|---|---|
| `demos/rmi` | Servidor e cliente RMI puros, sem Spring |
| `demos/threads` | Benchmark de threads de linha de comando |
| `docs/uml` | 11 diagramas (PlantUML, SVG, PNG) |
| `src/main/protobuf` | Contrato gRPC `vets.proto` |
| `src/main/resources/reports` | Layout `pets-and-prices.jrxml` |
| `src/main/resources/db/{h2,postgres,mysql}` | `schema.sql` e `data.sql` por banco |

## Ponto de entrada

[PetClinicApplication.java](../../src/main/java/org/springframework/samples/petclinic/PetClinicApplication.java#L28-L36)

```java
@SpringBootApplication
@ImportRuntimeHints(PetClinicRuntimeHints.class)
public class PetClinicApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetClinicApplication.class, args);
	}

}
```

`@SpringBootApplication` liga a varredura de componentes no pacote e subpacotes. Por isso as classes novas (`AuditAspect`, `VisitReminderListener`, as estratégias) só precisam de `@Component`, `@Service` ou `@Configuration` para entrar no sistema, sem registro manual.

## Idioma

A interface está em português por padrão. O idioma vem do `LocaleResolver` em [WebConfiguration.java](../../src/main/java/org/springframework/samples/petclinic/system/WebConfiguration.java#L32-L48):

```java
@Bean
public LocaleResolver localeResolver() {
	SessionLocaleResolver resolver = new SessionLocaleResolver();
	resolver.setDefaultLocale(Locale.of("pt", "BR"));
	return resolver;
}

@Bean
public LocaleChangeInterceptor localeChangeInterceptor() {
	LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
	interceptor.setParamName("lang");
	return interceptor;
}
```

Os textos ficam em `src/main/resources/messages/messages_*.properties`. Para trocar de idioma, adicione `?lang=en` à URL.
