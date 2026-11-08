FROM eclipse-temurin:17-jdk-jammy

VOLUME /tmp

EXPOSE 8082

RUN mkdir -p /app/
RUN mkdir -p /app/logs/

ADD target/business-service-0.0.1-SNAPSHOT.jar /app/app.jar

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dspring.profiles.active=dev", "-jar", "/app/app.jar"]
