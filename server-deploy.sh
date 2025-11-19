#!/bin/bash

# ==========================================
# Sheep Music - 服务器部署脚本
# ==========================================
# 用途：在阿里云服务器上快速更新部署
# 使用：bash server-deploy.sh
# ==========================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

echo "================================================"
echo "   🚀 Sheep Music - 服务器部署"
echo "================================================"
echo ""

# 1. 拉取最新代码
log_step "1/6 拉取最新代码..."
git pull origin main || git pull origin master
log_info "✓ 代码已更新"
echo ""

# 2. 检查环境变量
log_step "2/6 检查环境变量..."
if [ ! -f ".env" ]; then
    log_warn ".env 文件不存在"
    if [ -f "env.template" ]; then
        log_info "从模板创建 .env 文件..."
        cp env.template .env
        log_warn "请编辑 .env 文件配置环境变量"
        log_warn "运行: nano .env"
        exit 1
    else
        log_error "env.template 文件也不存在"
        exit 1
    fi
else
    log_info "✓ 环境变量文件存在"
fi
echo ""

# 3. 检查前端构建
log_step "3/6 检查前端构建..."
if [ ! -d "front/sheep-music/dist" ]; then
    log_warn "前端 dist 目录不存在"
    read -p "是否需要构建前端？(y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        log_info "开始构建前端..."
        cd front/sheep-music
        npm install
        npm run build
        cd ../..
        log_info "✓ 前端构建完成"
    else
        log_warn "跳过前端构建（确保 Docker 镜像中有构建步骤）"
    fi
else
    log_info "✓ 前端已构建"
fi
echo ""

# 4. 停止旧容器
log_step "4/6 停止旧容器..."
docker compose down
log_info "✓ 旧容器已停止"
echo ""

# 5. 构建并启动新容器
log_step "5/6 构建并启动新容器..."
log_info "这可能需要几分钟，请耐心等待..."
docker compose up -d --build
log_info "✓ 容器启动完成"
echo ""

# 6. 等待服务就绪
log_step "6/6 等待服务启动..."
sleep 15

# 检查容器状态
log_info "容器状态："
docker compose ps
echo ""

# 测试服务
log_info "测试服务连通性..."

# 测试前端
if curl -s http://localhost > /dev/null 2>&1; then
    log_info "✓ 前端服务正常"
else
    log_warn "✗ 前端服务可能未就绪"
fi

# 测试后端
if curl -s http://localhost:8080 > /dev/null 2>&1; then
    log_info "✓ 后端服务正常"
else
    log_warn "✗ 后端服务可能未就绪（可能还在启动中）"
fi

echo ""

# 获取公网 IP
PUBLIC_IP=$(curl -s ifconfig.me 2>/dev/null || echo "无法获取")

echo "================================================"
echo "   🎉 部署完成！"
echo "================================================"
echo ""
echo "📍 访问地址："
echo "   前端: http://${PUBLIC_IP}"
echo "   后端: http://${PUBLIC_IP}:8080"
echo ""
echo "📋 常用命令："
echo "   查看日志: docker compose logs -f"
echo "   查看状态: docker compose ps"
echo "   重启服务: docker compose restart"
echo "   停止服务: docker compose down"
echo ""
echo "💡 提示："
echo "   - 后端可能需要 30-60 秒才能完全启动"
echo "   - 如果服务异常，请查看日志: docker compose logs -f"
echo ""
echo "================================================"
