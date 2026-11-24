FROM eclipse-temurin:25-jre

WORKDIR /app
COPY bootstrap/build/libs/fair-share.jar /app/fair-share.jar

RUN mkdir -p /data && chown 10001:10001 /data
USER 10001:10001

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/fair-share.jar"]
