FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
RUN useradd --create-home --shell /bin/bash spring
COPY --from=build /app/target/*.jar app.jar
USER spring

# Most platforms (Render, Railway, Fly) inject PORT; 8080 when run locally.
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar --server.port=${PORT}"]
