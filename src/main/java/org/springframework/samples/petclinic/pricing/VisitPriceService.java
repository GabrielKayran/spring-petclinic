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
package org.springframework.samples.petclinic.pricing;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.stereotype.Service;

/**
 * Strategy context. Spring injects every {@link VisitPriceStrategy} keyed by bean name,
 * so picking one is a map lookup instead of an {@code if}/{@code switch} on the pet type.
 */
@Service
public class VisitPriceService {

	private final Map<String, VisitPriceStrategy> strategiesByType;

	private final VisitPriceStrategy defaultStrategy;

	public VisitPriceService(Map<String, VisitPriceStrategy> strategiesByType) {
		this.strategiesByType = strategiesByType;
		this.defaultStrategy = strategiesByType.get(DefaultPriceStrategy.NAME);
	}

	public BigDecimal calculatePrice(Pet pet) {
		return strategyFor(pet).calculatePrice(pet);
	}

	public VisitPriceStrategy strategyFor(Pet pet) {
		String type = Optional.ofNullable(pet.getType()).map(PetType::getName).orElse(DefaultPriceStrategy.NAME);
		return this.strategiesByType.getOrDefault(type, this.defaultStrategy);
	}

}
