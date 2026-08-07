FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built jar
COPY target/lms-notification-service-0.0.1-SNAPSHOT.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
