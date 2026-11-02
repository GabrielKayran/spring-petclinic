# Testes

[← Início](Home.md)

## Rodando

```bash
./mvnw test
```

Resultado atual: **108 testes, 2 ignorados**. `MySqlIntegrationTests` e `PostgresIntegrationTests` sobem um banco de verdade em contêiner e são pulados automaticamente quando o Docker não está disponível.

Para rodar uma classe só:

```bash
./mvnw test -Dtest=VisitPriceServiceTests
```

## O que cada teste prova

Todos ficam em `src/test/java/org/springframework/samples/petclinic/`.

### Extensões do grupo

| Classe | Prova |
|---|---|
| `audit/AuditAspectTests` | só controllers com `@Audited` viram proxy; a linha `[AUDITORIA]` sai no log com argumentos e tempo |
| `reminder/VisitRegisteredEventTests` | registrar visita publica o evento com os dados certos e o lembrete roda em outra thread |
| `pricing/VisitPriceServiceTests` | cão paga 100 + 5/ano, gato 90, demais tipos e pet sem tipo 70 |
| `pricing/VisitPricePageTests` | o preço aparece na tela de nova visita |
| `grpc/VetGrpcServiceTests` | o serviço gRPC responde com os veterinários do banco |
| `virtualthreads/ThreadInfoControllerTests` | `/thread` informa que a requisição rodou numa virtual thread |
| `report/ReportControllerTests` | `/relatorios/pets-e-precos.pdf` devolve um PDF válido |
| `dashboard/DashboardControllerTests` | endpoints do painel (ambiente, gRPC, benchmark, AOP, genéricos, logs) |
| `dashboard/DatabaseDemoControllerTests` | contagem de tabelas, SQL de query derivada, rollback sem deixar linha, demo desconhecida → 400 |
| `system/HealthControllerTests` | `/health` devolve `{"status":"UP"}` |
| `PetClinicConcurrencyTests` | duas inserções simultâneas do mesmo pet: só uma passa |

### Base do PetClinic

| Classe | Prova |
|---|---|
| `owner/OwnerControllerTests`, `PetControllerTests`, `VisitControllerTests` | formulários, validação e redirecionamentos com `MockMvc` |
| `owner/PetValidatorTests`, `model/ValidatorTests` | regras de validação |
| `service/ClinicServiceTests` | repositórios JPA contra o H2 |
| `system/I18nPropertiesSyncTest` | todos os `messages_*.properties` têm as mesmas chaves |
| `system/CrashControllerIntegrationTests` | a página de erro aparece em português ("Algo deu errado") |
| `PetClinicIntegrationTests` | a aplicação sobe inteira e responde |

## Técnicas usadas

**Teste de fatia, só com as classes necessárias** ([VisitPriceServiceTests](../../src/test/java/org/springframework/samples/petclinic/pricing/VisitPriceServiceTests.java#L28-L30)):

```java
@SpringJUnitConfig({ VisitPriceService.class, DogPriceStrategy.class, CatPriceStrategy.class,
		DefaultPriceStrategy.class })
class VisitPriceServiceTests {
```

Sobe um contexto Spring mínimo, sem banco nem web, mas com a injeção do `Map` funcionando de verdade.

**Capturando eventos publicados** ([VisitRegisteredEventTests](../../src/test/java/org/springframework/samples/petclinic/reminder/VisitRegisteredEventTests.java#L40-L51)):

```java
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class VisitRegisteredEventTests {
```

- `@RecordApplicationEvents` guarda todo evento publicado durante o teste.
- `OutputCaptureExtension` captura o log para conferir a thread.
- `@Transactional` desfaz a visita criada ao fim do teste.
- `await().atMost(...)` (Awaitility) espera o `@Async` terminar sem `sleep` fixo.

**Requisição HTTP simulada com `MockMvc`** ([DatabaseDemoControllerTests](../../src/test/java/org/springframework/samples/petclinic/dashboard/DatabaseDemoControllerTests.java#L60-L70)):

```java
@Test
void rollbackDemoLeavesNoRow() throws Exception {
	long before = this.petTypes.count();

	this.mockMvc.perform(get("/api/dashboard/sql").param("demo", "rollback"))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.sql[0]").value(containsString("insert")))
		.andExpect(jsonPath("$.millis").value(greaterThan(-1)));

	assertThat(this.petTypes.count()).isEqualTo(before);
}
```

## Qualidade de código

O build também roda o `spring-javaformat` (formatação) e o Checkstyle `nohttp` (proíbe links `http://`). Para corrigir a formatação:

```bash
./mvnw spring-javaformat:apply
```
