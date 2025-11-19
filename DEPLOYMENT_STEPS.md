# Sheep Music 项目部署指南

## 📋 部署流程概览

1. **本地 WSL 环境**：推送代码到 GitHub
2. **阿里云服务器**：通过 Docker 拉取并部署

---

## 第一部分：在 WSL 中推送代码到 GitHub

### 1. 进入 WSL 环境

在 Windows PowerShell 中执行：

```bash
wsl
```

### 2. 导航到项目目录

```bash
cd /mnt/d/workspace/java-project/project1/sheep-music
```

### 3. 构建前端（如果修改了前端代码）

```bash
# 进入前端目录
cd front/sheep-music

# 安装依赖（首次或依赖更新时）
npm install --legacy-peer-deps

# 构建生产版本
npm run build

# 返回项目根目录
cd ../..
```

### 4. 检查 Git 状态

```bash
git status
```

### 5. 添加所有更改到暂存区

```bash
git add .
```

### 6. 提交更改

```bash
git commit -m "部署版本 - $(date '+%Y-%m-%d %H:%M:%S')"
```

### 7. 推送到 GitHub

如果是首次推送到新仓库：

```bash
# 添加远程仓库（替换为你的 GitHub 仓库地址）
git remote add origin https://github.com/你的用户名/sheep-music.git

# 推送到主分支
git push -u origin main
```

如果已经配置过远程仓库：

```bash
git push
```

### 8. 验证推送成功

访问你的 GitHub 仓库页面，确认代码已更新。

---

## 第二部分：在阿里云服务器上部署

### 1. 连接到阿里云服务器

通过阿里云 Workbench 或 SSH 连接到服务器。

### 2. 安装必要的工具（如果未安装）

```bash
# 更新包管理器
sudo apt update

# 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 安装 Docker Compose
sudo apt install docker-compose-plugin -y

# 验证安装
docker --version
docker compose version

# 将当前用户添加到 docker 组（避免每次使用 sudo）
sudo usermod -aG docker $USER

# 重新登录以使组权限生效
exit
# 重新连接到服务器
```

### 3. 克隆项目（首次部署）

```bash
# 导航到部署目录
cd ~

# 克隆项目（替换为你的 GitHub 仓库地址）
git clone https://github.com/你的用户名/sheep-music.git

# 进入项目目录
cd sheep-music
```

### 4. 更新项目（后续部署）

如果项目已存在，只需拉取最新代码：

```bash
# 进入项目目录
cd ~/sheep-music

# 拉取最新代码
git pull origin main
```

### 5. 配置环境变量

```bash
# 复制环境变量模板
cp env.template .env

# 编辑环境变量文件
nano .env
```

**重要配置项**（按 `Ctrl+X` 然后 `Y` 保存）：

```env
# MySQL 配置
MYSQL_ROOT_PASSWORD=你的强密码
MYSQL_DATABASE=sheepmusic
MYSQL_USER=sheepmusic
MYSQL_PASSWORD=你的MySQL密码

# JWT 密钥（必须修改！）
JWT_SECRET=至少32位的随机字符串

# 阿里云 OSS（如果使用）
ALIYUN_OSS_ACCESS_KEY_ID=你的AccessKeyId
ALIYUN_OSS_ACCESS_KEY_SECRET=你的AccessKeySecret
ALIYUN_OSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com
ALIYUN_OSS_BUCKET_NAME=你的Bucket名称
ALIYUN_OSS_URL_PREFIX=https://你的Bucket.oss-cn-hangzhou.aliyuncs.com
```

### 6. 构建前端（如果需要）

如果 GitHub 上没有 `dist` 目录，需要在服务器上构建：

```bash
# 安装 Node.js（如果未安装）
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# 进入前端目录
cd front/sheep-music

# 安装依赖
npm install

# 构建生产版本
npm run build

# 返回项目根目录
cd ../..
```

### 7. 运行部署脚本

```bash
# 给脚本添加执行权限
chmod +x deploy.sh

# 运行部署脚本
bash deploy.sh
```

或者手动执行 Docker Compose：

```bash
# 停止旧容器
docker compose down

# 构建并启动服务
docker compose up -d --build

# 查看容器状态
docker compose ps

# 查看日志
docker compose logs -f
```

