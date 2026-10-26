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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.samples.petclinic.grpc.proto.ListarVeterinariosRequest;
import org.springframework.samples.petclinic.grpc.proto.ListarVeterinariosResponse;
import org.springframework.samples.petclinic.grpc.proto.Veterinario;
import org.springframework.samples.petclinic.grpc.proto.VeterinarioServiceGrpc;
import org.springframework.samples.petclinic.model.NamedEntity;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.stereotype.Service;

import io.grpc.stub.StreamObserver;

/**
 * Extensão 1: implementação do serviço gRPC definido em
 * {@code src/main/protobuf/veterinarios.proto}.
 * <p>
 * A classe base {@code VeterinarioServiceImplBase} é gerada pelo Maven a partir do
 * .proto. Ela faz o papel do "skeleton" do RMI: recebe a chamada da rede e delega para
 * este método. Por ser um bean do Spring, o Spring Boot registra o serviço
 * automaticamente no servidor gRPC (porta 9090 por padrão), sem nenhum código extra de
 * infraestrutura.
 * <p>
 * Os dados vêm do {@link VetRepository} que o PetClinic já usa nas telas, e nada foi
 * duplicado.
 */
@Service
public class VeterinarioGrpcService extends VeterinarioServiceGrpc.VeterinarioServiceImplBase {

	private static final Logger log = LoggerFactory.getLogger(VeterinarioGrpcService.class);

	private final VetRepository vetRepository;

	public VeterinarioGrpcService(VetRepository vetRepository) {
		this.vetRepository = vetRepository;
	}

	@Override
	public void listarVeterinarios(ListarVeterinariosRequest request,
			StreamObserver<ListarVeterinariosResponse> responseObserver) {
		log.info("[gRPC] ListarVeterinarios recebido na thread {}", Thread.currentThread().getName());

		// Converte as entidades JPA (Vet) nas mensagens do protobuf (Veterinario)
		ListarVeterinariosResponse.Builder resposta = ListarVeterinariosResponse.newBuilder();
		for (Vet vet : this.vetRepository.findAll()) {
			resposta.addVeterinarios(paraMensagem(vet));
		}

		// No gRPC a resposta é um fluxo: enviamos uma mensagem e sinalizamos o fim
		responseObserver.onNext(resposta.build());
		responseObserver.onCompleted();
	}

	private Veterinario paraMensagem(Vet vet) {
		return Veterinario.newBuilder()
			.setId(vet.getId())
			.setNome(vet.getFirstName())
			.setSobrenome(vet.getLastName())
			.addAllEspecialidades(vet.getSpecialties().stream().map(NamedEntity::getName).toList())
			.build();
	}

}
