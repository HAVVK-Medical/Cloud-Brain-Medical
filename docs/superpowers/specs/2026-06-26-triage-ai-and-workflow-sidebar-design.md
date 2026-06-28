# 移动端分诊 AI 增强 + AI 工作流侧边栏 — 设计文档

**日期**：2026-06-26  
**关联**：PRD v1.2 §4.3 智能分诊、§5.3 问诊录入、§5.4 AI 病历、§5.6 AI 处方审核、§7.1-7.4 AI 引擎、E01 AI 诊疗建议、E05 SSE 流式

---

## 1. 总体范围

### Feature A：移动端分诊双模式 (P0)
- 保留现有"快速分诊"（文本框 + 按钮 → 一次性结果）
- 新增"对话式 AI 分诊"（多轮追问 → 流式对话 → 精准推荐）
- 两种模式在 `PatientTriagePanel` 内共存，默认快捷入口，可选展开对话

### Feature B：医生端 AI 工作流侧边栏 (P1)
- 在 `DoctorConsultationPanel` 右侧新增可收起侧边栏
- 按步骤组织 AI 能力：Step 1 生成病历 → Step 2 诊断建议 → Step 3 处方审核
- 侧边栏内容为 SSE 流式输出，各步骤可独立触发、重新生成
- 每一步结果可通过"采纳"按钮回填到主工作区对应表单

---

## 2. Feature A：移动端分诊双模式

### 2.1 组件结构

```
PatientHomeView.vue
└── PatientTriagePanel.vue  (重构)
    ├── 标题区："智能分诊"
    ├── QuickTriageSection.vue  (新拆分)
    │   ├── 症状输入 textarea
    │   ├── "智能分诊" 按钮 + loading 状态
    │   └── TriageResultCard（复用现有结果卡片）
    └── ConversationalTriageSection.vue  (新)
        ├── 折叠入口卡片（条件展示：无对话历史时）
        ├── 对话消息列表（ChatMessage 样式复用）
        ├── 追问输入框 + 发送按钮
        └── 内联结果卡片（对话末尾结构化展示）
```

### 2.2 状态新增（workspace 内）

```typescript
// 对话分诊状态
conversationMessages: [] as ChatMessage[],  // 对话历史
conversationActive: boolean,                // 对话模式已开启
conversationStreaming: boolean,             // AI 正在回复
conversationResult: TriageResponse | null,  // 对话最终结构化结果
conversationSessionId: number | null,       // 后端 session ID
```

### 2.3 交互流程

1. 患者进入分诊 tab → 看到快捷输入框 + 下方折叠卡片"需要更详细的 AI 问诊？"
2. **快速路径**：输入症状 → 点击"智能分诊" → 调用现有 `POST /api/triage/consult` → 展示结果（与现有行为完全一致）
3. **对话路径**：点击折叠卡片展开 → 显示 AI 问候 + 输入框 → 患者输入症状 → `GET /api/triage/consult/stream` → SSE 流式显示 AI 的追问内容 → 患者回答 → AI 继续追问 → 3-5 轮后 AI 输出结构化推荐
4. 对话结束后，结果卡片内嵌在消息列表末尾，包含科室/医生推荐 + "去挂号"按钮
5. 两种模式互不阻塞：快速分诊有结果后，对话入口显示"已有快筛结果，AI 详细问诊可更精确"

### 2.4 后端：TriageStreamController

新增 `GET /api/triage/consult/stream`

- 复用 `ChatService` 的 SSE 模式（30s 超时、分块发送、完成事件）
- Task scope: `TRIAGE_CONVERSATION`
- Prompt 模板：追问式分诊指令（"你是分诊护士，需要先了解足够信息再得出结论"）
- 对话历史上下文：最近 10 轮
- 结构化结果标记：AI 在对话末尾输出 `[TRIAGE_RESULT]{"department":"xxx","doctors":[...],"reason":"xxx"}[/TRIAGE_RESULT]`，前端解析
- 安全性：需要 PATIENT 角色认证

### 2.5 Prompt 模板（TRIAGE_CONVERSATION）

```
你是一位经验丰富的分诊护士。你的任务是通过对话了解患者症状，然后给出科室和医生推荐。

规则：
1. 每次只问 1-2 个问题，逐步缩小范围
2. 关键信息包括：主要症状、持续时间、严重程度、伴随症状、既往病史
3. 3-5 轮对话后给出结论
4. 给出结论时，末尾输出标记：[TRIAGE_RESULT]{"department":"科室名","reason":"推荐理由","suggestedQuestions":["追问1","追问2"]}[/TRIAGE_RESULT]
5. 语气亲切、专业，避免使用医学术语
```

### 2.6 错误与降级

| 场景 | 处理 |
|------|------|
| SSE 连接失败 | 显示"对话连接失败，请使用快速分诊或稍后重试"，保留已输入文本 |
| AI 不可用 | 退回快速分诊模式，顶部 notice 提示"AI 详细问诊暂时不可用" |
| 对话超时（60s 无新消息） | 自动关闭 SSE，显示已完成的内容 |
| 解析 [TRIAGE_RESULT] 失败 | 显示原始 AI 回复作为文本结果，不展示结构化卡片 |

