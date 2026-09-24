FROM maven:3.9.16-eclipse-temurin-25-alpine AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:25.0.4_7-jre-alpine-3.24

WORKDIR /app
RUN addgroup -S spring && adduser -S -G spring spring
COPY --from=build /workspace/target/lms-0.0.1-SNAPSHOT.jar /app/app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
