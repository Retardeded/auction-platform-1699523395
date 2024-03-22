# First stage: build the application
FROM openjdk:17 as build
WORKDIR /app

# Copy the Gradle configuration files first (for caching purposes)
COPY gradlew /app/
COPY gradle /app/gradle
COPY build.gradle /app/
COPY settings.gradle /app/

# Copy the source code
COPY src /app/src

# Grant execution permissions and run the build
RUN chmod +x /app/gradlew && ./app/gradlew clean build --no-daemon

# Second stage: setup the runtime environment
FROM openjdk:17
WORKDIR /app

# Copy only the built jar from the build stage to the runtime stage
COPY --from=build /app/build/libs/auction-0.0.1-SNAPSHOT.jar /app/auction.jar

# Add a volume pointing to /tmp
VOLUME /tmp

# Make port $PORT available to the world outside this container
ARG PORT=8080
EXPOSE $PORT

# Define environment variable for Spring's config import
ENV SPRING_CONFIG_IMPORT=optional:classpath:application-secret.properties

# Run the jar file, use $PORT
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/auction.jar"]