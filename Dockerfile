FROM openjdk:17-jdk-slim

WORKDIR /app

ARG JAR_FILE=target/bookhouse-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
