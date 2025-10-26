# 🖥️ 使用阿里云 Workbench 部署项目

## 📋 前提条件

- ✅ 项目已推送到 GitHub
- ✅ 服务器已开通（阿里云 ECS）
- ✅ 安全组已配置（80、8080 端口）

---

## 🔌 步骤 1: 连接服务器

### 使用阿里云 Workbench

1. 登录阿里云控制台
2. 进入 **云服务器 ECS** → **实例**
3. 找到你的实例（`iZuf6fh8iq3k9834th9pskZ`）
4. 点击 **远程连接** → **通过 Workbench 远程连接**
5. 选择 **立即登录**（如果已配置密码）

**或者使用命令行：**

```bash
# 在本地 WSL 或 PowerShell 中
ssh root@8.153.206.169
# 输入密码
```

---

## 🧹 步骤 2: 清理旧资源（如果需要）

```bash
# 进入旧项目目录
cd /opt/sheep-music

# 停止并删除容器
docker compose down

# 删除镜像（可选）
docker rmi sheep-music-backend sheep-music-frontend -f

# 删除旧项目目录
cd /opt
rm -rf sheep-music

# 清理 Docker 资源（可选）
docker system prune -af
```

---

## 📦 步骤 3: 安装必要软件

### 3.1 检查 Docker 是否已安装

```bash
docker --version
docker compose version
```

### 3.2 如果未安装，执行安装

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | bash

# 启动 Docker 服务
systemctl start docker
systemctl enable docker

# 验证安装
docker --version
```

### 3.3 安装 Git（如果未安装）

```bash
# 检查
git --version

# 安装
apt update
apt install git -y
```

---

## 📥 步骤 4: 克隆项目

```bash
# 创建项目目录
mkdir -p /opt
cd /opt

# 克隆你的 GitHub 项目
git clone https://github.com/你的用户名/你的仓库名.git sheep-music

# 示例：
# git clone https://github.com/username/sheep-music-project.git sheep-music

# 进入项目目录
cd sheep-music

# 查看文件
ls -la
```

---

## 🔍 步骤 5: 验证项目文件

```bash
# 检查关键文件
ls -la docker-compose.yml
ls -la front/sheep-music/Dockerfile
ls -la back/music-project/Dockerfile

# 重要：检查 dist 目录是否存在
ls -la front/sheep-music/dist/
ls -la front/sheep-music/dist/assets/

# 应该看到：
# - index.html
# - favicon.ico
# - assets/ 目录（包含 JS 和 CSS 文件）
```

### ⚠️ 如果 dist 目录不存在或为空

说明 dist 没有提交到 Git，需要：

```bash
# 回到本地 WSL，重新添加 dist 并提交
cd /mnt/e/java-project/project1
git add front/sheep-music/dist/
git commit -m "Add dist directory"
git push origin main

# 然后在服务器上拉取
cd /opt/sheep-music
git pull
```

---

## ⚙️ 步骤 6: 配置环境变量（可选）

```bash
# 如果需要自定义配置
cp env.template .env

# 编辑环境变量
nano .env

# 修改以下内容：
# - MYSQL_ROOT_PASSWORD
# - MYSQL_PASSWORD
# - JWT_SECRET（生产环境必须修改！）
# - 阿里云 OSS 配置（如果使用）

# 保存并退出：Ctrl + X，然后 Y，回车
```

---

## 🚀 步骤 7: 部署项目

### 方法 1: 使用部署脚本（推荐）

```bash
# 运行部署脚本
bash deploy.sh
```

脚本会自动：
- 检查环境
- 停止旧容器
- 构建并启动新容器
- 显示访问地址

### 方法 2: 手动部署

```bash
# 构建并启动所有服务
docker compose up -d --build

# 查看启动日志
docker compose logs -f

# 按 Ctrl+C 退出日志查看（不会停止容器）
```

---

## 📊 步骤 8: 监控启动进度

```bash
# 查看容器状态
docker compose ps

# 应该看到：
# mysql     - healthy (约 10-20 秒)
# backend   - running (约 30-60 秒)
# frontend  - running (约 5-10 秒)

# 实时查看日志
docker compose logs -f backend

# 查看所有容器日志
docker compose logs --tail=50
```

**等待后端完全启动**（看到 "Started SheepMusicApplication" 日志）

---

## ✅ 步骤 9: 验证部署

### 9.1 服务器本地测试

```bash
# 测试前端
curl http://localhost

