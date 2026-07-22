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
RUN mvn -B -pl media-acquisition-service -am dependency:go-offline

COPY sql-lib/src sql-lib/src
COPY shared-lib/src shared-lib/src
COPY media-acquisition-service/src media-acquisition-service/src
COPY frontend-spring/src frontend-spring/src
RUN mvn -B -pl media-acquisition-service -am clean package

# FROM openjdk:25-rc AS extractor
# WORKDIR /tmp

# COPY --from=build /root/.m2/repository/org/libtorrent4j/libtorrent4j-linux/2.0.5-25/libtorrent4j-linux-2.0.5-25.jar /tmp/libtorrent4j-linux-2.0.5-25.jar

# RUN jar xf /tmp/libtorrent4j-linux-2.0.5-25.jar lib/x86_64/libtorrent4j.so

# FROM mcr.microsoft.com/playwright:v1.60.0-noble
# WORKDIR /app

# RUN apt-get update && apt-get install -y openjdk-25-jre-headless && rm -rf /var/lib/apt/lists/*

# ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
# ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
# ENV LD_LIBRARY_PATH=/app/lib
# ENV DISPLAY=:99

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=build /workspace/media-acquisition-service/target/media-acquisition-service-0.1.0.jar app.jar
EXPOSE 8082

# ENTRYPOINT ["xvfb-run", "java", "-Djava.library.path=/app/lib", "-jar", "/app/app.jar"]
ENTRYPOINT ["java", "-Djava.library.path=/app/lib", "-jar", "/app/app.jar"]