# 🚀 快速部署指南

完整的 WSL → GitHub → 服务器部署流程。

---

## 📤 第一步：WSL 上传到 GitHub

### 在 Windows 上打开 WSL

```bash
# PowerShell 中输入
wsl
```

### 上传项目

```bash
# 1. 进入项目目录
cd /mnt/e/java-project/project1

# 2. 查看状态
git status

# 3. 添加所有更改
git add .

# 4. 提交
git commit -m "Prepare for deployment"

# 5. 推送到 GitHub
git push origin main
```

**详细说明** → [WSL_DEPLOY.md](WSL_DEPLOY.md)

---

## 🖥️ 第二步：服务器部署

### 连接服务器

**阿里云 Workbench**:
1. 登录阿里云控制台
2. ECS → 实例 → 远程连接 → Workbench

**或 SSH**:
```bash
ssh root@8.153.206.169
```

### 部署项目

```bash
# 1. 清理旧项目（如果存在）
cd /opt/sheep-music
docker compose down
cd /opt
rm -rf sheep-music

# 2. 安装 Docker（如果未安装）
curl -fsSL https://get.docker.com | bash

# 3. 克隆项目
cd /opt
git clone https://github.com/你的用户名/你的仓库.git sheep-music
cd sheep-music

# 4. 验证 dist 目录
ls -la front/sheep-music/dist/assets/

# 5. 部署
bash deploy.sh
# 或
docker compose up -d --build

# 6. 查看日志
docker compose logs -f
```

**详细说明** → [SERVER_DEPLOY.md](SERVER_DEPLOY.md)

---

## ✅ 验证部署

### 服务器测试

```bash
# 查看容器状态
docker compose ps

# 测试服务
curl http://localhost
curl http://localhost:8080
```

### 浏览器访问

- **前端**: http://8.153.206.169
- **后端**: http://8.153.206.169:8080

---

## 🐛 快速排错

| 问题 | 解决方法 |
|------|---------|
| 前端空白 | `docker exec sheep-music-frontend ls /usr/share/nginx/html/assets/` 检查文件 |
| 后端启动失败 | `docker compose logs backend` 查看日志 |
| 无法访问 | 检查阿里云安全组（80、8080端口） |
| dist 目录为空 | 本地重新 `git add dist/` 并 push |

---

## 🔄 常用命令

```bash
# 查看日志
docker compose logs -f backend

# 重启服务
docker compose restart

# 停止服务
docker compose down

# 更新项目
git pull && docker compose up -d --build

# 备份数据库
docker exec sheep-music-mysql mysqldump -u root -proot_password sheepmusic > backup.sql
```

---

## 📚 完整文档

- [WSL 上传详细步骤](WSL_DEPLOY.md)
- [服务器部署详细步骤](SERVER_DEPLOY.md)
- [项目说明](README.md)

---

**🎉 完成！享受你的音乐应用吧！**


