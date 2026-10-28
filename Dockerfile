# Imagem usada no deploy do Render (e em qualquer outro ambiente com Docker).
# Duas etapas: a primeira compila com Maven + JDK 21, e a segunda roda só o .jar sobre um JRE 21 enxuto.

# ---------- Etapa 1: build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copia primeiro só o wrapper e o pom, para o Docker reaproveitar o cache das dependências
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:resolve dependency:resolve-plugins || true

# Agora o código. Os testes rodam no CI/localmente; aqui só empacotamos.
COPY src src
RUN ./mvnw -B -DskipTests package && cp target/spring-petclinic-*.jar app.jar

# ---------- Etapa 2: execução ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar

# O plano free do Render tem 512 MB de RAM: limitamos o heap e usamos um GC simples.
# O servidor do Render roda em UTC: sem o fuso de Brasília, depois das 21h a "data de amanhã"
# do navegador já é "hoje" no servidor e o formulário de visita recusa a data.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -Duser.timezone=America/Sao_Paulo"

# 8080 = HTTP (o Render informa a porta pela variável PORT). 9090 = gRPC, usado só dentro do container.
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
