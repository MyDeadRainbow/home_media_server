FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /workspace

# Copy Maven descriptors first for better layer cache reuse.
COPY pom.xml ./pom.xml
COPY shared-lib/pom.xml shared-lib/pom.xml
COPY api-gateway-service/pom.xml api-gateway-service/pom.xml
COPY media-catalog-service/pom.xml media-catalog-service/pom.xml
COPY media-acquisition-service/pom.xml media-acquisition-service/pom.xml
COPY media-stream-service/pom.xml media-stream-service/pom.xml

# Download dependencies before copying sources.
RUN mvn -B -pl media-acquisition-service -am dependency:go-offline

COPY shared-lib/src shared-lib/src
COPY media-acquisition-service/src media-acquisition-service/src
RUN mvn -B -pl media-acquisition-service -am clean package -DskipTests

# WORKDIR /tmp

# Extract native libtorrent4j library from the maven artifact
# RUN mkdir -p /tmp/libtorrent-lib \
#     && cd /tmp/libtorrent-lib \
#     && jar xf /root/.m2/repository/org/libtorrent4j/libtorrent4j-linux/2.1.0-39/libtorrent4j-linux-2.1.0-39.jar \
#     && find /tmp/libtorrent-lib -name "*.so" -type f

# RUN apt-get update && apt-get install -y \
#     build-essential \
#     cmake \
#     git \
#     wget \
#     && rm -rf /var/lib/apt/lists/*
FROM openjdk:25-rc AS extractor
WORKDIR /tmp
# RUN mkdir -p /tmp/libtorrent-lib
COPY --from=build /root/.m2/repository/org/libtorrent4j/libtorrent4j-linux/2.0.5-25/libtorrent4j-linux-2.0.5-25.jar /tmp/libtorrent4j-linux-2.0.5-25.jar
# Extract only the specific file you need 
# RUN git clone https://github.com/aldenml/libtorrent4j.git
# WORKDIR /tmp/libtorrent4j
# RUN ./gradlew build
# WORKDIR /tmp
RUN jar xf /tmp/libtorrent4j-linux-2.0.5-25.jar lib/x86_64/libtorrent4j.so

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV LD_LIBRARY_PATH=/app/lib
#:$LD_LIBRARY_PATH

# Use Playwright's installer to resolve distro-specific system deps and Chromium.
RUN mkdir -p /tmp/playwright
COPY --from=build /root/.m2/repository/com/microsoft/playwright/playwright/1.60.0/playwright-1.60.0.jar /tmp/playwright/playwright.jar
COPY --from=build /root/.m2/repository/com/microsoft/playwright/driver/1.60.0/driver-1.60.0.jar /tmp/playwright/driver.jar
COPY --from=build /root/.m2/repository/com/microsoft/playwright/driver-bundle/1.60.0/driver-bundle-1.60.0.jar /tmp/playwright/driver-bundle.jar
RUN java -cp "/tmp/playwright/*" com.microsoft.playwright.CLI install --with-deps chromium \
    && rm -rf /tmp/playwright

# Copy your local JAR into the extraction layer
# RUN mkdir -p /tmp/libtorrent-lib
# COPY --from=build /root/.m2/repository/org/libtorrent4j/libtorrent4j-linux/2.1.0-39/libtorrent4j-linux-2.1.0-39.jar /tmp/libtorrent-lib/libtorrent4j-linux-2.1.0-39.jar
# # Extract only the specific file you need 
# RUN jar xf /tmp/libtorrent-lib/libtorrent4j-linux-2.1.0-39.jar lib/x86_64/libtorrent4j.so
# RUN ls

# COPY /tmp/libtorrent-lib/lib/x86_64/libtorrent4j.so /app/lib/libtorrent4j.so
COPY --from=extractor /tmp/lib/x86_64/libtorrent4j.so /app/lib/libtorrent4j.so

# Copy native libtorrent4j library extracted in build stage
# RUN mkdir -p /app/lib
# COPY --from=build /tmp/libtorrent-lib/lib/ /app/lib/
# RUN apt-get update && apt-get install -y \
#     build-essential \
#     cmake \
#     git \
#     wget \
#     && rm -rf /var/lib/apt/lists/*

# RUN apt-get update && apt-get install -y \
#     libc6 \*
    # libgconf-2-4 \
    # libnss3 \
    # libatk-bridge2.0-0 \
    # libcups2 \
    # libdrm2 \
    # libgbm-dev \
    # libasound2 \
    # libxcomposite-dev \
    # libxdamage1 \
    # libxrandr2 \
    # xdg-utils \
    # && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/media-acquisition-service/target/media-acquisition-service-0.1.0.jar app.jar
EXPOSE 8082
# EXPOSE 6881/udp
# EXPOSE 6891/tcp
ENTRYPOINT ["java", "-Djava.library.path=/app/lib", "-jar", "/app/app.jar"]
