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

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * Painel: guarda em memória as últimas linhas de log das extensões para exibi-las no
 * "console ao vivo" da página {@code /painel}.
 * <p>
 * As extensões não sabem que o painel existe: elas só escrevem no log, como sempre. Este
 * appender se pendura nos loggers dos pacotes {@code auditoria}, {@code lembrete} e
 * {@code grpc}. Assim a apresentação mostra os logs reais, inclusive no Render, onde não
 * temos um terminal à vista.
 */
@Component
class PainelLogAppender extends AppenderBase<ILoggingEvent> implements InitializingBean, DisposableBean {

	private static final List<String> PACOTES_OBSERVADOS = List.of("org.springframework.samples.petclinic.auditoria",
			"org.springframework.samples.petclinic.lembrete", "org.springframework.samples.petclinic.grpc");

	private static final int CAPACIDADE = 200;

	private final Deque<LinhaDeLog> linhas = new ArrayDeque<>();

	private final AtomicLong sequencia = new AtomicLong();

	record LinhaDeLog(long id, Instant hora, String thread, String origem, String mensagem) {
	}

	@Override
	public void afterPropertiesSet() {
		if (LoggerFactory.getILoggerFactory() instanceof LoggerContext contexto) {
			setContext(contexto);
			setName("painel");
			start();
			PACOTES_OBSERVADOS.forEach(pacote -> ((Logger) LoggerFactory.getLogger(pacote)).addAppender(this));
		}
	}

	@Override
	public void destroy() {
		PACOTES_OBSERVADOS.forEach(pacote -> {
			if (LoggerFactory.getLogger(pacote) instanceof Logger logger) {
				logger.detachAppender(this);
			}
		});
		stop();
	}

	@Override
	protected void append(ILoggingEvent evento) {
		String loggerName = evento.getLoggerName();
		LinhaDeLog linha = new LinhaDeLog(this.sequencia.incrementAndGet(), evento.getInstant(), evento.getThreadName(),
				loggerName.substring(loggerName.lastIndexOf('.') + 1), evento.getFormattedMessage());
		synchronized (this.linhas) {
			this.linhas.addLast(linha);
			if (this.linhas.size() > CAPACIDADE) {
				this.linhas.removeFirst();
			}
		}
	}

	/** Linhas com id maior que {@code depoisDe}, para o navegador buscar só as novas. */
	List<LinhaDeLog> linhasDepoisDe(long depoisDe) {
		synchronized (this.linhas) {
			return this.linhas.stream().filter(linha -> linha.id() > depoisDe).toList();
		}
	}

}
