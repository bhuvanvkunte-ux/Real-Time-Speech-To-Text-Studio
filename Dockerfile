# Multi-Stage Docker Build for Procucev Transcription Service
# Stage 1: Build Application with Maven
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn/
RUN chmod +x ./mvnw

# Memory optimization for cloud build environments (Render, Railway, etc.)
ENV MAVEN_OPTS="-Xmx384m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# Copy source code and package
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal Production Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy built JAR from builder stage
COPY --from=builder /app/target/transcription-service-1.0.0.jar app.jar

# Expose port (overridden by cloud provider $PORT)
EXPOSE 8080 10000

ENV JAVA_OPTS="-Xmx384m -Xms128m"
ENV SPRING_PROFILES_ACTIVE=h2

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]

