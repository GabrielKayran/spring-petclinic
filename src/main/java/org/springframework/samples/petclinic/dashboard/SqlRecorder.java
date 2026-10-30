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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Captures the SQL that Hibernate sends to the database while an action runs on the
 * current thread.
 */
@Component
class SqlRecorder implements StatementInspector, HibernatePropertiesCustomizer {

	private static final ThreadLocal<List<String>> RECORDING = new ThreadLocal<>();

	record Recorded<T>(T result, List<String> statements, long millis) {
	}

	@Override
	public void customize(Map<String, Object> hibernateProperties) {
		hibernateProperties.put(AvailableSettings.STATEMENT_INSPECTOR, this);
	}

	@Override
	public String inspect(String sql) {
		List<String> statements = RECORDING.get();
		if (statements != null) {
			statements.add(sql);
		}
		return sql;
	}

	<T> Recorded<T> record(Supplier<T> action) {
		List<String> statements = new ArrayList<>();
		RECORDING.set(statements);
		long start = System.nanoTime();
		try {
			T result = action.get();
			return new Recorded<>(result, List.copyOf(statements), (System.nanoTime() - start) / 1_000_000);
		}
		finally {
			RECORDING.remove();
		}
	}

}
