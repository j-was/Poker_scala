# Build stage
FROM sbtscala/scala-sbt:eclipse-temurin-17.0.14_7_1.10.11_3.6.4 AS build

WORKDIR /app

# Copy build files first for better caching
COPY build.sbt ./
COPY project/ ./project/

# Download dependencies
RUN sbt update

# Copy source code
COPY src/ ./src/

# Build the application
RUN sbt stage

# Run stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for health checks (optional)
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -S poker && adduser -S poker -G poker

# Copy staged application from build
COPY --from=build /app/target/universal/stage/ /app/

# Change ownership to non-root user
RUN chown -R poker:poker /app

# Switch to non-root user
USER poker

# Expose the port (Azure Container Apps uses PORT env variable)
EXPOSE 8080

# Default environment variables
ENV POKER_HOST=0.0.0.0
ENV POKER_PORT=8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:${POKER_PORT}/health || exit 1

# Run the application
ENTRYPOINT ["./bin/poker-server"]