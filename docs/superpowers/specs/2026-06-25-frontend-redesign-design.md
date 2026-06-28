# 东软智慧云脑诊疗平台 — 前端重构设计文档

**日期**：2026-06-25  
**依据**：[前端交互设计文档](../前端交互设计文档-东软智慧云脑诊疗平台.md)、[技术设计](../../designs-tech/202606221721_cloud-brain-medical-tech-design/tech_v1.md)  
**范围**：仅前端。后端 API 零修改，DTO 和接口协议完全复用。

---

## 1. 目标

将现有功能原型重构为统一设计语言的三端工作台，使患者端在模拟手机上完成分诊到反馈的闭环，医生端和管理端在 PC 工作台上完成各自任务。不改后端代码，不引入逻辑错误。

---

## 2. 设计 Token 系统

### 2.1 色板

以松石绿（Teal）为品牌基因，延续现有 `#116b66` 方向并调优为更温润的明度。

| Token | 色值 | CSS 变量 |
|---|---|---|
| 品牌主色 | `#0D7C73` | `--color-brand` |
| 品牌浅底 | `#E6F4F2` | `--color-brand-soft` |
| 品牌深色 | `#095952` | `--color-brand-deep` |
| 页面底色 | `#F7FAF8` | `--color-surface` |
| 卡片背景 | `#FFFFFF` | `--color-card` |
| 主文字 | `#1B2B2A` | `--color-text` |
| 辅助文字 | `#6B7D7B` | `--color-text-secondary` |
| 边框分割 | `#DEE7E5` | `--color-border` |
| 危险/高风险 | `#DC4E3E` | `--color-danger` |
| 警告/中风险 | `#E8952C` | `--color-warning` |
| 成功/低风险 | `#3D9142` | `--color-success` |
| 信息/蓝色 | `#3B82C5` | `--color-info` |

### 2.2 字体

```css
font-family: "PingFang SC", "Microsoft YaHei", "Noto Sans CJK SC", sans-serif;
```

- 标题：`font-weight: 600`，正文：`font-weight: 400`
- 等宽数字（统计数据）：`"SF Mono", "Consolas", "Menlo", monospace`
- 手机端正文最小 `14px`，表单控件 `16px`（防 iOS 缩放）

### 2.3 圆角

| 元素 | 值 |
|---|---|
| 卡片/面板 | `8px` |
| 按钮 | `6px` |
| 输入框 | `6px` |
| 标签/徽章 | `4px` |
| 手机框壳 | `16px` |

### 2.4 间距

使用 Tailwind CSS 默认 spacing scale（基于 `4px`），常用：
- 卡片内边距：`p-5` (20px)
- 区域间距：`gap-4` (16px) / `gap-6` (24px)
- 工作区外边距：手机端 `px-3`，PC 端 `p-6`

### 2.5 阴影

```css
--shadow-card: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04);
--shadow-elevated: 0 4px 12px rgba(0,0,0,0.08);
--shadow-phone: 0 8px 32px rgba(0,0,0,0.12);
```

---

## 3. 布局架构

### 3.1 全局 Shell

```
┌──────────────────────────────────────────────────────────────┐
│  GlobalTopbar   品牌 · 角色标签 · 通知铃铛 · 健康灯 · 退出   │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─── PatientWorkspace ───┐  ┌── DoctorWorkspace ───────┐   │
│  │      375px 手机框       │  │ ┌SideNav┐ ┌── Router ──┐ │   │
│  │  ┌──────────────────┐  │  │ │总览    │ │            │ │   │
│  │  │   StatusBar      │  │  │ │接诊    │ │   Panel    │ │   │
│  │  │   TabBar (底部)   │  │  │ │历史    │ │            │ │   │
│  │  │   RouterView     │  │  │ │排班    │ │            │ │   │
│  │  └──────────────────┘  │  │ └───────┘ └────────────┘ │   │
│  └────────────────────────┘  └──────────────────────────┘   │
│                                                              │
│  AdminWorkspace = 同上侧栏结构，左侧导航项不同               │
└──────────────────────────────────────────────────────────────┘
```

### 3.2 顶栏（GlobalTopbar）

- 高度：`h-12` (48px)
- 背景：品牌色 `#0D7C73`
- 左侧：平台名称「智慧云脑诊疗平台」
- 中间：当前角色标签（患者/医生/管理员）+ 用户名
- 右侧：通知铃铛（WebSocket 状态指示）、系统健康灯（绿/黄/红）、退出按钮
- 固定在顶部，z-50

