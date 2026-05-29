# 🔧 后端服务管理指南

**项目**: AI Chat Server  
**框架**: Spring Boot 3.2.5  
**Java 版本**: 21+  
**数据库**: H2 (开发环境)  
**端口**: 9090

---

## 📋 快速检查

### 检查服务状态

```bash
# 检查 9090 端口是否被占用
lsof -i :9090

# 测试服务是否响应
curl -s http://172.25.40.47:9090/health || echo "服务未响应"
```

### 当前状态

- ✅ Java 进程运行中 (PID: 21954)
- ⚠️ 无法连接到服务 (可能是启动中或配置问题)

---

## 🚀 启动后端服务

### 方式 1: Maven 直接运行 (推荐开发)

```bash
cd /Users/ryan/Documents/catDesk/ai-chat-server

# 清理并编译
mvn clean compile

# 启动服务
mvn spring-boot:run
```

**输出示例**:
```
[INFO] BUILD SUCCESS
[INFO] ai-chat-server started on port 9090
```

### 方式 2: Maven 构建 JAR 后运行

```bash
cd /Users/ryan/Documents/catDesk/ai-chat-server

# 构建 JAR
mvn clean package -DskipTests

# 运行 JAR
java -jar target/ai-chat-server-1.0.0.jar
```

### 方式 3: IDE 运行

在 IDE 中打开 `AiChatServerApplication.java` 并运行 `main` 方法

---

## 🛑 停止后端服务

### 方式 1: 使用 Ctrl+C

如果在终端中运行，直接按 `Ctrl+C` 停止

### 方式 2: 杀死进程

```bash
# 查找 Java 进程
lsof -i :9090

# 杀死进程 (替换 PID)
kill -9 21954
```

### 方式 3: 使用 pkill

```bash
# 杀死所有 ai-chat-server 进程
pkill -f "ai-chat-server"
```

---

## 🔄 重启后端服务

### 完整重启流程

```bash
# 1. 停止服务
pkill -f "ai-chat-server" || echo "服务未运行"

# 2. 等待 2 秒
sleep 2

# 3. 启动服务
cd /Users/ryan/Documents/catDesk/ai-chat-server
mvn spring-boot:run &

# 4. 等待启动完成
sleep 5

# 5. 验证服务
curl -s http://172.25.40.47:9090/health && echo "✅ 服务已启动" || echo "❌ 服务启动失败"
```

### 快速重启脚本

创建 `restart.sh`:

```bash
#!/bin/bash
echo "🛑 停止后端服务..."
pkill -f "ai-chat-server" || echo "服务未运行"
sleep 2

echo "🚀 启动后端服务..."
cd /Users/ryan/Documents/catDesk/ai-chat-server
mvn spring-boot:run &

echo "⏳ 等待服务启动..."
sleep 5

echo "✅ 验证服务..."
curl -s http://172.25.40.47:9090/health && echo "✅ 服务已启动" || echo "❌ 服务启动失败"
```

使用:
```bash
chmod +x restart.sh
./restart.sh
```

---

## 📊 服务配置

### 端口配置

**文件**: `src/main/resources/application.yml`

```yaml
server:
  port: 9090
  servlet:
    context-path: /
```

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/aichatdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL
    driver-class-name: org.h2.Driver
    username: sa
    password:
