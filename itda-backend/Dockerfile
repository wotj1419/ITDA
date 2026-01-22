FROM eclipse-temurin:17-jre

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} /app/

RUN set -e; \
    JAR="$(ls /app/*.jar | grep -v plain | head -n1)"; \
    mv "$JAR" /app/app.jar; \
    find /app -maxdepth 1 -name "*.jar" -type f ! -name "app.jar" -delete

ENV JAVA_OPTS=""
EXPOSE 18080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
