FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /workspace

# Copy Maven descriptors first for better layer cache reuse.
COPY pom.xml ./pom.xml
COPY sql-lib/pom.xml sql-lib/pom.xml
COPY shared-lib/pom.xml shared-lib/pom.xml
COPY api-gateway-service/pom.xml api-gateway-service/pom.xml
COPY media-catalog-service/pom.xml media-catalog-service/pom.xml
COPY media-acquisition-service/pom.xml media-acquisition-service/pom.xml
COPY media-stream-service/pom.xml media-stream-service/pom.xml
COPY frontend-spring/pom.xml frontend-spring/pom.xml

# Download dependencies before copying sources.
RUN mvn -B -pl frontend-spring -am dependency:go-offline

COPY sql-lib/src sql-lib/src
COPY shared-lib/src shared-lib/src
COPY frontend-spring/src frontend-spring/src
RUN mvn -B -pl frontend-spring -am clean package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=build /workspace/frontend-spring/target/frontend-spring-0.1.0.jar app.jar
EXPOSE 8085

# ENTRYPOINT ["xvfb-run", "java", "-Djava.library.path=/app/lib", "-jar", "/app/app.jar"]
ENTRYPOINT ["java", "-Djava.library.path=/app/lib", "-jar", "/app/app.jar"]