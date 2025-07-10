FROM maven:3.9.9-eclipse-temurin-21-alpine AS DEPENDENCIES

WORKDIR /opt/app
COPY pom.xml .
RUN mvn -B -e org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline -DexcludeArtifactIds=domain


FROM maven:3.9.9-eclipse-temurin-21 AS BUILDER
WORKDIR /opt/app
COPY --from=DEPENDENCIES /root/.m2 /root/.m2
COPY --from=DEPENDENCIES /opt/app/ /opt/app
COPY src /opt/app/src

RUN mvn -B -e clean package -DskipTests

RUN ls /opt/app/target/

FROM eclipse-temurin:21-jdk-alpine

WORKDIR /opt/app

COPY --from=BUILDER /opt/app/target/app.jar /app.jar

EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app.jar"]