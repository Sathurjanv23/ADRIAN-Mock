# ADRN Backend — Production Dockerfile
# Multi-stage build: compile then package minimal JRE image

# ── Stage 1: Build ────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn -B package -DskipTests

# ── Stage 2: Runtime ──────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S adrn && adduser -S adrn -G adrn
USER adrn

COPY --from=builder /app/target/*.jar app.jar

# Health check for ECS/ALB
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# Use SPRING_PROFILES_ACTIVE=prod to activate application-prod.yml
ENTRYPOINT ["java", \
  "-Djdk.tls.client.protocols=TLSv1.2", \
  "-Dhttps.protocols=TLSv1.2", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
