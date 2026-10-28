# Build stage: Maven + JDK 21
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy the wrapper and pom first so the dependency layer is cached
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:resolve dependency:resolve-plugins || true

COPY src src
# UML diagrams served by the dashboard gallery (copied to /uml at build time)
COPY docs/uml/svg docs/uml/svg
RUN ./mvnw -B -DskipTests package && cp target/spring-petclinic-*.jar app.jar

# Runtime stage: JRE 21 only
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar

# Render free plan has 512 MB of RAM. The host runs in UTC; without the Brazilian time zone
# the visit form rejects "tomorrow" after 9 p.m. local time.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -Duser.timezone=America/Sao_Paulo"

# 8080 = HTTP (Render passes the real port in PORT); 9090 = gRPC, internal only
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
