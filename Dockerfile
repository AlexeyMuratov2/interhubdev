# Stage 1: Maven build
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# Install Maven (Alpine JDK image does not include it)
RUN apk add --no-cache maven

COPY pom.xml .
# Download dependencies (cached unless pom changes)
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
RUN adduser -D -g '' appuser
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
