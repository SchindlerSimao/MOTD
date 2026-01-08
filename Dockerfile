# Multi-stage Dockerfile: build with Maven, run with Eclipse Temurin JRE
FROM eclipse-temurin:21-jdk AS build

# install maven in the build image
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -DskipTests package && mkdir -p /out && \
    sh -c 'for f in target/*-shaded.jar target/*.jar; do if [ -f "$f" ]; then cp "$f" /out/motd.jar && exit 0; fi; done; echo "No jar found in target/" >&2; exit 1'

FROM eclipse-temurin:21-jre
WORKDIR /app
# copy the stable jar produced by the build stage
COPY --from=build /out/motd.jar /app/motd.jar
# copy log4j2 configuration to ensure logs inside container
COPY src/main/resources/log4j2.xml /app/log4j2.xml
# copy entrypoint
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh
EXPOSE 7000
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
