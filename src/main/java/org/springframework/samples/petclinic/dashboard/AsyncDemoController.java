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

import java.time.LocalDate;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.samples.petclinic.reminder.VisitRegisteredEvent;
import org.springframework.samples.petclinic.reminder.VisitReminderListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Calls the real reminder listener with and without its {@code @Async} proxy, so the
 * difference shows up without saving a visit. Bypassing the proxy is what happens when
 * {@code @Async} is missing or the method is called through {@code this}.
 */
@RestController
class AsyncDemoController {

	static final String OWNER = "Demo do painel";

	private final VisitReminderListener listener;

	AsyncDemoController(VisitReminderListener listener) {
		this.listener = listener;
	}

	record AsyncCall(boolean throughProxy, String calledClass, String requestThread, long millis) {
	}

	@GetMapping("/api/dashboard/async")
	AsyncCall call(@RequestParam boolean proxy) throws InterruptedException {
		VisitReminderListener target = proxy ? this.listener : unproxied(this.listener);
		VisitRegisteredEvent event = new VisitRegisteredEvent(this, OWNER, "nenhum (visita não gravada)",
				LocalDate.now().plusDays(1), proxy ? "pelo proxy, com @Async" : "direto no objeto, sem @Async");

		long start = System.nanoTime();
		target.sendReminder(event);
		long millis = (System.nanoTime() - start) / 1_000_000;

		return new AsyncCall(proxy, target.getClass().getSimpleName(), Thread.currentThread().getName(), millis);
	}

	private static VisitReminderListener unproxied(VisitReminderListener listener) {
		Object target = AopProxyUtils.getSingletonTarget(listener);
		return (target != null) ? (VisitReminderListener) target : listener;
	}

}
