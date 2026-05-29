.PHONY: help clean compile build run stop restart test logs

help:
	@echo "AI Chat Server - 快速命令"
	@echo ""
	@echo "开发命令:"
	@echo "  make run          启动后端服务"
	@echo "  make stop         停止后端服务"
	@echo "  make restart      重启后端服务"
	@echo "  make logs         查看实时日志"
	@echo ""
	@echo "构建命令:"
	@echo "  make clean        清理构建文件"
	@echo "  make compile      编译源代码"
	@echo "  make build        构建 JAR 包"
	@echo "  make package      打包 (跳过测试)"
	@echo ""
	@echo "测试命令:"
	@echo "  make test         运行单元测试"
	@echo "  make health       检查服务健康状态"
	@echo ""
	@echo "维护命令:"
	@echo "  make deps         查看依赖树"
	@echo "  make clean-db     删除数据库文件"
	@echo ""

run:
	@echo "🚀 启动后端服务..."
	mvn spring-boot:run

stop:
	@echo "🛑 停止后端服务..."
	@pkill -f "ai-chat-server" && echo "✅ 已停止" || echo "ℹ️  没有运行中的服务"

restart:
	@echo "🔄 重启后端服务..."
	@make stop
	@sleep 2
	@make run

clean:
	@echo "🧹 清理构建文件..."
	mvn clean

compile:
	@echo "🔨 编译源代码..."
	mvn compile

build:
	@echo "📦 构建 JAR 包..."
	mvn clean package -DskipTests

package:
	@echo "📦 打包 (跳过测试)..."
	mvn clean package -DskipTests

test:
	@echo "🧪 运行单元测试..."
	mvn test

health:
	@echo "🏥 检查服务健康状态..."
	@curl -s http://172.25.40.47:9090/health && echo "✅ 服务正常" || echo "❌ 服务未响应"

logs:
	@echo "📝 查看实时日志..."
	@tail -f /tmp/ai-chat-server.log 2>/dev/null || echo "日志文件不存在，请先启动服务"

deps:
	@echo "📊 查看依赖树..."
	mvn dependency:tree

clean-db:
	@echo "🗑️  删除数据库文件..."
	@rm -f ./data/aichatdb.mv.db && echo "✅ 已删除" || echo "ℹ️  数据库文件不存在"

.DEFAULT_GOAL := help
