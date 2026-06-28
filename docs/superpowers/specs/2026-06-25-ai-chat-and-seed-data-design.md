# AI Chat & Seed Data Enhancement — Design Document

**Date:** 2026-06-25
**Status:** Approved
**Scope:** AI Chat Assistant (Doctor + Patient), Workflow AI UX Optimization, Prompt Engineering, Hospital Seed Data

---

## Overview

This design covers two parallel workstreams:

1. **AI Interaction Enhancement** — new standalone AI chat assistant for doctors and patients, streaming UX optimization across all AI-powered clinical workflows, and prompt engineering improvements for all task types.
2. **Hospital Seed Data** — enriching the database from 1 department / 1 doctor / 1 patient to a realistic small clinic with 5 departments, 8 doctors, 25 patients, 15 drugs, schedules, and sample visit records.

---

## Part A: AI Chat Assistant

### A.1 Backend Architecture

#### New Entities

**ChatSessionEntity**

| Field | Type | Notes |
|-------|------|-------|
| id | Long (auto) | PK |
| userId | Long | Actor who owns the session |
| userRole | String | PATIENT or DOCTOR |
| title | String | Auto-generated from first user message (truncated to 50 chars) |
| createdAt | Instant | |
| updatedAt | Instant | |

**ChatMessageEntity**

| Field | Type | Notes |
|-------|------|-------|
| id | Long (auto) | PK |
| sessionId | Long | FK → chat_session.id |
| role | String | USER or ASSISTANT |
| content | TEXT | Message body |
| aiMeta | JSON/TEXT | provider, model, durationMs, traceId (null for user messages) |
| createdAt | Instant | |

#### New JPA Repositories
- `ChatSessionRepository` — findByUserIdAndUserRole, deleteById
- `ChatMessageRepository` — findBySessionIdOrderByCreatedAtAsc

#### New Task Type
- `CHAT` added to `TaskType` enum: temperature=0.5, max_tokens=2000
- Corresponding row in `ai_config` table (default config, enabled)

#### New Service: `ChatService`

```
ChatService
  - listSessions(userId, userRole) → List<ChatSessionEntity>
  - getMessages(sessionId) → List<ChatMessageEntity>
  - createSession(userId, userRole, firstMessage) → ChatSessionEntity
  - deleteSession(sessionId, userId) → void
  - streamChat(sessionId, message, SseEmitter) → void
```

**streamChat flow:**
1. Validate session belongs to user
2. Save user message to DB
3. Load last N messages as conversation context (max 10 rounds)
4. Resolve `ai_config` for `CHAT` task type
5. Resolve `prompt_template` for `CHAT` (system prompt with role awareness)
6. Call `AIInvocationService.chatStream()` → SSE events
7. On `done`: save assistant message to DB with ai_meta, emit final event with messageId
8. On `error`: emit error event to client

#### API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/chat/sessions` | PATIENT \| DOCTOR | List user's sessions |
| POST | `/api/chat/sessions` | PATIENT \| DOCTOR | Create session (body: `{firstMessage}`), returns session with ID |
| GET | `/api/chat/sessions/{id}/messages` | PATIENT \| DOCTOR | Get message history |
| GET | `/api/chat/stream` | PATIENT \| DOCTOR | SSE stream (query: `sessionId`, `message`) |
| DELETE | `/api/chat/sessions/{id}` | PATIENT \| DOCTOR | Delete session and messages |

### A.2 Frontend Architecture

#### Component Tree

```
AiChatLauncher.vue           ← floating button (bottom-right)
  └─ AiChatPanel.vue          ← draggable panel (overlay or sidebar)
       ├─ AiChatHeader.vue     ← title, minimize, close, new-session
       ├─ AiChatHistoryList.vue ← session list (left sidebar within panel)
       ├─ AiChatMessageList.vue ← message area
       │    └─ AiChatMessage.vue ← single message (markdown + cursor)
       └─ AiChatInput.vue      ← textarea + send button + stop button
```

#### Pinia Store: `useChatStore`

```
{
  sessions: ChatSession[]
  currentSessionId: string | null
  messages: ChatMessage[]
  isStreaming: boolean
  streamAbortController: AbortController | null
  
  actions:
    fetchSessions()
    selectSession(id)
    createSession(firstMessage)
    deleteSession(id)
    sendMessage(text)
    stopStreaming()
    appendChunk(text)
    finishMessage(messageId, meta)
}
```

