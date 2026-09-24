FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# MAVEN_MIRROR 决定容器里的 Maven 走哪个源：
#   aliyun（默认）= 保留下面这份 settings.xml，本机行为零变化（ADR 0010）
#   central       = 删掉它，走官方中央仓；CI 在境外 runner 上直连更快也更稳
# ⚠️ ARG 只在声明它的这个 stage 内可见，写到第二个 FROM 之后就取不到值；
#    且删除动作必须排在首次解析依赖之前，否则 aliyun 之外的场景也已经
#    先拿这份 settings 下过一轮依赖了。
ARG MAVEN_MIRROR=aliyun

# 容器内 Maven 默认走阿里云：本机实测 repo1 也能通（约 1.8s vs 阿里云 0.9s），
# 选国内源只为更快更稳，不是唯一解（ADR 0010 Amendment 1）
COPY docker-maven-settings.xml /root/.m2/settings.xml
RUN if [ "$MAVEN_MIRROR" != "aliyun" ]; then rm -f /root/.m2/settings.xml; fi

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
