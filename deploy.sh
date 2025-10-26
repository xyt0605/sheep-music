#!/bin/bash

# ==========================================
# Sheep Music - 快速部署脚本
# ==========================================
# 用途：在服务器上快速部署项目
# 使用：bash deploy.sh
# ==========================================

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 未安装，请先安装 $1"
        exit 1
    fi
}

# 打印横幅
echo "================================================"
echo "   🎵 Sheep Music - 自动部署脚本 🎵"
echo "================================================"
echo ""

# 1. 检查必要的命令
log_info "检查系统环境..."
check_command "docker"
check_command "docker-compose"
check_command "git"

log_info "✓ 环境检查通过"
echo ""

# 2. 检查关键文件
log_info "检查项目文件..."

if [ ! -f "docker-compose.yml" ]; then
    log_error "docker-compose.yml 不存在"
    exit 1
fi

if [ ! -f "front/sheep-music/Dockerfile" ]; then
    log_error "前端 Dockerfile 不存在"
    exit 1
fi

if [ ! -d "front/sheep-music/dist" ]; then
    log_warn "前端 dist 目录不存在"
    log_warn "请在本地运行 'cd front/sheep-music && npm run build'"
    read -p "是否继续？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

log_info "✓ 文件检查完成"
echo ""

# 3. 停止旧容器
log_info "停止旧容器..."
docker compose down 2>/dev/null || true
log_info "✓ 旧容器已停止"
echo ""

# 4. 构建并启动服务
log_info "构建并启动服务..."
log_info "这可能需要几分钟时间，请耐心等待..."
docker compose up -d --build

log_info "✓ 服务启动完成"
echo ""

# 5. 等待服务就绪
log_info "等待服务启动..."
sleep 10

# 6. 检查容器状态
log_info "检查容器状态..."
docker compose ps

echo ""

# 7. 测试服务
log_info "测试服务连通性..."

# 测试前端
if curl -s http://localhost > /dev/null; then
    log_info "✓ 前端服务正常"
else
    log_warn "✗ 前端服务可能未就绪"
fi

# 测试后端
if curl -s http://localhost:8080 > /dev/null; then
    log_info "✓ 后端服务正常"
else
    log_warn "✗ 后端服务可能未就绪（可能还在启动中）"
fi

echo ""

# 8. 显示访问信息
PUBLIC_IP=$(curl -s ifconfig.me 2>/dev/null || echo "无法获取公网IP")

echo "================================================"
echo "   🎉 部署完成！"
echo "================================================"
echo ""
echo "访问地址："
echo "  前端页面: http://${PUBLIC_IP}"
echo "  后端 API: http://${PUBLIC_IP}:8080"
echo ""
echo "常用命令："
echo "  查看日志: docker compose logs -f"
echo "  查看状态: docker compose ps"
echo "  停止服务: docker compose down"
echo "  重启服务: docker compose restart"
echo ""
echo "注意事项："
echo "  - 确保防火墙已开放 80 和 8080 端口"
echo "  - 确保云服务器安全组已配置"
echo "  - 后端可能需要 30-60 秒才能完全启动"
echo ""
echo "详细部署文档请查看: DEPLOYMENT_GUIDE.md"
echo "================================================"


