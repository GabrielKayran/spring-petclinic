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

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Extensão 3: observador que "envia" um lembrete ao dono do pet.
 * <p>
 * {@code @EventListener} inscreve o método para receber {@link VisitaRegistradaEvent}.
 * {@code @Async} faz o Spring executá-lo em outra thread do executor de tarefas. Assim, a
 * requisição HTTP que registrou a visita responde na hora, sem esperar o envio do
 * lembrete. Compare os nomes de thread no log: a requisição roda numa thread do Tomcat e
 * o lembrete numa thread "task-N".
 */
@Component
public class LembreteVisitaListener {

	private static final Logger log = LoggerFactory.getLogger(LembreteVisitaListener.class);

	/** Tempo simulado de envio de um e-mail ou SMS. */
	static final Duration TEMPO_DE_ENVIO = Duration.ofSeconds(2);

	@Async
	@EventListener
	public void enviarLembrete(VisitaRegistradaEvent evento) throws InterruptedException {
		Thread thread = Thread.currentThread();
		log.info("[Observer] Thread '{}' (virtual={}): preparando lembrete para {} sobre a visita de {} em {}",
				thread.getName(), thread.isVirtual(), evento.getNomeDono(), evento.getNomePet(),
				evento.getDataVisita());

		// Simula a demora de um serviço externo de e-mail/SMS
		Thread.sleep(TEMPO_DE_ENVIO);

		log.info("[Observer] Thread '{}': lembrete enviado para {} ({})", thread.getName(), evento.getNomeDono(),
				evento.getDescricao());
	}

}
