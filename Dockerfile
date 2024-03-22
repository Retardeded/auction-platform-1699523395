# Use OpenJDK 17 as the base image
FROM openjdk:17

# Add a volume pointing to /tmp
VOLUME /tmp

# Make port $PORT available to the world outside this container
ARG PORT=8080
EXPOSE $PORT

# Copy the built jar from your host to the container
COPY build/libs/auction-0.0.1-SNAPSHOT.jar auction.jar

# Define environment variable for Spring's config import
ENV SPRING_CONFIG_IMPORT=optional:classpath:application-secret.properties

# Run the jar file, use $PORT
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /auction.jar"]