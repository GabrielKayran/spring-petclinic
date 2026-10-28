# Demo: RMI puro (Java RMI, sem Spring e sem Maven)

Esta é a versão "clássica" da chamada remota. Compare com o serviço gRPC em
`src/main/protobuf/vets.proto` e no pacote `org.springframework.samples.petclinic.grpc`.

| Arquivo                   | Papel                                                    |
|---------------------------|----------------------------------------------------------|
| `VetService.java` | Interface remota (o contrato, só em Java)                |
| `Server.java`           | Implementação + registro do stub no RMI Registry (porta 1099) |
| `Client.java`            | Busca o stub pelo nome e chama o método remoto           |

## Como rodar (JDK 21+)

Execute a partir desta pasta (`demos/rmi`):

```bash
javac *.java

# Terminal 1: servidor (fica rodando)
java Server

# Terminal 2: cliente
java Client
```

Saída esperada no cliente:

```
 - James Carter
 - Helen Leary
 ...
```

No terminal do servidor aparece `Chamada remota recebida na thread RMI TCP Connection(...)`.

## Pontos para comentar

- O contrato é uma **interface Java**, então cliente e servidor precisam ser Java.
- Os dados trafegam com **serialização Java**, um formato binário que historicamente teve
  várias falhas de segurança (desserialização de objetos maliciosos).
- O RMI usa a porta 1099 e portas dinâmicas para os objetos, o que dificulta passar por
  firewalls e proxies.
- Para limpar: `rm *.class` (ou `del *.class` no Windows).
