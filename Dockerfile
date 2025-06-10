# 빌드 스테이지
FROM gradle:8.5-jdk17-alpine AS builder

WORKDIR /app

# Gradle 캐시를 위한 의존성 파일 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 활용)
RUN gradle dependencies --no-daemon

# 소스 코드 복사 및 빌드
COPY src ./src
RUN gradle build -x test --no-daemon

# 실행 스테이지
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 필요한 패키지 설치
RUN apk add --no-cache curl

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 사용자 생성 (보안)
RUN addgroup -g 1000 ocean && \
    adduser -D -u 1000 -G ocean ocean && \
    chown -R ocean:ocean /app

USER ocean

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 포트 노출
EXPOSE 8080

# 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]