# syntax=docker/dockerfile:1

# ---- Build stage --------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy the POM first so dependency resolution is cached in its own layer
# and only re-run when pom.xml actually changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Now copy the sources and build the application jar.
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime stage -------------------------------------------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Run as a non-root user.
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
