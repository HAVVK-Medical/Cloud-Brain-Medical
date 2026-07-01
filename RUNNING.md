# Running

## 环境要求

| 工具 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | 推荐 [Eclipse Temurin](https://adoptium.net/) 或 Oracle JDK |
| Maven | 内置 Wrapper | 无需单独安装，使用 `mvnw` 即可 |
| Node.js | 18+ | 推荐 20 LTS |
| MySQL | 8.0+ / 9.x | 本地开发用；脚本会自动发现安装位置 |
| Docker | (可选) | 用于 Docker MySQL 方案 |

## 启动方式

### 方式一：一键启动（推荐）

```powershell
.\start-backend-mysql.ps1
```

会自动：发现 MySQL 安装位置 → 启动本地 MySQL（端口 3307）→ 创建数据库和用户 → 启动后端（端口 8088）。

修改后端端口：
```powershell
.\start-backend-mysql.ps1 -Port 8081
```

> **注意**：如果自动发现 MySQL 失败，请设置环境变量 `$env:MYSQL_HOME` 指向你的 MySQL 安装目录。

### 方式二：Docker MySQL

```powershell
docker compose -f docker-compose.mysql.yml up -d

$env:SPRING_PROFILES_ACTIVE="mysql"
$env:DB_URL="jdbc:mysql://127.0.0.1:3306/cloudbrain_medical?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="cloudbrain"
$env:DB_PASSWORD="cloudbrain_dev"
.\mvnw.cmd -pl backend spring-boot:run
```

### 方式三：内置 H2（无需数据库）

```powershell
.\mvnw.cmd -pl backend spring-boot:run
```

使用内存 H2 数据库，无需安装任何数据库。首次运行会自动执行 Flyway 建表。

> **已知限制**：H2 模式下部分 MySQL 特有语法可能不兼容，仅用于快速预览。

## 前端

```powershell
cd frontend
npm install
npm run dev
```

## 生产构建

```powershell
# 后端
.\mvnw.cmd -pl backend package

# 前端
cd frontend
npm install
npm run build
```

启动 Jar：
```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:DB_URL="jdbc:mysql://127.0.0.1:3307/cloudbrain_medical?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="cloudbrain"
$env:DB_PASSWORD="cloudbrain_dev"
java -jar backend\target\cloud-brain-medical-backend-0.1.0-SNAPSHOT.jar
```

## 访问地址

| 服务 | 地址 |
|------|------|
| 后端 API | `http://localhost:8088` |
| 健康检查 | `http://localhost:8088/api/health` |
| Swagger 文档 | `http://localhost:8088/swagger-ui.html` |
| 前端 | `http://localhost:5173` |

## 测试账号

详见 [docs/测试账号.md](docs/测试账号.md)。

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 医生 | doctor01 | doctor123 |
| 患者 | patient01 | patient123 |

## 实时功能检查

- WebSocket 通知：`/ws/notifications?token=<jwt>`
- SSE 会话流：`POST /api/ai-stream-sessions` → `GET /api/ai-stream-sessions/{id}/events?token=<streamToken>`
- 仪表盘 API：`/api/dashboard/overview`, `/api/dashboard/trends`, `/api/dashboard/ai-usage`, `/api/dashboard/prescription-review-rate`, `/api/dashboard/risk-distribution`, `/api/dashboard/triage-accuracy`

## 部署参考

- Nginx 配置：[deploy/nginx.conf](deploy/nginx.conf)
- 部署验收清单：[docs/部署验收.md](docs/部署验收.md)
