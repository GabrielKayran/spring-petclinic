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
package org.springframework.samples.petclinic.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AuditAspectTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApplicationContext context;

	@Test
	void controllersWithAuditedMethodsAreProxied() {
		assertThat(AopUtils.isAopProxy(this.context.getBean("vetController"))).isTrue();
		assertThat(AopUtils.isAopProxy(this.context.getBean("ownerController"))).isTrue();
		assertThat(AopUtils.isAopProxy(this.context.getBean("visitController"))).isTrue();
		assertThat(AopUtils.isAopProxy(this.context.getBean("welcomeController"))).isFalse();
	}

	@Test
	void logsMethodArgumentsAndDuration(CapturedOutput output) throws Exception {
		this.mockMvc.perform(get("/owners/{ownerId}", 1)).andExpect(status().isOk());

		assertThat(output.getOut()).contains("[AUDITORIA] OwnerController.showOwner(..) (Exibir dono) | args=[1] |")
			.containsPattern("\\| \\d+ ms \\|");
	}

	@Test
	void worksForMethodsWithoutArguments(CapturedOutput output) throws Exception {
		this.mockMvc.perform(get("/vets")).andExpect(status().isOk());

		assertThat(output.getOut()).contains("[AUDITORIA] VetController.showResourcesVetList()").contains("args=[]");
	}

}
