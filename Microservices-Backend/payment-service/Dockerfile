FROM maven:3.8-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY eureka-server/pom.xml ./eureka-server/
COPY api-gateway/pom.xml ./api-gateway/
COPY user-service/pom.xml ./user-service/
COPY movie-service/pom.xml ./movie-service/
COPY theater-service/pom.xml ./theater-service/
COPY showtime-service/pom.xml ./showtime-service/
COPY booking-service/pom.xml ./booking-service/
COPY payment-service/pom.xml ./payment-service/
COPY review-service/pom.xml ./review-service/
COPY dashboard-service/pom.xml ./dashboard-service/
COPY notification-service/pom.xml ./notification-service/
COPY settings-service/pom.xml ./settings-service/
COPY search-service/pom.xml ./search-service/
COPY payment-service/src ./payment-service/src
RUN mvn clean package -pl payment-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/payment-service/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
