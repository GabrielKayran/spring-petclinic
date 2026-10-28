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

import java.time.LocalDate;

import org.springframework.context.ApplicationEvent;

/**
 * Published when a visit is saved (Observer pattern). Carries plain values instead of JPA
 * entities because listeners run on another thread, outside the transaction.
 */
public class VisitRegisteredEvent extends ApplicationEvent {

	private final String ownerName;

	private final String petName;

	private final LocalDate visitDate;

	private final String description;

	public VisitRegisteredEvent(Object source, String ownerName, String petName, LocalDate visitDate,
			String description) {
		super(source);
		this.ownerName = ownerName;
		this.petName = petName;
		this.visitDate = visitDate;
		this.description = description;
	}

	public String getOwnerName() {
		return this.ownerName;
	}

	public String getPetName() {
		return this.petName;
	}

	public LocalDate getVisitDate() {
		return this.visitDate;
	}

	public String getDescription() {
		return this.description;
	}

}
