FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY . .
RUN mvn clean package -pl vpn-config-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
COPY --from=builder /build/vpn-config-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]