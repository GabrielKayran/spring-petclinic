# Estudo de caso: Spring PetClinic (POO2, UFU)

Este fork do [spring-projects/spring-petclinic](https://github.com/spring-projects/spring-petclinic)
recebeu cinco extensões que cobrem os tópicos da disciplina ausentes no projeto original.
Em cada uma mostramos a **tecnologia clássica** ensinada na disciplina e o seu **equivalente moderno**.

| # | Extensão | Pacote / pasta | Branch |
|---|----------|----------------|--------|
| 1 | RMI (clássico) → gRPC (moderno) | `demos/rmi`, `...petclinic.grpc` | `extensao/1-rmi-grpc` |
| 2 | Threads de plataforma → Virtual Threads | `demos/threads`, `...petclinic.virtualthreads` | `extensao/2-virtual-threads` |
| 3 | Padrão Observer + processamento assíncrono | `...petclinic.lembrete` | `extensao/3-observer-async` |
| 4 | Padrão Strategy | `...petclinic.preco` | `extensao/4-strategy` |
| 5 | AOP próprio (`@Auditado`) | `...petclinic.auditoria` | `extensao/5-aop-auditoria` |
| + | **Painel `/painel`** (vitrine ao vivo das 5) + deploy no Render | `...petclinic.painel`, `Dockerfile`, `render.yaml` | `extensao/6-painel-render` |

As branches 1 a 5 saem do mesmo commit base (atualização para Java 21) e não dependem umas
das outras. O painel depende de todas, porque só exibe o que elas fazem. A branch `estudo-de-caso` juntou tudo e foi mesclada na `main`, que é a branch usada no deploy.

**Pré-requisitos:** JDK 21 ou mais recente (necessário para as virtual threads). O projeto usa
Spring Boot 4.1.0. Se o `java -version` da máquina mostrar 17, aponte o `JAVA_HOME` para um JDK 21
antes de rodar o Maven:

```bash
# Git Bash / Linux / macOS
export JAVA_HOME=/caminho/para/jdk-21
# PowerShell
$env:JAVA_HOME = "C:\caminho\para\jdk-21"
```

> Nos comandos abaixo, `./mvnw` funciona no Git Bash, Linux e macOS. No PowerShell, use `.\mvnw`.

### Banco de dados: PostgreSQL no Neon

A aplicação apresentada usa um **PostgreSQL 18 gerenciado no [Neon](https://neon.tech)**, por meio do
profile `postgres` que o PetClinic já tinha (`application-postgres.properties`). Na primeira subida,
os scripts `db/postgres/schema.sql` e `data.sql` criam as tabelas e os dados. Eles são idempotentes
e podem rodar a cada inicialização sem duplicar nada.

As credenciais **nunca** vão para o repositório. Elas entram por variáveis de ambiente:

| Variável | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `postgres` |
| `POSTGRES_URL` | `jdbc:postgresql://<host-do-neon>/neondb?sslmode=require&channelBinding=require` |
| `POSTGRES_USER` | usuário do Neon (`neondb_owner`) |
| `POSTGRES_PASS` | senha do Neon |

O Neon mostra a conexão no formato `postgresql://usuario:senha@host/banco?...`. Para o Java (JDBC) ela
vira `jdbc:postgresql://host/banco?...`, com usuário e senha separados. O parâmetro `channel_binding`
também muda de nome e vira `channelBinding`.

Rodar localmente apontando para o Neon (PowerShell):

```powershell
$env:SPRING_PROFILES_ACTIVE = "postgres"
$env:POSTGRES_URL  = "jdbc:postgresql://<host-do-neon>/neondb?sslmode=require&channelBinding=require"
$env:POSTGRES_USER = "neondb_owner"
$env:POSTGRES_PASS = "<senha>"
.\mvnw spring-boot:run
```

> **Testes automatizados continuam no H2 em memória**, de propósito: são rápidos, não dependem de
> rede e não sujam o banco da apresentação. Sem `SPRING_PROFILES_ACTIVE`, a aplicação também sobe no H2.

---

## 1. Diagrama de pacotes

O Mermaid não tem um diagrama de pacotes nativo, então cada pacote aparece como um
`subgraph`. As setas são dependências do tipo "usa". Os pacotes em cinza são do PetClinic original.

```mermaid
flowchart LR
    subgraph original["PetClinic original"]
        owner["owner<br/>(Owner, Pet, Visit,<br/>VisitController...)"]
        vet["vet<br/>(Vet, VetRepository,<br/>VetController)"]
        model["model"]
    end

    subgraph ext1["Extensão 1"]
        grpc["grpc<br/>VeterinarioGrpcService"]
        grpcproto["grpc.proto<br/>(código gerado do .proto)"]
        grpccli["grpc.cliente<br/>ClienteGrpc"]
    end
    subgraph ext2["Extensão 2"]
        vt["virtualthreads<br/>ThreadInfoController"]
    end
    subgraph ext3["Extensão 3"]
        lembrete["lembrete<br/>VisitaRegistradaEvent<br/>LembreteVisitaListener"]
    end
    subgraph ext4["Extensão 4"]
        preco["preco<br/>PrecoConsultaStrategy<br/>PrecoConsultaService"]
    end
    subgraph ext5["Extensão 5"]
        auditoria["auditoria<br/>@Auditado<br/>AuditoriaAspect"]
    end
    subgraph vitrine["Painel (apresentação)"]
        painel["painel<br/>PainelController<br/>GrpcDemoCliente<br/>BenchmarkDemo<br/>PainelLogAppender"]
    end

    painel -.-> grpcproto
    painel -.-> preco
    painel -.-> auditoria
    painel -.-> owner
    painel -. "lê os logs de" .-> lembrete
    grpc --> grpcproto
    grpc --> vet
    grpc --> model
    grpccli --> grpcproto
    owner -- "publica evento" --> lembrete
    preco --> owner
    owner -- "@Auditado" --> auditoria
    vet -- "@Auditado" --> auditoria
    vet --> model
    owner --> model

    classDef orig fill:#eee,stroke:#999,color:#333;
    class owner,vet,model orig;
```

Pontos de contato com o código original. Nenhuma lógica de negócio existente foi alterada:

- `VisitController`: recebeu o `ApplicationEventPublisher` e uma chamada `publishEvent(...)` (Extensão 3),
  além de `@Auditado` em um método (Extensão 5).
- `OwnerController.showOwner` e `VetController.showResourcesVetList`: apenas `@Auditado` (Extensão 5).
- `createOrUpdateVisitForm.html` + `messages*.properties`: exibição do preço (Extensão 4).
- `application.properties`: `spring.threads.virtual.enabled=true` (Extensão 2) e `server.port=${PORT:8080}` (Render).
- `fragments/layout.html`: item "Estudo de caso" no menu, que leva ao `/painel`.
- `pom.xml` / `build.gradle`: Java 21, starters de gRPC e AspectJ e geração de código protobuf.

---

## 2. Extensão 1: RMI → gRPC

**Clássico** (`demos/rmi`, 57 linhas, sem Spring e sem Maven): uma interface remota Java,
um servidor que publica o stub no RMI Registry e um cliente que procura o stub pelo nome.

**Moderno**: o contrato fica em `src/main/protobuf/veterinarios.proto`. No build, o Maven gera
as classes Java (`protobuf-maven-plugin`). `VeterinarioGrpcService` implementa o serviço reaproveitando o
`VetRepository` existente. O Spring Boot 4.1 sobe o servidor gRPC (Netty) na porta **9090**.
`ClienteGrpc` é um programa separado, sem Spring, que chama o serviço pelo stub gerado.

```mermaid
classDiagram
    direction LR
    namespace RMI_classico {
        class Remote {
            <<interface>>
        }
        class VeterinarioService_RMI["VeterinarioService"] {
            <<interface>>
            +listarVeterinarios() List~String~ throws RemoteException
        }
        class Servidor {
            +listarVeterinarios() List~String~
            +main(String[])
        }
        class Cliente {
            +main(String[])
        }
    }
    Remote <|-- VeterinarioService_RMI
    VeterinarioService_RMI <|.. Servidor
    Cliente ..> VeterinarioService_RMI : stub via Registry (porta 1099)
```

```mermaid
classDiagram
    direction LR
    class VeterinarioServiceImplBase {
        <<gerada a partir do .proto>>
        +listarVeterinarios(request, responseObserver)
    }
    class VeterinarioServiceBlockingStub {
        <<gerada a partir do .proto>>
        +listarVeterinarios(request) ListarVeterinariosResponse
    }
    class VeterinarioGrpcService {
        <<@Service>>
        -VetRepository vetRepository
        +listarVeterinarios(request, responseObserver)
        -paraMensagem(Vet) Veterinario
    }
    class ClienteGrpc {
        +main(String[])
    }
    class VetRepository {
        <<interface>>
        +findAll() Collection~Vet~
    }
    class ListarVeterinariosResponse {
        +List~Veterinario~ veterinarios
    }
    class Veterinario {
        +int id
        +String nome
        +String sobrenome
        +List~String~ especialidades
    }
    VeterinarioServiceImplBase <|-- VeterinarioGrpcService
    VeterinarioGrpcService --> VetRepository
    VeterinarioGrpcService ..> ListarVeterinariosResponse : cria
    ListarVeterinariosResponse o-- Veterinario
    ClienteGrpc --> VeterinarioServiceBlockingStub
    VeterinarioServiceBlockingStub ..> VeterinarioServiceImplBase : HTTP/2 + Protobuf (porta 9090)
```

---

## 3. Extensão 2: threads de plataforma → Virtual Threads

**Clássico x moderno** (`demos/threads/BenchmarkThreads.java`): 10.000 tarefas com `Thread.sleep(1s)`,
primeiro em `newFixedThreadPool(200)` e depois em `newVirtualThreadPerTaskExecutor()`.
Resultado medido: **50.346 ms × 1.218 ms**.

**No PetClinic**: uma linha no `application.properties` (`spring.threads.virtual.enabled=true`)
faz o Tomcat atender cada requisição numa virtual thread. O executor do `@Async` também passa
a usá-las (veja a Extensão 3). O endpoint `GET /thread` mostra qual thread atendeu a requisição.

```mermaid
classDiagram
    direction LR
    class BenchmarkThreads {
        +main(String[])
        -medir(String nome, ExecutorService executor, int tarefas)
    }
    class ExecutorService {
        <<interface>>
    }
    class ThreadInfoController {
        <<@RestController>>
        +threadAtual() ThreadInfo  «GET /thread»
    }
    class ThreadInfo {
        <<record>>
        +String nome
        +boolean virtual
        +String tipo
        +String detalhes
    }
    BenchmarkThreads ..> ExecutorService : newFixedThreadPool(200)\nnewVirtualThreadPerTaskExecutor()
    ThreadInfoController ..> ThreadInfo : retorna (JSON)
```

---

## 4. Extensão 3: padrão Observer + `@Async`

Quando uma visita é salva, o `VisitController` (o **sujeito**) publica um `VisitaRegistradaEvent`
(a **notificação**). O `LembreteVisitaListener` (o **observador**) recebe o evento numa
**outra thread** (`@Async`) e simula o envio de um lembrete, que leva 2 s. A resposta HTTP não espera esse envio.

```mermaid
classDiagram
    direction LR
    class ApplicationEvent {
        <<Spring>>
        +getSource() Object
    }
    class ApplicationEventPublisher {
        <<interface, Spring>>
        +publishEvent(Object)
    }
    class VisitaRegistradaEvent {
        -String nomeDono
        -String nomePet
        -LocalDate dataVisita
        -String descricao
    }
    class VisitController {
        -OwnerRepository owners
        -ApplicationEventPublisher eventos
        +processNewVisitForm(...) String
    }
    class LembreteVisitaListener {
        <<@Component>>
        +enviarLembrete(VisitaRegistradaEvent) «@Async @EventListener»
    }
    class LembreteAsyncConfiguration {
        <<@Configuration @EnableAsync>>
    }
    ApplicationEvent <|-- VisitaRegistradaEvent
    VisitController --> ApplicationEventPublisher
    VisitController ..> VisitaRegistradaEvent : cria e publica
    LembreteVisitaListener ..> VisitaRegistradaEvent : observa
```

### Diagrama de sequência: registrar visita → evento → lembrete assíncrono

```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Usuário (navegador)
    participant Tomcat as Tomcat<br/>(virtual thread tomcat-handler-N)
    participant Proxy as Proxy AOP<br/>(AuditoriaAspect)
    participant VC as VisitController
    participant Repo as OwnerRepository
    participant Pub as ApplicationEventPublisher
    participant Exec as applicationTaskExecutor<br/>(virtual thread task-N)
    participant L as LembreteVisitaListener

    Usuario->>Tomcat: POST /owners/6/pets/7/visits/new
    Tomcat->>Proxy: processNewVisitForm(...)
    Note over Proxy: @Around: marca o tempo de início
    Proxy->>VC: proceed()
    VC->>Repo: save(owner)
    Repo-->>VC: ok
    VC->>Pub: publishEvent(VisitaRegistradaEvent)
    Pub->>Exec: submete enviarLembrete(evento)
    Note right of Pub: @Async: retorna na hora,<br/>sem esperar o listener
    Pub-->>VC: retorno imediato
    VC-->>Proxy: "redirect:/owners/{ownerId}"
    Note over Proxy: loga [AUDITORIA] método, args, tempo
    Proxy-->>Tomcat: view
    Tomcat-->>Usuario: 302 Redirect (cerca de 0,1 s)
    par em paralelo, em outra thread
        Exec->>L: enviarLembrete(evento)
        Note over L: loga "[Observer] preparando lembrete..."
        L->>L: Thread.sleep(2 s), simulando e-mail/SMS
        Note over L: loga "[Observer] lembrete enviado"
    end
```

---

## 5. Extensão 4: padrão Strategy

`PrecoConsultaStrategy` define o algoritmo. Cada implementação é um bean cujo **nome é o tipo de
pet** do banco (`"dog"`, `"cat"`), mais uma estratégia `"padrao"`. O `PrecoConsultaService` recebe
um `Map<String, PrecoConsultaStrategy>` **injetado pelo Spring** com todas as estratégias e faz
`getOrDefault(tipo, padrao)`, sem nenhum `if` ou `switch` sobre o tipo do pet. Para criar uma regra
para hamster, basta adicionar uma classe `@Component("hamster")`.

| Estratégia | Bean | Regra |
|---|---|---|
| `PrecoConsultaCao` | `dog` | R$ 100,00 + R$ 5,00 por ano de idade |
| `PrecoConsultaGato` | `cat` | R$ 90,00 |
| `PrecoConsultaPadrao` | `padrao` | R$ 70,00 (lagarto, cobra, pássaro, hamster...) |

O preço aparece na tela de nova visita. O `PrecoConsultaWebConfiguration` (um `HandlerInterceptor`)
adiciona `precoConsulta` ao model depois que o controller executa, sem alterar o `VisitController`.

```mermaid
classDiagram
    direction LR
    class PrecoConsultaStrategy {
        <<interface>>
        +calcularPreco(Pet) BigDecimal
    }
    class PrecoConsultaCao {
        <<@Component("dog")>>
        +calcularPreco(Pet) BigDecimal
    }
    class PrecoConsultaGato {
        <<@Component("cat")>>
        +calcularPreco(Pet) BigDecimal
    }
    class PrecoConsultaPadrao {
        <<@Component("padrao")>>
        +calcularPreco(Pet) BigDecimal
    }
    class PrecoConsultaService {
        <<@Service — contexto>>
        -Map~String, PrecoConsultaStrategy~ estrategiasPorTipo
        -PrecoConsultaStrategy estrategiaPadrao
        +calcularPreco(Pet) BigDecimal
    }
    class PrecoConsultaWebConfiguration {
        <<WebMvcConfigurer, HandlerInterceptor>>
        +addInterceptors(InterceptorRegistry)
        +postHandle(..., ModelAndView)
    }
    class Pet
    PrecoConsultaStrategy <|.. PrecoConsultaCao
    PrecoConsultaStrategy <|.. PrecoConsultaGato
    PrecoConsultaStrategy <|.. PrecoConsultaPadrao
    PrecoConsultaService o-- "*" PrecoConsultaStrategy : injetadas pelo Spring
    PrecoConsultaWebConfiguration --> PrecoConsultaService
    PrecoConsultaStrategy ..> Pet
```

---

## 6. Extensão 5: AOP próprio (`@Auditado`)

`@Auditado` é só um marcador. O `AuditoriaAspect` usa o pointcut `@annotation(auditado)` e um advice
`@Around` para registrar o método, os argumentos, o tempo e a thread. A anotação foi aplicada, **sem alterar
o corpo dos métodos**, em:

- `VisitController.processNewVisitForm` (`"Registrar visita"`)
- `OwnerController.showOwner` (`"Exibir dono"`)
- `VetController.showResourcesVetList` (`"Listar veterinários (JSON)"`)

**Weaving:** na inicialização, o Spring detecta que esses controllers têm métodos que casam com o pointcut
e registra no contexto um **proxy CGLIB** (uma subclasse gerada em tempo de execução) no lugar do objeto
original. O teste `AuditoriaAspectTests` comprova isso com `AopUtils.isAopProxy(...)`.

```mermaid
classDiagram
    direction LR
    class Auditado {
        <<@interface>>
        +value() String
    }
    class AuditoriaAspect {
        <<@Aspect @Component>>
        +auditar(ProceedingJoinPoint, Auditado) Object «@Around("@annotation(auditado)")»
    }
    class VisitController {
        +processNewVisitForm(...) «@Auditado»
    }
    class OwnerController {
        +showOwner(int) «@Auditado»
    }
    class VetController {
        +showResourcesVetList() «@Auditado»
    }
    class ProxyCGLIB["VetController$$SpringCGLIB"] {
        <<gerado em runtime>>
    }
    AuditoriaAspect ..> Auditado : pointcut
    VisitController ..> Auditado
    OwnerController ..> Auditado
    VetController ..> Auditado
    VetController <|-- ProxyCGLIB
    ProxyCGLIB ..> AuditoriaAspect : chama o advice
```

---

## 7. Clássico vs Moderno

| Clássico | Moderno | Por que foi substituído |
|---|---|---|
| **RMI** | **gRPC** | O RMI só funciona entre Java e Java: o contrato é uma interface Java e os dados usam **serialização Java**, que tem histórico grave de vulnerabilidades de desserialização. Ele usa a porta 1099 mais portas dinâmicas, o que dificulta firewalls, proxies e balanceadores. O gRPC mantém a ideia de "chamar um método remoto por um stub", mas o contrato `.proto` é **neutro de linguagem** (Java, C#, Go, Python...), os dados viajam em **Protocol Buffers** (binário compacto e com schema) sobre **HTTP/2** (multiplexação, streaming, TLS, compatível com infraestrutura web e nuvem) e o versionamento é explícito (campos numerados). |
| **Threads de plataforma** (pool fixo, `ExecutorService`) | **Virtual Threads** (Java 21) | Cada thread de plataforma é uma thread do sistema operacional: cerca de 1 MB de pilha, criação cara e um limite prático de poucos milhares por servidor. Em aplicações de I/O (banco, HTTP) as threads passam a maior parte do tempo bloqueadas, e o pool vira o gargalo. A alternativa anterior, a programação reativa, resolvia isso mas tornava o código difícil de ler e depurar. Virtual threads são gerenciadas pela JVM, custam poucos KB, podem existir aos milhões e **liberam a thread do SO ao bloquear**, então o código continua simples e bloqueante. No benchmark: 50 s → 1,2 s. |
| **AspectJ** (weaving em compilação ou load-time) | **Spring AOP** (proxies em runtime) | O AspectJ completo exige um compilador especial (`ajc`) ou um agente Java (`-javaagent`) para alterar o bytecode, o que complica o build, a IDE e a depuração. O Spring AOP reaproveita a **sintaxe de anotações do AspectJ** (`@Aspect`, `@Around`, pointcuts), mas faz o weaving com **proxies dinâmicos** (JDK Proxy ou CGLIB) na inicialização, sem ferramentas extras. O custo: só intercepta chamadas a métodos públicos de beans Spring que passam pelo proxy (chamadas internas via `this` não são interceptadas). Para a maioria dos *cross-cutting concerns* (transações com `@Transactional`, cache, segurança, auditoria), isso basta. |
| **Struts** (1 e 2) | **Spring MVC** (já usado pelo PetClinic) | O Struts 1 chegou ao fim de vida em 2013, e o Struts 2 acumulou vulnerabilidades críticas de execução remota de código via OGNL. A **CVE-2017-5638**, não corrigida a tempo, foi a porta de entrada do vazamento da **Equifax** em 2017, que expôs dados de cerca de 147 milhões de pessoas. O Spring MVC usa controllers como POJOs anotados (`@Controller`, `@GetMapping`), injeção de dependência, testes fáceis com MockMvc e integração com o ecossistema Spring Boot, além de ser mantido ativamente. |

---

## 8. Painel do estudo de caso (`/painel`)

Para explicar melhor ao vivo, a aplicação tem uma página própria: **menu "Estudo de caso"** ou
`/painel`. Ela é a "vitrine" das cinco extensões. Não muda nenhuma delas, só chama os endpoints reais e
mostra o resultado.

| Área da página | O que faz ao vivo |
|---|---|
| Cabeçalho | Mostra o ambiente real: versão do Java, do Spring Boot, **banco em uso (PostgreSQL no Neon)**, virtual threads ligadas e profile ativo |
| 1. RMI → gRPC | Diagrama antes/depois, os dois contratos lado a lado (interface Java × `.proto`) e um botão que faz uma **chamada gRPC real** (HTTP/2, porta 9090) e compara o tamanho da resposta: **169 B em Protobuf × 447 B em JSON (62% menor)** |
| 2. Virtual Threads | Botão `GET /thread` (nome da thread e *carrier*) e **benchmark no servidor** com 500, 1.000 ou 2.000 tarefas, em gráfico de barras (500 tarefas: cerca de 3.000 ms × 1.000 ms) |
| 3. Observer + @Async | Diagrama de sequência e um botão que **registra uma visita de verdade** (o mesmo POST do formulário) e mostra uma linha do tempo: resposta HTTP em cerca de 0,3 s, lembrete iniciado em `task-1` (virtual) e "lembrete enviado" **+2.000 ms** depois |
| 4. Strategy | Tabela com **todos os pets do banco**, a estratégia escolhida (classe) e o preço, com link para a tela real de visita |
| 5. AOP | Tabela dos controllers no contexto do Spring, mostrando quais viraram **proxy CGLIB** (`VetController$$SpringCGLIB$$0`), e botões que chamam métodos auditados e exibem a linha `[AUDITORIA]` |
| Console ao vivo | Os **logs reais** do aspecto, do listener e do gRPC, capturados por um appender do Logback. Funciona também no Render, onde não há terminal à vista |

Arquivos: `painel/PainelController.java` (página + API `/api/painel/*`), `GrpcDemoCliente.java`,
`BenchmarkDemo.java`, `PainelLogAppender.java`, `templates/painel/painel.html`,
`static/resources/js/painel.js` e `static/resources/css/painel.css`. Os diagramas da página usam
[Mermaid](https://mermaid.js.org) carregado de um CDN, então o navegador da apresentação precisa de internet.

---

## 9. Deploy no Render

Sim, dá para publicar no Render. Como o Render não tem runtime Java nativo, o deploy usa **Docker**:

- `Dockerfile`: build em duas etapas (Maven + JDK 21 → JRE 21) com memória ajustada ao plano free (512 MB);
- `.dockerignore`: deixa `target/`, `.git/` e `demos/` fora da imagem;
- `render.yaml`: Blueprint do Render (serviço web Docker, região Virginia, health check em `/actuator/health`);
- `server.port=${PORT:8080}`: a aplicação usa a porta que o Render informa.

### Passo a passo

1. Envie a `main` para o GitHub do grupo (o Render faz deploy a partir do repositório).
2. No [dashboard do Render](https://dashboard.render.com): **New → Blueprint**, escolha o repositório e a branch `main`.
   O Render lê o `render.yaml`.
3. Ele vai pedir os valores marcados como `sync: false`. Preencha com os dados do Neon:
   - `POSTGRES_URL` = `jdbc:postgresql://<host-do-neon>/neondb?sslmode=require&channelBinding=require`
   - `POSTGRES_USER` = `neondb_owner`
   - `POSTGRES_PASS` = a senha do Neon
4. Clique em **Apply**. O primeiro build leva alguns minutos (o Maven baixa as dependências).
5. Quando aparecer **Live**, abra `https://<nome-do-servico>.onrender.com/painel`.

> Alternativa sem Blueprint: **New → Web Service**, escolha o repositório, *Language: Docker*, e cadastre as
> mesmas variáveis em *Environment*, incluindo `SPRING_PROFILES_ACTIVE=postgres` e
> `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info`.

### Cuidados no Render

- **Plano free "dorme"** após cerca de 15 min sem acesso, e a primeira requisição depois disso leva perto de 1 minuto.
  **Abra o `/painel` uns 5 minutos antes de apresentar.**
- **gRPC externo não funciona no Render free:** só a porta HTTP é publicada. Por isso o painel faz a chamada
  gRPC **de dentro do container** (HTTP → servidor → gRPC na 9090), o que continua sendo uma chamada de rede real.
  O `ClienteGrpc` de linha de comando e a demo RMI rodam localmente.
- **Actuator restrito:** no Render só `health` e `info` ficam expostos. O `/actuator/heapdump` revelaria a senha do banco.
- **Os dados ficam no Neon:** visitas registradas durante os ensaios continuam lá. Isso é esperado e não atrapalha a demo.

---

## 10. Roteiro de demo

Preparação (antes da aula):

1. Abra o painel publicado no Render (`https://<servico>.onrender.com/painel`) **5 minutos antes**, para acordar o serviço.
2. Para as partes de terminal (RMI, benchmark completo e cliente gRPC): JDK 21 no `JAVA_HOME` e
   `./mvnw -q -DskipTests package` uma vez, para gerar o código do gRPC.
3. No PowerShell, rode `chcp 65001` antes, para os acentos saírem certos.
4. Plano B sem internet: rode localmente com H2 (`./mvnw spring-boot:run`) e use `http://localhost:8080/painel`.
   O painel funciona igual; só os diagramas precisam do CDN.

### Abertura (painel)

Abra o `/painel`. **Mostrar** o cabeçalho com o ambiente: Java 21, Spring Boot 4.1.0,
**PostgreSQL 18 (Neon)** e virtual threads ligadas. Deixe o console ao vivo visível à direita durante toda a demo.

### Extensão 1: RMI → gRPC

1. **RMI puro** (terminal, a partir da raiz do projeto):
   ```bash
   cd demos/rmi
   javac *.java
   java Servidor            # terminal B: "Servidor RMI pronto na porta 1099"
   java Cliente             # terminal C: lista de veterinários
   ```
   Abrir `VeterinarioService.java` e destacar `extends Remote` e `throws RemoteException`. Ctrl+C no servidor.
2. **No painel, card 1:** comparar os dois contratos lado a lado e o diagrama antes/depois.
3. Clicar em **Chamar ListarVeterinarios via gRPC**. **Mostrar:** a tabela de veterinários vinda do
   Neon, o tempo da chamada e **169 B (Protobuf) × 447 B (JSON), 62% menor**. No console:
   `[gRPC] ListarVeterinarios recebido na thread grpc-default-executor-0`.
4. (Opcional, local, com o app rodando em `localhost`) Cliente de linha de comando, em outra JVM:
   `./mvnw -q exec:java@cliente-grpc`. Não rode `compile` com o app no ar, porque o DevTools reinicia a aplicação.

### Extensão 2: Virtual Threads

1. **Benchmark completo** (terminal):
   ```bash
   cd demos/threads
   java BenchmarkThreads.java          # cerca de 50 s + 1 s (10.000 tarefas)
   java BenchmarkThreads.java 2000     # versão curta: cerca de 10 s + 1 s
   ```
   Resultado medido: **50.346 ms × 1.218 ms**.
2. **No painel, card 2:** clicar em **GET /thread**. **Mostrar:** `tomcat-handler-N` com selo *virtual thread* e
   `VirtualThread[#65,tomcat-handler-20]/runnable@ForkJoinPool-1-worker-2`. A parte depois da `/` é a *carrier thread*.
3. Escolher **1.000 tarefas** e clicar em **Rodar** (cerca de 6 s). **Mostrar** as barras: pool fixo cerca de 5.000 ms ×
   virtual threads cerca de 1.000 ms. Lembrar que é a mesma linha `spring.threads.virtual.enabled=true` do `application.properties`.

### Extensão 3: Observer + @Async

1. **Card 3:** explicar o diagrama de sequência (sujeito → evento → observador em outra thread).
2. Escolher um pet (por exemplo, **Samantha (cat) de Jean Coleman**), manter "Vacina anual" e clicar em **Registrar visita**.
3. **Mostrar** na linha do tempo do card e no console:
   - resposta HTTP **302 em cerca de 0,3 s** (visita gravada no Neon);
   - `+0 ms task-1 [Observer] Thread 'task-1' (virtual=true): preparando lembrete para Jean Coleman...`;
   - `+0 ms tomcat-handler-N [AUDITORIA] VisitController.processNewVisitForm(..)` (a Extensão 5 também atuou aqui);
   - `+2.012 ms task-1 [Observer] ... lembrete enviado para Jean Coleman (Vacina anual)`.

   Destacar: a resposta voltou antes do lembrete, e as threads são diferentes (`tomcat-handler-N` × `task-N`).
4. Abrir `VisitController.java` e mostrar que a única mudança de lógica é o `publishEvent(...)`.

### Extensão 4: Strategy

1. **Card 4:** mostrar a tabela com a estratégia escolhida para cada pet do Neon:
   gatos → `PrecoConsultaGato` **R$ 90,00**; cães → `PrecoConsultaCao` (100 + 5 × idade): Rosy, 25 anos,
   **R$ 225,00**; Jewel, 26 anos, **R$ 230,00**; lagarto, cobra, hamster e pássaro → `PrecoConsultaPadrao` **R$ 70,00**.
   (No banco do Postgres os pets nasceram por volta de 2000, por isso as idades são maiores que no H2.)
2. Clicar em **tela** na linha da Rosy. Abre a tela real de nova visita com `Preço da consulta: R$ 225,00`,
   colocado pelo `PrecoConsultaWebConfiguration` sem alterar o `VisitController`.
3. Abrir `PrecoConsultaService.java`: o `Map<String, PrecoConsultaStrategy>` injetado e o `getOrDefault`, sem `if`.

### Extensão 5: AOP

1. **Card 5:** a tabela mostra `ownerController`, `vetController` e `visitController` como **proxy AOP**
   (`VetController$$SpringCGLIB$$0`) e os demais como objeto comum. Esse é o **weaving** acontecendo.
2. Clicar em **GET /vets**. **Mostrar:**
   `[AUDITORIA] VetController.showResourcesVetList() (Listar veterinários (JSON)) | args=[] | 0 ms | thread=tomcat-handler-62`.
   Clicar em **GET /owners/1** (`args=[1]`) e em **GET / (não auditado)**, que não gera linha nenhuma.
3. Abrir `AuditoriaAspect.java` (pointcut `@annotation(auditado)`, advice `@Around`) e `VetController.java`
   (a única mudança é a linha `@Auditado(...)`).

### Encerrando: os testes

```bash
./mvnw test
```

Mostrar `Tests run: 99, Failures: 0, Errors: 0, Skipped: 2` e `BUILD SUCCESS`. Os testes rodam no H2 em memória.
Os 2 pulados são os de MySQL via Testcontainers, que exigem o Docker em execução.

| Extensão | Teste | O que prova |
|---|---|---|
| 1 | `grpc/VeterinarioGrpcServiceTests` | Uma chamada gRPC real (Netty, porta dinâmica) retorna os 6 veterinários do banco |
| 2 | `virtualthreads/ThreadInfoControllerTests` | Uma requisição HTTP real ao Tomcat é atendida por uma virtual thread |
| 3 | `lembrete/VisitaRegistradaEventTests` | O POST da visita publica o evento com os dados corretos e o listener roda em outra thread |
| 4 | `preco/PrecoConsultaServiceTests`, `preco/PrecoConsultaTelaTests` | Cada tipo de pet usa sua estratégia, e o preço aparece no HTML |
| 5 | `auditoria/AuditoriaAspectTests` | Os controllers viraram proxies AOP e o log traz método, argumentos e tempo |
| Painel | `painel/PainelControllerTests` | A página e cada endpoint da API (`ambiente`, `grpc`, `pets`, `aop`, `benchmark`, `logs`) respondem corretamente |

---

## 11. Limitações e observações

- **Credenciais:** a senha do Neon fica só nas variáveis de ambiente (máquina local e painel do Render), nunca no Git. Como ela já circulou em conversas, **troque a senha no Neon depois da apresentação** (Neon → *Roles* → *Reset password*) e atualize a variável no Render.
- **Render free:** o serviço dorme após cerca de 15 min sem uso (a primeira resposta demora perto de 1 min) e só expõe HTTP; o gRPC é acessado de dentro do container pelo painel.
- **Docker local:** o `Dockerfile` não foi testado com `docker build` nesta máquina, porque o Docker não estava rodando. O mesmo `.jar` foi validado com as mesmas variáveis do Render (`PORT=10000`, profile `postgres`, Neon, actuator restrito e limite de memória).
- **Docker nos testes:** `MySqlIntegrationTests` e `PostgresIntegrationTests` precisam do Docker e ficam pulados sem ele, como já acontecia antes das extensões.
- **Porta do gRPC nos testes:** `src/test/resources/config/application.properties` define `spring.grpc.server.port=0`, porque vários contextos Spring de teste ficam em cache ao mesmo tempo e brigariam pela 9090.
- **Segurança do gRPC:** o servidor e o cliente usam texto puro (`usePlaintext()`), sem TLS e sem autenticação. Isso só é adequado para a demo.
- **Painel público:** no Render qualquer pessoa com o link pode registrar visitas e rodar o benchmark. O benchmark é limitado a 2.000 tarefas e a um por vez.
- **Preço da consulta:** o valor é apenas calculado e exibido. Ele não é gravado na visita (não houve mudança no schema).
- **`@Auditado` e argumentos:** o log usa `toString()` dos argumentos. `Visit` não sobrescreve `toString()`, então aparece como `Visit@1a2b3c`. Em produção, cuidado para não logar dados sensíveis.
- **Spring AOP:** chamadas internas (via `this`) não passam pelo proxy e não são auditadas. É uma limitação do modelo de proxies e um bom ponto de discussão com AspectJ.
- **Acentos no console do Windows:** sem `chcp 65001`, a saída do Maven/Java pode mostrar `Veterin�rios`. É só exibição.