# 测试后端
curl http://localhost:8080

# 检查前端文件
docker exec sheep-music-frontend ls -la /usr/share/nginx/html/

# 应该看到：
# - index.html
# - favicon.ico
# - assets/ 目录
```

### 9.2 浏览器访问测试

在你的本地浏览器中访问：

- **前端页面**: `http://8.153.206.169`
- **后端 API**: `http://8.153.206.169:8080`

### 9.3 功能测试

- [ ] 前端页面正常显示（不是空白）
- [ ] 样式正常加载
- [ ] 可以注册新用户
- [ ] 可以登录
- [ ] 可以浏览音乐列表

---

## 🐛 常见问题排查

### 问题 1: 前端页面空白

```bash
# 检查前端容器的文件
docker exec sheep-music-frontend ls -la /usr/share/nginx/html/assets/

# 如果没有 assets 目录，说明 dist 目录有问题
# 解决：在本地重新构建并提交 dist
```

### 问题 2: 后端启动失败

```bash
# 查看详细日志
docker compose logs backend

# 常见原因：
# - MySQL 未就绪（等待更长时间）
# - 数据库连接失败（检查 docker-compose.yml 配置）
# - Maven 构建失败（查看日志中的错误信息）

# 重启后端
docker compose restart backend
```

### 问题 3: 无法外部访问

```bash
# 1. 检查服务是否运行
docker compose ps

# 2. 检查端口监听
netstat -tlnp | grep 80
netstat -tlnp | grep 8080

# 3. 检查防火墙
ufw status
ufw allow 80
ufw allow 8080

# 4. 确认阿里云安全组已开放 80 和 8080 端口（在控制台检查）
```

### 问题 4: Docker 磁盘空间不足

```bash
# 清理未使用的资源
docker system prune -af --volumes

# 查看磁盘使用
df -h
docker system df
```

---

## 🔄 日常维护命令

### 更新项目

```bash
cd /opt/sheep-music

# 拉取最新代码
git pull

# 重新构建并启动
docker compose up -d --build
```

### 查看日志

```bash
# 所有服务
docker compose logs -f

# 特定服务
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql

# 最近 100 行
docker compose logs --tail=100
```

### 重启服务

```bash
# 重启所有服务
docker compose restart

# 重启特定服务
docker compose restart backend
docker compose restart frontend
```

### 停止服务

```bash
# 停止所有服务
docker compose stop

# 停止并删除容器
docker compose down

# 停止并删除容器和数据卷（会删除数据库数据！）
docker compose down -v
```

### 备份数据库

```bash
# 导出数据库
docker exec sheep-music-mysql mysqldump -u root -proot_password sheepmusic > backup_$(date +%Y%m%d).sql

# 恢复数据库
docker exec -i sheep-music-mysql mysql -u root -proot_password sheepmusic < backup_20251026.sql
```

---

## 📝 部署完成检查清单

- [ ] 所有容器状态正常（docker compose ps）
- [ ] 前端页面可以访问且显示正常
- [ ] 后端 API 可以访问
- [ ] 用户注册功能正常
- [ ] 用户登录功能正常
- [ ] 音乐播放功能正常
- [ ] 文件上传功能正常（如果配置了 OSS）

---

## 🎉 部署成功！

你的项目已成功部署！

**访问地址：**
- 前端: http://8.153.206.169
- 后端: http://8.153.206.169:8080

**下一步建议：**
1. 配置域名（可选）
2. 配置 HTTPS（使用 Let's Encrypt）
3. 配置 CDN 加速
4. 设置定期数据库备份

---

## 💡 提示

### Workbench 使用技巧

1. **复制粘贴**: 
   - 从本地复制后，在 Workbench 中右键选择粘贴
   - 或使用快捷键 Ctrl+Shift+V

2. **多窗口**: 
   - 可以开多个 Workbench 窗口同时查看日志

3. **断开重连**: 
   - Workbench 断开后容器继续运行
   - 重新连接即可继续操作

4. **查看实时日志**: 
   ```bash
   # 使用 -f 参数实时查看日志
   docker compose logs -f backend
   ```

---

**遇到问题？** 请查看上面的"常见问题排查"章节。


