# 使用多阶段构建
# 第一阶段：构建前端
FROM node:16 AS frontend-build
WORKDIR /app
COPY src/5ganalysis-ui/package*.json ./
RUN npm install
COPY src/5ganalysis-ui .
RUN npm run build

# 第二阶段：构建后端
FROM maven:3.8.4-openjdk-17 AS backend-build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 第三阶段：最终镜像
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=backend-build /app/target/*.jar app.jar
COPY --from=frontend-build /app/dist /app/static

# 设置环境变量
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms512m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 