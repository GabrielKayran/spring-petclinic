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

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Component;

/**
 * Server-side version of {@code demos/threads/BenchmarkThreads.java}. Task count is
 * capped and only one run is allowed at a time.
 */
@Component
class BenchmarkDemo {

	static final int MAX_TASKS = 2000;

	static final int POOL_SIZE = 200;

	private final Semaphore oneAtATime = new Semaphore(1);

	record BenchmarkResult(int tasks, int poolSize, long platformMs, long virtualMs) {
	}

	/** Returns {@code null} when another run is in progress. */
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
