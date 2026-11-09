import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The same comparison as BenchmarkThreads, but each task computes instead of sleeping. With the
 * CPU busy, the limit is the number of cores, so virtual threads do not help. Run with
 * {@code java CpuBound.java [taskCount]}.
 */
public class CpuBound {

	public static void main(String[] args) throws Exception {
		int tasks = args.length > 0 ? Integer.parseInt(args[0]) : 200;
		System.out.printf("Disparando %,d tarefas, cada uma contando primos (só CPU, sem espera)%n", tasks);
		System.out.printf("Núcleos nesta máquina: %d%n%n", Runtime.getRuntime().availableProcessors());

		measure("Pool fixo (200 threads de plataforma)", Executors.newFixedThreadPool(200), tasks);
		measure("Virtual threads (uma por tarefa)", Executors.newVirtualThreadPerTaskExecutor(), tasks);
	}

	private static void measure(String label, ExecutorService executor, int tasks) {
		AtomicLong primes = new AtomicLong();
		Instant start = Instant.now();

		try (executor) {
			for (int i = 0; i < tasks; i++) {
				executor.submit(() -> primes.addAndGet(countPrimes(150_000)));
			}
		}

		long ms = Duration.between(start, Instant.now()).toMillis();
		System.out.printf("%-40s %,6d tarefas em %,7d ms%n", label, tasks, ms);
	}

	private static long countPrimes(int limit) {
		long count = 0;
		for (int n = 2; n < limit; n++) {
			boolean prime = true;
			for (int d = 2; (long) d * d <= n; d++) {
				if (n % d == 0) {
					prime = false;
					break;
				}
			}
			if (prime) {
				count++;
			}
		}
		return count;
	}

}
