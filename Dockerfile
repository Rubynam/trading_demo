# Multi-stage build for optimized image size
# Stage 1: Build the application
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy gradle wrapper and build files first (for caching)
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the application (skip tests for faster build)
RUN ./gradlew clean build -x test --no-daemon

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre

WORKDIR /app

# Install required runtime dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    bash \
    tzdata \
    && rm -rf /var/lib/apt/lists/*

# Set timezone
ENV TZ=UTC

# Create app user for security
RUN groupadd appgroup && useradd -g appgroup appuser

# Copy built jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create directories for data
RUN mkdir -p /app/binance-data && \
    chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 11080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:11080/api/actuator/health || exit 1

# JVM options for container environment
ENV JAVA_OPTS="-Xmx1G -Xms1G -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]