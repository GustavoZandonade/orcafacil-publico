
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/orcafacil-0.1.0.jar app.jar

EXPOSE 10000

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0"

CMD ["java", "-jar", "app.jar"]
