# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# No Maven wrapper in this repo -- the maven: base image already bundles mvn itself.
# Copy the backend source into the project root so the Maven build runs in the current folder
COPY source ./
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /workspace

# Copy only the packaged JAR from the build stage into the runtime working directory
COPY --from=build /workspace/applications/main/target/*.jar /workspace/app.jar


EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/workspace/app.jar"]
