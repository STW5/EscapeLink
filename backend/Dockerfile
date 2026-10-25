FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S escapelink && adduser -S escapelink -G escapelink \
    && mkdir -p /app /srv/escapelink/uploads \
    && chown -R escapelink:escapelink /app /srv/escapelink
WORKDIR /app
COPY --from=build --chown=escapelink:escapelink /workspace/build/libs/*.jar app.jar

USER escapelink
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
