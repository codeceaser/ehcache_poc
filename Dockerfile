FROM openjdk:11-jre-slim
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} user_service_app.jar
ENTRYPOINT ["java", "-jar", "/user_service_app.jar"]