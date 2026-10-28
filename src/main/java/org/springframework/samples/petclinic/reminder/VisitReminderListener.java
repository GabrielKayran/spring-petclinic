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
package org.springframework.samples.petclinic.reminder;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Observer that simulates sending a reminder to the pet owner. {@code @Async} runs it on
 * another thread, so the HTTP request that saved the visit does not wait for it.
 */
@Component
public class VisitReminderListener {

	private static final Logger log = LoggerFactory.getLogger(VisitReminderListener.class);

	static final Duration SEND_DELAY = Duration.ofSeconds(2);

	@Async
	@EventListener
	public void sendReminder(VisitRegisteredEvent event) throws InterruptedException {
		Thread thread = Thread.currentThread();
		log.info("[Observer] Thread '{}' (virtual={}): preparando lembrete para {} sobre a visita de {} em {}",
				thread.getName(), thread.isVirtual(), event.getOwnerName(), event.getPetName(), event.getVisitDate());

		Thread.sleep(SEND_DELAY);

		log.info("[Observer] Thread '{}': lembrete enviado para {} ({})", thread.getName(), event.getOwnerName(),
				event.getDescription());
	}

}
