# Backend (연구 서비스) - Java Spring Boot
# 서버 2: 연구 CRUD, 참여자 관리. 블록체인 필요 시 Core(서버 1) API 호출.
# Alpine 이미지는 Apple Silicon 등에서 manifest 없음 오류가 나므로 non-alpine 사용.

FROM gradle:8-jdk17 AS builder

WORKDIR /app

COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
COPY src ./src

RUN gradle bootJar -x test --no-daemon

# ---

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN useradd -m -u 1001 appuser

COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown appuser:appuser /app/app.jar

USER appuser

ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
