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
package org.springframework.samples.petclinic.report;

import java.math.BigDecimal;

/**
 * One report line. A JavaBean because {@code JRBeanCollectionDataSource} reads each
 * {@code <field>} of the layout through the getter with the same name.
 */
public class ReportRow {

	private final String type;

	private final String strategy;

	private final String pet;

	private final String owner;

	private final Integer age;

	private final Integer visits;

	private final BigDecimal price;

	public ReportRow(String type, String strategy, String pet, String owner, Integer age, Integer visits,
			BigDecimal price) {
		this.type = type;
		this.strategy = strategy;
		this.pet = pet;
		this.owner = owner;
		this.age = age;
		this.visits = visits;
		this.price = price;
	}

	public String getType() {
		return this.type;
	}

	public String getStrategy() {
		return this.strategy;
	}

	public String getPet() {
		return this.pet;
	}

	public String getOwner() {
		return this.owner;
	}

	public Integer getAge() {
		return this.age;
	}

	public Integer getVisits() {
		return this.visits;
	}

	public BigDecimal getPrice() {
		return this.price;
	}

}
