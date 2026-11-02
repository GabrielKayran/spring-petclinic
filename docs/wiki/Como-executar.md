# Como executar

[← Início](Home.md)

## Pré-requisitos

- JDK 21 (o `maven-enforcer-plugin` recusa versões menores)
- Nada mais: o Maven Wrapper (`mvnw`) baixa o Maven sozinho

## Local, com H2 em memória

```bash
./mvnw spring-boot:run
```

Abra http://localhost:8080. O banco H2 é criado na subida a partir de `db/h2/schema.sql` e `db/h2/data.sql`, e some quando a aplicação para.

O gRPC sobe junto na porta 9090. Para testá-lo de fora, rode a classe `GrpcClient` (pacote `grpc.client`) pela IDE com a aplicação no ar, ou use o card 2 do painel (`/painel`).

## Local, com PostgreSQL

O perfil `postgres` está em [application-postgres.properties](../../src/main/resources/application-postgres.properties):

```properties
database=postgres
spring.datasource.url=${POSTGRES_URL:jdbc:postgresql://localhost/petclinic}
spring.datasource.username=${POSTGRES_USER:petclinic}
spring.datasource.password=${POSTGRES_PASS:petclinic}
# SQL is written to be idempotent so this is safe
spring.sql.init.mode=always
```

As credenciais vêm de variáveis de ambiente e nunca vão para o repositório. Com um Postgres local (por exemplo, `docker compose up postgres`):

```bash
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
```

## Configuração principal

Trechos de [application.properties](../../src/main/resources/application.properties):

```properties
# Render and similar platforms pass the HTTP port in PORT
server.port=${PORT:8080}

spring.jpa.hibernate.ddl-auto=none
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.default_batch_fetch_size=16

# Virtual threads (Java 21+) for Tomcat requests and the @Async executor
spring.threads.virtual.enabled=true
```

- `ddl-auto=none`: o Hibernate não cria tabelas, quem cria é o `schema.sql`.
- `open-in-view=false`: a sessão do banco fecha ao fim do repositório, não ao fim da página.
- `spring.threads.virtual.enabled=true`: uma linha liga as virtual threads no Tomcat e no `@Async` (veja [Concorrência](Concorrencia-e-virtual-threads.md)).

## Docker

O [Dockerfile](../../Dockerfile) tem dois estágios: compila com JDK 21 e roda só com JRE 21.

```dockerfile
FROM eclipse-temurin:21-jdk AS build
...
RUN ./mvnw -B -DskipTests package && cp target/spring-petclinic-*.jar app.jar

FROM eclipse-temurin:21-jre
COPY --from=build /app/app.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -Duser.timezone=America/Sao_Paulo"
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
```

As opções da JVM servem para caber nos 512 MB do plano gratuito do Render. O fuso de São Paulo evita que o formulário de visita recuse "amanhã" depois das 21h, porque o servidor roda em UTC.

```bash
docker build -t petclinic .
docker run -p 8080:8080 petclinic
```

## Produção: Render + Neon

O [render.yaml](../../render.yaml) descreve o serviço:

```yaml
services:
  - type: web
    name: petclinic-poo2
    runtime: docker
    plan: free
    region: virginia
    healthCheckPath: /health
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: postgres
      - key: POSTGRES_URL
        sync: false
      - key: POSTGRES_USER
        sync: false
      - key: POSTGRES_PASS
        sync: false
      - key: MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE
        value: health,info
```

- `sync: false` faz o Render pedir o valor no painel, então a senha do Neon não fica no Git.
- `healthCheckPath: /health` é o endpoint do [HealthController](Painel-e-API.md#health).
- Em produção o Actuator expõe só `health` e `info`.

**Para publicar uma versão nova:** faça o push para `main` e, no painel do Render, use *Manual Deploy → Deploy latest commit*. No plano gratuito a primeira requisição depois de um tempo parado leva uns 50 s, porque o serviço "acorda".

Diagrama: [UML 11, implantação em produção](../uml/svg/11_implantacao_producao_render_neon.svg).
