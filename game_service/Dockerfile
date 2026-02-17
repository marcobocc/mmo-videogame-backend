# ---------------------------------
# BUILD STAGE
# ---------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY .mvn .mvn
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# ---------------------------------
# RUNTIME STAGE
# ---------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN useradd -m appuser
USER appuser
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
