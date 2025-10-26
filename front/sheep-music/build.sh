#!/bin/bash

# ==========================================
# 前端构建脚本
# ==========================================
# 用途：在本地构建前端生产版本
# 使用：bash build.sh
# ==========================================

set -e

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

echo "================================================"
echo "   🎵 Sheep Music - 前端构建脚本 🎵"
echo "================================================"
echo ""

# 检查 Node.js
if ! command -v node &> /dev/null; then
    log_error "Node.js 未安装，请先安装 Node.js"
    exit 1
fi

log_info "Node.js 版本: $(node --version)"
log_info "npm 版本: $(npm --version)"
echo ""

# 检查 package.json
if [ ! -f "package.json" ]; then
    log_error "package.json 不存在，请在前端目录运行此脚本"
    exit 1
fi

# 清理旧的构建文件
if [ -d "dist" ]; then
    log_info "清理旧的 dist 目录..."
    rm -rf dist
fi

# 安装依赖
log_info "安装依赖..."
npm install --legacy-peer-deps

# 构建生产版本
log_info "构建生产版本..."
npm run build

# 检查构建结果
if [ -d "dist" ]; then
    log_info "✓ 构建成功！"
    echo ""
    log_info "dist 目录内容:"
    ls -lh dist/
    echo ""
    log_info "dist 目录大小:"
    du -sh dist/
    echo ""
    log_info "下一步："
    log_info "1. 检查 dist 目录内容是否正确"
    log_info "2. 提交到 Git: git add dist/ && git commit -m 'Build frontend'"
    log_info "3. 推送到远程: git push"
    log_info "4. 在服务器上拉取并部署"
else
    log_error "构建失败，dist 目录未生成"
    exit 1
fi

echo "================================================"


