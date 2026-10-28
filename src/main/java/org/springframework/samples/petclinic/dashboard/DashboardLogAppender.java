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
 * Keeps the latest log lines of the audit, reminder and gRPC packages in memory for the
 * dashboard's live console. Those packages do not know the dashboard exists.
 */
@Component
class DashboardLogAppender extends AppenderBase<ILoggingEvent> implements InitializingBean, DisposableBean {

	private static final List<String> WATCHED_PACKAGES = List.of("org.springframework.samples.petclinic.audit",
			"org.springframework.samples.petclinic.reminder", "org.springframework.samples.petclinic.grpc");

	private static final int CAPACITY = 200;

	private final Deque<LogLine> lines = new ArrayDeque<>();

	private final AtomicLong sequence = new AtomicLong();

	record LogLine(long id, Instant time, String thread, String source, String message) {
	}

	@Override
	public void afterPropertiesSet() {
		if (LoggerFactory.getILoggerFactory() instanceof LoggerContext context) {
			setContext(context);
			setName("dashboard");
			start();
			WATCHED_PACKAGES.forEach(name -> ((Logger) LoggerFactory.getLogger(name)).addAppender(this));
		}
	}

	@Override
	public void destroy() {
		WATCHED_PACKAGES.forEach(name -> {
			if (LoggerFactory.getLogger(name) instanceof Logger logger) {
				logger.detachAppender(this);
			}
		});
		stop();
	}

	@Override
	protected void append(ILoggingEvent event) {
		String loggerName = event.getLoggerName();
		LogLine line = new LogLine(this.sequence.incrementAndGet(), event.getInstant(), event.getThreadName(),
				loggerName.substring(loggerName.lastIndexOf('.') + 1), event.getFormattedMessage());
		synchronized (this.lines) {
			this.lines.addLast(line);
			if (this.lines.size() > CAPACITY) {
				this.lines.removeFirst();
			}
		}
	}

	List<LogLine> linesAfter(long lastId) {
		synchronized (this.lines) {
			return this.lines.stream().filter(line -> line.id() > lastId).toList();
		}
	}

}
