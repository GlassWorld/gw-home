# ---- Build Stage ----
FROM gradle:8.10.2-jdk21 AS builder

WORKDIR /home/gradle/app

COPY gradle ./gradle
COPY gradle.properties settings.gradle build.gradle ./
COPY gw-home-api/build.gradle ./gw-home-api/build.gradle
COPY gw-home-share/build.gradle ./gw-home-share/build.gradle
COPY gw-home-infra-db/build.gradle ./gw-home-infra-db/build.gradle

RUN gradle :gw-home-api:dependencies --no-daemon > /dev/null

COPY gw-home-api/src ./gw-home-api/src
COPY gw-home-share/src ./gw-home-share/src
COPY gw-home-infra-db/src ./gw-home-infra-db/src

RUN gradle :gw-home-api:bootJar --no-daemon -x test && \
    find /home/gradle/app/gw-home-api/build/libs -name "*.jar" ! -name "*-plain.jar" -exec cp {} /home/gradle/app/app.jar \;

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --from=builder /home/gradle/app/app.jar app.jar

RUN mkdir -p /tmp/gw-home/uploads && \
    chown -R appuser:appgroup /tmp/gw-home /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
