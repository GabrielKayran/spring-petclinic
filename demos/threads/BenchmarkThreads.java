import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Benchmark: threads de plataforma x virtual threads (Java 21+).
 *
 * Cada tarefa só "espera" 1 segundo, simulando uma chamada a banco de dados ou a outro
 * serviço (I/O bloqueante). Durante a espera a thread não usa CPU, mas continua ocupada.
 *
 * Como rodar (não precisa compilar antes):  java BenchmarkThreads.java
 * Opcionalmente, informe o número de tarefas:  java BenchmarkThreads.java 100000
 */
public class BenchmarkThreads {

	public static void main(String[] args) throws Exception {
		int tarefas = args.length > 0 ? Integer.parseInt(args[0]) : 10_000;
		System.out.printf("Disparando %,d tarefas, cada uma com Thread.sleep(1s)%n%n", tarefas);

		// 1) Clássico: pool fixo de 200 threads de plataforma (cada uma é uma thread do SO).
		//    Só 200 tarefas rodam por vez, então 10.000 tarefas levam cerca de 10.000 / 200 = 50 s.
		medir("Pool fixo (200 threads de plataforma)", Executors.newFixedThreadPool(200), tarefas);

		// 2) Moderno: uma virtual thread por tarefa. Quando a virtual thread bloqueia no sleep,
		//    a JVM a "estaciona" e libera a thread do SO (carrier) para outra tarefa.
		//    Todas as tarefas esperam ao mesmo tempo, então o total fica em torno de 1 s.
		medir("Virtual threads (uma por tarefa)", Executors.newVirtualThreadPerTaskExecutor(), tarefas);
	}

	private static void medir(String nome, ExecutorService executor, int tarefas) {
		AtomicInteger concluidas = new AtomicInteger();
		Instant inicio = Instant.now();

		// try-with-resources: close() espera todas as tarefas terminarem (Java 19+)
		try (executor) {
			for (int i = 0; i < tarefas; i++) {
				executor.submit(() -> {
					Thread.sleep(Duration.ofSeconds(1));
					return concluidas.incrementAndGet();
				});
			}
		}

		long ms = Duration.between(inicio, Instant.now()).toMillis();
		System.out.printf("%-40s %,6d tarefas em %,7d ms%n", nome, concluidas.get(), ms);
	}

}