---

## 3. Feature B：医生端 AI 工作流侧边栏

### 3.1 组件结构

```
DoctorConsultationPanel.vue  (修改)
├── 患者队列 (240px, 保持不变)
├── 主工作区 (flex-1, 保持不变)
│   ├── 快捷操作按钮（保持不变）
│   ├── 问诊记录区（保持不变）
│   ├── 病历表单（保持不变）
│   ├── 处方编辑（保持不变）
│   └── [缩小后] AI 诊断建议区
│
├── WorkflowSidebar.vue  (新)
│   ├── WorkflowSidebarHeader
│   │   ├── 标题 "AI 工作台"
│   │   ├── 收起按钮
│   │   └── 连接状态指示灯
│   ├── WorkflowStepList.vue
│   │   ├── Step 1: 生成病历 [触发/重试] [状态徽章]
│   │   ├── Step 2: 诊断建议 [触发/重试] [状态徽章]
│   │   └── Step 3: 处方审核 [触发/重试] [状态徽章]
│   └── WorkflowStepDetail.vue
│       └── 当前选中步骤的流式输出区域 + 采纳按钮
│
└── WorkflowToggleButton.vue (新, 固定在右侧边缘)
    └── 点击展开/收起侧边栏
```

### 3.2 交互行为

1. 默认状态：侧边栏收起，右侧边缘显示竖排"AI 工作台"触发按钮
2. 点击触发按钮 → 侧边栏从右侧滑入（宽度 400px），主工作区自适应收缩
3. 侧边栏内显示 3 个步骤，当前未完成步骤为灰色，已完成为绿色
4. 选中一个步骤 → 下方显示详情区，包含：
   - 触发/重新生成按钮
   - SSE 流式输出文字
   - "采纳"按钮（将结果回填到主工作区表单）
5. 再次点击触发按钮或遮罩 → 侧边栏收起
6. 步骤间的数据共享：
   - Step 1 生成的病历可被 Step 2 作为上下文
   - Step 2 的诊断可被 Step 3 作为审核参考

### 3.3 状态管理

现有的 `useAIStreamStore` (ai-stream.ts) 已经管理了流式会话。复用并扩展：

```typescript
// ai-stream.ts 扩展
interface WorkflowStepState {
  step: 'MEDICAL_RECORD' | 'DIAGNOSIS' | 'PRESCRIPTION_REVIEW'
  status: 'idle' | 'streaming' | 'completed' | 'error'
  content: string
  sessionId: string | null
}

// 新增
const workflowSteps: Ref<WorkflowStepState[]>
const sidebarOpen: Ref<boolean>
const activeStep: Ref<string | null>
```

### 3.4 与现有功能的集成

- 现有主工作区中的"AI 生成病历""AI 诊断建议"按钮**保留**，点击后：
  - 如果侧边栏关闭 → 自动打开侧边栏
  - 选中对应步骤 → 开始流式生成
  - 流式输出同时在侧边栏和主工作区现有位置显示
- 处方审核仍然在主工作区底部显示双引擎结果（本地规则 + AI），侧边栏 Step 3 同步显示 AI 分析部分
- 侧边栏关闭时，所有 AI 功能行为与现有完全一致

### 3.5 错误处理

| 场景 | 处理 |
|------|------|
| 生成失败 | 步骤徽章变红，详情区显示错误信息 + "重试"按钮 |
| SSE 断开 | 保留已生成内容，显示"连接中断，已生成内容已保留" |
| 侧边栏打开状态切换患者 | 自动清空步骤状态，重置为 idle |

---

## 4. 路由与权限

- 对话分诊 SSE：`GET /api/triage/consult/stream` — `hasRole('PATIENT')`
- AI 侧边栏使用现有的 AI 调用接口，无需新增路由
- 侧边栏纯前端组件，不影响路由结构

---

## 5. 实现顺序

| 序号 | 任务 | 优先级 | 预估复杂度 |
|------|------|--------|-----------|
| 1 | 后端：TRIAGE_CONVERSATION prompt 模板 + SSE 端点 | P0 | 中 |
| 2 | 前端：PatientTriagePanel 重构 + ConversationalTriageSection | P0 | 高 |
| 3 | 前端：WorkflowSidebar + WorkflowStepList + Detail | P1 | 中 |
| 4 | 前端：DoctorConsultationPanel 集成侧边栏 | P1 | 中 |
| 5 | 联调 + 验证 | - | 中 |

---

## 6. 验收标准

### Feature A
- [ ] 快速分诊模式保持现有功能不变
- [ ] 对话分诊入口可见，可展开/收起
- [ ] 多轮对话可正常流转，AI 追问合理
- [ ] 流式输出顺畅，有打字光标动画
- [ ] 对话结束展示结构化科室/医生推荐
- [ ] "去挂号"按钮可正常跳转
- [ ] AI 不可用时降级提示正确

### Feature B
- [ ] 侧边栏可滑入/滑出，无闪烁
- [ ] 3 个步骤可独立触发
- [ ] 流式输出正确显示
- [ ] "采纳"按钮可回填到主工作区
- [ ] 现有内联 AI 按钮仍可正常工作
- [ ] 切换患者时步骤状态正确重置
