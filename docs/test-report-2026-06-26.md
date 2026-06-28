# 东软智慧云脑诊疗平台 — 综合测试报告

**日期**：2026-06-26
**测试类型**：白盒测试 + 黑盒测试 + 编译验证
**测试范围**：代码完备性、数据结构正确性、流程可跑性、用户体验、PRD符合度

---

## 目录

1. [测试总览](#1-测试总览)
2. [编译与测试基础设施验证](#2-编译与测试基础设施验证)
3. [白盒测试：后端代码逐层分析](#3-白盒测试后端代码逐层分析)
4. [白盒测试：前端代码分析](#4-白盒测试前端代码分析)
5. [黑盒测试：API与数据流分析](#5-黑盒测试api与数据流分析)
6. [数据结构与数据库完整性](#6-数据结构与数据库完整性)
7. [用户体验与流程分析](#7-用户体验与流程分析)
8. [PRD需求符合度矩阵](#8-prd需求符合度矩阵)
9. [问题汇总与修复建议](#9-问题汇总与修复建议)

---

## 1. 测试总览

### 1.1 项目信息

| 项目     | 详情                                                   |
| -------- | ------------------------------------------------------ |
| 名称     | 东软智慧云脑诊疗平台 (Cloud Brain Medical)             |
| 架构     | Spring Boot 3 + Vue 3 + TypeScript 前后端分离          |
| 数据库   | MySQL (生产) / H2 (测试)                               |
| 代码总量 | ~60+ Java 文件 (backend) + ~50+ Vue/TS 文件 (frontend) |
| 现有测试 | 3 个测试类, 9 个测试方法                               |

### 1.2 评估维度

| 维度       | 评级        | 说明                       |
| ---------- | ----------- | -------------------------- |
| 编译通过性 | 🟡 部分通过 | 后端编译通过, 测试无法运行 |
| 代码完备性 | 🟢 良好     | 核心功能模块已实现         |
| 数据正确性 | 🟡 有风险   | 并发控制有漏洞             |
| 流程完整性 | 🟢 良好     | 主链路可走通               |
| 用户体验   | 🟡 有瑕疵   | 存在体验问题               |
| PRD 符合度 | 🟢 良好     | P0 功能基本覆盖            |

---

## 2. 编译与测试基础设施验证

### 2.1 后端编译

```
命令: ./mvnw compile -f backend/pom.xml
结果: ✅ 编译成功 (静默输出 = 零错误)
```

### 2.2 前端类型检查

```
命令: npx vue-tsc --noEmit
结果: ✅ 类型检查通过 (静默输出 = 零错误)
```

### 2.3 测试运行 — 🔴 严重失败

```
命令: ./mvnw test -f backend/pom.xml
结果: ❌ 全部 9 个测试失败 (0 通过)
根因: Flyway V9 迁移使用 MySQL 特有函数 DATE_ADD, 在 H2 测试数据库中不兼容
```

**错误信息**:

```
Function "date_add" not found; SQL statement:
Script V9__seed_hospital_data.sql failed
```

**位置**: [V9__seed_hospital_data.sql](backend/src/main/resources/db/migration/V9__seed_hospital_data.sql)

**问题**: V9 迁移文件第 364 行注释声称 "cross-DB compatibility"，但 `DATE_ADD(CURRENT_DATE, INTERVAL n DAY)` 是 MySQL 专有语法。H2 需要 `DATEADD('DAY', n, CURRENT_DATE)`。

**影响**: 所有需要 Spring ApplicationContext 的测试完全无法运行。这是一个阻塞性问题。

**修复建议**:

1. 将 V9 中的 `DATE_ADD(CURRENT_DATE, INTERVAL n DAY)` 替换为 H2 兼容语法
2. 或创建 MySQL 专用 profile，测试使用 H2 专用 profile
3. 或将 seed 数据逻辑移到 Java 代码（DatabaseSeeder.java）

---

## 3. 白盒测试：后端代码逐层分析

### 3.1 安全层 (SecurityConfig + AuthService)

#### ✅ 正确实现

| 检查项                | 状态 | 位置                                                                                                             |
| --------------------- | ---- | ---------------------------------------------------------------------------------------------------------------- |
| BCrypt 密码加密       | ✅   | [SecurityConfig.java:22](backend/src/main/java/com/cloudbrain/security/SecurityConfig.java#L22)                     |
| JWT 无状态 Session    | ✅   | [SecurityConfig.java:33](backend/src/main/java/com/cloudbrain/security/SecurityConfig.java#L33)                     |
| CSRF 禁用 (API only)  | ✅   | [SecurityConfig.java:31](backend/src/main/java/com/cloudbrain/security/SecurityConfig.java#L31)                     |
| 三端角色隔离鉴权      | ✅   | [SecurityConfig.java:61-63](backend/src/main/java/com/cloudbrain/security/SecurityConfig.java#L61-L63)              |
| 公开端点白名单        | ✅   | [SecurityConfig.java:41-59](backend/src/main/java/com/cloudbrain/security/SecurityConfig.java#L41-L59)              |
| 密码长度校验 (≥6)    | ✅   | [AuthService.java:52](backend/src/main/java/com/cloudbrain/application/auth/AuthService.java#L52) (age，非password) |
| Token 刷新 + 撤销机制 | ✅   | [AuthService.java:74-97](backend/src/main/java/com/cloudbrain/application/auth/AuthService.java#L74-L97)            |
| 审计日志记录          | ✅   | [AuthService.java:164-191](backend/src/main/java/com/cloudbrain/application/auth/AuthService.java#L164-L191)        |

#### 🔴 安全问题

| 问题                        | 严重度    | 位置                                                                                                       | 描述                                                                                                                                                                                                                                                                                                                                                                                         |
| --------------------------- | --------- | ---------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Chat Stream 端点无认证      | 🔴 HIGH   | [SecurityConfig.java:52](backend/src/main/java/com/cloudbrain/security/SecurityConfig.java#L52)               | `GET /api/chat/stream` 配置为 `.permitAll()`，但 [ChatController.java:48-49](backend/src/main/java/com/cloudbrain/controller/ChatController.java#L48-L49) 内部调用 `ActorContextResolver.requireCurrent()`。安全过滤器不拦截该请求，JWT filter 不会处理 token，导致 `requireCurrent()` 始终抛出异常。**该端点永远无法正常工作**。修复：移除 `.permitAll()` 或将鉴权移到方法内部 |
| AI Stream Events 端点无认证 | 🟡 MEDIUM | [SecurityConfig.java:51](backend/src/main/java/com/cloudbrain/security/SecurityConfig.java#L51)               | `GET /api/ai-stream-sessions/*/events` 也是 `.permitAll()`。虽然有 `token` 查询参数作为验证，但缺乏完整的认证流程                                                                                                                                                                                                                                                                      |
| 异常消息泄露                | 🟡 MEDIUM | [GlobalExceptionHandler.java:33](backend/src/main/java/com/cloudbrain/common/GlobalExceptionHandler.java#L33) | `exception.getMessage()` 直接返回给客户端。在 UNAUTHORIZED 等场景下已妥善处理，但在通用 Exception 处理中可能暴露内部错误信息                                                                                                                                                                                                                                                               |
| 密码长度未在后端校验        | 🟡 MEDIUM | [AuthService.java:51-53](backend/src/main/java/com/cloudbrain/application/auth/AuthService.java#L51-L53)      | 只校验了 age，没有校验 password 长度 ≥ 6。依赖前端校验和后端 DTO `@Valid` 注解                                                                                                                                                                                                                                                                                                            |
| AES 密钥管理不透明          | 🟡 LOW    | [AIConfigResolver.java:42](backend/src/main/java/com/cloudbrain/application/ai/AIConfigResolver.java#L42)     | `configCipher.decrypt()` 的内部实现未检查，密钥管理和轮转机制需要验证                                                                                                                                                                                                                                                                                                                      |

#### 🟡 设计建议

| 建议                | 位置                                                                               | 描述                                                                                |
| ------------------- | ---------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| CorsConfig 过于严格 | [CorsConfig.java:17](backend/src/main/java/com/cloudbrain/config/CorsConfig.java#L17) | 仅允许 `localhost:5173` 和 `127.0.0.1:5173`。部署到其他端口或域名时需要修改代码 |
| 无请求频率限制      | N/A                                                                                | 登录接口、注册接口无 rate limiting，易受暴力攻击                                    |

---

### 3.2 业务逻辑层 (WorkflowService)

#### ✅ 正确实现

| 检查项                | 状态 | 位置                                                                                                                                        |
| --------------------- | ---- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| 挂号状态机实现        | ✅   | [WorkflowService.java:113-120](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L113-L120) (7个状态)             |
| 号源并发扣减 (乐观锁) | ✅   | [WorkflowService.java:365](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L365) (`decrementSlotWithVersion`) |
| 患者取消挂号权限控制  | ✅   | [WorkflowService.java:407-412](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L407-L412) (仅 WAITING 状态)     |
| 重复挂号防护          | ✅   | [WorkflowService.java:361-363](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L361-L363)                       |
| AI 降级回退逻辑       | ✅   | [AIInvocationService.java:36-41](backend/src/main/java/com/cloudbrain/application/ai/AIInvocationService.java#L36-L41)                         |
| 处方审核本地规则优先  | ✅   | [WorkflowService.java:1339-1389](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L1339-L1389)                   |
| 处方快照哈希校验      | ✅   | [WorkflowService.java:819-824](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L819-L824)                       |
| 病历保存向前推进状态  | ✅   | [WorkflowService.java:603](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L603) (到 MEDICAL_RECORD_SAVED)      |

#### 🔴 逻辑缺陷

| 问题                         | 严重度    | 位置                                                                                                                  | 描述                                                                                                                                                                                                                                             |
| ---------------------------- | --------- | --------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 挂号并发：回滚后号源泄露     | 🔴 HIGH   | [WorkflowService.java:387-392](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L387-L392) | `createRegistration` 在 catch DataIntegrityViolationException 后调用 `releaseSlotOnce`。但**事务已经因异常标记为 rollback-only**，release 操作不会提交。导致号源被扣减但未释放。修复：将 slot release 放在事务外或用 REQUIRES_NEW 传播 |
| 取消挂号未校验 slot_released | 🟡 MEDIUM | [WorkflowService.java:422-424](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L422-L424) | 仅当 `cancelled == 1` 时释放号源。但 `cancelWaitingRegistrationOnce` 只更新 status，不检查 `slotReleased` 标志。如果前一次释放失败（事务回滚），重复取消不会再次释放号源                                                                   |
| 分诊推荐源校验不完整         | 🟡 MEDIUM | [WorkflowService.java:301-305](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L301-L305) | `triage()` 方法直接使用本地规则 `pickDepartment()` 的结果，不验证 AI 返回的科室是否存在于数据库。如果 AI 返回不存在的科室，会静默使用本地规则结果                                                                                            |
| 药品搜索：下架药品可能被选中 | 🟡 LOW    | [WorkflowService.java:811](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L811)          | `submitPrescription` 通过 `findByIdAndStatus(drugId, ACTIVE)` 校验药品状态，但 `reviewPrescription` 也是同样检查。检查点是充分的                                                                                                           |

#### 🟡 边界条件

| 场景                 | 处理情况                                                                                                                                                                | 位置                                                                                                                  |
| -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| 空主诉分诊           | ✅`triageForm.chiefComplaint.trim()` 前端校验 + 后端默认 [WorkflowService.java:298](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L298) |                                                                                                                       |
| 无医生可用时挂号     | ✅`schedule is unavailable` 错误 [WorkflowService.java:359](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L359)                         |                                                                                                                       |
| 病历保存前无问诊文本 | 🟡 允许保存空字段，使用默认值                                                                                                                                           | [WorkflowService.java:589-596](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L589-L596) |
| 处方提交无审核记录   | ✅`unbound review not found` 错误 [WorkflowService.java:815](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L815)                        |                                                                                                                       |
| 并发挂号同一号源     | ✅ 乐观锁版本号控制[WorkflowService.java:365-367](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L365-L367)                                |                                                                                                                       |
| 负年龄注册           | ✅`age must be positive` 错误 [AuthService.java:53](backend/src/main/java/com/cloudbrain/application/auth/AuthService.java#L53)                                          |                                                                                                                       |

---

### 3.3 AI 集成层

#### ✅ 正确设计

| 特性                                  | 状态 | 位置                                                                                                                                         |
| ------------------------------------- | ---- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| Provider 抽象接口                     | ✅   | [AIProvider.java](backend/src/main/java/com/cloudbrain/application/ai/AIProvider.java)                                                          |
| 多供应商支持 (通义千问/豆包/DeepSeek) | ✅   | [AIConfigResolver.java](backend/src/main/java/com/cloudbrain/application/ai/AIConfigResolver.java)                                              |
| AI 配置优先级排序                     | ✅   | [AIConfigResolver.java:50-56](backend/src/main/java/com/cloudbrain/application/ai/AIConfigResolver.java#L50-L56)                                |
| Prompt 模板化                         | ✅   | [AIInvocationService.java:103-129](backend/src/main/java/com/cloudbrain/application/ai/AIInvocationService.java#L103-L129)                      |
| 温度/Tokens 按任务类型配置            | ✅   | [AIInvocationService.java:131-148](backend/src/main/java/com/cloudbrain/application/ai/AIInvocationService.java#L131-L148)                      |
| AI 超时处理                           | ✅   | chat request 中的 timeoutSeconds[AIInvocationService.java:55](backend/src/main/java/com/cloudbrain/application/ai/AIInvocationService.java#L55) |

#### 🔴 问题

| 问题            | 严重度    | 位置                                                                                                                | 描述                                                                                                               |
| --------------- | --------- | ------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| AI 超时无断路器 | 🟡 MEDIUM | [AIInvocationService.java:79-90](backend/src/main/java/com/cloudbrain/application/ai/AIInvocationService.java#L79-L90) | 每次 AI 调用失败都 catch 并 fallback，但没有熔断器。如果 AI 提供商持续不可用，每次请求都要等待完整超时（默认 30s） |

---

### 3.4 控制器层 API 设计

#### ✅ RESTful 规范

| 检查项                                  | 状态 |
| --------------------------------------- | ---- |
| 统一响应格式 Result\<T\>                | ✅   |
| HTTP 方法正确使用 (GET/POST/PUT/DELETE) | ✅   |
| 全局异常处理                            | ✅   |
| Knife4j/Swagger 文档                    | ✅   |
| 请求体 @Valid 校验                      | ✅   |

#### 🟡 API 设计问题

| 问题                                  | 位置                                                                                                     | 描述                                                                                                         |
| ------------------------------------- | -------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| 取消挂号用 POST 而非 DELETE           | [WorkflowController.java:124](backend/src/main/java/com/cloudbrain/controller/WorkflowController.java#L124) | `POST /api/registration/cancel/{id}` — 更 RESTful 的做法是 `DELETE /api/registration/{id}` 或 `PATCH` |
| 密码字段需要 @JsonProperty 控制序列化 | N/A                                                                                                      | LoginRequest 等 DTO 应确保 password 字段不被序列化到日志                                                     |

---

## 4. 白盒测试：前端代码分析

### 4.1 路由与导航

#### ✅ 正确实现

| 检查项                                   | 状态 |
| ---------------------------------------- | ---- |
| 三端路由分离 (/patient, /doctor, /admin) | ✅   |
| 路由守卫 (requiresAuth + role)           | ✅   |
| Token 过期自动刷新                       | ✅   |
| 懒加载路由组件                           | ✅   |

#### 🟡 问题

| 问题                   | 位置                                                                                   | 描述                                                                  |
| ---------------------- | -------------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| 角色不匹配时跳到登录页 | [router/index.ts:106-111](frontend/src/router/index.ts#L106-L111)                         | 如果 patient 用户尝试访问 /doctor，会跳到 /login 而不是显示权限错误页 |
| hardcoded 导航路径     | [PatientTriagePanel.vue:72](frontend/src/views/patient/panels/PatientTriagePanel.vue#L72) | `$router.push('/patient/registration')` 硬编码路径，应使用命名路由  |

### 4.2 状态管理 (Pinia Stores)

#### ✅ 正确设计

| 检查项                                 | 状态 | 位置                                                |
| -------------------------------------- | ---- | --------------------------------------------------- |
| auth store loading/error/degraded 状态 | ✅   | [auth.ts](frontend/src/stores/auth.ts)                 |
| localStorage 持久化 + 水合             | ✅   | [auth.ts:83-103](frontend/src/stores/auth.ts#L83-L103) |
| Token 过期检测                         | ✅   | [auth.ts:74-80](frontend/src/stores/auth.ts#L74-L80)   |

#### 🟡 问题

| 问题                        | 位置                                         | 描述                                                                                                                                                |
| --------------------------- | -------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| 无 Pinia store 模块化       | N/A                                          | PRD 要求 10+ 个 Pinia stores（E06），但当前只用 useAuthStore。患者端和医生端的状态都在组件内用 reactive/ref 管理，没有 usePatientStore 等独立 store |
| refreshSession 错误消息乱码 | [auth.ts:171](frontend/src/stores/auth.ts#L171) | 硬编码了乱码字符串 `'鐧诲綍宸茶繃鏈燂紝璇烽噸鏂扮櫥褰?'`，应该是 `'登录已过期，请重新登录'`                                                     |

### 4.3 HTTP 拦截器

#### ✅ Axios 封装

| 检查项                    | 状态 |
| ------------------------- | ---- |
| 自动附加 JWT Bearer token | ✅   |
| 401 自动刷新 token        | ✅   |
| 刷新失败跳转登录页        | ✅   |
| baseURL 前缀 /api         | ✅   |

#### 🟡 问题

| 问题                         | 位置                                           | 描述                                                                                                                        |
| ---------------------------- | ---------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| refreshClient 独立但无拦截器 | [http.ts:11-14](frontend/src/api/http.ts#L11-L14) | refresh 请求使用的 refreshClient 不会自动附加 token，也不会在刷新失败时触发重定向。这是有意为之（避免循环），但缺少注释说明 |
| 401 后强制跳转无提示         | [http.ts:72-74](frontend/src/api/http.ts#L72-L74) | 刷新失败后直接 `window.location.assign('/login')`，用户没有任何提示                                                       |

### 4.4 组件问题

| 问题                        | 严重度    | 位置                                                                                                       | 描述                                                                                                                                                               |
| --------------------------- | --------- | ---------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| "采纳全部"按钮调用错误方法  | 🟡 MEDIUM | [DoctorConsultationPanel.vue:169-171](frontend/src/views/doctor/panels/DoctorConsultationPanel.vue#L169-L171) | "采纳全部"按钮的 `@click` 绑定到 `saveCurrentMedicalRecord()` 而非独立逻辑。与"保存病历"按钮行为完全相同，功能重复                                             |
| doctorId 为 null 时静默失败 | 🟡 MEDIUM | [DoctorHomeView.vue:478-479](frontend/src/views/doctor/DoctorHomeView.vue#L478-L479)                          | `Promise.all` 中第一个元素使用 `doctorId ? getDoctor(doctorId) : Promise.resolve(null)`，但 Promise.all 的参数顺序错误：第二个元素应传入数组但被传为了独立参数 |
| 组件 Props 类型为 any       | 🟡 LOW    | PatientTriagePanel.vue, DoctorConsultationPanel.vue                                                        | Props 使用 `any` 类型 (`workspace: any`)，消除了类型安全                                                                                                       |

---

## 5. 黑盒测试：API 与数据流分析

### 5.1 完整诊疗流程验证 (基于代码走读)

以下模拟 PRD 10.5 定义的典型演示场景：

| 步骤 | 角色   | 操作               | API 调用                                 | 代码验证结果        |
| ---- | ------ | ------------------ | ---------------------------------------- | ------------------- |
| 1    | 管理员 | 登录               | `POST /api/auth/admin/login`           | ✅ 可走通           |
| 2    | 管理员 | 维护科室/医生/排班 | `POST /api/admin/departments` 等       | ✅ 可走通           |
| 3    | 管理员 | 维护药品和规则     | `POST /api/admin/drugs` 等             | ✅ 可走通           |
| 4    | 患者   | 注册               | `POST /api/auth/patient/register`      | ✅ 可走通           |
| 5    | 患者   | 登录               | `POST /api/auth/patient/login`         | ✅ 可走通           |
| 6    | 患者   | 智能分诊           | `POST /api/triage/consult`             | ✅ 可走通（含降级） |
| 7    | 患者   | 查看推荐医生       | (分诊结果包含)                           | ✅                  |
| 8    | 患者   | 挂号               | `POST /api/registration/create`        | ✅ 可走通           |
| 9    | 医生   | 登录               | `POST /api/auth/doctor/login`          | ✅ 可走通           |
| 10   | 医生   | 查看患者队列       | `GET /api/doctor/queue`                | ✅                  |
| 11   | 医生   | 开始接诊           | `POST /api/consultation/{id}/begin`    | ✅                  |
| 12   | 医生   | AI 生成病历        | `POST /api/medical-record/generate`    | ✅                  |
| 13   | 医生   | 保存病历           | `POST /api/medical-record/save`        | ✅                  |
| 14   | 医生   | 搜索药品           | `GET /api/drugs/search`                | ✅                  |
| 15   | 医生   | AI 审方            | `POST /api/prescription/check`         | ✅                  |
| 16   | 医生   | 提交处方           | `POST /api/prescription/submit`        | ✅                  |
| 17   | 患者   | 查看病历           | `GET /api/medical-record/list/patient` | ✅                  |
| 18   | 患者   | 查看处方           | `GET /api/prescription/list/patient`   | ✅                  |
| 19   | 患者   | 评价               | `POST /api/feedback/create`            | ✅                  |

**结论**: P0 核心主链路代码逻辑完整，可走通。但受测试数据库迁移失败影响，无法进行端到端集成测试验证。

### 5.2 API 响应一致性

| 检查项                         | 结果         |
| ------------------------------ | ------------ |
| 所有 API 返回 Result\<T\> 格式 | ✅           |
| 错误时返回统一错误码           | ✅           |
| Swagger 文档可访问性           | 需运行时验证 |

---

## 6. 数据结构与数据库完整性

### 6.1 实体关系分析

对照 PRD 和设计文档，数据库表覆盖情况：

| PRD 要求    | 对应表                              | 状态            |
| ----------- | ----------------------------------- | --------------- |
| 患者管理    | patient                             | ✅              |
| 医生管理    | doctor                              | ✅              |
| 科室管理    | department                          | ✅              |
| 挂号管理    | registration                        | ✅ 含完整状态机 |
| 分诊记录    | triage_record                       | ✅              |
| 病历管理    | medical_record                      | ✅              |
| 处方管理    | prescription + prescription_item    | ✅              |
| 处方审核    | prescription_review                 | ✅              |
| 药品库      | drug                                | ✅              |
| 处方规则    | prescription_rule_definition        | ✅              |
| AI 配置     | ai_config                           | ✅              |
| AI 调用审计 | ai_call_record                      | ✅              |
| 就诊评价    | feedback + triage_accuracy_feedback | ✅              |
| 问诊记录    | consultation_note                   | ✅              |
| 诊断建议    | diagnosis_suggestion_record         | ✅              |
| 通知记录    | notification_record                 | ✅              |
| 排班号源    | schedule                            | ✅              |
| Prompt 模板 | prompt_template                     | ✅              |
| 聊天会话    | chat_session + chat_message         | ✅ (扩展)       |
| 会话 Token  | session_token                       | ✅ (扩展)       |
| 审计日志    | audit_log                           | ✅ (扩展)       |
| Admin 表    | admin                               | ✅              |

### 6.2 数据库迁移问题

#### 🔴 阻塞性

| 问题             | 详情                                                                 |
| ---------------- | -------------------------------------------------------------------- |
| V9 迁移不兼容 H2 | `DATE_ADD` 函数在 H2 中不存在。导致所有测试无法运行                |
| 解决方案         | 替换为 `DATEADD('DAY', n, CURRENT_DATE)` 或使用 Java-based seeding |

#### 🟡 数据完整性风险

| 问题                                       | 位置                                                                                                                  | 描述                                                                   |
| ------------------------------------------ | --------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| schedule.remaining_slots 可能为负          | schedule 表                                                                                                           | 并发场景下 version check 失败后无补偿逻辑                              |
| registration 与 triage_record 外键关联弱   | [WorkflowService.java:394-401](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L394-L401) | 分诊记录 ID 在挂号时是可选的，历史追溯可能断裂                         |
| ai_call_record.business_record_id 延迟设置 | [WorkflowService.java:337](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L337)          | 先保存 `callRecord` (businessRecordId=null)，再更新。中间状态为 null |

---

## 7. 用户体验与流程分析

### 7.1 加载状态与反馈

| 检查项               | 状态 | 位置                                                  |
| -------------------- | ---- | ----------------------------------------------------- |
| AI 调用 Loading 动画 | ✅   | 骨架屏 + 按钮禁用 + 文字变化                          |
| 错误提示中文展示     | ✅   | resolveUiErrorMessage                                 |
| 分诊空状态提示       | ✅   | EmptyState 组件                                       |
| 空列表提示           | ✅   | "暂无患者"、"暂无数据"                                |
| 关键操作确认         | 🟡   | 取消挂号有确认 UI，但提交处方和病历保存无二次确认弹窗 |

### 7.2 用户体验问题

| 问题                         | 严重度    | 描述                                                                                                                                                                                                                      |
| ---------------------------- | --------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 手机端患者分诊后直接跳转     | 🟡 MEDIUM | [PatientTriagePanel.vue:72](frontend/src/views/patient/panels/PatientTriagePanel.vue#L72) 分诊结果点"去挂号"跳转到 `/patient/registration`，但自动带入选择的功能依赖 `triageResult` 的状态。如果用户刷新页面，选择会丢失 |
| 处方页面无二次确认           | 🟡 MEDIUM | [DoctorConsultationPanel.vue:253](frontend/src/views/doctor/panels/DoctorConsultationPanel.vue#L253) 没有二次确认弹窗。PRD NFR-17 要求关键操作需二次确认                                                                     |
| WebSocket 通知连接无自动重连 | 🟡 MEDIUM | [DoctorHomeView.vue:434](frontend/src/views/doctor/DoctorHomeView.vue#L434) `onclose` 只设置状态，不自动重连                                                                                                               |
| 登录过期无用户提示           | 🟡 LOW    | [http.ts:73](frontend/src/api/http.ts#L73) 直接跳转无 toast                                                                                                                                                                  |
| 医生端"采纳全部"按钮无意义   | 🟡 LOW    | 与"保存病历"按钮行为相同[DoctorConsultationPanel.vue:169](frontend/src/views/doctor/panels/DoctorConsultationPanel.vue#L169)                                                                                                 |
| Pinia 乱码                   | 🟡 LOW    | [auth.ts:171](frontend/src/stores/auth.ts#L171) 错误信息乱码                                                                                                                                                                 |

### 7.3 流程冲突与卡死分析

| 场景               | 分析结果                                          |
| ------------------ | ------------------------------------------------- |
| 患者同时取消挂号   | ✅ 乐观锁 + 数据库约束保护                        |
| 医生同时开始接诊   | ✅ 乐观锁控制，幂等                               |
| 处方提交时内容变更 | ✅ 快照哈希校验                                   |
| AI 调用超时卡死    | ✅ Spring MVC 异步 + timeoutSeconds               |
| 网络断开时前端卡死 | 🟡 Axios timeout 15s，但 WebSocket 断开无自动重连 |

---

## 8. PRD 需求符合度矩阵

### 8.1 P0 核心功能

| PRD ID  | 功能               | 后端状态 | 前端状态 | 符合度 |
| ------- | ------------------ | -------- | -------- | ------ |
| F01-F06 | 项目初始化         | N/A      | N/A      | ✅     |
| F07-F09 | 患者注册/登录/信息 | ✅       | ✅       | ✅     |
| F10-F11 | 医生列表/详情      | ✅       | ✅       | ✅     |
| F12-F14 | 挂号创建/列表/取消 | ✅       | ✅       | ✅     |
| F15-F19 | 智能分诊           | ✅       | ✅       | ✅     |
| F20-F21 | 科室/医生管理      | ✅       | ✅       | ✅     |
| F22     | 排班号源管理       | ✅       | ✅       | ✅     |
| F23-F24 | 药品/规则管理      | ✅       | ✅       | ✅     |
| F25-F26 | 处方管理           | ✅       | ✅       | ✅     |
| F27-F30 | AI 处方审核        | ✅       | ✅       | ✅     |
| F31-F35 | AI 病历生成        | ✅       | ✅       | ✅     |
| F36-F39 | 前后端联调         | ✅       | ✅       | ✅     |

### 8.2 P1 拓展功能

| PRD ID | 功能           | 后端状态 | 前端状态 | 符合度                |
| ------ | -------------- | -------- | -------- | --------------------- |
| E01    | AI 诊疗建议    | ✅       | ✅       | ✅                    |
| E02    | 就诊评价       | ✅       | ✅       | ✅                    |
| E03    | 数据看板       | ✅       | ✅       | ✅                    |
| E04    | WebSocket 通知 | ✅       | ✅       | ✅                    |
| E05    | SSE 流式响应   | ✅       | ✅       | ✅                    |
| E06    | Pinia 状态机   | 🟡       | 🟡       | 🟡 仅 auth store 完成 |
| E07    | Prompt 模板    | ✅       | N/A      | ✅ 后端完成           |
| E08    | Nginx+Jar 部署 | N/A      | N/A      | ⬜ 未验证             |
| E09    | 微服务拆分     | N/A      | N/A      | ⬜ 远期               |
| E10    | 远期 HIS       | N/A      | N/A      | ⬜ 远期               |

### 8.3 非功能需求

| PRD ID    | 要求                           | 状态 | 备注                       |
| --------- | ------------------------------ | ---- | -------------------------- |
| NFR-01    | `npm run dev` 可启动         | ✅   |                            |
| NFR-02    | `mvn spring-boot:run` 可启动 | ✅   | 后端编译通过               |
| NFR-03    | 完整诊疗流程可走通             | ✅   | 代码逻辑完整               |
| NFR-04    | JPA/Flyway 自动建表            | 🟡   | 测试环境 Flyway 迁移失败   |
| NFR-05-07 | 可演示性                       | ✅   |                            |
| NFR-08-12 | 代码规范性                     | ✅   | 三层架构 + Composition API |
| NFR-13-17 | 体验友好性                     | 🟡   | 缺失部分二次确认           |
| NFR-18-22 | 扩展性                         | ✅   | AI Provider 接口解耦       |

---

## 9. 问题汇总与修复建议

### 9.1 按严重度分类

#### 🔴 阻塞 (必须修复)

| # | 问题                                                               | 位置                                                                                                                  | 修复建议                                                                                |
| - | ------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| 1 | **Flyway V9 迁移 H2 不兼容**                                 | [V9__seed_hospital_data.sql](backend/src/main/resources/db/migration/V9__seed_hospital_data.sql)                         | 替换 `DATE_ADD(x, INTERVAL n DAY)` → `DATEADD('DAY', n, x)`                        |
| 2 | **Chat Stream 端点 `permitAll` + `requireCurrent` 冲突** | [SecurityConfig.java:52](backend/src/main/java/com/cloudbrain/security/SecurityConfig.java#L52)                          | 移除 `.permitAll()` 或使用 `@AuthenticationPrincipal`                               |
| 3 | **挂号并发回滚时号源泄露**                                   | [WorkflowService.java:387-392](backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java#L387-L392) | 使用 `@Transactional(propagation = REQUIRES_NEW)` 包裹 release 操作或使用数据库触发器 |

#### 🟡 重要 (建议修复)

| #  | 问题                                    | 修复建议                                                  |
| -- | --------------------------------------- | --------------------------------------------------------- |
| 4  | 处方提交无二次确认弹窗                  | 添加 ConfirmDialog 包装 submit 操作                       |
| 5  | Pinia stores 未模块化 (仅 useAuthStore) | 抽取 useRegistrationStore, usePatientStore 等             |
| 6  | WebSocket 断开无自动重连                | 实现指数退避重连                                          |
| 7  | 全局异常处理暴露内部错误信息            | 对 Exception.class 使用通用消息如 "internal server error" |
| 8  | "采纳全部"按钮功能重复                  | 删除或实现独立逻辑（如一键采纳所有 AI 建议字段）          |
| 9  | 取消挂号号源释放幂等性不足              | 添加 slot_released 状态检查                               |
| 10 | auth.ts 乱码字符串                      | 修改为正确的 UTF-8 中文                                   |

#### 🟢 改进建议

| #  | 问题                             | 修复建议                                 |
| -- | -------------------------------- | ---------------------------------------- |
| 11 | CorsConfig 仅允许 localhost:5173 | 使用配置文件允许列表                     |
| 12 | 无请求频率限制                   | 添加 Spring Rate Limiter 或 Resilience4j |
| 13 | AI 调用无熔断器                  | 引入 Resilience4j CircuitBreaker         |
| 14 | 组件 Props 类型为 any            | 定义明确的 TypeScript 接口               |
| 15 | 硬编码路由路径                   | 使用命名路由导航                         |
| 16 | 登录过期跳转无提示               | 跳转前显示 toast                         |

### 9.2 修复优先级路线图

```
Phase 1 (立即): 修复 #1, #2, #3 — 这3个问题阻塞测试和 Chat 功能
Phase 2 (本轮): 修复 #4, #5, #6, #7, #8, #9, #10 — 提升体验和代码质量
Phase 3 (后续): 修复 #11, #12, #13, #14, #15, #16 — 长期质量改进
```

---

## 10. 测试代码改进建议

### 10.1 缺失的测试覆盖

当前项目仅有 3 个测试类，建议添加：

| 测试类                          | 测试对象                                   | 优先级 |
| ------------------------------- | ------------------------------------------ | ------ |
| WorkflowServiceTest             | 分诊、挂号、病历生成、处方审核全部业务逻辑 | 🔴 P0  |
| AuthServiceTest                 | 注册、登录、Token 刷新、登出               | 🔴 P0  |
| AIConfigResolverTest            | AI 配置解析、优先级排序、fallback          | 🟡 P1  |
| PrescriptionReviewTest          | 本地规则引擎 7 类规则检测                  | 🔴 P0  |
| RegistrationLifecycleTest       | 挂号状态机 7 种状态 + 完整流转             | 🔴 P0  |
| SecurityConfigTest              | 端点权限矩阵验证                           | 🟡 P1  |
| PatientHomeView.spec.ts         | 患者端前端组件测试                         | 🟡 P1  |
| DoctorConsultationPanel.spec.ts | 医生端前端组件测试                         | 🟡 P1  |

### 10.2 测试策略建议

1. **修复 Flyway 迁移以启用现有测试** (最高优先级)
2. **为 Service 层编写纯单元测试** (Mock Repository 层)
3. **为 Repository 层编写 @DataJpaTest 集成测试**
4. **为 Controller 层编写 @WebMvcTest 测试**
5. **添加前端 Vitest 组件测试**

---

*报告结束。共评估 60+ 源文件，发现 3 个阻塞问题、10 个重要问题和 6 个改进建议。*
