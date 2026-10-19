# Build and run the API. Multi-stage so the shipped image carries a JRE and a
# jar, not a JDK and a Maven cache.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Dependencies first: this layer is cached unless the pom changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Run as a non-root user.
RUN addgroup -S pos && adduser -S pos -G pos
COPY --from=build /build/target/hardware-pos-*.jar app.jar
USER pos
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