### 3.3 患者端 — 手机模拟布局

- 375×700px 手机框居中，带阴影模拟设备感
- 框顶：虚假状态栏（时间 9:41、信号 ▂▄▆█、电池）
- 框底：5 标签 TabBar
- 内容区：`overflow-y-auto`，单列布局，全部组件全宽
- 手机框右侧空白区域可放品牌水印或留白

### 3.4 医生端 & 管理端 — 侧栏工作台

- 左侧导航：`w-52` (208px)，固定不滚动
  - 顶部放角色头像 + 名称
  - 导航项带图标，选中态左边框品牌色高亮
  - 医生端：总览 / 接诊 / 历史 / 排班
  - 管理端：总览 / 基础数据 / 排班与资源 / 配置 / 审计
- 右侧内容：`flex-1`，`overflow-y-auto`，内边距 `p-6`

---

## 4. 路由设计

```text
/                          → 入口导流页（三端角色卡片）
/login                     → 统一登录 / 注册
/health                    → 系统健康详情页

/patient                   → 重定向 /patient/overview
/patient/overview          → 概览面板
/patient/triage            → 分诊面板
/patient/registration      → 挂号面板
/patient/records           → 病历面板
/patient/profile           → 个人资料
/patient/history           → 历史记录

/doctor                    → 重定向 /doctor/overview
/doctor/overview           → 总览面板
/doctor/consultation       → 接诊面板（患者队列）
/doctor/consultation/:id   → 接诊面板（指定患者工作区）
/doctor/history            → 历史面板
/doctor/schedule           → 排班面板

/admin                     → 重定向 /admin/overview
/admin/overview            → 总览面板
/admin/master-data         → 基础数据（科室/医生/药品）
/admin/resources           → 排班与资源
/admin/config              → 配置（规则/AI/Prompt）
/admin/audit               → 审计（AI记录/日志/告警）
```

路由懒加载保持现有 `import()` 模式。导航守卫保持现有 JWT 校验和角色匹配逻辑。

---

## 5. 组件树

### 5.1 完整组件清单

```
App.vue
└── GlobalTopbar.vue                  (品牌、角色、通知、健康灯、退出)
    └── ConnectionBadge.vue           (SSE/WebSocket 连接状态绿灰点)

├── HomeView.vue                      (入口导流页，三端角色卡片)
├── AuthView.vue                      (登录/注册表单)
├── HealthView.vue                    (系统健康详情)

├── PatientWorkspace.vue              (手机框 + TabBar + RouterView)
│   ├── PhoneFrame.vue                (375px 手机外壳)
│   ├── PhoneTabBar.vue               (底部5标签导航)
│   ├── PatientOverviewPanel.vue
│   ├── PatientTriagePanel.vue
│   ├── PatientRegistrationPanel.vue
│   ├── PatientRecordsPanel.vue
│   ├── PatientProfilePanel.vue
│   └── PatientHistoryPanel.vue

├── DoctorWorkspace.vue               (侧栏 + RouterView)
│   ├── SideNav.vue                   (导航树 + 角色信息)
│   ├── DoctorOverviewPanel.vue
│   ├── DoctorConsultationPanel.vue
│   ├── DoctorHistoryPanel.vue
│   └── DoctorSchedulePanel.vue

└── AdminWorkspace.vue                (侧栏 + RouterView)
    ├── SideNav.vue                   (同上结构，导航项不同)
    ├── AdminOverviewPanel.vue
    ├── AdminMasterDataPanel.vue      (科室/医生/药品 列表+编辑)
    ├── AdminResourcesPanel.vue       (排班/号源)
    ├── AdminConfigPanel.vue          (规则/AI配置/Prompt)
    └── AdminAuditPanel.vue           (AI记录/审计日志/告警)

通用组件 (components/shared/)
├── StatusChip.vue                    (状态标签组件)
├── EmptyState.vue                    (空状态：图标 + 标题 + 描述 + 操作)
├── ConfirmDialog.vue                 (二次确认弹窗 Danger/Warning/Info)
├── LoadingSkeleton.vue               (骨架屏容器)
├── SectionCard.vue                   (分区卡片，有标题栏+内容区+可折叠)
├── DashboardCharts.vue               (ECharts 图表，已有，保留)
├── Toast.vue / useToast.ts           (全局 Toast 通知)
├── RiskBadge.vue                     (风险等级徽章：高/中/低)
└── SearchInput.vue                   (带防抖的搜索输入框)
```

