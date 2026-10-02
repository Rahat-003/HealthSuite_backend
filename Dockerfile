# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build
COPY pom.xml .
# Download dependencies first (Docker layer cache-friendly)
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime

# Install Playwright system dependencies (required by Chromium)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libnss3 libnspr4 libatk1.0-0 libatk-bridge2.0-0 libcups2 \
    libdrm2 libdbus-1-3 libxkbcommon0 libxcomposite1 libxdamage1 \
    libxrandr2 libgbm1 libpango-1.0-0 libasound2 libxshmfence1 \
    libx11-6 libxext6 libxfixes3 wget ca-certificates fonts-liberation \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Extract layered JAR for better Docker caching
COPY --from=builder /build/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Install Playwright Chromium browser for scraper module
RUN java -cp "dependencies/BOOT-INF/lib/*:snapshot-dependencies/BOOT-INF/lib/*" \
    com.microsoft.playwright.CLI install chromium 2>/dev/null || \
    echo "Playwright browser install skipped (will use JSoup fallback)"

EXPOSE 20580

# Use layered startup for faster boot
ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
