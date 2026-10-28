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
package org.springframework.samples.petclinic.system;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Short health check for load balancers and uptime monitors. Delegates to the actuator
 * health endpoint (database, disk space, ...) and answers 503 when the status is not UP.
 * If the actuator endpoint is disabled, it only reports that the app is serving requests.
 */
@RestController
class HealthController {

	private final ObjectProvider<HealthEndpoint> healthEndpoint;

	HealthController(ObjectProvider<HealthEndpoint> healthEndpoint) {
		this.healthEndpoint = healthEndpoint;
	}

	@GetMapping("/health")
	ResponseEntity<Map<String, String>> health() {
		HealthEndpoint endpoint = this.healthEndpoint.getIfAvailable();
		Status status = (endpoint != null) ? endpoint.health().getStatus() : Status.UP;
		HttpStatus httpStatus = Status.UP.equals(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
		return ResponseEntity.status(httpStatus).body(Map.of("status", status.getCode()));
	}

}
