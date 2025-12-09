FROM eclipse-temurin:17-jdk AS build
WORKDIR /build
COPY pom.xml .
COPY --from=maven:3.8-eclipse-temurin-17 /usr/share/maven /usr/share/maven
RUN ln -s /usr/share/maven/bin/mvn /usr/bin/mvn && mvn install -N
WORKDIR /build/eureka-server
COPY eureka-server/pom.xml .
COPY eureka-server/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/eureka-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
