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
package org.springframework.samples.petclinic.painel;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.grpc.server.lifecycle.GrpcServerStartedEvent;
import org.springframework.samples.petclinic.grpc.proto.ListarVeterinariosRequest;
import org.springframework.samples.petclinic.grpc.proto.ListarVeterinariosResponse;
import org.springframework.samples.petclinic.grpc.proto.VeterinarioServiceGrpc;
import org.springframework.stereotype.Component;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import tools.jackson.databind.json.JsonMapper;

/**
 * Painel: faz uma chamada gRPC <b>de verdade</b>, pela rede, ao servidor gRPC da própria
 * aplicação, do mesmo jeito que o {@code ClienteGrpc}.
 * <p>
 * Isso é necessário no Render, que só expõe uma porta HTTP para a internet. O navegador
 * chama este endpoint por HTTP, e o servidor chama o gRPC (porta 9090) internamente.
 */
@Component
class GrpcDemoCliente implements DisposableBean {

	private static final Logger log = LoggerFactory.getLogger(GrpcDemoCliente.class);

	private final JsonMapper jsonMapper;

	private volatile int porta = -1;

	private volatile ManagedChannel canal;

	GrpcDemoCliente(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	record VeterinarioDto(int id, String nome, String sobrenome, List<String> especialidades) {
	}

	record ResultadoGrpc(String alvo, long duracaoMs, int bytesProtobuf, int bytesJson,
			List<VeterinarioDto> veterinarios) {
	}

	/**
	 * O servidor gRPC avisa em qual porta subiu (9090, ou uma porta livre nos testes).
	 */
	@EventListener
	synchronized void servidorGrpcIniciado(GrpcServerStartedEvent evento) {
		// Se o servidor reiniciar em outra porta, o canal antigo aponta para o lugar
		// errado
		if (this.canal != null && evento.getPort() != this.porta) {
			this.canal.shutdownNow();
			this.canal = null;
		}
		this.porta = evento.getPort();
	}

	/**
	 * Aquece o canal assim que a aplicação fica pronta. A primeira chamada paga o custo
	 * de abrir a conexão HTTP/2 e de compilar o código (no Render, cerca de 5 s). Sem
	 * isso, a primeira demonstração ao vivo mostraria esse tempo em vez dos poucos
	 * milissegundos de uma chamada normal.
	 */
	@EventListener(ApplicationReadyEvent.class)
	void aquecerCanal() {
		try {
			listarVeterinarios();
		}
		catch (RuntimeException ex) {
			log.warn("Não foi possível aquecer o canal gRPC: {}", ex.getMessage());
		}
	}

	ResultadoGrpc listarVeterinarios() {
		if (this.porta < 0) {
			throw new IllegalStateException("O servidor gRPC ainda não está no ar");
		}
		long inicio = System.nanoTime();
		ListarVeterinariosResponse resposta = VeterinarioServiceGrpc.newBlockingStub(canal())
			.withDeadlineAfter(10, TimeUnit.SECONDS)
			.listarVeterinarios(ListarVeterinariosRequest.getDefaultInstance());
		long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;

		List<VeterinarioDto> veterinarios = resposta.getVeterinariosList()
			.stream()
			.map(v -> new VeterinarioDto(v.getId(), v.getNome(), v.getSobrenome(), v.getEspecialidadesList()))
			.toList();
		// Para comparar: o mesmo conteúdo em JSON ocupa bem mais bytes que em Protobuf
		int bytesJson = this.jsonMapper.writeValueAsBytes(veterinarios).length;
		return new ResultadoGrpc("localhost:" + this.porta, duracaoMs, resposta.getSerializedSize(), bytesJson,
				veterinarios);
	}

	private ManagedChannel canal() {
		if (this.canal == null) {
			synchronized (this) {
				if (this.canal == null) {
					this.canal = ManagedChannelBuilder.forAddress("localhost", this.porta).usePlaintext().build();
				}
			}
		}
		return this.canal;
	}

	@Override
	public void destroy() throws InterruptedException {
		if (this.canal != null) {
			this.canal.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
		}
	}

}
