FROM dhi.io/eclipse-temurin:25

WORKDIR /app
COPY bootstrap/build/libs/fair-share.jar fair-share.jar

USER nonroot
RUN mkdir -p /data

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "fair-share.jar"]
