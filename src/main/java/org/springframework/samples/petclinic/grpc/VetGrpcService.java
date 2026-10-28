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
import org.springframework.samples.petclinic.grpc.proto.ListVetsRequest;
import org.springframework.samples.petclinic.grpc.proto.ListVetsResponse;
import org.springframework.samples.petclinic.grpc.proto.VetInfo;
import org.springframework.samples.petclinic.grpc.proto.VetServiceGrpc;
import org.springframework.samples.petclinic.model.NamedEntity;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.stereotype.Service;

import io.grpc.stub.StreamObserver;

/**
 * gRPC implementation of the service defined in {@code src/main/protobuf/vets.proto}.
 * Spring Boot registers it on the gRPC server (port 9090) because it is a bean.
 */
@Service
public class VetGrpcService extends VetServiceGrpc.VetServiceImplBase {

	private static final Logger log = LoggerFactory.getLogger(VetGrpcService.class);

	private final VetRepository vetRepository;

	public VetGrpcService(VetRepository vetRepository) {
		this.vetRepository = vetRepository;
	}

	@Override
	public void listVets(ListVetsRequest request, StreamObserver<ListVetsResponse> responseObserver) {
		log.info("[gRPC] ListVets recebido na thread {}", Thread.currentThread().getName());

		ListVetsResponse.Builder response = ListVetsResponse.newBuilder();
		for (Vet vet : this.vetRepository.findAll()) {
			response.addVets(toMessage(vet));
		}
		responseObserver.onNext(response.build());
		responseObserver.onCompleted();
	}

	private VetInfo toMessage(Vet vet) {
		return VetInfo.newBuilder()
			.setId(vet.getId())
			.setFirstName(vet.getFirstName())
			.setLastName(vet.getLastName())
			.addAllSpecialties(vet.getSpecialties().stream().map(NamedEntity::getName).toList())
			.build();
	}

}
