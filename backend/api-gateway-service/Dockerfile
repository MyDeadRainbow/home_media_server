FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy Maven descriptors first for better layer cache reuse.
COPY pom.xml ./pom.xml
COPY api-gateway-service/pom.xml api-gateway-service/pom.xml

# Download dependencies before copying sources.
RUN mvn -B -f api-gateway-service/pom.xml dependency:go-offline

COPY api-gateway-service/src api-gateway-service/src
RUN mvn -B -f api-gateway-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/api-gateway-service/target/api-gateway-service-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
