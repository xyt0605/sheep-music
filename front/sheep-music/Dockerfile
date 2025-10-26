# 前端 Dockerfile
# 使用简化版本 - 直接使用本地构建的 dist 目录
# 优点：避免容器内构建问题，构建速度更快
# 使用方法：在本地先运行 npm run build 生成 dist 目录

FROM nginx:alpine

# 设置维护者信息
LABEL maintainer="sheep-music"

# 复制自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制已构建的前端静态文件
# 注意：部署前需要在本地运行 npm run build 生成 dist 目录
COPY dist /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]

