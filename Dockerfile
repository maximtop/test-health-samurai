FROM openjdk:8-alpine

COPY target/uberjar/test-health-samurai.jar /test-health-samurai/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/test-health-samurai/app.jar"]
