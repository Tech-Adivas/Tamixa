# Build stage: Gradle build (monorepo: backend module)
FROM gradle:8.7-jdk21-alpine AS build
WORKDIR /app

# Copy Gradle config and wrapper
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradlew ./
COPY gradle ./gradle

# Copy backend module
COPY backend ./backend

# Download dependencies and build backend JAR (skips mobile)
RUN chmod +x gradlew && \
    ./gradlew :backend:bootJar -Ptamixa.backendOnly=true -x test --no-daemon

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

RUN addgroup -g 1000 app && adduser -u 1000 -G app -D app
USER app

COPY --from=build /app/backend/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
