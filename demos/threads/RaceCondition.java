import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lost updates: several threads increment the same counter. {@code count++} is read, add and
 * write, so two threads can read the same value and one increment disappears. Run with
 * {@code java RaceCondition.java [threads] [incrementsPerThread]}.
 */
public class RaceCondition {

	private static int unsafeCount;

	private static int lockedCount;

	private static final AtomicInteger atomicCount = new AtomicInteger();

	private static final Object lock = new Object();

	public static void main(String[] args) throws InterruptedException {
		int threads = args.length > 0 ? Integer.parseInt(args[0]) : 8;
		int increments = args.length > 1 ? Integer.parseInt(args[1]) : 1_000_000;
		int expected = threads * increments;
		System.out.printf("%d threads, cada uma soma 1 no contador %,d vezes. Esperado: %,d%n%n", threads, increments,
				expected);

		run(threads, increments, () -> unsafeCount++);
		report("Sem sincronização (count++)", unsafeCount, expected);

		run(threads, increments, () -> {
			synchronized (lock) {
				lockedCount++;
			}
		});
		report("synchronized", lockedCount, expected);

		run(threads, increments, atomicCount::incrementAndGet);
		report("AtomicInteger", atomicCount.get(), expected);
	}

	private static void run(int threads, int increments, Runnable increment)
			throws InterruptedException {
		List<Thread> workers = new ArrayList<>();
		for (int t = 0; t < threads; t++) {
			Thread worker = new Thread(() -> {
				for (int i = 0; i < increments; i++) {
					increment.run();
				}
			});
			workers.add(worker);
			worker.start();
		}
		for (Thread worker : workers) {
			worker.join();
		}
	}

	private static void report(String label, int actual, int expected) {
		String result = actual == expected ? "correto" : String.format("perdeu %,d somas", expected - actual);
		System.out.printf("%-30s %,12d   %s%n", label, actual, result);
	}

}
