# Builds the jar inside Docker, so neither CI nor a host-side `docker compose
# build` depends on a pre-existing target/ directory.

# The jar is platform-independent: build it once on the native builder
# platform rather than under QEMU for each target architecture.
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk-noble AS builder

# mvnw downloads Maven as a .zip; the JDK image ships neither curl nor unzip.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl unzip \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

# Dependencies first, so source-only changes reuse the cached layer.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp package -DskipTests

FROM eclipse-temurin:25-jre-noble

# The Ubuntu base ships Canonical Pebble (/usr/bin/pebble), an unused Go binary
# that trips image scans with fixable HIGH CVEs. We launch java directly, so drop it.
RUN rm -f /usr/bin/pebble

RUN groupadd --system --gid 1001 bourdain \
 && useradd  --system --uid 1001 --gid bourdain app

WORKDIR /app
COPY --from=builder /workspace/target/bourdain-*.jar app.jar
ENV JAVA_OPTS="-Xms128m -Xmx512m"

USER app

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
