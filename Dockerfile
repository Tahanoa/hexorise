FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B package -DskipTests
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S hexora && adduser -S hexora -G hexora
WORKDIR /app
COPY --from=build /build/target/hexorise-0.1.0-SNAPSHOT.jar app.jar
USER hexora
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
