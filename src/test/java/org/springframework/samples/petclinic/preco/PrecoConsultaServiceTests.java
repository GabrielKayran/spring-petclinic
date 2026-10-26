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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Extensão 4: verifica que o Spring injeta as estratégias no mapa pelo nome do bean e que
 * o serviço escolhe a estratégia certa para cada tipo de pet.
 * <p>
 * O contexto contém só as classes do Strategy, sem banco e sem web, então o teste é
 * rápido.
 */
@SpringJUnitConfig({ PrecoConsultaService.class, PrecoConsultaCao.class, PrecoConsultaGato.class,
		PrecoConsultaPadrao.class })
class PrecoConsultaServiceTests {

	@Autowired
	private PrecoConsultaService servico;

	@Test
	void caoPagaBaseMaisCincoReaisPorAnoDeIdade() {
		Pet rex = pet("dog", LocalDate.now().minusYears(4));
		// 100 + 4 * 5
		assertThat(this.servico.calcularPreco(rex)).isEqualByComparingTo("120.00");
	}

	@Test
	void gatoPagaPrecoFixo() {
		assertThat(this.servico.calcularPreco(pet("cat", LocalDate.now().minusYears(10))))
			.isEqualByComparingTo("90.00");
	}

	@Test
	void tipoSemEstrategiaPropriaUsaAPadrao() {
		assertThat(this.servico.calcularPreco(pet("hamster", LocalDate.now()))).isEqualByComparingTo("70.00");
		assertThat(this.servico.calcularPreco(pet("lizard", LocalDate.now()))).isEqualByComparingTo("70.00");
	}

	@Test
	void petSemTipoUsaAPadrao() {
		assertThat(this.servico.calcularPreco(new Pet())).isEqualByComparingTo("70.00");
	}

	private static Pet pet(String tipo, LocalDate nascimento) {
		PetType petType = new PetType();
		petType.setName(tipo);
		Pet pet = new Pet();
		pet.setType(petType);
		pet.setBirthDate(nascimento);
		return pet;
	}

}
