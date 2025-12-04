# 第一阶段：构建前端Vue应用
FROM node:20-alpine as frontend-builder
WORKDIR /app/frontend

# 复制前端项目文件
COPY front-ui/package*.json ./
COPY front-ui/pnpm-lock.yaml ./

# 安装pnpm并安装依赖
RUN npm install -g pnpm && pnpm install

# 复制前端源代码并构建
COPY front-ui .
RUN pnpm build

# 第二阶段：构建后端Spring Boot应用
FROM maven:3.9.8-eclipse-temurin-21-alpine as backend-builder
WORKDIR /app/backend

# 复制后端项目文件
COPY pom.xml .
COPY datacopilotx-common ./datacopilotx-common
COPY datacopilotx-aigateway ./datacopilotx-aigateway
COPY datacopilotx-ai ./datacopilotx-ai

# 构建后端应用
RUN mvn clean package -DskipTests

# 第三阶段：最终镜像，运行应用
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 设置环境变量
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE=prod

# 复制构建好的后端jar文件
COPY --from=backend-builder /app/backend/datacopilotx-ai/target/datacopilotx-ai-1.0.0.jar ./app.jar

# 复制构建好的前端静态资源到后端可访问的位置
RUN mkdir -p /app/static
COPY --from=frontend-builder /app/frontend/dist /app/static

# 暴露应用端口
EXPOSE 8080

# 启动应用
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]