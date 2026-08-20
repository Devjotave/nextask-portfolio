# syntax=docker/dockerfile:1

FROM maven:3.9.16-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine-3.23
WORKDIR /app

RUN addgroup -S nextask && adduser -S nextask -G nextask

COPY --from=build --chown=nextask:nextask \
    /workspace/target/nextask-backend-*.jar ./nextask.jar

USER nextask
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/urandom"

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/nextask.jar"]
