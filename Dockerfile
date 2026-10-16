FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /app/target/idea-propia-primero-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/data

CMD ["sh", "-c", "exec java -jar app.jar --server.address=0.0.0.0 --server.port=${PORT:-8080}"]