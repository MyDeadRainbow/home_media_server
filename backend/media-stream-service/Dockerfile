FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy Maven descriptors first for better layer cache reuse.
COPY pom.xml ./pom.xml
COPY shared-lib/pom.xml shared-lib/pom.xml
COPY api-gateway-service/pom.xml api-gateway-service/pom.xml
COPY media-catalog-service/pom.xml media-catalog-service/pom.xml
COPY media-acquisition-service/pom.xml media-acquisition-service/pom.xml
COPY media-stream-service/pom.xml media-stream-service/pom.xml

# Download dependencies before copying sources.
RUN mvn -B -pl media-stream-service -am dependency:go-offline

COPY shared-lib/src shared-lib/src
COPY media-stream-service/src media-stream-service/src
RUN mvn -B -pl media-stream-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/media-stream-service/target/media-stream-service-0.1.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
