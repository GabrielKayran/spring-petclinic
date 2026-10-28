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
package org.springframework.samples.petclinic.dashboard;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.grpc.server.lifecycle.GrpcServerStartedEvent;
import org.springframework.samples.petclinic.grpc.proto.ListVetsRequest;
import org.springframework.samples.petclinic.grpc.proto.ListVetsResponse;
import org.springframework.samples.petclinic.grpc.proto.VetServiceGrpc;
import org.springframework.stereotype.Component;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import tools.jackson.databind.json.JsonMapper;

/**
 * Makes a real gRPC call over the network to this application's own gRPC server. Render
 * only exposes the HTTP port, so the browser goes through this HTTP endpoint instead.
 */
@Component
class GrpcDemoClient implements DisposableBean {

	private static final Logger log = LoggerFactory.getLogger(GrpcDemoClient.class);

	private final JsonMapper jsonMapper;

	private volatile int port = -1;

	private volatile ManagedChannel channel;

	GrpcDemoClient(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	record VetDto(int id, String firstName, String lastName, List<String> specialties) {
	}

	record GrpcResult(String target, long durationMs, int protobufBytes, int jsonBytes, List<VetDto> vets) {
	}

	@EventListener
	synchronized void onServerStarted(GrpcServerStartedEvent event) {
		// The server can restart on another port, so drop the stale channel
		if (this.channel != null && event.getPort() != this.port) {
			this.channel.shutdownNow();
			this.channel = null;
		}
		this.port = event.getPort();
	}

	/**
	 * The first call opens the HTTP/2 connection (about 5 s on Render), so do it early.
	 */
	@EventListener(ApplicationReadyEvent.class)
	void warmUp() {
		try {
			listVets();
		}
		catch (RuntimeException ex) {
			log.warn("Could not warm up the gRPC channel: {}", ex.getMessage());
		}
	}

	GrpcResult listVets() {
		if (this.port < 0) {
			throw new IllegalStateException("gRPC server is not running yet");
		}
		long start = System.nanoTime();
		ListVetsResponse response = VetServiceGrpc.newBlockingStub(channel())
			.withDeadlineAfter(10, TimeUnit.SECONDS)
			.listVets(ListVetsRequest.getDefaultInstance());
		long durationMs = (System.nanoTime() - start) / 1_000_000;

		List<VetDto> vets = response.getVetsList()
			.stream()
			.map(v -> new VetDto(v.getId(), v.getFirstName(), v.getLastName(), v.getSpecialtiesList()))
			.toList();
		int jsonBytes = this.jsonMapper.writeValueAsBytes(vets).length;
		return new GrpcResult("localhost:" + this.port, durationMs, response.getSerializedSize(), jsonBytes, vets);
	}

	private ManagedChannel channel() {
		if (this.channel == null) {
			synchronized (this) {
				if (this.channel == null) {
					this.channel = ManagedChannelBuilder.forAddress("localhost", this.port).usePlaintext().build();
				}
			}
		}
		return this.channel;
	}

	@Override
	public void destroy() throws InterruptedException {
		if (this.channel != null) {
			this.channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
		}
	}

}
