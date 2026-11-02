# RMI e gRPC

[← Início](Home.md)

As duas versões fazem a mesma coisa: um cliente chama `listVets()` num objeto que está em outro processo. A ideia é igual (um **stub** local que se passa pelo objeto remoto); o que muda é o contrato e o formato na rede.

## RMI clássico

Fica em [`demos/rmi`](../../demos/rmi), sem Spring e sem Maven.

**Contrato:** uma interface Java ([VetService.java](../../demos/rmi/VetService.java)).

```java
public interface VetService extends Remote {

	List<String> listVets() throws RemoteException;

}
```

**Servidor** ([Server.java](../../demos/rmi/Server.java#L18-L24)): exporta o objeto e registra o stub com um nome.

```java
public static void main(String[] args) throws Exception {
	service = new Server();
	VetService stub = (VetService) UnicastRemoteObject.exportObject(service, 0);
	registry = LocateRegistry.createRegistry(1099);
	registry.rebind("VetService", stub);
	System.out.println("Servidor RMI pronto na porta 1099. Ctrl+C para encerrar.");
}
```

**Cliente** ([Client.java](../../demos/rmi/Client.java#L6-L13)): procura o stub pelo nome e chama como se fosse local.

```java
Registry registry = LocateRegistry.getRegistry(host, 1099);
VetService service = (VetService) registry.lookup("VetService");
for (String vet : service.listVets()) {
	System.out.println(" - " + vet);
}
```

Como rodar:

```bash
cd demos/rmi
javac *.java
java Server     # terminal 1
java Client     # terminal 2
```

## gRPC moderno

**Contrato:** um arquivo `.proto`, neutro de linguagem ([vets.proto](../../src/main/protobuf/vets.proto)).

```protobuf
service VetService {
  rpc ListVets (ListVetsRequest) returns (ListVetsResponse);
}

message VetInfo {
  int32 id = 1;
  string first_name = 2;
  string last_name = 3;
  repeated string specialties = 4;
}

message ListVetsResponse {
  repeated VetInfo vets = 1;
}
```

Na compilação, o plugin do Maven gera as classes Java (`VetServiceGrpc`, `VetInfo`, `ListVetsResponse`) no pacote `grpc.proto`. Os números (`= 1`, `= 2`) identificam cada campo no binário, e é por isso que a mensagem fica menor que o JSON.

**Servidor** ([VetGrpcService.java](../../src/main/java/org/springframework/samples/petclinic/grpc/VetGrpcService.java#L35-L66)): estende a classe base gerada e usa o repositório JPA de verdade.

```java
@Service
public class VetGrpcService extends VetServiceGrpc.VetServiceImplBase {

	private final VetRepository vetRepository;

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
```

O `spring-boot-starter-grpc-server` encontra qualquer bean que estenda uma `*ImplBase` e o publica na porta 9090, sem configuração extra.

**Cliente** ([GrpcClient.java](../../src/main/java/org/springframework/samples/petclinic/grpc/client/GrpcClient.java#L42-L57)):

```java
ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
try {
	ListVetsResponse response = VetServiceGrpc.newBlockingStub(channel)
		.listVets(ListVetsRequest.getDefaultInstance());
	...
}
finally {
	channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
}
```

O painel tem um cliente parecido, o [GrpcDemoClient.java](../../src/main/java/org/springframework/samples/petclinic/dashboard/GrpcDemoClient.java#L85-L101), que também mede o tamanho da resposta nos dois formatos:

```java
long start = System.nanoTime();
ListVetsResponse response = VetServiceGrpc.newBlockingStub(channel())
	.withDeadlineAfter(10, TimeUnit.SECONDS)
	.listVets(ListVetsRequest.getDefaultInstance());
long durationMs = (System.nanoTime() - start) / 1_000_000;
...
int jsonBytes = this.jsonMapper.writeValueAsBytes(vets).length;
return new GrpcResult("localhost:" + this.port, durationMs, response.getSerializedSize(), jsonBytes, vets);
```

Resultado com os 6 veterinários de exemplo: **169 bytes em Protobuf contra 447 bytes em JSON**, uma mensagem 62% menor.

## Comparação

| | RMI | gRPC |
|---|---|---|
| Contrato | interface Java (`extends Remote`) | arquivo `.proto` |
| Linguagens | só Java | Java, Go, Python, C#, JS... |
| Transporte | JRMP sobre TCP, porta 1099 | HTTP/2, porta 9090 |
| Formato | serialização Java | Protobuf binário |
| Localizar o serviço | `Registry.lookup("VetService")` | endereço e porta |
| Stub | gerado em tempo de execução | gerado na compilação a partir do `.proto` |

## Padrões envolvidos

- **Proxy:** o stub (`VetServiceGrpc.newBlockingStub`, ou o objeto vindo do `lookup`) implementa a mesma interface do serviço e repassa a chamada pela rede.
- **Broker:** a infraestrutura (Registry no RMI, HTTP/2 + Protobuf no gRPC) esconde a localização e o transporte.

Diagrama: [UML 08, sequência do gRPC](../uml/svg/08_sequencia_grpc.svg).
