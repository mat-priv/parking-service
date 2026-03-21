FROM eclipse-temurin:25-jre

COPY build/libs/parking-service-0.0.1-SNAPSHOT.jar parking-service-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "parking-service-0.0.1-SNAPSHOT.jar"]
