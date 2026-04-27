FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml

RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src

RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN useradd --create-home --shell /bin/bash spring

COPY --from=build /app/target/*.jar app.jar

USER spring

ENV PORT=10000

EXPOSE 10000

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar --server.port=${PORT:-10000}"]
