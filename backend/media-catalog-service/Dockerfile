FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy Maven descriptors first for better layer cache reuse.
COPY pom.xml ./pom.xml
COPY media-catalog-service/pom.xml media-catalog-service/pom.xml

# Download dependencies before copying sources.
RUN mvn -B -f media-catalog-service/pom.xml dependency:go-offline

COPY media-catalog-service/src media-catalog-service/src
RUN mvn -B -f media-catalog-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/media-catalog-service/target/media-catalog-service-0.1.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