#### Streaming UX
- User sends message → message appears immediately
- Assistant placeholder bubble appears with blinking cursor (`▌`)
- `EventSource` receives SSE `chunk` events → content appended character-by-character
- Markdown rendered progressively (re-render on each significant chunk, debounced 80ms)
- `done` event → cursor removed, message finalized, title auto-generated for first message
- Stop button available during streaming

#### Entry Points
- **Doctor PC**: chat bubble icon in header toolbar → opens `AiChatPanel`
- **Patient Mobile**: "智能分诊" FAB button on home screen → opens `AiChatPanel`

---

## Part B: Workflow AI Interaction Optimization

### B.1 Triage — Card-based Recommendations

- AI-recommended department displayed as a prominent card with icon + confidence badge ("AI 推荐")
- Recommended doctors shown as a list: avatar placeholder + name + title + specialty
- AI reasoning in a collapsible section ("查看 AI 分析依据 ▼")
- Degraded (non-AI) results marked with "本地规则匹配" badge + light-yellow info bar

### B.2 Medical Record Generation — Streaming Field-by-Field

- Each record field fills progressively: chief complaint → present illness → past history → physical exam → diagnosis → treatment plan
- Active field shows blinking underscore cursor animation
- Completed fields show "AI 生成" badge, hover reveals provider/model/duration
- Three action buttons: "采纳全部" / "逐段采纳" (per-field accept/reject) / "重新生成"

### B.3 Diagnosis Suggestions — Tags + Confidence

- Each diagnosis displayed as a colored tag: `[血管性头痛 75%]`, color intensity = confidence
- Each item has "采纳" / "忽略" buttons
- Suggested exam items displayed as a checklist

### B.4 Prescription Review — Dual-Engine Visualization

- Two clearly separated areas:
  - **本地规则引擎**: list of triggered rules with risk labels (红/橙/黄)
  - **AI 分析补充**: natural language explanation with "AI 分析" badge, streamed in real-time
- Final risk level shown as a prominent color bar (green/yellow/orange/red)

### B.5 Global AI Interaction Standards

- All AI-generated content: light-blue/lavender background + blue left border stripe
- Non-streaming AI: skeleton screen / pulse animation loading
- Streaming AI: typewriter cursor effect
- AI failure: degraded error tip, does not block manual operation
- All AI content: copyable; key areas: feedbackable (👍👎)

---

## Part C: Prompt Engineering

### C.1 Migration

New Flyway migration: `V8__optimize_prompt_templates.sql`

### C.2 System Prompt Rewrites

#### TRIAGE
- Role: 急诊分诊护士长 (20 years experience)
- Output: JSON with `recommended_dept`, `recommended_doctors`, `urgency_level`, `reasoning`
- Safety: no definitive diagnosis, urgency flag for critical symptoms
- Few-shot examples with department matching rules

#### MEDICAL_RECORD
- Role: 资深主治医师
- Output: structured sections (chief complaint, present illness, past history, physical exam, preliminary diagnosis, treatment plan)
- Professional medical terminology required
- Missing info flagged as `[待补充]`

#### DIAGNOSIS
- Role: 多学科会诊顾问
- Output: differential diagnosis list with confidence scores
- Separate "likely diagnoses" from "diagnoses to rule out"
- Suggested exam items ordered by priority

#### PRESCRIPTION_REVIEW
- Role: 临床药师
- Dimensions: indication match, dosage appropriateness, drug interactions, contraindications, special populations
- Each dimension: `pass / warning / danger`
- Risk level calculation logic embedded in prompt

### C.3 New Chat Assistant System Prompt

```markdown
你是"云脑医疗助手"，为医生和患者提供专业支持。

## 身份感知
- 医生用户：作为同行顾问，用专业术语交流，辅助临床决策
- 患者用户：作为健康顾问，用语通俗易懂，强调就医必要性

## 核心规则
1. 不提供确定性诊断结论，始终以执业医师判断为准
2. 不推荐具体药品剂量，仅提供一般性药物知识
3. 紧急症状描述，首先建议立即就医或拨打急救电话
4. 未知或不确认的医学问题，诚实说明并建议咨询专科医生

## 医生能力
- 鉴别诊断思路、诊疗指南摘要、药物信息查询
- 病历质量建议、处方合理性讨论
- 医学文献解读方向

## 患者能力
- 症状初步分析、就医科室建议
- 检查项目解释、健康生活方式建议
- 用药注意事项提醒

## 输出风格
- 简洁清晰，分点列举，避免大段文字
- 使用 Markdown 格式化
- 引用权威指南时标注来源
```

