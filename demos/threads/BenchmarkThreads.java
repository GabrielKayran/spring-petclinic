import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Platform threads vs. virtual threads. Each task sleeps for one second to simulate
 * blocking I/O. Run with {@code java BenchmarkThreads.java [taskCount]}.
 */
public class BenchmarkThreads {

	public static void main(String[] args) throws Exception {
		int tasks = args.length > 0 ? Integer.parseInt(args[0]) : 10_000;
		System.out.printf("Disparando %,d tarefas, cada uma com Thread.sleep(1s)%n%n", tasks);

		measure("Pool fixo (200 threads de plataforma)", Executors.newFixedThreadPool(200), tasks);
		measure("Virtual threads (uma por tarefa)", Executors.newVirtualThreadPerTaskExecutor(), tasks);
	}

	private static void measure(String label, ExecutorService executor, int tasks) {
		AtomicInteger completed = new AtomicInteger();
		Instant start = Instant.now();

		// close() waits for every submitted task (Java 19+)
		try (executor) {
			for (int i = 0; i < tasks; i++) {
				executor.submit(() -> {
					Thread.sleep(Duration.ofSeconds(1));
					return completed.incrementAndGet();
				});
			}
		}

		long ms = Duration.between(start, Instant.now()).toMillis();
		System.out.printf("%-40s %,6d tarefas em %,7d ms%n", label, completed.get(), ms);
	}

}
