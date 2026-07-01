# 东软智慧云脑诊疗平台

基于 Spring Boot 3 + Vue 3 前后端分离架构的**三端智能门诊诊疗原型系统**，融合大模型 API、本地处方规则库与传统 HIS 门诊业务流程，覆盖"诊前分诊 → 线上挂号 → 医生接诊 → 病历生成 → 处方审核 → 患者查看"的完整诊疗闭环。

> 本项目为高校软件工程实训课程项目，定位为**教学实训产品 + 可演示产品原型**。

---

## 项目成员

| 姓名 | 角色 |
|------|------|
| 周子鸣 | - |
| 朱帅霖 | - |
| 王瑜康 | - |
| 刘子杰 | - |
| 李响 | - |

---

## 系统概览

平台围绕"AI + 诊疗"展开，包含三个用户端：

| 终端 | 形态 | 核心功能 |
|------|------|---------|
| 患者端 | 手机端 | 智能分诊、在线挂号、病历/处方查看、就诊评价 |
| 医生端 | PC 工作台 | 接诊队列、AI 辅助诊断、病历生成、处方审核与开具、数据看板 |
| 管理端 | PC 后台 | 科室/医生/药品管理、排班与号源、处方规则、AI 配置、审计日志 |

### 核心 AI 能力

- **智能分诊引擎** — 根据患者主诉推荐科室与医生，AI 不可用时自动降级为本地规则
- **AI 病历生成** — 从医患对话自动生成结构化病历（主诉、现病史、诊断、治疗方案），支持 SSE 流式输出
- **处方审核双引擎** — 本地规则校验（过敏、相互作用、重复用药、剂量） + 大模型二次审核
- **AI 诊疗建议** — 辅助诊断建议，支持医生采纳或忽略
- **对话式分诊** — 多轮对话采集症状信息，精细化分诊推荐

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.5 + Java 17 |
| 数据持久化 | Spring Data JPA + Flyway 迁移 |
| 数据库 | MySQL 8.0+（生产） / H2（快速预览） |
| 前端框架 | Vue 3 + TypeScript + Vite |
| UI | Tailwind CSS 4 + Lucide Icons + ECharts |
| 状态管理 | Pinia |
| HTTP 客户端 | Axios |
| 实时通信 | SSE（AI 流式响应） + WebSocket（通知推送） |
| API 文档 | Knife4j / Swagger3 |
| 认证 | JWT（HMAC-SHA256）+ 角色鉴权（PATIENT / DOCTOR / ADMIN） |
| AI 集成 | 抽象 Provider 层，支持 DeepSeek 等多模型切换 |
| 构建 | Maven Wrapper（后端） + Vite（前端） |

---

## 项目结构

```
Cloud-Brain-Medical/
├── backend/                     # Spring Boot 后端
│   └── src/main/java/com/cloudbrain/
│       ├── application/         # 业务服务层（auth/ai/chat/patient/workflow/admin）
│       ├── controller/          # REST 控制器（7 个，40+ API 端点）
│       ├── entity/              # JPA 实体（22 张表）
│       ├── repository/          # JPA Repository 接口
│       ├── security/            # Spring Security + JWT 认证授权
│       ├── config/              # CORS / Swagger / WebSocket 配置
│       └── common/              # 统一响应 / 异常处理
├── frontend/                    # Vue 3 前端
│   └── src/
│       ├── views/               # 页面（Home / patient / doctor / admin / auth）
│       ├── components/          # 组件（chat / layout / shared / workflow）
│       ├── stores/              # Pinia 状态管理
│       ├── api/                 # API 封装 + Axios 拦截器
│       └── router/              # 路由（含鉴权守卫 + token 自动刷新）
├── docs/                        # 项目文档
│   ├── PRD-东软智慧云脑诊疗平台.md
│   ├── 系统设计文档-东软智慧云脑诊疗平台.md
│   ├── 前端交互设计文档-东软智慧云脑诊疗平台.md
│   ├── 部署验收.md
│   └── 测试账号.md
├── deploy/                      # 部署配置（Nginx 配置）
├── start-backend-mysql.ps1      # 一键启动脚本
├── docker-compose.mysql.yml     # Docker MySQL 配置
└── pom.xml                      # Maven 父 POM
```

---

## 快速开始

### 环境要求

- **JDK** 17+
- **MySQL** 8.0+ / 9.x（H2 可跳过）
- **Node.js** 18+

### 1. 启动后端

```powershell
# 一键启动（自动发现 MySQL，推荐）
.\start-backend-mysql.ps1

# 或使用内置 H2 数据库（无需 MySQL）
.\mvnw.cmd -pl backend spring-boot:run
```

### 2. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

### 3. 访问

| 地址 | 说明 |
|------|------|
| `http://localhost:5173` | 前端页面 |
| `http://localhost:8088` | 后端 API |
| `http://localhost:8088/swagger-ui.html` | API 文档 |

### 4. 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `admin123` |
| 医生 | `doctor01` | `doctor123` |
| 患者 | `patient01` | `patient123` |

> 完整测试账号列表见 [docs/测试账号.md](docs/测试账号.md)

---

## 业务流程

```
患者登录 → 智能分诊（AI 推荐科室/医生）→ 在线挂号
  → 医生接诊 → 问诊对话 → AI 生成病历草稿 → 医生确认保存
  → AI 辅助诊断建议 → 处方开具 → AI 处方审核（本地规则 + 大模型）
  → 处方提交 → 就诊完成 → 患者查看病历/处方 → 反馈评价
```

挂号状态流转：`WAITING → IN_CONSULTATION → MEDICAL_RECORD_SAVED → PRESCRIPTION_REVIEWED → PRESCRIPTION_SUBMITTED → COMPLETED`

---

## 实时功能

- **SSE 流式响应**：AI 病历生成、诊断建议、处方审核结果逐字流式展示，支持 reasoning 思考过程折叠
- **WebSocket 通知**：实时推送处方审核结果、就诊状态变更
- **数据看板**：6 类仪表盘 API（概览、趋势、AI 使用量、处方审核率、风险分布、分诊准确率）

---

## 部署

```powershell
# 后端构建
.\mvnw.cmd -pl backend package

# 前端构建
cd frontend && npm run build

# Nginx 双端分离部署（参考 deploy/nginx.conf）
```

详见 [RUNNING.md](RUNNING.md) 和 [docs/部署验收.md](docs/部署验收.md)

---

## 项目文档

| 文档 | 说明 |
|------|------|
| [docs/需求.md](docs/需求.md) | 课程任务描述 |
| [docs/PRD-东软智慧云脑诊疗平台.md](docs/PRD-东软智慧云脑诊疗平台.md) | 产品需求文档 v1.2 |
| [docs/系统设计文档-东软智慧云脑诊疗平台.md](docs/系统设计文档-东软智慧云脑诊疗平台.md) | 系统设计文档 v3.0 |
| [docs/前端交互设计文档-东软智慧云脑诊疗平台.md](docs/前端交互设计文档-东软智慧云脑诊疗平台.md) | 前端交互设计 v1.0 |
| [docs/测试账号.md](docs/测试账号.md) | 测试账号与场景 |
| [docs/部署验收.md](docs/部署验收.md) | 部署与验收清单 |
| [docs/test-report-2026-06-26.md](docs/test-report-2026-06-26.md) | 测试报告 |

---

## License

本项目仅用于教学实训。
