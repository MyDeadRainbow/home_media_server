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

FROM openjdk:25-rc AS extractor
WORKDIR /tmp

COPY --from=build /root/.m2/repository/org/libtorrent4j/libtorrent4j-linux/2.0.5-25/libtorrent4j-linux-2.0.5-25.jar /tmp/libtorrent4j-linux-2.0.5-25.jar

RUN jar xf /tmp/libtorrent4j-linux-2.0.5-25.jar lib/x86_64/libtorrent4j.so

FROM mcr.microsoft.com/playwright:v1.60.0-noble
WORKDIR /app

RUN apt-get update && apt-get install -y openjdk-25-jre-headless && rm -rf /var/lib/apt/lists/*

ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
ENV LD_LIBRARY_PATH=/app/lib
ENV DISPLAY=:99

# Use Playwright's installer to resolve distro-specific system deps and Chromium.
# RUN mkdir -p /tmp/playwright
# COPY --from=build /root/.m2/repository/com/microsoft/playwright/playwright/1.60.0/playwright-1.60.0.jar /tmp/playwright/playwright.jar
# COPY --from=build /root/.m2/repository/com/microsoft/playwright/driver/1.60.0/driver-1.60.0.jar /tmp/playwright/driver.jar
# COPY --from=build /root/.m2/repository/com/microsoft/playwright/driver-bundle/1.60.0/driver-bundle-1.60.0.jar /tmp/playwright/driver-bundle.jar
# RUN java -cp "/tmp/playwright/*" com.microsoft.playwright.CLI install --with-deps chromium \
#     && rm -rf /tmp/playwright

# # This gets rid of a warning when starting playwright, but it is not strictly nesessary for the functionality we use.
# # this greatly increases the size of the image, but it is needed for playwright to work properly.
# RUN apt-get update && apt-get install -y \
#     libxcursor1 \
#     libgtk-3-0 \
#     libpangocairo-1.0-0 \
#     libcairo-gobject2 \
#     libgdk-pixbuf-2.0-0 \
#     xvfb 



COPY --from=extractor /tmp/lib/x86_64/libtorrent4j.so /app/lib/libtorrent4j.so

COPY --from=build /workspace/media-acquisition-service/target/media-acquisition-service-0.1.0.jar app.jar
EXPOSE 8082

ENTRYPOINT ["java", "-Djava.library.path=/app/lib", "-jar", "/app/app.jar"]