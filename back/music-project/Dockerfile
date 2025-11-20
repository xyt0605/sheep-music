# 后端 Dockerfile
# 使用多阶段构建优化镜像大小

# 阶段1：构建阶段
FROM maven:3.8-eclipse-temurin-11 AS builder

# 设置工作目录
WORKDIR /app

# 复制 pom.xml 并下载依赖（利用 Docker 缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# 复制源代码
COPY src ./src

# 打包应用（跳过测试）
RUN mvn clean package -DskipTests

# 验证构建结果
RUN echo "=== Build completed ===" && ls -la target/

# 阶段2：运行阶段
FROM eclipse-temurin:11-jre-alpine

# 设置工作目录
WORKDIR /app

# 创建非 root 用户（Alpine 使用 addgroup 和 adduser）
RUN addgroup -S spring && adduser -S spring -G spring

# 从构建阶段复制 jar 包
COPY --from=builder /app/target/*.jar app.jar

# 创建上传目录
RUN mkdir -p /app/uploads && chown -R spring:spring /app

# 暴露端口
EXPOSE 8080

# 安装 wget 用于健康检查（Alpine 需要手动安装）
USER root
RUN apk add --no-cache wget

# 健康检查（端口改为9000）
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:9000/ || exit 1

# 切换回 spring 用户
USER spring:spring

# 启动应用
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", \
    "app.jar"]

