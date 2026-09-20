FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# 容器内 Maven 走阿里云镜像，默认 repo1 在国内极易超时
COPY docker-maven-settings.xml /root/.m2/settings.xml

# 先只拷 pom，把依赖缓存成独立层；之后改源码不会重新下载依赖
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
