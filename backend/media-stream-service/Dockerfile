ARG JLIBTORRENT_VERSION=1.2.0.18

FROM maven:3-eclipse-temurin-25-alpine AS build
WORKDIR /workspace

# Copy Maven descriptors first for better layer cache reuse.
COPY pom.xml ./pom.xml
COPY sql-lib/pom.xml sql-lib/pom.xml
COPY shared-lib/pom.xml shared-lib/pom.xml
COPY api-gateway-service/pom.xml api-gateway-service/pom.xml
COPY media-catalog-service/pom.xml media-catalog-service/pom.xml
COPY media-acquisition-service/pom.xml media-acquisition-service/pom.xml
COPY media-stream-service/pom.xml media-stream-service/pom.xml

# Download dependencies before copying sources.
RUN mvn -B -pl media-stream-service -am dependency:go-offline

COPY sql-lib/src sql-lib/src
COPY shared-lib/src shared-lib/src
COPY media-stream-service/src media-stream-service/src
RUN mvn -B -pl media-stream-service -am clean package

FROM openjdk:25-rc AS extractor
ARG JLIBTORRENT_VERSION
WORKDIR /tmp

# COPY --from=build /root/.m2/repository/org/libtorrent4j/libtorrent4j-linux/${JLIBTORRENT_VERSION}/libtorrent4j-linux-${JLIBTORRENT_VERSION}.jar /tmp/libtorrent4j-linux-${JLIBTORRENT_VERSION}.jar
COPY --from=build /root/.m2/repository/com/frostwire/jlibtorrent-linux/${JLIBTORRENT_VERSION}/jlibtorrent-linux-${JLIBTORRENT_VERSION}.jar /tmp/jlibtorrent-linux-${JLIBTORRENT_VERSION}.jar

RUN jar xf /tmp/jlibtorrent-linux-${JLIBTORRENT_VERSION}.jar lib/x86_64/libjlibtorrent-${JLIBTORRENT_VERSION}.so

FROM eclipse-temurin:25-jre-noble

ARG JLIBTORRENT_VERSION
WORKDIR /app
COPY --from=extractor /tmp/lib/x86_64/libjlibtorrent-${JLIBTORRENT_VERSION}.so /app/lib/libjlibtorrent.so
COPY --from=build /workspace/media-stream-service/target/media-stream-service-0.1.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-Djava.library.path=/app/lib", "-jar", "/app/app.jar"]
