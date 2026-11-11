FROM eclipse-temurin:17-jdk-jammy

VOLUME /tmp

EXPOSE 8083

RUN mkdir -p /app/
RUN mkdir -p /app/logs/

ADD target/notification-service-0.0.1-SNAPSHOT.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]