### 8. 配置防火墙和安全组

**在服务器上开放端口：**

```bash
# 如果使用 UFW
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 8080/tcp
sudo ufw enable
sudo ufw status
```

**在阿里云控制台配置安全组：**

1. 登录阿里云控制台
2. 进入 ECS 实例管理
3. 找到你的实例，点击"安全组配置"
4. 添加入方向规则：
   - 端口 80（HTTP）
   - 端口 443（HTTPS）
   - 端口 8080（后端 API）

### 9. 验证部署

```bash
# 测试前端
curl http://localhost

# 测试后端
curl http://localhost:8080

# 获取公网 IP
curl ifconfig.me
```

访问：
- 前端：`http://你的公网IP`
- 后端 API：`http://你的公网IP:8080`

---

## 常用运维命令

### 查看服务状态

```bash
docker compose ps
```

### 查看日志

```bash
# 查看所有服务日志
docker compose logs -f

# 查看特定服务日志
docker compose logs -f frontend
docker compose logs -f backend
docker compose logs -f mysql
```

### 重启服务

```bash
# 重启所有服务
docker compose restart

# 重启特定服务
docker compose restart backend
```

### 停止服务

```bash
docker compose down
```

### 更新部署

```bash
# 拉取最新代码
git pull origin main

# 重新构建并启动
docker compose up -d --build
```

### 备份数据库

```bash
# 导出数据库
docker exec sheep-music-mysql mysqldump -u root -p sheepmusic > backup_$(date +%Y%m%d_%H%M%S).sql
```

### 清理 Docker 资源

```bash
# 清理未使用的镜像
docker image prune -a

# 清理未使用的容器
docker container prune

# 清理未使用的卷
docker volume prune
```

---

## 🔧 故障排查

### 1. 容器启动失败

```bash
# 查看详细日志
docker compose logs backend

# 检查容器状态
docker compose ps
```

### 2. 数据库连接失败

```bash
# 进入 MySQL 容器
docker exec -it sheep-music-mysql mysql -u root -p

# 检查数据库
SHOW DATABASES;
USE sheepmusic;
SHOW TABLES;
```

### 3. 前端无法访问后端

检查 `front/sheep-music/.env.production` 中的 API 地址是否正确：

```env
VITE_API_BASE_URL=http://你的公网IP:8080
```

### 4. 端口被占用

```bash
# 查看端口占用
sudo netstat -tulpn | grep :80
sudo netstat -tulpn | grep :8080

# 停止占用端口的进程
sudo kill -9 进程ID
```

---

## 📝 快速命令参考

### WSL 推送代码

```bash
cd /mnt/d/workspace/java-project/project1/sheep-music
git add .
git commit -m "更新内容描述"
git push
```

### 服务器更新部署

```bash
cd ~/sheep-music
git pull origin main
docker compose down
docker compose up -d --build
```

---

## 🎯 完整部署流程（一键复制）

### WSL 环境

```bash
wsl
cd /mnt/d/workspace/java-project/project1/sheep-music
git add .
git commit -m "部署版本 - $(date '+%Y-%m-%d %H:%M:%S')"
git push
```

### 阿里云服务器（首次部署）

```bash
# 克隆项目
git clone https://github.com/你的用户名/sheep-music.git
cd sheep-music

# 配置环境变量
cp env.template .env
nano .env  # 编辑配置

# 部署
chmod +x deploy.sh
bash deploy.sh
```

### 阿里云服务器（更新部署）

```bash
cd ~/sheep-music
git pull origin main
docker compose down
docker compose up -d --build
docker compose logs -f
```

---

## ⚠️ 注意事项

1. **首次部署前**，确保修改 `.env` 文件中的所有密码和密钥
2. **数据库初始化**会在首次启动时自动执行
3. **后端服务**可能需要 30-60 秒才能完全启动
4. **确保防火墙和安全组**已正确配置
5. **定期备份数据库**，避免数据丢失
6. **生产环境建议**配置 HTTPS 证书

---

## 📞 需要帮助？

如果遇到问题，请检查：
1. Docker 和 Docker Compose 是否正确安装
2. 环境变量配置是否正确
3. 防火墙和安全组是否开放端口
4. 查看容器日志获取详细错误信息

祝部署顺利！🎉
