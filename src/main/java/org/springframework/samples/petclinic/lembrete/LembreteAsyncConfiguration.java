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
package org.springframework.samples.petclinic.lembrete;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Extensão 3: liga o suporte a {@code @Async}.
 * <p>
 * Sem esta anotação, o {@code @Async} é ignorado e o listener roda na mesma thread da
 * requisição. O Spring Boot já fornece o executor ({@code applicationTaskExecutor}): um
 * pool de threads de plataforma ou, se {@code spring.threads.virtual.enabled=true}, uma
 * virtual thread por tarefa.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
class LembreteAsyncConfiguration {

}
