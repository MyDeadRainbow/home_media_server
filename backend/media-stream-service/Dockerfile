ARG LIBTORRENT4J_VERSION=2.1.0-39

FROM maven:3-eclipse-temurin-25-alpine AS build
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

FROM openjdk:25-rc AS extractor
ARG LIBTORRENT4J_VERSION
WORKDIR /tmp

COPY --from=build /root/.m2/repository/org/libtorrent4j/libtorrent4j-linux/${LIBTORRENT4J_VERSION}/libtorrent4j-linux-${LIBTORRENT4J_VERSION}.jar /tmp/libtorrent4j-linux-${LIBTORRENT4J_VERSION}.jar

RUN jar xf /tmp/libtorrent4j-linux-${LIBTORRENT4J_VERSION}.jar lib/x86_64/libtorrent4j.so

FROM eclipse-temurin:25-jre-noble

WORKDIR /app
COPY --from=extractor /tmp/lib/x86_64/libtorrent4j.so /app/lib/libtorrent4j.so
COPY --from=build /workspace/media-stream-service/target/media-stream-service-0.1.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-Djava.library.path=/app/lib", "-jar", "/app/app.jar"]
