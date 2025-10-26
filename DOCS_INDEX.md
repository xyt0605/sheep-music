# 📚 文档索引

快速找到你需要的文档。

---

## 🚀 开始部署

### 第一次部署？从这里开始 👇

1. **[快速开始指南](QUICK_START.md)** ⭐ 推荐
   - 最简洁的部署流程
   - WSL → GitHub → 服务器
   - 适合快速上手

2. **[部署工作流程](DEPLOYMENT_WORKFLOW.md)**
   - 可视化流程图
   - 了解完整部署架构
   - 时间估算和检查点

---

## 📖 详细文档

### WSL 和 GitHub

- **[WSL 部署指南](WSL_DEPLOY.md)**
  - 如何使用 WSL 上传项目
  - Git 配置和认证
  - GitHub Token 设置
  - 常见问题解决

### 服务器部署

- **[服务器部署指南](SERVER_DEPLOY.md)**
  - 阿里云 Workbench 使用
  - Docker 安装和配置
  - 完整部署步骤
  - 故障排查指南
  - 日常维护命令

---

## 🛠️ 开发相关

### 项目说明

- **[README.md](README.md)**
  - 项目概述
  - 技术栈
  - 本地开发指南
  - 功能特性

### 配置文件

- **[环境变量模板](env.template)**
  - 数据库配置
  - JWT 密钥
  - 阿里云 OSS 配置

---

## 🔧 工具和脚本

### 部署脚本

| 脚本 | 用途 | 平台 |
|------|------|------|
| `deploy.sh` | 服务器快速部署 | Linux |
| `front/sheep-music/build.sh` | 前端构建 | Linux/Mac |
| `front/sheep-music/build.bat` | 前端构建 | Windows |

### Docker 配置

| 文件 | 说明 |
|------|------|
| `docker-compose.yml` | 服务编排配置 |
| `front/sheep-music/Dockerfile` | 前端镜像（使用预构建 dist） |
| `front/sheep-music/Dockerfile.build` | 前端镜像（容器内构建） |
| `back/music-project/Dockerfile` | 后端镜像 |

---

## 🐛 问题排查

### 常见问题速查

| 问题 | 查看文档 |
|------|---------|
| 前端页面空白 | [SERVER_DEPLOY.md - 问题1](SERVER_DEPLOY.md#问题-1-前端页面空白) |
| 后端启动失败 | [SERVER_DEPLOY.md - 问题2](SERVER_DEPLOY.md#问题-2-后端启动失败) |
| 无法外部访问 | [SERVER_DEPLOY.md - 问题3](SERVER_DEPLOY.md#问题-3-无法外部访问) |
| Git 认证失败 | [WSL_DEPLOY.md - 认证问题](WSL_DEPLOY.md#⚠️-如果遇到认证问题) |
| Docker 空间不足 | [SERVER_DEPLOY.md - 问题4](SERVER_DEPLOY.md#问题-4-docker-磁盘空间不足) |

---

## 📋 检查清单

### 部署前检查

- [ ] 前端已构建（`npm run build`）
- [ ] dist 目录包含完整文件
- [ ] 代码已提交到 GitHub
- [ ] 服务器已安装 Docker
- [ ] 安全组已配置（80、8080 端口）

### 部署后验证

- [ ] 所有容器运行正常
- [ ] 前端页面可访问
- [ ] 后端 API 可访问
- [ ] 用户注册登录功能正常

---

## 🎯 根据情况选择文档

### 我是第一次部署
→ 从 **[快速开始指南](QUICK_START.md)** 开始

### 我想了解完整流程
→ 查看 **[部署工作流程](DEPLOYMENT_WORKFLOW.md)**

### 我在 WSL 上传时遇到问题
→ 参考 **[WSL 部署指南](WSL_DEPLOY.md)**

### 我在服务器部署时遇到问题
→ 参考 **[服务器部署指南](SERVER_DEPLOY.md)**

### 我想了解项目架构
→ 查看 **[README.md](README.md)**

### 我需要维护和更新项目
→ 查看 **[SERVER_DEPLOY.md - 日常维护](SERVER_DEPLOY.md#🔄-日常维护命令)**

---

## 📞 获取帮助

1. **查看对应文档** - 根据上面的索引找到相关文档
2. **查看日志** - `docker compose logs -f`
3. **检查容器状态** - `docker compose ps`
4. **搜索错误信息** - 复制错误日志搜索解决方案
5. **提交 Issue** - 在 GitHub 上提交问题

---

## 🔄 文档更新日志

- **2025-10-26**: 创建完整文档体系
  - 快速开始指南
  - WSL 部署指南
  - 服务器部署指南
  - 工作流程说明
  - 文档索引

---

## 📌 重要链接

- **项目仓库**: https://github.com/your-username/your-repo
- **阿里云控制台**: https://ecs.console.aliyun.com
- **Docker 官方文档**: https://docs.docker.com
- **Vite 官方文档**: https://vitejs.dev

---

**祝部署顺利！** 🚀

从 **[QUICK_START.md](QUICK_START.md)** 开始你的部署之旅吧！


