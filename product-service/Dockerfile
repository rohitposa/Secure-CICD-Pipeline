FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
RUN addgroup -S app && adduser -S app -G app
USER app
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]

