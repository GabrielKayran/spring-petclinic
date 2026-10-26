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
package org.springframework.samples.petclinic.painel;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Component;

/**
 * Painel: a mesma ideia do {@code demos/threads/BenchmarkThreads.java}, só que rodando no
 * servidor para ser disparada pelo navegador.
 * <p>
 * Para não sobrecarregar o servidor (no Render a máquina é pequena), o número de tarefas
 * é limitado e só um benchmark roda por vez.
 */
@Component
class BenchmarkDemo {

	static final int MAXIMO_DE_TAREFAS = 2000;

	static final int THREADS_NO_POOL = 200;

	private final Semaphore umPorVez = new Semaphore(1);

	record ResultadoBenchmark(int tarefas, int threadsNoPool, long plataformaMs, long virtualMs) {
	}

	/** Retorna {@code null} se já houver um benchmark em andamento. */
	ResultadoBenchmark executar(int tarefasPedidas) {
		int tarefas = Math.clamp(tarefasPedidas, 1, MAXIMO_DE_TAREFAS);
		if (!this.umPorVez.tryAcquire()) {
			return null;
		}
		try {
			long plataforma = medir(Executors.newFixedThreadPool(THREADS_NO_POOL), tarefas);
			long virtual = medir(Executors.newVirtualThreadPerTaskExecutor(), tarefas);
			return new ResultadoBenchmark(tarefas, THREADS_NO_POOL, plataforma, virtual);
		}
		finally {
			this.umPorVez.release();
		}
	}

	private static long medir(ExecutorService executor, int tarefas) {
		long inicio = System.nanoTime();
		try (executor) {
			for (int i = 0; i < tarefas; i++) {
				executor.submit(() -> {
					Thread.sleep(Duration.ofSeconds(1));
					return null;
				});
			}
		}
		return (System.nanoTime() - inicio) / 1_000_000;
	}

}
