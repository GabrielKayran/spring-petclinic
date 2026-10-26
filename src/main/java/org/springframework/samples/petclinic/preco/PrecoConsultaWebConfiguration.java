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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Extensão 4: coloca o preço da consulta na tela de nova visita sem alterar o
 * {@code VisitController}.
 * <p>
 * Um {@link HandlerInterceptor} roda depois do controller e antes de o Thymeleaf
 * renderizar a página. Ele lê o pet que o controller colocou no model e adiciona o
 * atributo {@code precoConsulta}, que o template {@code createOrUpdateVisitForm.html}
 * exibe.
 * <p>
 * Usamos {@link ObjectProvider} porque, nos testes {@code @WebMvcTest}, o
 * {@link PrecoConsultaService} não é carregado. Nesse caso o preço simplesmente não é
 * exibido.
 */
@Configuration(proxyBeanMethods = false)
class PrecoConsultaWebConfiguration implements WebMvcConfigurer, HandlerInterceptor {

	private final ObjectProvider<PrecoConsultaService> precoConsultaService;

	PrecoConsultaWebConfiguration(ObjectProvider<PrecoConsultaService> precoConsultaService) {
		this.precoConsultaService = precoConsultaService;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(this).addPathPatterns("/owners/*/pets/*/visits/new");
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) {
		if (modelAndView != null && modelAndView.getModel().get("pet") instanceof Pet pet) {
			this.precoConsultaService
				.ifAvailable(servico -> modelAndView.addObject("precoConsulta", servico.calcularPreco(pet)));
		}
	}

}
