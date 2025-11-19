# 🚀 快速部署指令卡

## 📤 第一步：WSL 推送到 GitHub

### 方式一：使用脚本（推荐）

```bash
# 进入 WSL
wsl

# 导航到项目
cd /mnt/d/workspace/java-project/project1/sheep-music

# 给脚本执行权限（首次）
chmod +x push-to-github.sh

# 推送代码
bash push-to-github.sh "你的提交信息"
```

### 方式二：手动命令

```bash
# 进入 WSL
wsl

# 导航到项目
cd /mnt/d/workspace/java-project/project1/sheep-music

# 添加所有更改
git add .

# 提交更改
git commit -m "部署版本 - $(date '+%Y-%m-%d %H:%M:%S')"

# 推送到 GitHub（首次需要设置远程仓库）
git push

# 如果是首次推送，先添加远程仓库
# git remote add origin https://github.com/你的用户名/sheep-music.git
# git push -u origin main
```

---

## 🌐 第二步：阿里云服务器部署

### 首次部署

```bash
# 1. 克隆项目
git clone https://github.com/你的用户名/sheep-music.git
cd sheep-music

# 2. 配置环境变量
cp env.template .env
nano .env  # 编辑配置（Ctrl+X 保存）

# 3. 给脚本执行权限
chmod +x server-deploy.sh deploy.sh

# 4. 运行部署
bash server-deploy.sh
```

### 更新部署（推荐）

```bash
# 进入项目目录
cd ~/sheep-music

# 运行更新脚本
bash server-deploy.sh
```

### 手动更新部署

```bash
# 进入项目目录
cd ~/sheep-music

# 拉取最新代码
git pull

# 停止旧容器
docker compose down

# 重新构建并启动
docker compose up -d --build

# 查看日志
docker compose logs -f
```

---

## 🔧 常用运维命令

### 查看服务状态

```bash
docker compose ps
```

### 查看日志

```bash
# 所有服务
docker compose logs -f

# 特定服务
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

### 重启服务

```bash
# 重启所有
docker compose restart

# 重启特定服务
docker compose restart backend
```

### 停止服务

```bash
docker compose down
```

### 进入容器

```bash
# 进入后端容器
docker exec -it sheep-music-backend bash

# 进入数据库容器
docker exec -it sheep-music-mysql mysql -u root -p

# 进入前端容器
docker exec -it sheep-music-frontend sh
```

---

## 🛡️ 防火墙配置

### Ubuntu/Debian (UFW)

```bash
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 8080/tcp
sudo ufw enable
sudo ufw status
```

### CentOS/RHEL (firewalld)

```bash
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

### 阿里云安全组

1. 登录阿里云控制台
2. ECS 实例 → 安全组配置
3. 添加入方向规则：
   - 端口 80（HTTP）
   - 端口 443（HTTPS）
   - 端口 8080（后端 API）

---

## 💾 数据库管理

### 备份数据库

```bash
# 导出数据库
docker exec sheep-music-mysql mysqldump -u root -p sheepmusic > backup_$(date +%Y%m%d_%H%M%S).sql

# 输入密码后等待导出完成
```

### 恢复数据库

```bash
# 导入数据库
docker exec -i sheep-music-mysql mysql -u root -p sheepmusic < backup_20240101_120000.sql
```

### 进入数据库

```bash
docker exec -it sheep-music-mysql mysql -u root -p

# 然后执行 SQL 命令
SHOW DATABASES;
USE sheepmusic;
SHOW TABLES;
SELECT * FROM user LIMIT 10;
```

---

## 🧹 清理命令

### 清理 Docker 资源

```bash
# 清理未使用的镜像
docker image prune -a

# 清理未使用的容器
docker container prune

# 清理未使用的卷
docker volume prune

# 清理所有未使用的资源
docker system prune -a --volumes
```

### 清理日志

```bash
# 清理 Docker 日志
sudo sh -c "truncate -s 0 /var/lib/docker/containers/*/*-json.log"
```

---

## 🔍 故障排查

### 检查端口占用

```bash
sudo netstat -tulpn | grep :80
sudo netstat -tulpn | grep :8080
sudo netstat -tulpn | grep :3306
```

### 检查 Docker 状态

```bash
# Docker 服务状态
sudo systemctl status docker

# 重启 Docker
sudo systemctl restart docker
```

### 查看容器资源使用

```bash
docker stats
```

### 查看容器详细信息

```bash
docker inspect sheep-music-backend
docker inspect sheep-music-frontend
docker inspect sheep-music-mysql
```

---

## 📊 监控命令

### 实时查看日志

```bash
# 后端日志（实时）
docker compose logs -f --tail=100 backend

# 前端日志（实时）
docker compose logs -f --tail=100 frontend

# 数据库日志（实时）
docker compose logs -f --tail=100 mysql
```

### 查看系统资源

```bash
# CPU 和内存使用
htop

# 磁盘使用
df -h

# Docker 磁盘使用
docker system df
```

---

## 🎯 完整部署流程（复制粘贴）

### 在 Windows PowerShell 中

```powershell
# 进入 WSL
wsl

# 导航并推送
cd /mnt/d/workspace/java-project/project1/sheep-music
git add .
git commit -m "部署更新 - $(date '+%Y-%m-%d %H:%M:%S')"
git push
exit
```

### 在阿里云服务器中

```bash
# 更新部署
cd ~/sheep-music
git pull
docker compose down
docker compose up -d --build
docker compose logs -f
```

---

## ⚡ 一键命令

### WSL 一键推送

```bash
cd /mnt/d/workspace/java-project/project1/sheep-music && git add . && git commit -m "更新 $(date '+%Y%m%d_%H%M%S')" && git push
```

### 服务器一键更新

```bash
cd ~/sheep-music && git pull && docker compose down && docker compose up -d --build
```

---

## 📞 获取帮助

### 查看服务器 IP

```bash
curl ifconfig.me
```

### 测试服务

```bash
# 测试前端
curl http://localhost

# 测试后端
curl http://localhost:8080

# 测试数据库连接
docker exec sheep-music-mysql mysqladmin -u root -p ping
```

### 查看环境变量

```bash
cat .env
```

---

## ⚠️ 重要提示

1. **首次部署**必须配置 `.env` 文件
2. **修改密码**：MySQL 密码、JWT 密钥必须修改
3. **防火墙**：确保开放 80、443、8080 端口
4. **安全组**：阿里云控制台配置安全组规则
5. **备份数据**：定期备份数据库
6. **查看日志**：遇到问题先查看日志

---

## 🎉 部署成功后

访问地址：
- **前端**：`http://你的服务器IP`
- **后端 API**：`http://你的服务器IP:8080`

检查服务：
```bash
docker compose ps
docker compose logs -f
```

---

**祝部署顺利！** 🚀
