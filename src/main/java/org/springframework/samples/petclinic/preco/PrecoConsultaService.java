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
import java.util.Map;
import java.util.Optional;

import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.stereotype.Service;

/**
 * Extensão 4: o <b>contexto</b> do padrão Strategy, que escolhe e executa a estratégia
 * certa.
 * <p>
 * O ponto principal está no construtor: ao pedir um {@code Map<String,
 * PrecoConsultaStrategy>}, o Spring injeta <b>todas</b> as implementações, indexadas pelo
 * nome do bean ("dog", "cat", "padrao"). A escolha vira uma consulta ao mapa, sem nenhum
 * {@code if} ou {@code switch} sobre o tipo do pet.
 */
@Service
public class PrecoConsultaService {

	private final Map<String, PrecoConsultaStrategy> estrategiasPorTipo;

	private final PrecoConsultaStrategy estrategiaPadrao;

	public PrecoConsultaService(Map<String, PrecoConsultaStrategy> estrategiasPorTipo) {
		this.estrategiasPorTipo = estrategiasPorTipo;
		this.estrategiaPadrao = estrategiasPorTipo.get(PrecoConsultaPadrao.NOME);
	}

	public BigDecimal calcularPreco(Pet pet) {
		return estrategiaPara(pet).calcularPreco(pet);
	}

	/**
	 * Escolhe a estratégia pelo tipo do pet. É público para o painel do estudo de caso
	 * mostrar qual classe foi usada.
	 */
	public PrecoConsultaStrategy estrategiaPara(Pet pet) {
		String tipo = Optional.ofNullable(pet.getType()).map(PetType::getName).orElse(PrecoConsultaPadrao.NOME);
		return this.estrategiasPorTipo.getOrDefault(tipo, this.estrategiaPadrao);
	}

}
