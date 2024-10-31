FROM maven:3-eclipse-temurin-21-alpine AS build
ARG VER=SNAPSHOT
COPY src /build/src
COPY pom.xml /build/pom.xml
WORKDIR /build
RUN mvn -Drevision=${VER} package

FROM maven:3-eclipse-temurin-21-alpine AS builder
WORKDIR /tmp
COPY --from=build /build/target/*.jar /tmp/
RUN java -Djarmode=layertools -jar *.jar extract

FROM gcr.io/distroless/java21-debian12:nonroot
ENV APP=db-sts.jar
WORKDIR /app
LABEL org.opencontainers.image.source=https://github.com/scc-digitalhub/db-sts
COPY --from=builder /tmp/dependencies/ ./
COPY --from=builder /tmp/snapshot-dependencies/ ./
COPY --from=builder /tmp/spring-boot-loader/ ./
COPY --from=builder /tmp/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