### 5.2 组件职责边界

| 组件 | 职责 | 无依赖 |
|---|---|---|
| `StatusChip` | 根据 status string 显示对应颜色和文字 | ✅ |
| `EmptyState` | 三端统一的空状态插画+文案+操作 | ✅ |
| `ConfirmDialog` | 可配置标题/文案/危险等级/确认回调 | ✅ |
| `LoadingSkeleton` | 可配置行数/形状的骨架占位 | ✅ |
| `SectionCard` | 可选标题、可折叠、内容 slot | ✅ |
| `ConnectionBadge` | WebSocket/SSE 连接状态绿点/灰点 | ✅ |
| `RiskBadge` | HIGH/MEDIUM/LOW 对应红/黄/绿色 | ✅ |
| `PhoneFrame` | 纯视觉壳，slot 内容 | ✅ |

---

## 6. 数据流与 Store

### 6.1 Store 规划

当前已有：`useAuthStore`、`useAppStore`（保留，不改）

新增/拆分：

```
useAuthStore          → 已有。认证 session、login/logout/refresh
useAppStore           → 已有。健康检查状态

usePatientStore       → 患者端：分诊结果、挂号列表、病历列表、个人信息
useDoctorStore        → 医生端：队列、当前患者、接诊数据
useAdminStore         → 管理端：各资源列表、选中项、编辑模态状态
useNotificationStore  → 三端 WebSocket 通知，未读数，通知列表
useAIStore            → SSE 流式会话：连接状态、chunk buffer、result
```

### 6.2 数据流方向

```
API (http.ts 拦截器)
  ↓
Store (Pinia action 调用 API，存状态)
  ↓
HomeView / Workspace (从 Store 读取，组织为 workspace 对象)
  ↓
Panel (props 接收数据 + emit 回传事件，不直接调 API)
```

Panel 组件不直接 import API 模块，通过 props 接收数据和回调函数。保持组件纯净，便于测试和复用。

### 6.3 实时数据

- **WebSocket 通知**：`useNotificationStore` 在 `DoctorWorkspace`/`AdminWorkspace` 挂载时连接，卸载时断开
- **SSE 流式**：`useAIStore` 在 `DoctorConsultationPanel` 使用，创建 session → 订阅 EventSource → 缓冲 chunk → 解析 result → 取消 session
- 连接状态统一通过 `ConnectionBadge` 组件展示

---

## 7. 各端面板详细设计

### 7.1 患者端

#### 概览面板
- 顶部问候语「你好，{姓名}」
- 当前挂号卡片（科室、医生、时段、状态）
- 最近分诊结果摘要
- 待办：未就诊挂号数

#### 分诊面板
- 顶部大输入框「请描述您的不适症状...」
- 发送后下方展开结果区：
  - 推荐科室卡片（带匹配度标签）
  - 推荐医生列表（头像、姓名、职称、专长）
  - 可用号源（时段、剩余量）
  - 一键跳转去挂号

#### 挂号面板
- 步骤条：选科室 → 选医生 → 选时段 → 确认
- 确认弹窗（ConfirmDialog）展示挂号详情
- 挂号成功卡片 + 跳转病历

#### 病历面板
- 就诊记录卡片列表（日期、科室、医生、诊断摘要）
- 点击展开：完整病历、处方明细、审核结果、风险提示
- 完成后可提交反馈（星级评分 + 文字）

#### 资料面板
- 分区表单：基本信息 / 联系方式 / 既往史 / 过敏史
- 编辑模式切换，保存二次确认

#### 历史面板
- 分诊历史列表
- 挂号历史列表
- 反馈记录列表

### 7.2 医生端

#### 总览面板
- 指标卡片行：今日接诊 / 待处理 / 高风险 / AI 调用
- 待处理患者队列（来自 `/api/doctor/queue`）
- 最近通知和风险提醒
- DashboardCharts（趋势图，已有组件复用）

