# ---- Build Stage ----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .

COPY gw-home-share gw-home-share
COPY gw-home-infra-db gw-home-infra-db
COPY gw-home-api gw-home-api

RUN chmod +x gradlew && \
    ./gradlew :gw-home-api:bootJar --no-daemon -x test && \
    find /app/gw-home-api/build/libs -name "*.jar" ! -name "*-plain.jar" -exec cp {} /app/app.jar \;

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/app.jar app.jar

RUN mkdir -p /tmp/gw-home/uploads && \
    chown -R appuser:appgroup /tmp/gw-home /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
