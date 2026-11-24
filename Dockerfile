FROM dhi.io/eclipse-temurin:25

USER nonroot

WORKDIR /data
WORKDIR /app

COPY --chmod=444 bootstrap/build/libs/fair-share.jar fair-share.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "fair-share.jar"]
