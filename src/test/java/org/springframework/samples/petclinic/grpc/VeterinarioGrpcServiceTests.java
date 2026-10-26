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
import org.springframework.samples.petclinic.grpc.proto.ListarVeterinariosRequest;
import org.springframework.samples.petclinic.grpc.proto.ListarVeterinariosResponse;
import org.springframework.samples.petclinic.grpc.proto.Veterinario;
import org.springframework.samples.petclinic.grpc.proto.VeterinarioServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Extensão 1: sobe a aplicação inteira com o servidor gRPC real (Netty) em uma porta
 * livre e faz uma chamada de rede de verdade, do mesmo jeito que o {@code ClienteGrpc}
 * faz.
 */
@SpringBootTest
class VeterinarioGrpcServiceTests {

	@LocalGrpcServerPort
	private int portaGrpc;

	private ManagedChannel canal;

	@BeforeEach
	void abrirCanal() {
		this.canal = ManagedChannelBuilder.forAddress("localhost", this.portaGrpc).usePlaintext().build();
	}

	@AfterEach
	void fecharCanal() throws InterruptedException {
		this.canal.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
	}

	@Test
	void deveListarOsVeterinariosDoBancoViaGrpc() {
		VeterinarioServiceGrpc.VeterinarioServiceBlockingStub stub = VeterinarioServiceGrpc.newBlockingStub(this.canal);

		ListarVeterinariosResponse resposta = stub.listarVeterinarios(ListarVeterinariosRequest.getDefaultInstance());

		// O data.sql do H2 cadastra 6 veterinários
		assertThat(resposta.getVeterinariosList()).hasSize(6);
		assertThat(resposta.getVeterinariosList()).extracting(Veterinario::getSobrenome)
			.contains("Carter", "Leary", "Douglas", "Ortega", "Stevens", "Jenkins");
		Veterinario douglas = resposta.getVeterinariosList()
			.stream()
			.filter(v -> v.getSobrenome().equals("Douglas"))
			.findFirst()
			.orElseThrow();
		assertThat(douglas.getEspecialidadesList()).containsExactly("dentistry", "surgery");
	}

}
