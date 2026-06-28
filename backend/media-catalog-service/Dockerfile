FROM maven:3-eclipse-temurin-25-alpine AS build
WORKDIR /workspace

# Copy Maven descriptors first for better layer cache reuse.
COPY pom.xml ./pom.xml
COPY shared-lib/pom.xml shared-lib/pom.xml
COPY sql-lib/pom.xml sql-lib/pom.xml
COPY api-gateway-service/pom.xml api-gateway-service/pom.xml
COPY media-catalog-service/pom.xml media-catalog-service/pom.xml
COPY media-acquisition-service/pom.xml media-acquisition-service/pom.xml
COPY media-stream-service/pom.xml media-stream-service/pom.xml

# Download dependencies before copying sources.
RUN mvn -B -pl media-catalog-service -am dependency:go-offline

COPY shared-lib/src shared-lib/src
COPY sql-lib/src sql-lib/src
COPY media-catalog-service/src media-catalog-service/src
RUN mvn -B -pl media-catalog-service -am clean package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /workspace/media-catalog-service/target/media-catalog-service-0.1.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
