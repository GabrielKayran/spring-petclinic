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
package org.springframework.samples.petclinic.grpc.cliente;

import java.util.concurrent.TimeUnit;

import org.springframework.samples.petclinic.grpc.proto.ListarVeterinariosRequest;
import org.springframework.samples.petclinic.grpc.proto.ListarVeterinariosResponse;
import org.springframework.samples.petclinic.grpc.proto.Veterinario;
import org.springframework.samples.petclinic.grpc.proto.VeterinarioServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Extensão 1: cliente gRPC de linha de comando.
 * <p>
 * É um programa separado, que roda em outra JVM e não sobe o Spring. Ele usa apenas o
 * stub gerado a partir do .proto. Compare com o {@code Cliente.java} da demo RMI: a ideia
 * é a mesma (chamar um método remoto através de um stub), mas o transporte é HTTP/2 e os
 * dados são serializados com Protocol Buffers.
 * <p>
 * Para rodar, com o PetClinic no ar: {@code ./mvnw -q exec:java@cliente-grpc}. Não use
 * "compile" antes: recompilar faz o DevTools reiniciar a aplicação, e o gRPC fica fora do
 * ar por alguns segundos. Também aceita host e porta como argumentos.
 */
public final class ClienteGrpc {

	private ClienteGrpc() {
	}

	public static void main(String[] args) throws InterruptedException {
		String host = args.length > 0 ? args[0] : "localhost";
		int porta = args.length > 1 ? Integer.parseInt(args[1]) : 9090;

		// O canal é a conexão HTTP/2. usePlaintext() desliga o TLS, o que só é aceitável
		// numa demo local.
		ManagedChannel canal = ManagedChannelBuilder.forAddress(host, porta).usePlaintext().build();
		try {
			// Stub bloqueante: a chamada parece local, igual ao stub do RMI
			VeterinarioServiceGrpc.VeterinarioServiceBlockingStub stub = VeterinarioServiceGrpc.newBlockingStub(canal);
			ListarVeterinariosResponse resposta = stub
				.listarVeterinarios(ListarVeterinariosRequest.getDefaultInstance());

			System.out.println("Veterinários recebidos via gRPC de " + host + ":" + porta);
			for (Veterinario vet : resposta.getVeterinariosList()) {
				String especialidades = vet.getEspecialidadesList().isEmpty() ? "nenhuma"
						: String.join(", ", vet.getEspecialidadesList());
				System.out.printf(" - #%d %s %s (especialidades: %s)%n", vet.getId(), vet.getNome(), vet.getSobrenome(),
						especialidades);
			}
		}
		finally {
			canal.shutdown().awaitTermination(5, TimeUnit.SECONDS);
		}
	}

}
