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
package org.springframework.samples.petclinic.lembrete;

import java.time.LocalDate;

import org.springframework.context.ApplicationEvent;

/**
 * Extensão 3 (padrão Observer): evento publicado quando uma visita é registrada.
 * <p>
 * No vocabulário do Observer: o {@code VisitController} é o <b>sujeito</b> (quem
 * notifica), este evento é a <b>notificação</b>, e os listeners, como o
 * {@link LembreteVisitaListener}, são os <b>observadores</b>. O Spring faz o papel da
 * lista de observadores: o sujeito não sabe quem está ouvindo.
 * <p>
 * O evento carrega só dados simples (Strings e datas), e não as entidades JPA, porque ele
 * será processado em outra thread, fora da transação que salvou a visita.
 */
public class VisitaRegistradaEvent extends ApplicationEvent {

	private final String nomeDono;

	private final String nomePet;

	private final LocalDate dataVisita;

	private final String descricao;

	public VisitaRegistradaEvent(Object origem, String nomeDono, String nomePet, LocalDate dataVisita,
			String descricao) {
		super(origem);
		this.nomeDono = nomeDono;
		this.nomePet = nomePet;
		this.dataVisita = dataVisita;
		this.descricao = descricao;
	}

	public String getNomeDono() {
		return this.nomeDono;
	}

	public String getNomePet() {
		return this.nomePet;
	}

	public LocalDate getDataVisita() {
		return this.dataVisita;
	}

	public String getDescricao() {
		return this.descricao;
	}

}
