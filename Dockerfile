FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src/ src/
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home smartfinance

WORKDIR /app
COPY --from=build --chown=smartfinance:smartfinance \
    /workspace/target/smartfinance-*.jar /app/app.jar

USER smartfinance
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
