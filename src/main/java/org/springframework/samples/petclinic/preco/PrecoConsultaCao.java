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
package org.springframework.samples.petclinic.preco;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.stereotype.Component;

/**
 * Estratégia para cães: R$ 100,00 de base mais R$ 5,00 por ano de idade, já que cães mais
 * velhos costumam precisar de mais exames.
 * <p>
 * O nome do bean, "dog", é o mesmo nome do tipo na tabela {@code types}. É assim que o
 * {@link PrecoConsultaService} encontra esta estratégia.
 */
@Component("dog")
class PrecoConsultaCao implements PrecoConsultaStrategy {

	private static final BigDecimal BASE = new BigDecimal("100.00");

	private static final BigDecimal POR_ANO_DE_IDADE = new BigDecimal("5.00");

	@Override
	public BigDecimal calcularPreco(Pet pet) {
		int idade = (pet.getBirthDate() == null) ? 0 : Period.between(pet.getBirthDate(), LocalDate.now()).getYears();
		return BASE.add(POR_ANO_DE_IDADE.multiply(BigDecimal.valueOf(idade)));
	}

}
