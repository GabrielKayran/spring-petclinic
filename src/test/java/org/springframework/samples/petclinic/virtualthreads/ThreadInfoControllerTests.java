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
package org.springframework.samples.petclinic.virtualthreads;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.virtualthreads.ThreadInfoController.ThreadInfo;

/**
 * Uses a real HTTP request: MockMvc runs the controller on the JUnit thread, so it would
 * never show a virtual thread.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ThreadInfoControllerTests {

	@LocalServerPort
	private int port;

	@Autowired
	private RestTemplateBuilder builder;

	@Test
	void requestIsServedByAVirtualThread() {
		ResponseEntity<ThreadInfo> response = this.builder.baseUri("http://localhost:" + this.port)
			.build()
			.getForEntity("/thread", ThreadInfo.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		ThreadInfo info = response.getBody();
		assertThat(info).isNotNull();
		assertThat(info.virtual()).isTrue();
		assertThat(info.type()).isEqualTo("virtual");
		assertThat(info.details()).startsWith("VirtualThread");
	}

}
