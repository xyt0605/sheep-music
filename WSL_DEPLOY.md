# 📤 使用 WSL 上传项目到 GitHub

## 步骤 1: 打开 WSL

在 Windows 上打开 WSL（Ubuntu）：

```bash
# 在 Windows 搜索栏输入 wsl 或 ubuntu
# 或在 PowerShell 中输入：
wsl
```

## 步骤 2: 进入项目目录

```bash
# WSL 中访问 Windows 文件系统
cd /mnt/e/java-project/project1
```

## 步骤 3: 检查 Git 状态

```bash
# 查看修改的文件
git status

# 应该看到新增和修改的文件
```

## 步骤 4: 配置 Git（如果是第一次使用）

```bash
# 配置用户名和邮箱
git config --global user.name "你的名字"
git config --global user.email "你的邮箱@example.com"

# 查看配置
git config --list
```

## 步骤 5: 添加所有更改

```bash
# 添加所有更改的文件
git add .

# 查看将要提交的文件
git status
```

## 步骤 6: 提交更改

```bash
# 提交更改
git commit -m "Prepare for deployment: optimize configs, build frontend, add docs"

# 或更详细的提交信息
git commit -m "Deploy preparation
- Build frontend dist directory
- Add vite config and optimize Dockerfiles
- Create deployment scripts and documentation
- Update gitignore and dockerignore"
```

## 步骤 7: 推送到 GitHub

```bash
# 推送到远程仓库
git push origin main

# 如果是第一次推送，可能需要设置远程仓库
# git remote add origin https://github.com/your-username/your-repo.git
# git push -u origin main
```

### ⚠️ 如果遇到认证问题

GitHub 已不支持密码认证，需要使用 Personal Access Token：

```bash
# 方法 1: 使用 Personal Access Token
# 1. 访问 https://github.com/settings/tokens
# 2. 生成新的 token（Generate new token - classic）
# 3. 勾选 repo 权限
# 4. 复制 token（只显示一次！）
# 5. 推送时输入 token 作为密码

# 方法 2: 配置 Git 保存凭据
git config --global credential.helper store
# 下次 push 时输入 token，之后会自动保存
```

## ✅ 验证上传成功

```bash
# 查看提交历史
git log --oneline -5

# 查看远程分支状态
git status
```

然后访问你的 GitHub 仓库页面，确认文件已上传。

---

**完成后，进入下一步：服务器部署** 👉 [SERVER_DEPLOY.md](SERVER_DEPLOY.md)


