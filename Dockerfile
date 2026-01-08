# -----------------------------
# BUILD STAGE
# -----------------------------
# Use Maven with Java 8
FROM maven:3.8.6-openjdk-8 AS build

# Set working directory inside the container
WORKDIR /app

# Copy pom.xml first
COPY pom.xml .

# Copy any local JARs
COPY libs ./libs
# Copy any local JARs , remember this had an issue with unable to find mainclass exceptions
COPY src ./src

# Download dependencies and build the application
# We skip tests to make builds faster for time purposes
RUN mvn clean package spring-boot:repackage -DskipTests

# -----------------------------
# 2️⃣ RUNTIME STAGE
# -----------------------------
# Java 8 runtime image
FROM eclipse-temurin:8-jre

# Set working directory for the running app
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/fraudengine-2.7.1.jar app.jar

# Expose the Spring Boot port from application.prop
EXPOSE 8081

# Run the Spring Boot application entry yes
ENTRYPOINT ["java","-jar","app.jar"]

