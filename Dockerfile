FROM openjdk:17-jdk-slim

# 타임존 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 필요한 라이브러리 설치
RUN apt-get update && apt-get install -y \
    libfreetype6 \
    fontconfig \
    && rm -rf /var/lib/apt/lists/*

# Python, pytrends 설치
RUN apt-get update && apt-get install -y python3 python3-pip
RUN pip3 install pytrends

WORKDIR /app

# Get Gradle / Source
COPY . .

# Python 파일 복사
COPY src/main/resources/python /app/src/main/resources/python

RUN ./gradlew build -x test

CMD ["java", "-jar", "/app/build/libs/search-insights-0.0.1-SNAPSHOT.jar"]