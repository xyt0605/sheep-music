# 🎵 Sheep Music

一个基于 Vue.js + Spring Boot 的在线音乐播放系统，支持 Docker 一键部署。

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Docker](https://img.shields.io/badge/docker-ready-brightgreen.svg)
![Vue](https://img.shields.io/badge/vue-3.x-green.svg)
![Spring Boot](https://img.shields.io/badge/spring%20boot-2.4.x-green.svg)

---

## ✨ 特性

- 🎨 **现代化UI**: 基于 Element Plus 的美观界面
- 🎵 **音乐播放**: 支持在线播放、播放列表、收藏等功能
- 👤 **用户系统**: 注册、登录、个人资料管理
- 📊 **数据统计**: 播放历史、搜索记录
- 🔒 **安全认证**: JWT 令牌认证
- 🐳 **Docker部署**: 一键启动，开箱即用
- 📱 **响应式设计**: 支持移动端和桌面端

---

## 🏗️ 技术栈

### 前端
- Vue 3
- Vue Router
- Pinia (状态管理)
- Element Plus (UI组件库)
- Axios (HTTP客户端)
- Vite (构建工具)

### 后端
- Spring Boot 2.4
- Spring Security
- Spring Data JPA
- MySQL 8.0
- JWT
- Maven

### 部署
- Docker
- Docker Compose
- Nginx

---

## 📦 快速开始

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- Git
- Node.js 16+ (仅用于本地构建前端)

### 部署步骤

#### ⚠️ 重要提醒

**如果修改了前端代码，必须先构建 dist 目录！**

本项目使用简化版 Dockerfile，直接复制本地构建的 `dist` 目录。详见 [构建和推送指南.md](构建和推送指南.md)

#### 1. 克隆项目

```bash
git clone https://github.com/your-username/sheep-music.git
cd sheep-music
```

#### 2. 配置环境变量（可选）

```bash
cp env.template .env
# 编辑 .env 文件，修改数据库密码等配置
```

#### 3. 构建前端（如果修改了前端代码）

```bash
cd front/sheep-music

# 安装依赖（首次）
npm install --legacy-peer-deps

# 构建生产版本
npm run build

# 返回项目根目录
cd ../..
```

#### 4. 部署到服务器

```bash
# 上传项目到服务器后，运行部署脚本
bash deploy.sh

# 或手动部署
docker compose up -d --build
```

#### 5. 访问应用

- **前端页面**: `http://你的服务器IP`
- **后端 API**: `http://你的服务器IP:8080`

---

## 📖 详细文档

### 🚨 必读文档
- [⚠️部署前必读.txt](⚠️部署前必读.txt) - **重要提醒**：构建说明
- [构建和推送指南.md](构建和推送指南.md) - 前端构建详细说明

### 部署相关
- [一键部署命令.md](一键部署命令.md) - 一键复制粘贴的命令（最快）
- [快速部署指令](QUICK_DEPLOY.md) - 快速部署命令参考
- [详细部署步骤](DEPLOYMENT_STEPS.md) - 完整的部署流程说明
- [部署检查清单](DEPLOYMENT_CHECKLIST.md) - 部署前检查项
- [部署说明](部署说明.txt) - 简明部署指南

### 其他文档
- [完整部署指南](DEPLOYMENT_GUIDE.md) - 包含详细的部署步骤和故障排查（如果存在）
- [Docker 部署指南](DOCKER_DEPLOYMENT_GUIDE.md) - Docker 相关说明（如果存在）

---

## 🗂️ 项目结构

```
sheep-music/
├── front/                    # 前端项目
│   └── sheep-music/
│       ├── src/             # 源代码
│       ├── public/          # 静态资源
│       ├── dist/            # 构建输出（需提交）
│       ├── Dockerfile       # 简化版 Dockerfile
│       ├── Dockerfile.build # 容器内构建版本
│       ├── nginx.conf       # Nginx 配置
│       └── package.json     # 依赖配置
├── back/                    # 后端项目
│   └── music-project/
│       ├── src/            # 源代码
│       ├── pom.xml         # Maven 配置
│       ├── Dockerfile      # Docker 配置
│       └── database.sql    # 数据库初始化
├── docker-compose.yml       # Docker Compose 配置
├── deploy.sh               # 快速部署脚本
├── env.template            # 环境变量模板
└── README.md              # 项目说明
```

---

## 🔧 本地开发

### 前端开发

```bash
cd front/sheep-music

# 安装依赖
npm install --legacy-peer-deps

# 启动开发服务器
npm run dev

# 访问 http://localhost:8001
```

### 后端开发

```bash
cd back/music-project

# 使用 Maven 运行
mvn spring-boot:run

# 或使用 IDE (IntelliJ IDEA / Eclipse)
```

---

## 🐳 Docker 命令

```bash
# 启动所有服务
docker compose up -d

# 查看日志
docker compose logs -f

# 停止服务
docker compose down

# 重启服务
docker compose restart

# 重新构建
docker compose up -d --build
```

---

## 📝 配置说明

### 环境变量

在 `.env` 文件中配置：

```env
# MySQL 配置
MYSQL_ROOT_PASSWORD=your_password
MYSQL_DATABASE=sheepmusic
MYSQL_USER=sheepmusic
MYSQL_PASSWORD=your_password

# JWT 密钥（生产环境必须修改！）
JWT_SECRET=your-secret-key

# 阿里云 OSS（可选）
ALIYUN_OSS_ACCESS_KEY_ID=your_key
ALIYUN_OSS_ACCESS_KEY_SECRET=your_secret
```

### 端口配置

| 服务 | 容器端口 | 主机端口 | 说明 |
|------|---------|---------|------|
| 前端 | 80 | 80 | Web 页面 |
| 后端 | 9000 | 8080 | REST API |
| MySQL | 3306 | 3306 | 数据库 |

---

## 🚀 部署到云服务器

支持部署到：
- 阿里云 ECS
- 腾讯云 CVM
- 华为云 ECS
- AWS EC2
- 其他支持 Docker 的 Linux 服务器

详见 [部署指南](DEPLOYMENT_GUIDE.md)

---

## 🐛 故障排查

### 前端空白页

- 确保 `dist` 目录已构建并提交到 Git
- 查看浏览器控制台是否有 JS 错误
- 检查 Nginx 日志：`docker compose logs frontend`

### 后端无法连接

- 检查 MySQL 是否启动：`docker compose ps mysql`
- 查看后端日志：`docker compose logs backend`
- 确认端口未被占用：`netstat -tlnp | grep 8080`

### 无法外部访问

- 检查防火墙：`sudo ufw status`
- 检查云服务器安全组规则
- 确认服务正在监听：`curl http://localhost`

更多问题请查看 [部署指南 - 常见问题](DEPLOYMENT_GUIDE.md#常见问题)

---

## 📄 许可证

MIT License

---

## 👥 贡献

欢迎提交 Issue 和 Pull Request！

---

## 📞 联系方式

- 项目地址: [GitHub](https://github.com/your-username/sheep-music)
- 问题反馈: [Issues](https://github.com/your-username/sheep-music/issues)

---

**享受你的音乐之旅！** 🎶


