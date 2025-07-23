FROM openjdk:17-jdk-slim

# 타임존 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 필요한 라이브러리 설치
RUN apt-get update && apt-get install -y \
    libfreetype6 \
    fontconfig \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Get Gradle / Source
COPY . .

# dos2unix 설치 및 gradlew 줄바꿈 문자 변환
RUN apt-get update && apt-get install -y dos2unix && \
    dos2unix ./gradlew && \
    chmod +x ./gradlew && \
    rm -rf /var/lib/apt/lists/*

RUN ./gradlew build -x test

CMD ["java", "-jar", "/app/build/libs/search-insights-0.0.1-SNAPSHOT.jar"]