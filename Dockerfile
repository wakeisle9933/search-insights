FROM openjdk:17-jdk-slim

WORKDIR /app

# Get Gradle / Source
COPY . .

RUN ./gradlew build -x test

CMD ["java", "-jar", "/app/build/libs/search-insights-0.0.1-SNAPSHOT.jar"]