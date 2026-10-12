# Chapter 101: a layered, non-root image for the issue service. Build the jar first: mvn package
FROM eclipse-temurin:25-jre AS extract
WORKDIR /build
COPY target/part13-production-1.0.0.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 northwind
WORKDIR /app
# least-changing layers first, so a code change rebuilds only the last layer
COPY --from=extract /build/extracted/dependencies/ ./
COPY --from=extract /build/extracted/spring-boot-loader/ ./
COPY --from=extract /build/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/extracted/application/ ./
USER 10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
