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
package org.springframework.samples.petclinic.virtualthreads;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Extensão 2: mostra qual thread atendeu a requisição HTTP.
 * <p>
 * Com {@code spring.threads.virtual.enabled=true} no application.properties, o Tomcat
 * passa a criar uma virtual thread por requisição em vez de usar o pool de threads de
 * plataforma (que por padrão tem 200 threads). Abra {@code /thread} no navegador e
 * confira o campo {@code virtual}.
 */
@RestController
class ThreadInfoController {

	@GetMapping("/thread")
	ThreadInfo threadAtual() {
		Thread atual = Thread.currentThread();
		return new ThreadInfo(atual.getName(), atual.isVirtual(), atual.isVirtual() ? "virtual" : "plataforma",
				atual.toString());
	}

	/**
	 * Resposta convertida em JSON. O campo {@code detalhes} usa o toString() da thread.
	 * Numa virtual thread ele mostra também a carrier thread, ou seja, a thread do SO
	 * (ForkJoinPool) em que ela está montada naquele momento.
	 */
	record ThreadInfo(String nome, boolean virtual, String tipo, String detalhes) {
	}

}