#### 接诊面板
- 左侧患者队列列表（可搜索）
- 右侧工作区（可切换 tab）：
  1. 问诊记录（文本编辑 + AI 流式生成病历）
  2. 病历草稿（结构化编辑）
  3. 诊断建议（AI 建议 + 采纳/忽略操作）
  4. 处方编辑（药物搜索 → 添加 → 用量设置）
  5. 审方结果（本地规则 + AI 解释，高风险红色高亮）
  6. 提交就诊（汇总确认 + 二次确认弹窗）
- SSE 流式输出时显示骨架动画，完成后替换为正文

#### 历史面板
- 病历列表（搜索 + 分页，复用现有 API）
- 处方历史列表
- 审方历史列表

#### 排班面板
- 日历视图 / 列表视图切换
- 当前医生的排班和号源

### 7.3 管理端

#### 总览面板
- 核心指标卡片：今日挂号 / 接诊 / 处方 / AI 调用 / 反馈
- DashboardCharts 趋势图（已有）
- AI 调用统计

#### 基础数据面板
- 三 tab：科室 / 医生 / 药品
- 每个 tab：搜索栏 + 列表表格 + 新增/编辑模态窗
- 操作：新增、编辑、启/停用（PATCH toggle）
- 编辑复用现有 `AdminEditorPanel` 逻辑

#### 排班与资源面板
- 号源列表（医生、日期、时段、剩余/总量）
- 批量生成入口
- 启/停用操作

#### 配置面板
- 三 tab：审方规则 / AI 配置 / Prompt 模板
- AI 配置：脱敏展示 API Key（`****` + 后4位），提供密钥轮换操作
- 规则配置：规则类型、适用药品、风险等级、提示文案

#### 审计面板
- 两 tab：AI 调用记录 / 审计日志
- 表格展示：时间、操作人、操作类型、资源、结果
- 过滤和搜索

---

## 8. 状态反馈规范

### 8.1 加载

| 场景 | 实现 |
|---|---|
| 页面首屏 | `LoadingSkeleton`（骨架屏），3-6 行占位 |
| 局部按钮操作 | 按钮文字变化（"保存"→"保存中..."）+ 禁用 |
| SSE 流式输出 | 输入区灰色占位块 + 逐字填充动画 |

### 8.2 空状态

`EmptyState` 组件，三端统一：

```html
<EmptyState
  icon="CalendarDays"      ← Lucide 图标名
  title="暂无挂号记录"       ← 业务明确提示
  description="完成分诊后可在这里查看挂号信息"
  action-label="去分诊"
  @action="router.push('/patient/triage')"
/>
```

每种空状态配置：
- 无挂号：「暂无挂号记录」→ 去分诊
- 无通知：「暂无新消息」
- 无病历：「暂无就诊记录」
- 无号源：「当前科室暂无可用号源，请查看其他科室」
- 无搜索结果：「未找到匹配结果，请尝试其他关键词」

### 8.3 错误与降级

| 场景 | 处理 |
|---|---|
| 网络请求失败 | 控制台不出错 → `Toast.error("网络连接失败，请检查网络后重试")` |
| AI 接口不可用 | 顶部 `ConnectionBadge` 变灰 + 提示"智能辅助暂不可用，已切换至本地模式" |
| WebSocket 断连 | `ConnectionBadge` 变灰 + 5 秒自动重连，不弹窗 |
| 4xx 业务错误 | 显示后端返回的 `message` 字段（中文），不显示状态码 |
| 5xx 服务器错误 | `Toast.error("服务繁忙，请稍后重试")` |
| JWT 过期 | 自动调用 refresh 接口，失败则跳转登录页（已有逻辑，不改） |

### 8.4 确认与成功

- Toast 通知：绿色成功、红色失败、黄色警告，右上角 `fixed`，2 秒自动消失
- 关键操作有 `ConfirmDialog`：
  - 取消挂号："确认取消此挂号吗？取消后号源将释放。"
  - 提交处方："确认提交处方吗？提交后不可撤回。"
  - 结束就诊："确认结束本次就诊吗？"
  - 删除操作："确认删除？此操作不可撤销。"
- 高风险处方结果：红色强调框 + 风险描述 + 仍然需要医生手动确认才可提交

---

## 9. CSS 策略

使用 **Tailwind CSS v4**（`@tailwindcss/vite` 插件，与 Vite 8 兼容）。

- `tailwind.config.ts` 配置色板、字体、间距扩展
- 全局 CSS 仅保留：
  - `@tailwind base/components/utilities`
  - `@layer base` 中的字体和底色
  - 手机框、骨架动画等少量自定义 CSS
