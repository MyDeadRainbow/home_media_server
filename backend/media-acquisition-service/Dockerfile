FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy Maven descriptors first for better layer cache reuse.
COPY pom.xml ./pom.xml
COPY media-acquisition-service/pom.xml media-acquisition-service/pom.xml

# Download dependencies before copying sources.
RUN mvn -B -f media-acquisition-service/pom.xml dependency:go-offline

COPY media-acquisition-service/src media-acquisition-service/src
RUN mvn -B -f media-acquisition-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/media-acquisition-service/target/media-acquisition-service-0.1.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
