# Multi-stage build for Spring Boot 3 + Java 17
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy maven wrapper and pom.xml first for caching dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build jar
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Production runtime image (lightweight)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (default 8081 or dynamic $PORT on Render/Railway)
ENV PORT=8081
EXPOSE 8081

# Disable Cloudflare local tunnel on cloud server
ENV APP_CLOUDFLARE_TUNNEL_ENABLED=false

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8081} -Dfile.encoding=UTF-8 -jar app.jar"]