- 现有 `base.css`（1200+ 行）完全移除，功能性样式由 Tailwind 替代
- 组件内用 `@apply` 提取重复模式（如 `.btn-primary`、`.card`）

---

## 10. 图标

继续使用 **Lucide Icons** (`lucide-vue-next` v1.x)，MIT 开源，已集成。

图标分配规范：

| 用途 | 图标 |
|---|---|
| 导航-总览 | `LayoutDashboard` |
| 导航-分诊 | `ScanSearch` |
| 导航-挂号 | `Ticket` |
| 导航-病历 | `FileText` |
| 导航-资料 | `UserRound` |
| 导航-历史 | `Clock` |
| 导航-接诊 | `Stethoscope` |
| 导航-排班 | `CalendarDays` |
| 导航-基础数据 | `Building2` |
| 导航-资源 | `Layers` |
| 导航-配置 | `Settings` |
| 导航-审计 | `ShieldCheck` |
| 状态-成功/低风险 | `CheckCircle2` |
| 状态-警告/中风险 | `AlertTriangle` |
| 状态-危险/高风险 | `XCircle` |
| 状态-待处理 | `Clock` |
| 操作-添加 | `Plus` |
| 操作-搜索 | `Search` |
| 操作-编辑 | `Pencil` |
| 操作-删除 | `Trash2` |
| 操作-保存 | `Save` |
| 操作-发送 | `Send` |
| 操作-刷新 | `RefreshCw` |
| 操作-复制 | `Copy` |
| 操作-退出 | `LogOut` |
| 通知 | `Bell` / `BellRing` |
| AI | `Sparkles` |
| 连接状态 | `Wifi` / `WifiOff` |

---

## 11. 不修改项（后端零改动承诺）

以下文件和逻辑**完全不触碰**：

1. `backend/` 下全部 Java 代码
2. `frontend/src/api/http.ts` — Axios 拦截器和 base URL
3. `frontend/src/api/auth.ts` — 认证 API 函数
4. `frontend/src/api/workflow.ts` — 业务 API 函数（所有函数签名保持不变）
5. `frontend/src/api/system.ts` — 健康检查 API
6. `frontend/src/stores/auth.ts` — 认证 Store（逻辑完整，不变）
7. `frontend/src/stores/app.ts` — 健康检查 Store
8. `frontend/src/types/api.ts` — 所有 TypeScript 接口定义
9. `frontend/src/router/index.ts` — 导航守卫逻辑（只新增路由项，不改守卫）
10. `frontend/src/constants/` — 常量定义
11. `frontend/src/utils/zh.ts` — 中文工具函数

---

## 12. 实施顺序

```
Phase 1: 基础设施
  - 安装 Tailwind CSS + 配置
  - 移除 base.css，建立新的全局样式
  - 创建通用组件（StatusChip/EmptyState/ConfirmDialog/LoadingSkeleton/SectionCard）

Phase 2: Shell 层
  - GlobalTopbar 改造
  - SideNav 组件
  - PhoneFrame + PhoneTabBar
  - 更新 App.vue + 路由 + 主布局分发

Phase 3: 患者端
  - PatientWorkspace + 6 面板（逐个实现，保持现有 API 调用）

Phase 4: 医生端
  - DoctorWorkspace + 4 面板

Phase 5: 管理端
  - AdminWorkspace + 5 面板

Phase 6: 收尾
  - Toast 系统
  - ConnectionBadge
  - 入口导流页改造
  - 全局状态反馈覆盖
  - 删除旧 CSS 残留
```

---

## 13. 验收标准

1. 患者端在 375px 手机框内可完成：输入主诉 → 分诊 → 挂号 → 查看病历/处方 → 提交反馈
2. 医生端在侧栏工作台内可完成：选择队列患者 → 问诊 → 病历生成 → 处方审核 → 提交
3. 管理端可维护：科室/医生/药品/排班/审方规则/AI配置/Prompt模板
4. SSE 流式 AI 生成有骨架动画 + 完成替换
5. WebSocket 通知铃铛有未读数和连接状态指示
6. 所有空状态有业务文案 + 引导操作
7. 所有网络错误有中文 Toast 提示
8. 高风险处方有红色高亮 + 二次确认
9. 三端界面风格统一：同样的卡片、按钮、字体、间距、状态颜色
10. 后端零报错，所有现有 API 调用正常工作
