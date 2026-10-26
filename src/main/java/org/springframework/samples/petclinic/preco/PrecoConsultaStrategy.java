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

import org.springframework.samples.petclinic.owner.Pet;

/**
 * Extensão 4 (padrão Strategy): uma família de algoritmos intercambiáveis para calcular o
 * preço da consulta.
 * <p>
 * Cada implementação é um bean do Spring cujo <b>nome é o tipo de pet</b> da tabela
 * {@code types} ("dog", "cat"...). Para atender um novo tipo, basta criar uma nova classe
 * {@code @Component("hamster")}, sem alterar nenhum código existente (princípio
 * Aberto/Fechado).
 */
public interface PrecoConsultaStrategy {

	BigDecimal calcularPreco(Pet pet);

}