---

## Part D: Hospital Seed Data

### D.1 Migration

New Flyway migration: `V9__seed_hospital_data.sql` (runs after V8)

All inserts use `INSERT IGNORE` or `WHERE NOT EXISTS` guards for idempotency.

### D.2 Departments (5 total)

| code | name | type | status |
|------|------|------|--------|
| `internal-medicine` | 内科 | 一级科室 | ACTIVE |
| `cardiology` | 心内科 | 二级科室 | ACTIVE |
| `neurology` | 神经内科 | 二级科室 | ACTIVE |
| `orthopedics` | 骨科 | 一级科室 | ACTIVE |
| `dermatology` | 皮肤科 | 一级科室 | ACTIVE |

### D.3 Doctors (8 total)

| username | name | department_code | title | specialty |
|----------|------|-----------------|-------|-----------|
| doctor01 | 张明远 | internal-medicine | 主任医师 | 内科常见病、疑难杂症 |
| doctor02 | 李心怡 | cardiology | 副主任医师 | 冠心病、高血压、心律失常 |
| doctor03 | 王建华 | neurology | 主任医师 | 头痛、癫痫、帕金森综合征 |
| doctor04 | 陈思远 | neurology | 主治医师 | 脑血管病、眩晕、失眠 |
| doctor05 | 赵刚 | orthopedics | 副主任医师 | 骨折创伤、关节置换 |
| doctor06 | 刘磊 | orthopedics | 主治医师 | 运动损伤、脊柱微创 |
| doctor07 | 孙丽华 | dermatology | 主任医师 | 过敏性皮肤病、银屑病、痤疮 |
| doctor08 | 周晓峰 | dermatology | 主治医师 | 湿疹、荨麻疹、真菌感染 |

All passwords: `doctor123` (BCrypt encoded, same as existing).

### D.4 Patients (25 total)

Mix of genders, ages 6–78, various allergy and medical histories.

| username | name | gender | age | allergies | medical_history |
|----------|------|--------|-----|-----------|-----------------|
| patient01 | 患者一号 | MALE | 30 | 无 | 无 |
| patient02 | 王小明 | MALE | 8 | 无 | 无 |
| patient03 | 刘思琪 | FEMALE | 25 | 青霉素 | 无 |
| patient04 | 陈建国 | MALE | 58 | 无 | 高血压10年 |
| patient05 | 张秀兰 | FEMALE | 67 | 磺胺类 | 糖尿病15年、冠心病 |
| patient06 | 李明 | MALE | 35 | 无 | 无 |
| patient07 | 赵雪 | FEMALE | 28 | 头孢类 | 过敏性鼻炎 |
| patient08 | 孙文博 | MALE | 42 | 无 | 高血脂 |
| patient09 | 周婷婷 | FEMALE | 19 | 无 | 无 |
| patient10 | 吴国栋 | MALE | 55 | 阿司匹林 | 高血压、痛风 |
| patient11 | 郑小雨 | FEMALE | 12 | 无 | 哮喘 |
| patient12 | 冯丽 | FEMALE | 45 | 无 | 甲减 |
| patient13 | 褚志强 | MALE | 62 | 无 | 糖尿病、高血压 |
| patient14 | 蒋芳 | FEMALE | 31 | 无 | 无 |
| patient15 | 沈浩 | MALE | 48 | 碘造影剂 | 冠心病支架术后 |
| patient16 | 韩雪梅 | FEMALE | 72 | 青霉素、头孢类 | 骨质疏松、高血压 |
| patient17 | 杨波 | MALE | 26 | 无 | 无 |
| patient18 | 朱琳 | FEMALE | 38 | 无 | 慢性胃炎 |
| patient19 | 秦汉 | MALE | 70 | 无 | 前列腺增生、高血压 |
| patient20 | 许诺 | FEMALE | 16 | 无 | 无 |
| patient21 | 何勇 | MALE | 52 | 无 | 酒精性肝病 |
| patient22 | 吕萍 | FEMALE | 60 | 磺胺类 | 类风湿关节炎 |
| patient23 | 施伟 | MALE | 22 | 无 | 无 |
| patient24 | 张蕾 | FEMALE | 33 | 无 | 无（妊娠中） |
| patient25 | 许文强 | MALE | 78 | 无 | 高血压、糖尿病、慢性肾病3期 |

All passwords: `patient123` (BCrypt encoded, same as existing).

