# Concorrência e virtual threads

[← Início](Home.md)

## Conceitos em uma frase

- **Thread de plataforma:** uma thread do sistema operacional. É cara (cerca de 1 MB de pilha), então se usa um pool limitado.
- **Virtual thread (Java 21):** uma thread leve gerenciada pela JVM. Quando ela bloqueia (`sleep`, I/O, banco), sai da thread do SO (o *carrier*) e libera o lugar para outra. Dá para criar milhares.
- **Pool fixo:** N threads atendem uma fila de tarefas. Se todas estão bloqueadas, as outras tarefas esperam.

## Ligando no projeto inteiro

Uma linha em [application.properties](../../src/main/resources/application.properties):

```properties
# Virtual threads (Java 21+) for Tomcat requests and the @Async executor
spring.threads.virtual.enabled=true
```

Com isso, cada requisição HTTP do Tomcat e cada tarefa `@Async` roda numa virtual thread. Nenhuma classe do sistema precisou mudar.

## Conferindo: endpoint `/thread`

[ThreadInfoController.java](../../src/main/java/org/springframework/samples/petclinic/virtualthreads/ThreadInfoController.java#L25-L41)

```java
@RestController
class ThreadInfoController {

	@GetMapping("/thread")
	ThreadInfo currentThread() {
		Thread current = Thread.currentThread();
		return new ThreadInfo(current.getName(), current.isVirtual(), current.isVirtual() ? "virtual" : "plataforma",
				current.toString());
	}

	record ThreadInfo(String name, boolean virtual, String type, String details) {
	}

}
```

Abrindo `/thread` vem `"virtual": true` e um `details` parecido com `VirtualThread[#52,tomcat-handler-3]/runnable@ForkJoinPool-1-worker-1`. A parte depois do `@` é o carrier.

## Benchmark

[BenchmarkDemo.java](../../src/main/java/org/springframework/samples/petclinic/dashboard/BenchmarkDemo.java#L29-L70), chamado pelo card 2 do painel:

```java
@Component
class BenchmarkDemo {

	static final int MAX_TASKS = 2000;

	static final int POOL_SIZE = 200;

	private final Semaphore oneAtATime = new Semaphore(1);

	BenchmarkResult run(int requestedTasks) {
		int tasks = Math.clamp(requestedTasks, 1, MAX_TASKS);
		if (!this.oneAtATime.tryAcquire()) {
			return null;
		}
		try {
			long platform = measure(Executors.newFixedThreadPool(POOL_SIZE), tasks);
			long virtual = measure(Executors.newVirtualThreadPerTaskExecutor(), tasks);
			return new BenchmarkResult(tasks, POOL_SIZE, platform, virtual);
		}
		finally {
			this.oneAtATime.release();
		}
	}

	private static long measure(ExecutorService executor, int tasks) {
		long start = System.nanoTime();
		try (executor) {
			for (int i = 0; i < tasks; i++) {
				executor.submit(() -> {
					Thread.sleep(Duration.ofSeconds(1));
					return null;
				});
			}
		}
		return (System.nanoTime() - start) / 1_000_000;
	}

}
```

Linha a linha:

| Trecho | O que faz |
|---|---|
| `Math.clamp(requestedTasks, 1, MAX_TASKS)` | limita a 2000 tarefas para não derrubar o servidor gratuito |
| `Semaphore(1)` + `tryAcquire()` | só um benchmark por vez. Um segundo clique recebe HTTP 429 em vez de esperar |
| `newFixedThreadPool(200)` | 200 threads de plataforma |
| `newVirtualThreadPerTaskExecutor()` | uma virtual thread nova por tarefa |
| `submit(() -> { ...; return null; })` | o `return` torna o lambda um `Callable`, que pode lançar a `InterruptedException` do `sleep` |
| `try (executor)` | o `close()` do `ExecutorService` espera todas as tarefas terminarem |

**Resultado** com 10.000 tarefas de 1 s ([`demos/threads`](../../demos/threads/BenchmarkThreads.java)):

| Executor | Tempo |
|---|---|
| Pool fixo, 200 threads de plataforma | ~50.346 ms (10.000 ÷ 200 = 50 "ondas" de 1 s) |
| Virtual threads | ~1.218 ms (todas dormem ao mesmo tempo) |

O ganho aparece porque a tarefa **espera** (I/O). Para cálculo puro de CPU, virtual threads não aceleram nada.

Para rodar a versão de linha de comando:

```bash
cd demos/threads
java BenchmarkThreads.java 10000
```

## Outros pontos de concorrência no código

**Inicialização preguiçosa com dupla checagem** ([ReportService.java](../../src/main/java/org/springframework/samples/petclinic/report/ReportService.java#L71-L133)):

```java
private volatile JasperReport compiledLayout;

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
```

`volatile` garante que as outras threads vejam o valor pronto, e o `synchronized` impede que duas requisições compilem o layout ao mesmo tempo.

**Virtual thread criada à mão** ([ReportService.java](../../src/main/java/org/springframework/samples/petclinic/report/ReportService.java#L82-L92)): o layout é pré-compilado em segundo plano assim que a aplicação sobe.

```java
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
```

**Estrutura compartilhada protegida** ([DashboardLogAppender.java](../../src/main/java/org/springframework/samples/petclinic/dashboard/DashboardLogAppender.java#L73-L90)): várias threads escrevem logs e o painel lê ao mesmo tempo.

```java
private final Deque<LogLine> lines = new ArrayDeque<>();

private final AtomicLong sequence = new AtomicLong();

@Override
protected void append(ILoggingEvent event) {
	...
	synchronized (this.lines) {
		this.lines.addLast(line);
		if (this.lines.size() > CAPACITY) {
			this.lines.removeFirst();
		}
	}
}
```

`AtomicLong` gera ids sem trava. O `synchronized` protege o `ArrayDeque`, que não é thread-safe.

**`ThreadLocal`** no [SqlRecorder](Modelo-de-dominio-e-banco.md#vendo-o-sql-que-o-hibernate-gera): cada thread tem a sua lista de SQL, então requisições simultâneas não se misturam.

## Condição de corrida: pet com nome duplicado

Dois envios simultâneos do formulário "novo pet" com o mesmo nome passam os dois pela checagem em Java, porque nenhum deles foi gravado ainda. Quem resolve é o banco, com um índice único ([schema.sql do PostgreSQL](../../src/main/resources/db/postgres/schema.sql#L45)):

```sql
CREATE UNIQUE INDEX IF NOT EXISTS unique_owner_pet_name ON pets (owner_id, LOWER(name));
```

O controller trata a violação como um erro de formulário comum ([PetController.java](../../src/main/java/org/springframework/samples/petclinic/owner/PetController.java#L107-L132)):

```java
if (StringUtils.hasText(pet.getName()) && pet.isNew() && owner.getPet(pet.getName(), true) != null) {
	result.rejectValue("name", "duplicate");
}
...
try {
	owner.addPet(pet);
	this.owners.saveAndFlush(owner);
}
catch (DataIntegrityViolationException ex) {
	if (!isDuplicatePetNameViolation(ex)) {
		throw ex;
	}
	result.rejectValue("name", "duplicate");
```

A primeira checagem cobre o caso normal. O índice cobre a corrida, porque o banco serializa as duas inserções e só uma passa.

[PetClinicConcurrencyTests.java](../../src/test/java/org/springframework/samples/petclinic/PetClinicConcurrencyTests.java#L41-L131) prova isso com duas threads liberadas ao mesmo tempo por um `CountDownLatch`:

```java
CountDownLatch readyLatch = new CountDownLatch(threadCount);
CountDownLatch startLatch = new CountDownLatch(1);
CountDownLatch doneLatch = new CountDownLatch(threadCount);
...
assertThat(successCount.get()).isEqualTo(1);
assertThat(newPetCount).isEqualTo(initialPetCount + 1);
```
