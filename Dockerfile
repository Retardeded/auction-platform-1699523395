# Define the build stage with an official Gradle image
FROM gradle:8.4.0-jdk17 as build
WORKDIR /app

# Copy the Gradle configuration files
COPY build.gradle /app/
COPY settings.gradle /app/

# Copy the Gradle wrapper directory, necessary for Gradle to recognize the project structure
COPY gradle /app/gradle

# Copy the source code
COPY src /app/src
# Use the installed Gradle to run a clean build. No need to set executable permission.
RUN gradle clean build -x test --no-daemon

# Second stage: setup the runtime environment
FROM openjdk:17
WORKDIR /app

# Copy only the built jar from the build stage to the runtime stage
COPY --from=build /app/build/libs/auction-0.0.1-SNAPSHOT.jar /app/auction.jar

COPY src/main/resources /app/src/main/resources

# Add a volume pointing to /tmp
VOLUME /tmp

# Make port $PORT available to the world outside this container
ARG PORT=8080
EXPOSE $PORT

# Define environment variable for Spring's config import
ENV SPRING_CONFIG_IMPORT=optional:classpath:application-secret.properties

# Run the jar file, use $PORT
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/auction.jar"]