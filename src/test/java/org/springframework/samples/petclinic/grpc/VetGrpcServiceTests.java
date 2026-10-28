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
package org.springframework.samples.petclinic.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.samples.petclinic.grpc.proto.ListVetsRequest;
import org.springframework.samples.petclinic.grpc.proto.ListVetsResponse;
import org.springframework.samples.petclinic.grpc.proto.VetInfo;
import org.springframework.samples.petclinic.grpc.proto.VetServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/** Calls the real gRPC server (Netty, random port) over the network. */
@SpringBootTest
class VetGrpcServiceTests {

	@LocalGrpcServerPort
	private int grpcPort;

	private ManagedChannel channel;

	@BeforeEach
	void openChannel() {
		this.channel = ManagedChannelBuilder.forAddress("localhost", this.grpcPort).usePlaintext().build();
	}

	@AfterEach
	void closeChannel() throws InterruptedException {
		this.channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
	}

	@Test
	void listsTheVetsFromTheDatabase() {
		ListVetsResponse response = VetServiceGrpc.newBlockingStub(this.channel)
			.listVets(ListVetsRequest.getDefaultInstance());

		assertThat(response.getVetsList()).hasSize(6);
		assertThat(response.getVetsList()).extracting(VetInfo::getLastName)
			.contains("Carter", "Leary", "Douglas", "Ortega", "Stevens", "Jenkins");
		VetInfo douglas = response.getVetsList()
			.stream()
			.filter(v -> v.getLastName().equals("Douglas"))
			.findFirst()
			.orElseThrow();
		assertThat(douglas.getSpecialtiesList()).containsExactly("dentistry", "surgery");
	}

}
