FROM eclipse-temurin:17-jdk-jammy

# Create temporary drive
VOLUME /tmp

# Expose User Service port
EXPOSE 8081

# Create app directory
RUN mkdir -p /app/

# Create logs directory
RUN mkdir -p /app/logs/

# Copy generated JAR
ADD target/user-service-0.0.1-SNAPSHOT.jar /app/app.jar

# Run the JAR with the dev profile
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]
