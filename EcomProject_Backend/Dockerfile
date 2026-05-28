FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline

COPY src ./src
RUN ./mvnw -B clean package -DskipTests
RUN find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' -exec cp {} target/app.jar \;

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN useradd --system --create-home --shell /usr/sbin/nologin spring

COPY --from=build /app/target/app.jar app.jar

USER spring
EXPOSE 9005
ENTRYPOINT ["java","-jar","app.jar"]
