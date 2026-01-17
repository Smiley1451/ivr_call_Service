# Multi-stage Dockerfile
# Build stage: compile and package the Spring Boot app
FROM maven:3.10.1-jdk-21 AS build
WORKDIR /workspace
# copy maven config first to leverage layer caching
COPY pom.xml mvnw mvnw.cmd /workspace/
COPY .mvn /workspace/.mvn
# copy source
COPY src /workspace/src
# package the application (skip tests to speed up builds)
RUN mvn -B -DskipTests package

# Runtime stage: lightweight JRE image
FROM eclipse-temurin:21-jre
LABEL maintainer="you@example.com"
WORKDIR /app
# Copy the executable jar from the build stage
COPY --from=build /workspace/target/*.jar app.jar
# expose configured application port (matches application.properties default)
EXPOSE 8089
# Allow passing extra java options via environment variable
ENV JAVA_OPTS=""
# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
