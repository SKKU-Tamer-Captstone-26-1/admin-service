FROM eclipse-temurin:22-jdk-alpine AS builder
WORKDIR /app

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle gradle
COPY proto proto
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:22-jre-alpine
WORKDIR /app

COPY --from=builder /app/build/libs/admin-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8095 9095

ENTRYPOINT ["java", "-jar", "app.jar"]