```

**数据库文件位置**: `./data/aichatdb.mv.db`

### AI 上游配置

```yaml
ai:
  base-url: ${AI_BASE_URL:https://aigc.sankuai.com/v1/openai/native}
  app-id: ${AI_APP_ID:}
  default-model: ${AI_DEFAULT_MODEL:LongCat-8B-128K-Chat}
  timeout: 60000
```

### JWT 配置

```yaml
jwt:
  secret: ${JWT_SECRET:ai-chat-super-secret-key-change-in-production-must-be-at-least-256-bits}
  access-token-expiration: 3600000       # 1 小时
  refresh-token-expiration: 2592000000   # 30 天
```

---

## 🔍 常见问题

### Q: 服务启动失败，提示 "Address already in use"

**原因**: 9090 端口已被占用

**解决方案**:
```bash
# 查找占用端口的进程
lsof -i :9090

# 杀死进程
kill -9 <PID>

# 或改用其他端口
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9091"
```

### Q: 服务启动成功但无法连接

**原因**: 可能是防火墙或网络配置问题

**解决方案**:
```bash
# 检查服务是否真的在运行
lsof -i :9090

# 测试本地连接
curl -s http://localhost:9090/health

# 测试网络连接
curl -s http://172.25.40.47:9090/health
```

### Q: 数据库连接失败

**原因**: H2 数据库文件损坏或权限问题

**解决方案**:
```bash
# 删除数据库文件（会丢失数据）
rm -f ./data/aichatdb.mv.db

# 重启服务，会自动创建新数据库
mvn spring-boot:run
```

### Q: 内存不足导致服务崩溃

**原因**: JVM 内存配置过小

**解决方案**:
```bash
# 增加 JVM 内存
java -Xmx1024m -Xms512m -jar target/ai-chat-server-1.0.0.jar

# 或在 Maven 中配置
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx1024m -Xms512m"
```

---

## 📝 日志查看

### 实时日志

```bash
# 启动时显示日志
mvn spring-boot:run

# 或查看运行中的日志
tail -f logs/application.log
```

### 日志级别配置

**文件**: `src/main/resources/application.yml`

```yaml
logging:
  level:
    com.aichat: DEBUG          # 应用日志
    org.springframework: INFO   # Spring 框架日志
    org.hibernate: DEBUG       # Hibernate 日志
```

---

## 🧪 测试服务

### 健康检查

```bash
curl -s http://172.25.40.47:9090/health
```

### 获取 API 文档

```bash
# Swagger UI (如果配置了)
curl -s http://172.25.40.47:9090/swagger-ui.html
```

### 测试登录

```bash
curl -X POST http://172.25.40.47:9090/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'
```

---

## 🔐 环境变量配置

### 设置 AI 上游配置

```bash
export AI_BASE_URL="https://aigc.sankuai.com/v1/openai/native"
export AI_APP_ID="your-app-id"
export AI_DEFAULT_MODEL="LongCat-8B-128K-Chat"
```

### 设置 JWT 密钥

```bash
export JWT_SECRET="your-secret-key-at-least-256-bits"
```

### 启动时应用环境变量

```bash
AI_BASE_URL="..." AI_APP_ID="..." mvn spring-boot:run
```

---

## 📦 依赖管理

### 查看依赖树

```bash
mvn dependency:tree
```

### 更新依赖

```bash
mvn versions:display-dependency-updates
```

### 清理依赖缓存

```bash
rm -rf ~/.m2/repository
mvn clean install
```

---

## 🚀 生产部署

### 构建生产 JAR

```bash
mvn clean package -DskipTests -Pproduction
```

### 使用 systemd 管理服务

创建 `/etc/systemd/system/ai-chat-server.service`:

```ini
[Unit]
Description=AI Chat Server
After=network.target

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/ai-chat-server
ExecStart=/usr/bin/java -jar ai-chat-server-1.0.0.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务:
```bash
sudo systemctl start ai-chat-server
sudo systemctl enable ai-chat-server
```

---

## 📊 性能监控

### 查看 JVM 内存使用

```bash
jps -l
jstat -gc <PID>
```

### 使用 JConsole 监控

```bash
jconsole
```

### 使用 VisualVM 监控

```bash
jvisualvm
```

---

## 🔗 相关文档

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Spring Data JPA 文档](https://spring.io/projects/spring-data-jpa)
- [H2 数据库文档](https://www.h2database.com/)
- [JWT 认证指南](https://jwt.io/)

---

## 📞 获取帮助

### 常见命令速查

| 命令 | 说明 |
|------|------|
| `mvn clean` | 清理构建文件 |
| `mvn compile` | 编译源代码 |
| `mvn package` | 打包 JAR |
| `mvn spring-boot:run` | 启动服务 |
| `mvn test` | 运行测试 |
| `mvn dependency:tree` | 查看依赖树 |

### 快速链接

- 后端项目: `/Users/ryan/Documents/catDesk/ai-chat-server`
- 配置文件: `src/main/resources/application.yml`
- 主类: `src/main/java/com/aichat/server/AiChatServerApplication.java`
- 数据库: `./data/aichatdb.mv.db`

---

**最后更新**: 2026-05-29

**下一步**: 根据需要选择启动方式并启动后端服务！
