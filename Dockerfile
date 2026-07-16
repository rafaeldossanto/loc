# Empacota o boot jar ja compilado pelo CI (./gradlew bootJar gera build/libs/*.jar).
# Extrai no formato lib/ + app.jar para que as dependencias fiquem numa layer
# separada do codigo da aplicacao (cache de registry aproveita entre releases).

FROM eclipse-temurin:21-jre-alpine AS extract
WORKDIR /build
COPY build/libs/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --destination application

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
COPY --from=extract /build/application/lib lib/
COPY --from=extract /build/application/app.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
