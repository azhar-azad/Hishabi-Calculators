# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Resolve dependencies first — this layer is cached on code-only changes
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B dependency:resolve dependency:resolve-plugins

COPY src src
RUN ./mvnw package -DskipTests -B

# ── Stage 2: runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=build /app/target/calculators-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8080

# MaxRAMPercentage=60 leaves ~100 MB for JVM non-heap (Metaspace, code cache,
# thread stacks) on Render's 256 MB free tier. 75% would leave only ~64 MB and
# cause OOM kills under load with this JPA/Security/Flyway stack.
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=60.0", "-XX:MaxMetaspaceSize=96m", "-jar", "app.jar"]
