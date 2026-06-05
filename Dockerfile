# Builder: glibc 기반(Ubuntu Jammy) - protoc-gen-grpc-java 바이너리가 glibc 필요
FROM eclipse-temurin:22-jdk-jammy AS builder
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY proto proto
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# Runtime: 경량 Alpine (JAR만 복사하므로 glibc 불필요)
FROM eclipse-temurin:22-jre-alpine
WORKDIR /app

COPY --from=builder /app/build/libs/admin-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8095 9095

ENTRYPOINT ["java", "-jar", "app.jar"]
