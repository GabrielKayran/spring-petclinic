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
package org.springframework.samples.petclinic.relatorio;

import java.math.BigDecimal;

/**
 * Uma linha do relatório. É um JavaBean clássico (getters), porque o
 * {@code JRBeanCollectionDataSource} do Jasper lê cada {@code <field>} do .jrxml pelo
 * getter de mesmo nome: o campo {@code preco} vem de {@link #getPreco()}.
 */
public class LinhaRelatorio {

	private final String tipo;

	private final String estrategia;

	private final String pet;

	private final String dono;

	private final Integer idade;

	private final Integer visitas;

	private final BigDecimal preco;

	public LinhaRelatorio(String tipo, String estrategia, String pet, String dono, Integer idade, Integer visitas,
			BigDecimal preco) {
		this.tipo = tipo;
		this.estrategia = estrategia;
		this.pet = pet;
		this.dono = dono;
		this.idade = idade;
		this.visitas = visitas;
		this.preco = preco;
	}

	public String getTipo() {
		return this.tipo;
	}

	public String getEstrategia() {
		return this.estrategia;
	}

	public String getPet() {
		return this.pet;
	}

	public String getDono() {
		return this.dono;
	}

	public Integer getIdade() {
		return this.idade;
	}

	public Integer getVisitas() {
		return this.visitas;
	}

	public BigDecimal getPreco() {
		return this.preco;
	}

}
