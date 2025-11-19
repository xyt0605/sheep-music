#!/bin/bash

# ==========================================
# Sheep Music - GitHub 推送脚本
# ==========================================
# 用途：快速推送代码到 GitHub
# 使用：bash push-to-github.sh "提交信息"
# ==========================================

set -e

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# 获取提交信息
COMMIT_MSG="${1:-更新代码 - $(date '+%Y-%m-%d %H:%M:%S')}"

echo "================================================"
echo "   📤 推送代码到 GitHub"
echo "================================================"
echo ""

# 询问是否需要构建前端
read -p "是否需要重新构建前端？(y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    log_info "开始构建前端..."
    
    if [ ! -d "front/sheep-music" ]; then
        log_error "前端目录不存在"
        exit 1
    fi
    
    cd front/sheep-music
    
    # 检查 package.json
    if [ ! -f "package.json" ]; then
        log_error "package.json 不存在"
        exit 1
    fi
    
    # 检查 node_modules
    if [ ! -d "node_modules" ]; then
        log_info "安装依赖..."
        npm install --legacy-peer-deps
    fi
    
    # 构建
    log_info "构建生产版本..."
    npm run build
    
    if [ $? -eq 0 ]; then
        log_info "✓ 前端构建成功"
    else
        log_error "前端构建失败"
        exit 1
    fi
    
    cd ../..
    echo ""
fi

# 检查是否在 Git 仓库中
if [ ! -d ".git" ]; then
    log_warn "当前目录不是 Git 仓库"
    read -p "是否初始化 Git 仓库？(y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git init
        log_info "Git 仓库已初始化"
    else
        exit 1
    fi
fi

# 显示当前状态
log_info "当前 Git 状态："
git status --short

echo ""

# 添加所有更改
log_info "添加所有更改到暂存区..."
git add .

# 提交更改
log_info "提交更改..."
git commit -m "$COMMIT_MSG" || log_warn "没有更改需要提交"

# 检查远程仓库
if ! git remote get-url origin &> /dev/null; then
    log_warn "未配置远程仓库"
    echo ""
    echo "请输入 GitHub 仓库地址（例如：https://github.com/username/sheep-music.git）："
    read REPO_URL
    git remote add origin "$REPO_URL"
    log_info "远程仓库已添加"
fi

# 推送到 GitHub
log_info "推送到 GitHub..."
BRANCH=$(git branch --show-current)
git push -u origin "$BRANCH"

echo ""
echo "================================================"
echo "   ✅ 推送成功！"
echo "================================================"
echo ""
echo "提交信息: $COMMIT_MSG"
echo "分支: $BRANCH"
echo ""
