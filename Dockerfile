FROM gradle:8.14-jdk21 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle bootJar --no-daemon

FROM openjdk:21-jdk-slim
WORKDIR /app

COPY --from=build /app/build/libs/nebulazone-crawler-0.0.1-SNAPSHOT.jar .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "nebulazone-crawler-0.0.1-SNAPSHOT.jar"]
