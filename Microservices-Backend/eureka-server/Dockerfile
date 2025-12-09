FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY ../pom.xml /app/../pom.xml
COPY pom.xml .
COPY src ./src
COPY --from=maven:3.8-eclipse-temurin-17 /usr/share/maven /usr/share/maven
RUN ln -s /usr/share/maven/bin/mvn /usr/bin/mvn && mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
