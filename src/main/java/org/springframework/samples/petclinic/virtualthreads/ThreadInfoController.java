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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Shows which thread served the request. With {@code spring.threads.virtual.enabled=true}
 * Tomcat serves every request on a virtual thread.
 */
@RestController
class ThreadInfoController {

	@GetMapping("/thread")
	ThreadInfo currentThread() {
		Thread current = Thread.currentThread();
		return new ThreadInfo(current.getName(), current.isVirtual(), current.isVirtual() ? "virtual" : "plataforma",
				current.toString());
	}

	/**
	 * {@code details} holds {@link Thread#toString()}, which names the carrier thread.
	 */
	record ThreadInfo(String name, boolean virtual, String type, String details) {
	}

}
