# Spring PetClinic · Estudo de caso POO2 (Grupo 07)

Fork do [Spring PetClinic](https://github.com/spring-projects/spring-petclinic) usado no estudo de caso de Programação Orientada a Objetos 2 (UFU). Sobre a aplicação de exemplo oficial do Spring (uma clínica veterinária com tutores, pets, visitas e veterinários), o grupo acrescentou cinco extensões, cada uma ligada a um tema da disciplina.

**Integrantes:** Nathan Silva Neves, Ryan Carlos Cabral de Sousa, João Victor Bento Chagas Teixeira e Gabriel Kayran Silva.

**Sistema publicado:** https://petclinic-poo2.onrender.com · painel de demonstração em [`/painel`](https://petclinic-poo2.onrender.com/painel)

**Documentação completa:** [wiki do projeto](docs/wiki/Home.md), com trechos reais do código explicados.

## O que foi acrescentado

| Tema | Clássico → moderno | Onde está | Wiki |
|---|---|---|---|
| Objetos distribuídos | RMI → gRPC (Protobuf, HTTP/2, porta 9090) | `demos/rmi`, pacote `grpc` | [RMI e gRPC](docs/wiki/RMI-e-gRPC.md) |
| Concorrência | threads de plataforma → virtual threads (Java 21) | `virtualthreads`, `dashboard/BenchmarkDemo` | [Concorrência](docs/wiki/Concorrencia-e-virtual-threads.md) |
| Padrão Observer | chamada direta → evento + `@Async` | pacote `reminder` | [Observer](docs/wiki/Observer-e-Async.md) |
| Padrão Strategy | `if`/`switch` por tipo → uma estratégia por animal | pacote `pricing` | [Strategy](docs/wiki/Strategy-preco-da-consulta.md) |
| Aspectos | AspectJ (`ajc`) → Spring AOP com `@Audited` | pacote `audit` | [AOP](docs/wiki/AOP-auditoria.md) |
| Relatórios | iReport → JasperReports 7 | pacote `report` | [Relatório](docs/wiki/Relatorio-JasperReports.md) |
| Persistência | JDBC + SQL à mão → JPA/Hibernate + Spring Data, PostgreSQL no Neon | `owner`, `vet`, `dashboard/SqlRecorder` | [Domínio e banco](docs/wiki/Modelo-de-dominio-e-banco.md) |

Além disso, toda a interface foi traduzida para o português e ajustada para celular, e foram criados o endpoint `/health` e o painel `/painel`, que executa cada demonstração ao vivo. A modelagem UML (11 diagramas) está em [`docs/uml`](docs/uml/README.md).

## Executando localmente

É preciso ter o **JDK 21** instalado. O Maven Wrapper baixa o resto.

```bash
git clone https://github.com/GabrielKayran/spring-petclinic.git
cd spring-petclinic
./mvnw spring-boot:run
```

No Windows, use `mvnw.cmd spring-boot:run`. Com Gradle, o comando é `./gradlew bootRun`.

Depois abra:

| Endereço | O que é |
|---|---|
| http://localhost:8080 | aplicação |
| http://localhost:8080/painel | painel com as demonstrações do estudo de caso |
| http://localhost:8080/health | verificação de saúde (`{"status":"UP"}`) |
| http://localhost:8080/thread | mostra se a requisição rodou numa virtual thread |
| http://localhost:8080/relatorios/pets-e-precos.pdf | relatório PDF |
| `localhost:9090` | servidor gRPC (`petclinic.VetService/ListVets`) |

A interface abre em português. Para ver em outro idioma, adicione `?lang=en` (ou `es`, `de`...) à URL.

Para as demonstrações de linha de comando, veja [`demos/rmi`](demos/rmi/README.md) e [`demos/threads`](demos/threads/README.md).

## Banco de dados

Por padrão a aplicação usa o **H2 em memória**, criado e populado na subida a partir de `src/main/resources/db/h2/`. Os dados somem quando a aplicação para.

Para um banco persistente, ative o perfil correspondente:

| Banco | Perfil | Variáveis de ambiente |
|---|---|---|
| PostgreSQL | `postgres` | `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASS` |
| MySQL | `mysql` | `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASS` |

Subindo um PostgreSQL local com o `docker-compose.yml` do projeto:

```bash
docker compose up postgres
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
```

Sem as variáveis, o perfil `postgres` usa `jdbc:postgresql://localhost/petclinic` com usuário e senha `petclinic`, que são os valores do `docker-compose.yml`. Os scripts `schema.sql` e `data.sql` são idempotentes e rodam a cada subida.

**Nunca coloque credenciais reais no repositório.** Em produção elas ficam só nas variáveis de ambiente do Render.

## Docker e deploy

O projeto tem um [`Dockerfile`](Dockerfile) de dois estágios (JDK 21 para compilar, JRE 21 para rodar), com a JVM ajustada para os 512 MB do plano gratuito do Render.

```bash
docker build -t petclinic .
docker run -p 8080:8080 petclinic
```

A produção roda no **Render** com o banco **PostgreSQL no Neon**, conforme o [`render.yaml`](render.yaml). As credenciais do banco são pedidas no painel do Render (`sync: false`) e o health check usa `/health`. Para publicar uma versão nova, faça o push para a `main` e, no Render, use *Manual Deploy → Deploy latest commit*. Detalhes em [Como executar](docs/wiki/Como-executar.md).

## Testes

```bash
./mvnw test
```

São 111 testes. Os de integração com MySQL e PostgreSQL usam Docker e são pulados quando ele não está disponível. A wiki tem uma página explicando [o que cada teste prova](docs/wiki/Testes.md).

O build também verifica a formatação (`spring-javaformat`). Para corrigi-la automaticamente:

```bash
./mvnw spring-javaformat:apply
```

## Abrindo na IDE

- **IntelliJ IDEA:** `File → Open` e escolha o [`pom.xml`](pom.xml). Rode a classe `PetClinicApplication`.
- **VS Code:** abra a pasta com o *Extension Pack for Java* instalado e rode `PetClinicApplication`.
- **Eclipse / STS:** `File → Import → Maven → Existing Maven Projects`.

Antes da primeira execução pela IDE, rode `./mvnw generate-sources` para gerar as classes do gRPC a partir de `src/main/protobuf/vets.proto`.

## Estrutura

```
src/main/java/org/springframework/samples/petclinic/
├── model/          superclasses das entidades (BaseEntity, Person, NamedEntity)
├── owner/          tutores, pets e visitas
├── vet/            veterinários e especialidades
├── system/         configuração web, idioma, cache, erro e /health
├── audit/          @Audited e AuditAspect (AOP)
├── reminder/       evento e listener assíncrono (Observer)
├── pricing/        estratégias de preço (Strategy)
├── grpc/           serviço e cliente gRPC
├── virtualthreads/ endpoint /thread
├── report/         relatório JasperReports
└── dashboard/      painel /painel e sua API
src/main/protobuf/  contrato gRPC (vets.proto)
src/main/resources/ templates Thymeleaf, mensagens, scripts SQL, layout do relatório
demos/              RMI e benchmark de threads, sem Spring
docs/uml/           diagramas UML
docs/wiki/          documentação do projeto
```

## CSS

O `petclinic.css` é gerado a partir de `src/main/scss` junto com o Bootstrap. Depois de alterar o SCSS, recompile com `./mvnw package -P css`. Os estilos acrescentados pelo grupo ficam em `site.css` e `dashboard.css`, que são editados direto.

## Créditos e licença

Projeto original: [spring-projects/spring-petclinic](https://github.com/spring-projects/spring-petclinic), mantido pela equipe do Spring. Este fork mantém a mesma licença, a [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
