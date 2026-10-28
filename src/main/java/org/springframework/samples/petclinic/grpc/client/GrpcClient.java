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
package org.springframework.samples.petclinic.grpc.client;

import java.util.concurrent.TimeUnit;

import org.springframework.samples.petclinic.grpc.proto.ListVetsRequest;
import org.springframework.samples.petclinic.grpc.proto.ListVetsResponse;
import org.springframework.samples.petclinic.grpc.proto.VetInfo;
import org.springframework.samples.petclinic.grpc.proto.VetServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Standalone command-line gRPC client (no Spring). Run it with the application up:
 * {@code ./mvnw -q exec:java@grpc-client}. Avoid {@code compile} while the app is
 * running: DevTools would restart it and take the gRPC server down for a few seconds.
 */
public final class GrpcClient {

	private GrpcClient() {
	}

	public static void main(String[] args) throws InterruptedException {
		String host = args.length > 0 ? args[0] : "localhost";
		int port = args.length > 1 ? Integer.parseInt(args[1]) : 9090;

		ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
		try {
			ListVetsResponse response = VetServiceGrpc.newBlockingStub(channel)
				.listVets(ListVetsRequest.getDefaultInstance());

			System.out.println("Veterinários recebidos via gRPC de " + host + ":" + port);
			for (VetInfo vet : response.getVetsList()) {
				String specialties = vet.getSpecialtiesList().isEmpty() ? "nenhuma"
						: String.join(", ", vet.getSpecialtiesList());
				System.out.printf(" - #%d %s %s (especialidades: %s)%n", vet.getId(), vet.getFirstName(),
						vet.getLastName(), specialties);
			}
		}
		finally {
			channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
		}
	}

}
