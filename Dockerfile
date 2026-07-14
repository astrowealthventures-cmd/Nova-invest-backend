# --- Stage 1: build the jar with Maven ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml first and download dependencies separately so Docker can cache this
# layer - it only re-runs if pom.xml actually changes, not on every code edit.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Stage 2: run it on a minimal JRE (no Maven, no build tools in the final image) ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/nova-invest-backend-1.0.0.jar app.jar

# Railway sets $PORT at runtime - server.port in application.yml already
# defaults to 8000, but Railway expects the app to listen on whatever
# port it assigns, so we pass it through explicitly here.
ENV PORT=8000
EXPOSE 8000

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]