# ---------- Stage 1: build the jar with Maven ----------
# A multi-stage build keeps the final image small: Maven and the source code stay in this stage.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copy pom.xml first and download dependencies. Docker caches this layer, so dependencies are
# only re-downloaded when pom.xml changes, not on every code change.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---------- Stage 2: run it on a small JRE-only image ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Never run as root inside the container.
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
