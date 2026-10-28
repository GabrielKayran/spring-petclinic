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
 * Adds the visit price to the "new visit" page without touching {@code VisitController}.
 * The service is optional because {@code @WebMvcTest} slices do not load it.
 */
@Configuration(proxyBeanMethods = false)
class VisitPriceWebConfiguration implements WebMvcConfigurer, HandlerInterceptor {

	private final ObjectProvider<VisitPriceService> visitPriceService;

	VisitPriceWebConfiguration(ObjectProvider<VisitPriceService> visitPriceService) {
		this.visitPriceService = visitPriceService;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(this).addPathPatterns("/owners/*/pets/*/visits/new");
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) {
		if (modelAndView != null && modelAndView.getModel().get("pet") instanceof Pet pet) {
			this.visitPriceService
				.ifAvailable(service -> modelAndView.addObject("visitPrice", service.calculatePrice(pet)));
		}
	}

}
