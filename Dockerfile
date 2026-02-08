# --- 1단계: Build stage (빌드 전용 환경) ---
FROM amazoncorretto:21 AS build
WORKDIR /workspace

# Gradle Wrapper 및 설정 파일 복사 (캐시 활용의 핵심)
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 줄바꿈 문자 처리 및 권한 부여
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# 의존성 미리 다운로드 (소스 코드 변경 시에도 캐시 유지)
RUN ./gradlew --no-daemon dependencies || true

# 소스 코드 복사 및 빌드
COPY src src
RUN ./gradlew --no-daemon --build-cache bootJar

# --- 2단계: Run stage (Playwright 실행 환경) ---
# alpine 대신 브라우저 의존성이 보장된 Playwright 공식 이미지를 베이스로 사용합니다.
FROM mcr.microsoft.com/playwright/java:v1.50.0-jammy

# 환경 변수 설정: 타임존 및 JVM 옵션 [cite: 48, 49]
ENV TZ=Asia/Seoul
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -Duser.timezone=Asia/Seoul"

WORKDIR /app

# [핵심] 빌드된 JAR를 복사하고 브라우저를 미리 설치하여 런타임 시간을 단축합니다.
COPY --from=build /workspace/build/libs/*.jar /app/app.jar

# 결과물 저장을 위한 디렉토리 생성 [cite: 51]
RUN mkdir -p /app/output

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
