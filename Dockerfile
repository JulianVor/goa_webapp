# --- Build stage ---------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies separately from source so code changes don't re-download them.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- Runtime stage ---------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S goa && adduser -S goa -G goa
COPY --from=build /build/target/goa-webapp-*.jar app.jar
RUN mkdir -p /data/uploads && chown -R goa:goa /data /app
USER goa

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
