# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

RUN chmod +x mvnw && ./mvnw -B -ntp clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN mkdir -p /app/data/logs

COPY --from=build /workspace/target/*.jar app.jar

ENV SERVER_ADDRESS=0.0.0.0
ENV SERVER_PORT=8080
ENV APP_LOG_DIR=/app/data/logs

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
