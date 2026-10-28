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
import java.time.LocalDate;
import java.time.Period;

import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.stereotype.Component;

/** Base price plus a surcharge for each year of age. */
@Component("dog")
class DogPriceStrategy implements VisitPriceStrategy {

	private static final BigDecimal BASE_PRICE = new BigDecimal("100.00");

	private static final BigDecimal PER_YEAR_OF_AGE = new BigDecimal("5.00");

	@Override
	public BigDecimal calculatePrice(Pet pet) {
		int age = (pet.getBirthDate() == null) ? 0 : Period.between(pet.getBirthDate(), LocalDate.now()).getYears();
		return BASE_PRICE.add(PER_YEAR_OF_AGE.multiply(BigDecimal.valueOf(age)));
	}

}
