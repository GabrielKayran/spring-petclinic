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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig({ VisitPriceService.class, DogPriceStrategy.class, CatPriceStrategy.class,
		DefaultPriceStrategy.class })
class VisitPriceServiceTests {

	@Autowired
	private VisitPriceService service;

	@Test
	void dogPaysBasePriceAndFiveReaisPerYear() {
		Pet rex = pet("dog", LocalDate.now().minusYears(4));
		assertThat(this.service.calculatePrice(rex)).isEqualByComparingTo("120.00");
	}

	@Test
	void catPaysAFixedPrice() {
		assertThat(this.service.calculatePrice(pet("cat", LocalDate.now().minusYears(10))))
			.isEqualByComparingTo("90.00");
	}

	@Test
	void typesWithoutAStrategyUseTheDefault() {
		assertThat(this.service.calculatePrice(pet("hamster", LocalDate.now()))).isEqualByComparingTo("70.00");
		assertThat(this.service.calculatePrice(pet("lizard", LocalDate.now()))).isEqualByComparingTo("70.00");
	}

	@Test
	void petWithoutTypeUsesTheDefault() {
		assertThat(this.service.calculatePrice(new Pet())).isEqualByComparingTo("70.00");
	}

	private static Pet pet(String type, LocalDate birthDate) {
		PetType petType = new PetType();
		petType.setName(type);
		Pet pet = new Pet();
		pet.setType(petType);
		pet.setBirthDate(birthDate);
		return pet;
	}

}