### D.5 Drugs (15 drugs)

| code | name | specification | dosage_form | manufacturer |
|------|------|---------------|-------------|--------------|
| DRUG-001 | 阿托伐他汀钙片 | 20mg×7片 | 片剂 | 辉瑞制药 |
| DRUG-002 | 苯磺酸氨氯地平片 | 5mg×7片 | 片剂 | 辉瑞制药 |
| DRUG-003 | 阿司匹林肠溶片 | 100mg×30片 | 片剂 | 拜耳医药 |
| DRUG-004 | 盐酸氟桂利嗪胶囊 | 5mg×20粒 | 胶囊 | 西安杨森 |
| DRUG-005 | 卡马西平片 | 100mg×100片 | 片剂 | 诺华制药 |
| DRUG-006 | 布洛芬缓释胶囊 | 300mg×20粒 | 胶囊 | 中美史克 |
| DRUG-007 | 塞来昔布胶囊 | 200mg×6粒 | 胶囊 | 辉瑞制药 |
| DRUG-008 | 氯雷他定片 | 10mg×6片 | 片剂 | 先灵葆雅 |
| DRUG-009 | 糠酸莫米松乳膏 | 5g/支 | 乳膏 | 默沙东 |
| DRUG-010 | 奥美拉唑肠溶胶囊 | 20mg×14粒 | 胶囊 | 阿斯利康 |
| DRUG-011 | 盐酸二甲双胍片 | 500mg×20片 | 片剂 | 中美史克 |
| DRUG-012 | 头孢呋辛酯片 | 250mg×12片 | 片剂 | 葛兰素史克 |
| DRUG-013 | 阿莫西林胶囊 | 500mg×24粒 | 胶囊 | 联邦制药 |
| DRUG-014 | 氯化钠注射液 | 250ml/袋 | 注射液 | 科伦药业 |
| DRUG-015 | 葡萄糖注射液 | 500ml/袋 | 注射液 | 科伦药业 |

### D.6 Schedules (7-day rolling)

Each of the 8 doctors gets schedules for next 7 days from system start date:
- Morning period (AM): `total_slots=30`, some doctors partially filled (15–30 remaining)
- Afternoon period (PM): `total_slots=20`, most available

### D.7 Sample Registrations & Visit Records (~15 total)

Complete patient journey records covering all states:

| Status | Count | Description |
|--------|-------|-------------|
| COMPLETED | 8 | Full flow: triage → registration → consultation → medical record → prescription → review → feedback |
| IN_CONSULTATION | 3 | Mid-visit, consultation note exists |
| WAITING | 2 | Registered, waiting to be called |
| CANCELLED | 2 | Cancelled with reason |

Associated records created for COMPLETED visits:
- TriageRecord (AI or rule-based)
- ConsultationNote
- MedicalRecord (some AI-generated)
- Prescription + PrescriptionItems (2–3 drugs each)
- PrescriptionReview (dual-engine)
- Feedback (with TriageAccuracyFeedback)

### D.8 Implementation Notes

- All seed data delivered via Flyway migration `V9__seed_hospital_data.sql`
- Uses `INSERT IGNORE` or conditional `WHERE NOT EXISTS` to ensure safe re-runs
- BCrypt passwords use the same encoding as existing seeds (copy pattern from `DatabaseSeeder.java`)
- Drug default usage, contraindications, and precautions populated in Chinese
- Prescription rules remain as-is (already seeded via `V1/V6` migrations, or via `DatabaseSeeder`)

---

## Migration Order

1. `V8__optimize_prompt_templates.sql` — prompt rewrites + chat assistant prompt
2. `V9__seed_hospital_data.sql` — full hospital data

Backend code changes (entities, services, controllers, security config) deploy alongside but do not depend on migration order beyond Flyway's sequential execution.

---

## Security

- Chat endpoints gated by JWT authentication
- Users can only access their own sessions (userId + userRole check)
- AI system prompt includes medical safety guardrails
- SSE stream token validation (reuse existing `AiStreamSessionService` pattern or session-based auth)
- Chat messages do not contain PHI beyond what the user voluntarily types

---

## Testing Considerations

- Unit tests: `ChatService`, prompt template resolution, seed data idempotency
- Integration tests: SSE streaming endpoint, chat session CRUD
- Manual verification: launch app, login as doctor → open chat panel, send message → verify streaming
- Seed data: verify via `/api/departments`, `/api/doctors`, `/api/drugs`, etc.
