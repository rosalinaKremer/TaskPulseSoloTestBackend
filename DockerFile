# Step 1: Use an official JDK image to build the app
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy the maven wrapper and pom file first (optimizes build caching)
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline

# Copy the source code and build the jar
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Step 2: Use a lighter JRE image to run the app
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Expose the port Spring Boot runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]