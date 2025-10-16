# Stage 1: Build the application with Maven 3.9.10 and Java 21
FROM maven:3.9.10-eclipse-temurin-21 AS build

# Set working directory inside container
WORKDIR /app

# Copy Maven wrapper and configuration for caching
COPY pom.xml .

# Download dependencies (cache this layer)
RUN mvn dependency:go-offline

# Copy the source code
COPY src ./src

# Build the application with the demo profile
RUN mvn clean package -Pdemo -DskipTests

# Stage 2: Run the application using Java 21
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose Spring Boot default port
EXPOSE 8080

# Run the jar with demo profile
ENTRYPOINT ["java","-jar","/app/app.jar","--spring.profiles.active=demo"]
