# 1단계: 빌드 전용 상자
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

# 2단계: 실행 전용 상자 (여기가 최종적으로 남는 이미지)
FROM eclipse-temurin:25-jre
WORKDIR /app
# 서버 시각을 KST 로 맞춘다. 코드의 LocalDateTime.now()(작성·탈퇴 시각)가 JVM 기본 시간대를 따르므로,
# 이 값이 없으면 컨테이너 기본값인 UTC 로 저장되어 9시간 어긋나고 날짜 검색도 하루가 틀어진다.
ENV TZ=Asia/Seoul
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
