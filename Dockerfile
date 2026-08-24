FROM eclipse-temurin:25-jre

# The Ubuntu base ships Canonical Pebble (/usr/bin/pebble), an unused Go binary
# that trips image scans with fixable HIGH CVEs. We launch java directly, so drop it.
RUN rm -f /usr/bin/pebble

RUN groupadd --system --gid 1001 bourdain \
 && useradd  --system --uid 1001 --gid bourdain app

WORKDIR /app
COPY target/bourdain-*.jar app.jar
ENV JAVA_OPTS="-Xms128m -Xmx512m"

USER app

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
