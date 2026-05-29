#!/bin/bash

# AI Chat Server 快速重启脚本
# 用法: ./restart.sh

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                  AI Chat Server 重启脚本                      ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# 1. 停止现有服务
echo "🛑 停止现有服务..."
if pkill -f "ai-chat-server" 2>/dev/null; then
    echo "   ✅ 已停止现有进程"
    sleep 2
else
    echo "   ℹ️  没有运行中的服务"
fi

# 2. 清理构建
echo ""
echo "🧹 清理构建文件..."
mvn clean -q

# 3. 编译
echo ""
echo "🔨 编译源代码..."
mvn compile -q

# 4. 启动服务
echo ""
echo "🚀 启动后端服务..."
echo "   项目目录: $PROJECT_DIR"
echo "   端口: 9090"
echo "   数据库: H2 (./data/aichatdb.mv.db)"
echo ""

# 后台启动服务
mvn spring-boot:run > /tmp/ai-chat-server.log 2>&1 &
SERVER_PID=$!
echo "   进程 ID: $SERVER_PID"

# 5. 等待启动
echo ""
echo "⏳ 等待服务启动 (最多 30 秒)..."
COUNTER=0
MAX_ATTEMPTS=30

while [ $COUNTER -lt $MAX_ATTEMPTS ]; do
    if curl -s http://172.25.40.47:9090/health > /dev/null 2>&1; then
        echo ""
        echo "╔════════════════════════════════════════════════════════════════╗"
        echo "║                                                                ║"
        echo "║              ✅ 后端服务已成功启动！                          ║"
        echo "║                                                                ║"
        echo "║  服务地址: http://172.25.40.47:9090                           ║"
        echo "║  本地地址: http://localhost:9090                              ║"
        echo "║  H2 控制台: http://localhost:9090/h2-console                  ║"
        echo "║                                                                ║"
        echo "╚════════════════════════════════════════════════════════════════╝"
        echo ""
        echo "📝 日志文件: /tmp/ai-chat-server.log"
        echo "🛑 停止服务: pkill -f 'ai-chat-server'"
        echo ""
        exit 0
    fi
    
    COUNTER=$((COUNTER + 1))
    echo -n "."
    sleep 1
done

echo ""
echo "❌ 服务启动超时"
echo ""
echo "📝 查看日志:"
echo "   tail -f /tmp/ai-chat-server.log"
echo ""
exit 1
