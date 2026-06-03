FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app


COPY --from=build /app/target/*.jar app.jar

# Render default port (8080)
EXPOSE 8080

# App start command
ENTRYPOINT ["java", "-jar", "app.jar"]