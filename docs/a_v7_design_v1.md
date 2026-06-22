# 架构级 OOD 迭代升级设计（组件 A v7 design v1）

## 概述

本设计用于更新 `docs\系统设计文档-东软智慧云脑诊疗平台.md` 中的架构级 OOD 内容。目标是在保留原文档 Spring Boot 3 + Vue 3 + TypeScript、RESTful API、JWT 鉴权、模块化单体、AI Service 封装、本地处方规则优先等主干方向的基础上，把 PRD v1.2 及补充约束中的全部 P0/P1/P2 需求落到可指导编码的对象职责、协作契约、接口与数据落点、异常边界和并发策略。

平台仍定位为教学实训与课程答辩演示用的三端智能门诊原型系统，不承担真实医疗诊断、治疗或处方责任。P0 必须稳定跑通：管理员维护科室、医生、排班号源、药品、处方规则和 AI 配置；患者注册登录、输入症状、获得分诊推荐、选择号源并挂号；医生查看本人队列、开始接诊、录入问诊、生成并确认病历、审核并提交处方；患者查看自己的病历和处方；管理员追溯 AI 分诊、病历生成和处方审核记录。

PRD 中所有拓展需求都纳入设计。P1/P2 只影响交付批次和是否阻塞 P0 验收，不代表可以在 OOD 中缺席。E01 AI 诊疗建议、E02 就诊评价、E03 数据看板、E04 WebSocket 告警、E05 SSE 流式输出、E06 Pinia 状态机、E07 Prompt 模板、E08 Nginx + Jar 双端部署、E09 Spring Cloud 微服务拆分、E10 远期 HIS 边界均必须有模块归属、核心对象/接口、协作契约、接口/数据落点、异常和边界策略。

本版重点闭合第 6 轮组件 B 已定位的五类问题：一是补全挂号、接诊、病历、处方和评价之间的状态机，明确“保存病历不等于完成挂号”；二是补全 E03 看板的医生端/管理端双视角消费契约；三是补全 E09 四类 AI 微服务的内部接口、DTO、错误和降级映射；四是补全管理端三类 AI 记录、患者端病历/处方、医生端历史处方/审核记录的读侧 VO；五是补全处方规则依据结构、内置演示规则和管理端展示策略。

## 全 PRD 覆盖与拓展能力落位

| 需求范围 | 优先级 | 模块归属 | 核心对象/接口 | 接口与数据落点 | 异常和边界策略 |
|---|---|---|---|---|---|
| 项目初始化、统一响应、全局异常、Swagger、CORS、Axios、JWT | P0 | 认证与通用基础设施、前端基础设施 | `ResultEnvelope`、`GlobalExceptionTranslator`、`JwtAuthenticationFilter`、`ActorContext`、`ApiClient`、`AuthStore` | `/api/auth/**`、Swagger/Knife4j、前端 `src/api`、`src/stores/auth` | 未登录返回 401；角色不足返回 403；统一错误结构；前端 401 统一跳登录 |
| 患者注册、登录、个人信息 | P0 | 认证与患者资料 | `PatientAccount`、`PatientProfile`、`PatientAuthService`、`PatientProfileService` | `patient`/`user_account`；`/api/auth/patient/register`、`/api/auth/patient/login`、`/api/patient/info` | 手机号/用户名唯一；密码加密；患者只能读写本人资料 |
| 医生登录、医生公开列表和详情 | P0 | 认证、基础数据 | `DoctorAccount`、`DoctorProfile`、`DoctorDirectoryQueryService` | `doctor`、`department`；`/api/auth/doctor/login`、`/api/doctors`、`/api/doctors/{id}` | 停用医生不可登录或不可接诊；公开查询只返回可展示字段 |
| 管理员登录、科室/医生/排班/号源维护 | P0 | 管理端基础数据 | `Department`、`DoctorProfile`、`ScheduleSlot`、`ScheduleCapacityPolicy` | `department`、`doctor`、`schedule`；`/api/admin/departments/**`、`/api/admin/doctors/**`、`/api/admin/schedules/**` | 启停状态被业务查询尊重；号源条件扣减；取消只释放一次；历史记录保留快照 |
| 智能分诊、推荐科室医生、分诊记录持久化、降级手动挂号 | P0 | 分诊与挂号、AI 能力与审计 | `TriageCoordinator`、`TriageRecord`、`TriageRecommendation`、`TriageAIClientPort`、`AICallRecord` | `triage_record`、`ai_call_record`；`POST /api/triage/consult`、`GET /api/triage/history` | AI 失败返回手动选择入口；推荐必须匹配有效科室/医生；AI 技术调用记录与分诊业务记录分离 |
| 挂号创建、我的挂号、取消挂号、重复挂号控制 | P0 | 分诊与挂号 | `Registration`、`RegistrationStatus`、`RegistrationLifecyclePolicy`、`RegistrationCommandService` | `registration`、`schedule`；`/api/registration/create`、`/api/registration/list`、`/api/registration/cancel/{id}` | 同一患者同一号源有效挂号唯一；取消边界由状态机控制；并发扣号不超卖 |
| 医生队列、开始接诊、问诊工作区 | P0 | 问诊与病历 | `DoctorWorkQueueQueryService`、`ConsultationApplicationService`、`ConsultationWorkspace`、`ConsultationNote` | `registration`、`consultation_note`；建议补充 `/api/doctor/workbench/queue`、`/api/consultation/{registrationId}/begin`、`/api/consultation/{registrationId}/workspace` | 医生只能处理本人挂号；重复开始接诊幂等；状态不允许时返回状态冲突 |
| AI 病历生成、编辑确认、患者/医生病历列表详情 | P0 | 问诊与病历、AI 能力与审计 | `MedicalRecordDraft`、`MedicalRecord`、`MedicalRecordGenerationService`、`MedicalRecordQueryService`、`MedicalRecordAIClientPort` | `medical_record`、`ai_call_record`；`/api/medical-record/generate`、`/api/medical-record/save`、`/api/medical-record/**` | AI 草稿不自动成为正式病历；保存病历只推进到 `MEDICAL_RECORD_SAVED`；患者只读本人，医生只读接诊范围 |
| 药品库、药品搜索、处方编辑 | P0 | 基础数据、处方与审核 | `DrugCatalogItem`、`DrugSearchQuery`、`PrescriptionDraft`、`PrescriptionItem` | `drug`、`prescription`、`prescription_item`；`/api/admin/drugs/**`、`/api/drugs/search` | 下架药品不可新开；历史处方保留药品快照；药品搜索支持编码、名称、拼音、适应症等条件 |
| 处方规则维护、本地规则审核、大模型解释、处方提交 | P0 | 基础数据、处方与审核、AI 能力与审计 | `PrescriptionRuleDefinition`、`RuleBasis`、`RuleDefinitionCompiler`、`LocalRuleEngine`、`PrescriptionReview`、`PrescriptionReviewApplicationService`、`PrescriptionReviewAIClientPort` | `prescription_rule_definition`、`prescription_review`、`ai_call_record`；`/api/admin/prescription-rules/**`、`/api/prescription/check`、`/api/prescription/create` | 本地规则先于 LLM；规则不可用不得伪装低风险；提交必须绑定有效审核或人工确认；处方快照不一致强制重审 |
| AI 配置与 AI 记录查看 | P0 | 管理端基础数据、AI 能力与审计 | `AIConfig`、`AIConfigApplicationService`、`AIProviderResolver`、`SecretCipher`、`AICallRecordQueryService` | `ai_config`、`ai_call_record`；`/api/admin/ai-config/**`、`/api/admin/ai-records/**` | API Key 加密只写不回显；配置缺失/禁用/超时/解析失败写入降级状态；管理端记录脱敏展示 |
| E01 AI 诊疗建议 | P1 | AI 增强与问诊病历 | `DiagnosisSuggestionService`、`DiagnosisSuggestionRecord`、`SuggestedDiagnosis`、`SuggestedExamItem`、`DiagnosisSuggestionAdoption` | `diagnosis_suggestion_record`；`POST /api/diagnosis/suggest`、建议补充采纳接口 | 诊疗建议仅供参考；建议检查项目不自动创建检查检验医嘱；失败不影响手动诊断和病历保存 |
| E02 就诊评价与分诊准确度反馈 | P1 | 反馈子模块、分诊与挂号读侧 | `VisitFeedbackService`、`VisitFeedback`、`TriageAccuracyFeedback`、`TriageRecordReader` | `visit_feedback`/`triage_accuracy_feedback`；`POST /api/feedback/create`、`GET /api/feedback/registration/{id}` | 仅本人且挂号 `COMPLETED` 后评价；同一挂号一条有效评价；缺少分诊记录时标记手动来源 |
| E03 数据看板 | P1 | 看板读侧 | `DashboardQueryService`、`DashboardMetricReader`、`MetricScopePolicy`、`DashboardOverviewVO`、`AIUsageStatsVO`、`PrescriptionReviewRateVO`、`RiskDistributionVO`、`TriageAccuracyVO`、`TrendPointVO` | `/api/dashboard/**`；读 `registration`、`triage_record`、`ai_call_record`、`prescription_review`、`visit_feedback`、`notification_record` | 医生看本人，管理员看全院；无数据返回零值；`UNKNOWN` 单独统计；定时/手动刷新只重读聚合 |
| E04 WebSocket 高风险用药实时告警 | P1 | 实时通知 | `RiskAlertPublisher`、`RiskAlertEvent`、`RiskAlertType`、`DoctorNotificationGateway`、`NotificationRecord`、`RiskAlertNotificationVO` | STOMP `/ws`、`/user/queue/risk-alerts`、`/topic/admin/risk-alerts`、`notification_record`、通知补拉 API | `HIGH_RISK_PRESCRIPTION` 与 `REVIEW_UNCERTAIN_MANUAL_REQUIRED` 分离；推送失败不回滚审核；越权订阅拒绝 |
| E05 SSE 流式病历生成与诊疗建议 | P1 | 流式 AI | `AIStreamSessionService`、`StreamGenerationSession`、`MedicalRecordStreamService`、`DiagnosisSuggestionStreamService`、`AIStreamEventDTO` | `POST /api/ai-stream-sessions`、`GET /api/ai-stream-sessions/{id}/events`、`DELETE /api/ai-stream-sessions/{id}`；写 `AICallRecord` 与业务记录 | 短期 stream token；超时/取消/中断通过事件返回；失败可退回普通 POST；流式草稿不自动保存正式记录 |
| E06 Pinia 状态机模块化 | P1 | 前端状态层 | `useAuthStore`、`usePatientStore`、`useDoctorStore`、`useAdminStore`、`useRegistrationStore`、`useScheduleStore`、`usePrescriptionStore`、`useMedicalRecordStore`、`useAIStore`、`useDashboardStore`、`useNotificationStore` | `src/stores/**`、`src/types/api.ts`、枚举常量；消费 P0/P1 REST、SSE、WebSocket | 前端只反映后端权威状态；所有 Store 有 loading/error/degraded；不得绕过服务端状态推进 |
| E07 可配置 Prompt 模板 | P1 | AI 能力与管理端 | `PromptTemplateService`、`PromptTemplateRepository`、`PromptRenderer`、`PromptVariablePolicy`、`PromptTemplateVersion` | `prompt_template` 或启动 YAML；`/api/admin/prompt-templates/**`；`AICallRecord.promptVersion` | 模板缺失使用默认模板或降级；变量非法拒绝启用；API Key 和敏感隐私不进入模板或前端 |
| E08 Nginx + Jar 双端部署 | P1 | 部署基础设施 | `DeploymentProfile`、`ApiBaseUrlResolver`、`CorsPolicyConfig`、`HealthCheckController` | `application-prod.yml`、前端 `.env.production`、Nginx `/api/` 反向代理、健康检查 API | 代理/CORS/健康检查失败可诊断；生产 profile 不展示明文敏感配置 |
| E09 Spring Cloud 微服务拆分 | P1 | AI 微服务边界与服务治理 | `TriageAIApplicationService`、`DiagnosisAIApplicationService`、`PrescriptionReviewAIApplicationService`、`MedicalRecordAIApplicationService`、`RemoteAIServiceAdapter`、`AIServiceFallback`、OpenFeign Client | serviceId：`triage-ai-service`、`diagnosis-ai-service`、`prescription-review-ai-service`、`medical-record-ai-service`；`/internal/ai/v1/**`；核心服务本地写业务记录 | 不引入分布式事务；内部鉴权、traceId、超时、熔断、重试统一；远程失败映射为本地降级结果 |
| E10 远期 HIS 能力边界 | P2/远期 | HIS 防腐层 | `HISGateway`、`PatientIdentityMapper`、`OrderSettlementBoundary`、`ExamLabOrderBoundary`、`PharmacyDispenseBoundary`、`DrugStockTransaction`、`HISSyncRecord` | `his_sync_record`、药库交易台账、外部 ID 映射字段、管理端 HIS 配置/同步/药库交易查询重试 API；默认 Disabled/Mock | 未启用不影响 P0/P1；检查检验需医生显式选择并进入独立边界；外部失败只写同步状态或补偿 |

## 模块划分

后端采用模块化单体作为 P0 基线，保留 Controller、Application Service、Domain Object、Repository、Gateway/Adapter 分层。Controller 是 HTTP 适配层，只做参数形态校验、鉴权入口和统一响应转换；Application Service 是用例编排层，负责事务边界、权限校验、状态推进和跨模块协作；Domain Object 表达挂号、病历、处方、审核、规则、AI 配置等业务不变量；Repository 封装持久化；Gateway/Adapter 隔离外部 AI、远期 HIS、微服务远程调用和基础设施细节。

认证与用户上下文模块提供 `ActorContext`、`RolePolicy` 和三端身份解析。所有业务模块必须通过 `ActorContext` 判断患者本人、医生归属和管理员权限，不信任前端传入的 `patientId` 或 `doctorId`。

基础数据模块维护科室、医生、排班号源、药品、处方规则、AI 配置和 Prompt 模板。它向分诊、挂号、处方审核和 AI 能力模块提供有效数据视图和版本化配置快照。停用医生、停用排班、下架药品、停用规则、禁用 AI 配置都必须被业务查询尊重；历史挂号、处方、审核和 AI 调用继续展示当时快照。

分诊与挂号模块负责症状分诊、推荐落地、号源查询、挂号创建、取消和重复挂号控制。它不拥有科室/医生主数据，只读取基础数据有效视图。`TriageRecord` 是业务记录，不能用 `AICallRecord` 替代；`AICallRecord` 记录技术调用，`TriageRecord` 记录患者看到和可追溯的业务推荐。

问诊与病历模块负责医生工作台、患者队列、开始接诊、问诊上下文、AI 病历草稿、正式病历确认和患者病历读侧。`MedicalRecordDraft` 与 `MedicalRecord` 分离，AI 草稿不得直接成为正式病历。保存正式病历只表示病历已确认和可开方，不表示一次挂号已完成。

处方与审核模块负责药品选择、处方草稿、处方规则引擎、本地规则命中、大模型解释、人工确认降级、处方提交和审核绑定。处方提交必须绑定一个有效 `PrescriptionReview` 或显式人工确认的降级审核记录，不能绕过审核直接创建正式处方。处方提交或医生显式结束就诊才可把挂号推进到完成。

AI 能力与审计模块负责任务级 AI 接口、供应商解析、AI 配置读取、Prompt 渲染、调用记录和降级响应。业务模块依赖 `TriageAIClientPort`、`MedicalRecordAIClientPort`、`DiagnosisAIClientPort`、`PrescriptionReviewAIClientPort` 等任务级端口，任务级端口再由本地适配器或 E09 远程适配器实现。

模块六承载全部 P1/P2 拓展能力，并保持子模块边界。AI 建议子模块依赖问诊上下文和 AI 任务端口；反馈子模块依赖挂号完成状态和分诊业务记录；看板子模块只读 P0、反馈、通知和 AI 调用记录；实时通知子模块订阅处方审核风险事件并写通知记录；流式 AI 子模块用“创建会话 + EventSource 订阅”支撑病历和诊疗建议；Prompt 模板子模块服务所有 AI 任务；前端 Pinia 状态机属于前端应用架构；部署、微服务和 HIS 边界属于基础设施与集成架构。模块六不得让 P0 主链路直接依赖 WebSocket、SSE、看板、Spring Cloud 或真实 HIS。

前端三端保持职责隔离。患者端只消费自己的挂号、病历、处方和评价；医生端围绕患者队列、接诊工作区、病历和处方组织页面；管理端维护基础数据、AI 配置、Prompt、规则，并查看 AI 调用、分诊记录、审核记录、通知和 HIS 同步。共享 Axios、JWT、错误处理、loading、确认弹窗和 TypeScript 类型，但业务状态命名必须与后端枚举一致。

## 核心抽象

### 身份、基础数据与闭环锚点

`ActorContext` 是当前操作者值对象，封装账号、角色、患者/医生/管理员业务身份和可访问范围。它与 `RolePolicy` 协作，把“谁能查看、开始接诊、保存病历、审核处方、提交处方、查看追溯、接收告警、提交评价”放在应用层判断。`RolePolicy` 采用接口形态，便于 P0 简单 RBAC 和后续授权范围扩展共存。

`Department` 和 `DoctorProfile` 是基础数据实体，支撑公开科室/医生列表、分诊推荐匹配、挂号可选医生、医生登录和管理端维护。它们承担启停、展示快照和科室归属约束，不承担挂号状态推进或诊疗决策。

`ScheduleSlot` 是号源一致性聚合，表达医生在某出诊日期、午别/时间段和挂号级别下的总号源、剩余号源、启停状态和版本。它与 `Registration` 协作时只暴露条件扣减和条件释放语义：扣减成功表示号源仍有效且余量大于零；释放只能发生在可取消挂号首次取消成功之后。

`Registration` 是 P0 诊疗闭环锚点，连接患者、医生、科室、排班、分诊业务记录、问诊、病历、处方、评价、看板统计和远期 HIS 同步。它持有闭合的 `RegistrationStatus`，并保存挂号时科室、医生、职称、出诊日期、时间段和挂号级别快照。若患者由分诊进入挂号，必须保存 `triageRecordId`；若患者手动选择，也必须形成 `MANUAL` 来源记录或等价来源标记。

`DrugCatalogItem` 是药品目录聚合，承载药品编码、名称、拼音助记码、规格、剂型、包装单位、生产厂家、单价、默认用法、禁忌摘要、适应症、相互作用摘要和状态等处方所需信息。`DrugSearchQuery` 与 `DrugSearchResultVO` 是医生端和管理端共享的读侧契约。已下架药品不得被新处方选择，但历史处方项继续展示当时药品快照。

### 挂号/接诊/病历/处方状态机

`RegistrationStatus` 是一次挂号从预约到诊疗闭环完成的权威状态。建议枚举为 `WAITING`、`IN_CONSULTATION`、`MEDICAL_RECORD_SAVED`、`PRESCRIPTION_REVIEWED`、`PRESCRIPTION_SUBMITTED`、`COMPLETED`、`CANCELLED`。如实现需要保留旧 `COMPLETED` 字段，必须调整语义为“就诊完成”，不得再由保存病历直接触发。

| 状态 | 语义 | 允许主要操作 | 禁止或边界 |
|---|---|---|---|
| `WAITING` | 患者已挂号，尚未由医生开始接诊 | 患者取消挂号；医生开始接诊；患者/医生查看 | 不允许保存病历、审核处方、提交处方、评价 |
| `IN_CONSULTATION` | 医生已开始接诊，问诊工作区打开 | 录入问诊；生成 AI 病历草稿；保存正式病历；必要时医生结束无处方就诊 | 患者取消；处方提交前需满足病历或诊断上下文要求 |
| `MEDICAL_RECORD_SAVED` | 正式病历已保存，进入可开方阶段 | 开处方草稿；处方审核；重新保存病历并使旧审核失效；医生结束无处方就诊 | 不代表就诊完成；评价入口不开启 |
| `PRESCRIPTION_REVIEWED` | 最近一次处方草稿已通过审核或进入人工确认降级 | 修改处方后重新审核；提交处方；查看审核详情 | 若处方或审核上下文变化，旧审核不可用于提交 |
| `PRESCRIPTION_SUBMITTED` | 正式处方已提交并绑定审核记录 | 由提交处方用例自动或医生结束动作推进到完成；患者查看处方 | 不允许取消挂号；评价仍以 `COMPLETED` 为开放状态 |
| `COMPLETED` | 本次就诊已完成，患者可评价，患者端病历处方均可查看 | 患者评价；患者/医生/管理员历史查看；看板统计完成就诊数 | 终态，不再允许修改挂号状态；更正病历/处方需独立更正流程 |
| `CANCELLED` | 患者或系统在可取消阶段取消挂号 | 历史查看；取消后患者可重新挂同一号源或其他号源 | 终态，不允许接诊、病历、处方、评价；号源只释放一次 |

状态流转由明确动作触发，而不是由页面跳转或前端字符串决定。

| 触发动作 | 前置状态 | 后置状态 | 关键前置条件 | 幂等和冲突规则 |
|---|---|---|---|---|
| 创建挂号 | 无 | `WAITING` | 患者已登录；号源有效且有余量；同一患者同一 `schedule_id` 无有效挂号 | 重复提交若已创建同等挂号，可返回既有挂号摘要；并发唯一约束兜底 |
| 取消挂号 | `WAITING` | `CANCELLED` | 当前患者本人；未开始接诊；未取消过 | 重复取消返回已取消或幂等成功；不得重复释放号源 |
| 开始接诊 | `WAITING` | `IN_CONSULTATION` | 当前医生为挂号医生；挂号未取消 | 同一医生重复开始返回当前工作区；非归属医生返回 403/状态冲突 |
| 保存正式病历 | `IN_CONSULTATION` 或 `MEDICAL_RECORD_SAVED` | `MEDICAL_RECORD_SAVED` | 当前医生归属；病历结构化字段通过校验 | 重复保存更新病历版本；若处方已审核但病历影响审核上下文，旧审核失效 |
| 完成处方审核 | `MEDICAL_RECORD_SAVED` 或 `PRESCRIPTION_REVIEWED` | `PRESCRIPTION_REVIEWED` | 处方草稿存在；规则引擎完成或进入人工确认降级 | 同一处方快照可复用最近审核；处方内容变化必须新建审核 |
| 提交处方 | `PRESCRIPTION_REVIEWED` | `PRESCRIPTION_SUBMITTED` 或直接 `COMPLETED` | 有有效 `reviewId`；处方快照哈希和上下文哈希一致；医生确认风险 | 重复提交同一审核返回既有处方；同一审核只能绑定一次 |
| 医生结束就诊 | `MEDICAL_RECORD_SAVED` 或 `PRESCRIPTION_SUBMITTED` | `COMPLETED` | 至少有正式病历；若存在处方草稿则必须已提交或明确放弃 | 重复结束返回已完成；未保存病历不得完成 |
| 自动完成就诊 | `PRESCRIPTION_SUBMITTED` | `COMPLETED` | 处方提交事务成功，且业务选择“提交处方即完成本次就诊” | 自动完成需在 API 与时序图中明确；否则由结束就诊动作触发 |

允许操作矩阵必须进入目标系统设计文档，供后续前后端实现统一判断。

| 操作 | `WAITING` | `IN_CONSULTATION` | `MEDICAL_RECORD_SAVED` | `PRESCRIPTION_REVIEWED` | `PRESCRIPTION_SUBMITTED` | `COMPLETED` | `CANCELLED` |
|---|---|---|---|---|---|---|---|
| 患者取消挂号 | 允许 | 禁止 | 禁止 | 禁止 | 禁止 | 禁止 | 幂等查看 |
| 医生开始接诊 | 允许 | 幂等 | 禁止 | 禁止 | 禁止 | 禁止 | 禁止 |
| AI 生成病历草稿 | 禁止 | 允许 | 允许 | 允许但可能使旧审核失效 | 只读或更正流程 | 只读或更正流程 | 禁止 |
| 保存正式病历 | 禁止 | 允许 | 允许 | 允许但旧审核失效 | 只读或更正流程 | 只读或更正流程 | 禁止 |
| 处方审核 | 禁止 | 需至少有诊断上下文 | 允许 | 允许重审 | 禁止或更正流程 | 禁止或更正流程 | 禁止 |
| 提交处方 | 禁止 | 禁止 | 禁止 | 允许 | 幂等查看 | 禁止 | 禁止 |
| 结束就诊 | 禁止 | 可结束无处方但需病历 | 允许无处方完成 | 需提交或放弃处方草稿 | 允许/自动 | 幂等 | 禁止 |
| 患者评价 | 禁止 | 禁止 | 禁止 | 禁止 | 禁止 | 允许 | 禁止 |
| 患者查看病历 | 可无记录 | 可无记录 | 允许 | 允许 | 允许 | 允许 | 可无记录 |
| 患者查看处方 | 可无记录 | 可无记录 | 可无记录 | 可无正式处方 | 允许 | 允许 | 可无记录 |

可取消边界固定为 `WAITING`。一旦医生开始接诊进入 `IN_CONSULTATION`，患者端取消挂号必须被拒绝，避免释放已占用诊疗资源。若确需管理员强制取消，应作为独立管理更正流程，不复用患者取消接口。

评价开放条件固定为 `RegistrationStatus.COMPLETED` 且当前患者为挂号患者本人。`PRESCRIPTION_SUBMITTED` 如果未自动推进到 `COMPLETED`，不得开放评价。`COMPLETED` 是就诊终态，不是病历保存状态。

目标文档中的“保存病历即完成挂号”必须同步修正：模块二职责从“保存病历 -> 标记挂号完成”改为“保存病历 -> 推进到病历已确认/可开方”；模块二时序图中 `saveMR` 后的 `updateStatus completed` 改为 `updateStatus MEDICAL_RECORD_SAVED`；API 总览中 `/api/medical-record/save` 的说明改为“保存正式病历并推进到可开方”；新增或明确 `/api/consultation/{registrationId}/complete` 或处方提交后的完成动作，才可推进 `COMPLETED`。

### 管理端 AI 配置与 AI 审计

`AIConfig` 是管理端维护的 AI 供应商配置聚合或配置实体，对应 `ai_config` 表。它承担供应商、模型名、接口地址、启停状态、默认配置标记、任务适用范围、超时、配置版本、API Key 密文、密钥轮换时间、健康检查状态和审计信息的领域约束。它不直接暴露明文 API Key；业务层只能获取已解密但受控的供应商调用凭据。

`AIConfigApplicationService` 是管理端 AI 配置用例服务，负责配置列表、详情、创建、更新、启停、设置默认 Provider、API Key 写入/轮换、健康检查触发和审计记录。它采用 Application Service 形态，因为配置变更涉及权限、敏感字段、默认唯一性、加密、校验和供应商解析刷新，不应散落在 Controller 或 Provider 工厂中。

`AdminAIConfigWriteDTO` 是管理端写入契约，表达非敏感配置字段和可选 API Key 写入/更新意图。API Key 字段只允许写入或替换，空值表示不修改现有密钥，显式轮换动作生成新的密钥版本；不允许通过普通详情接口读回明文。`AdminAIConfigVO` 是管理端读侧契约，展示供应商、模型、接口地址、启停状态、默认标记、任务类型、超时、健康检查摘要、配置版本、最后更新时间和 API Key 脱敏摘要，不包含密文或明文。

`SecretCipher` 是敏感字段加解密接口，封装 API Key 加密存储和受控解密。它适合接口形态，因为开发环境可用本地密钥或 Mock，生产演示可接入环境变量或 KMS。`AIConfig` 只保存密文和密钥版本，`AICallRecord` 不保存明文凭据。

`AIProviderResolver` 根据任务类型、默认配置、启用状态和健康状态选择 `AIProvider`。默认 Provider 选择规则必须稳定：优先使用任务显式指定且启用的配置；其次使用该任务类型或全局默认配置；若存在多个默认，配置校验应阻止启用；若无可用配置，返回可识别的禁用结果而不是抛出未处理异常。`AIProviderResolver` 不执行具体远程调用，只解析可用 Provider 与调用参数。

`AICallRecord` 是技术调用审计实体，记录任务类型、业务关联 ID、操作者、provider、model、配置版本、prompt 版本、输入摘要、输出摘要、状态、错误摘要、耗时、traceId 和降级状态。它不替代 `TriageRecord`、`MedicalRecord`、`DiagnosisSuggestionRecord` 或 `PrescriptionReview` 等业务记录。

`AIFallbackStatus` 或等价枚举用于统一 AI 降级状态。配置缺失、禁用、密钥缺失、配置校验失败映射为 `AI_DISABLED`；调用超过配置超时映射为 `AI_TIMEOUT`；远程接口返回不可达、5xx、鉴权失败或供应商服务异常映射为 `REMOTE_AI_UNAVAILABLE`；模型输出格式无法解析映射为 `AI_PARSE_FAILED`。这些状态必须写入 `AICallRecord`，并进入对应业务记录的降级字段。

### 分诊、问诊与 AI 建议

`TriageCoordinator` 是智能分诊用例服务，负责接收症状文本、校验长度与安全边界、调用 `TriageAIClientPort`、把 AI 输出匹配到有效科室/医生、执行规则或手动降级、保存 `TriageRecord`，并返回可直接进入挂号页面的推荐 VO。它不拥有科室/医生主数据，只依赖基础数据模块的有效视图。

`TriageRecommendation` 是分诊推荐值对象，表达推荐科室、推荐医生列表、推荐理由、推荐来源和置信/排序语义。它保存科室 ID/名称快照、医生 ID/姓名/职称/擅长方向快照以及挂号入口所需引用。它是值对象，因为生命周期依附于 `TriageRecord`，历史推荐不应随主数据改名而变化。

`TriageRecord` 是分诊业务记录实体，保存患者、症状摘要、推荐科室/医生快照、推荐理由、推荐来源 `AI/RULE/MANUAL`、AI 状态、降级状态、`aiCallRecordId`、创建时间和后续 `registrationId`。管理端分诊追溯 VO 同时展示 `TriageRecord` 的业务推荐快照和 `AICallRecord` 的技术调用状态。

`ConsultationApplicationService` 是接诊用例编排服务，负责医生队列、开始接诊、问诊工作区、正式病历保存和状态推进。`ConsultationWorkspace` 是工作区读模型，组合患者摘要、挂号上下文、问诊记录、病历草稿、正式病历、AI 诊疗建议入口、处方入口和可执行动作。

`ConsultationNote` 是问诊上下文记录，承载医生录入的医患对话、主诉摘要、初步诊断方向、必要患者上下文和关联挂号。它为 AI 病历生成、E01 诊疗建议和处方审核患者上下文提供稳定输入，也让后续采纳记录能够追溯建议产生时的上下文。

`MedicalRecordDraft` 是 AI 生成的可编辑病历草稿值对象；`MedicalRecord` 是医生确认后的正式病历实体。草稿与正式记录分离，体现 AI 辅助而非自动确诊。正式病历保存后推进 `Registration` 到 `MEDICAL_RECORD_SAVED`，并可触发旧处方审核失效。

`DiagnosisSuggestionService` 是 E01 诊疗建议用例服务，基于 `ConsultationNote`、患者摘要、当前诊断方向和 AI 任务端口生成结构化建议。它负责写入 `DiagnosisSuggestionRecord`，记录建议列表、建议检查项目、AI 调用、降级状态和医生后续采纳状态。

`DiagnosisSuggestionRecord` 是诊疗建议业务记录，关联挂号、患者、医生、问诊快照、建议疾病列表、建议检查项目列表、置信度、依据摘要、`aiCallRecordId`、采纳状态、最终诊断方向、采纳人和采纳时间。它不是正式诊断，也不替代 `MedicalRecord`。

`SuggestedDiagnosis` 是单条可能疾病建议的值对象，表达疾病名称、依据、置信或排序、风险提示和后续建议。`SuggestedExamItem` 是建议进一步检查项目值对象，表达检查项目名称、建议原因、优先级或提示、关联疾病线索和展示摘要。`SuggestedExamItem` 只存在于诊疗建议记录中，不自动生成检查检验医嘱。

### 处方规则、审核与提交

`PrescriptionDraft` 是医生提交审核前的处方编辑状态，可以由前端临时保存，也可以由后端以草稿记录表达。它组合处方项、诊断方向、患者用药上下文和审核状态。它不是正式处方，不出现在患者正式处方列表中。

`Prescription` 是正式处方聚合，关联挂号、患者、医生、正式病历或诊断上下文、药品明细和审核记录。它只接受来自有效药品目录的药品引用，并保存药品名称、规格、剂型、单位、生产厂家、单价、默认用法等历史快照。

`PrescriptionItem` 是处方内药品明细值对象，生命周期附属于处方，表达药品引用、用法、剂量、频次、疗程、数量和备注等开方语义。它适合值对象形态，因为其存在意义依赖处方，不能独立转移所有权。

`PatientMedicationContext` 是处方审核患者个体上下文值对象，组合年龄/年龄段、性别、体重或缺失标记、过敏史、既往史、特殊人群标签、诊断方向、科室、必要生命体征或缺失项、当前病历摘要和来源优先级。影响规则判断的上下文字段必须纳入审核上下文哈希或审核快照，避免提交时上下文被替换。

`PrescriptionSnapshot` 是审核绑定快照值对象，包含规范化后的处方项、药品快照、诊断方向、患者用药上下文摘要和排序稳定的哈希。后端负责生成和比对快照哈希，前端只携带 `reviewId`，不能自行声明审核有效。

`PrescriptionReview` 是处方审核业务记录聚合，保存 `reviewId`、关联挂号/医生/患者、处方快照哈希、审核上下文哈希、规则引擎状态、本地规则命中快照、风险等级、LLM 解释摘要、AI 调用状态、人工确认说明、绑定状态、绑定处方 ID 和创建时间。它既支撑提交校验，也支撑患者、医生和管理员追溯。

`PrescriptionRuleDefinition` 是管理端可维护的规则定义聚合，对应 `prescription_rule_definition`。它表达规则类型、适用药品/药品组/疾病/人群/上下文字段、条件表达、风险等级、提示文案、处置建议、启停、版本、校验状态、是否内置演示规则和依据集合。它不是可执行规则本身，而是可读、可版本化、可审计的规则定义。

`RuleBasis` 是处方规则依据值对象，用于补齐规则可解释性和答辩展示。其结构应覆盖 `basisType`、`basisTitle`、`basisClause`、`basisSummary`、`sourceDrugField`、`sourceReference`、`seeded`、`version` 等语义。`basisType` 可表达药品说明书、处方审核规范、处方管理办法、处方点评管理规范、教学演示规则等来源类型；`sourceDrugField` 指向禁忌、适应症、相互作用、剂量范围、特殊人群、疗程等药品说明书字段。

`RuleCondition` 是规则定义中的条件表达值对象，表达规则适用范围和触发条件。它不暴露代码表达式，而通过可校验的条件结构描述药品、患者上下文、诊断、剂量、频次、疗程、相互作用等判断要素。

`RuleDefinitionCompiler` 是规则定义到运行时规则的桥接组件，负责启用前校验、版本冻结和运行时规则构造。它采用组件/服务形态而非领域实体，因为其职责是解释配置、校验引用和构造执行计划，不拥有业务生命周期。

`RuleEvaluationContext` 是本地规则引擎输入值对象，组合处方快照、患者用药上下文、药品目录快照和可用规则版本集合。它是处方审核的稳定输入边界，确保规则引擎不直接读取零散数据库状态。

`RuleHit` 是规则命中值对象，表达命中规则 ID、规则版本、规则类型、风险等级、提示文案、处置建议、依据摘要、命中的药品项、上下文缺失项和展示顺序。它会作为 JSON 快照写入 `PrescriptionReview`，历史展示不依赖当前规则定义是否已修改。

`LocalRuleEngine` 是本地规则执行端口或组件，输入 `RuleEvaluationContext`，输出规则命中集合、风险等级和规则引擎状态。它应无状态或只持有只读缓存，并且必须在 LLM 解释之前执行。LLM 只解释本地规则命中和上下文，不负责替代本地规则。

`PrescriptionReviewApplicationService` 是审核用例编排服务，负责构造患者用药上下文、生成处方快照、执行本地规则、调用 LLM 解释、创建 `PrescriptionReview`、发布风险事件和返回医生端审核 VO。它同时承担提交前校验：处方提交时必须以 `reviewId` 找到未绑定且未过期的审核记录，并重新计算处方快照和上下文哈希。

### 看板、通知、反馈与读侧查询

`VisitFeedback` 是患者评价实体，关联挂号、患者、评分、文字评价、分诊准确度反馈和创建时间。它只允许在挂号 `COMPLETED` 后创建，且同一挂号最多一条有效评价。

`TriageAccuracyFeedback` 是分诊准确度反馈值对象或实体，保存患者对推荐是否准确的主观反馈、推荐科室快照、实际就诊科室快照、原因标签和备注。它为 E03 分诊准确率看板提供来源。

`DashboardQueryService` 是 E03 看板读侧服务，面向医生端和管理端提供聚合 VO。它不参与 P0 写事务，也不维护复杂实时统计表；默认从 `registration`、`triage_record`、`ai_call_record`、`prescription_review`、`visit_feedback`、`notification_record` 聚合读取。若后续引入统计表，也必须保持与读侧 VO 契约一致。

`MetricScopePolicy` 是看板范围策略。医生端范围限定为当前医生本人接诊/挂号记录；管理端范围为全院，可按科室、医生、日期范围筛选。该策略采用接口形态，便于未来增加科主任、院区等范围。

`DashboardOverviewVO`、`AIUsageStatsVO`、`PrescriptionReviewRateVO`、`RiskDistributionVO`、`TriageAccuracyVO`、`TrendPointVO` 是看板稳定读侧契约。它们以 ECharts 友好的结构输出，不让前端理解数据库聚合细节。

`RiskAlertEvent` 是处方审核后发布的领域/应用事件，携带挂号、医生、患者摘要、审核记录、风险等级、规则命中摘要和告警类型。`RiskAlertPublisher` 负责事务成功后发布，不应在审核事务内直接依赖 WebSocket 成功。

`RiskAlertType` 区分 `HIGH_RISK_PRESCRIPTION` 与 `REVIEW_UNCERTAIN_MANUAL_REQUIRED`。前者表示明确高风险处方；后者表示规则不可用、上下文缺失、风险未知或需要人工确认。`UNKNOWN` 不得并入 `HIGH` 统计。

`NotificationRecord` 是通知持久化实体，保存接收人、告警类型、统计归属、展示级别、业务关联、已读状态和创建时间。它支持 WebSocket 推送失败后的补拉。`RiskAlertNotificationVO` 是医生端/管理端消费的通知 VO，必须包含 `alertType`、`statisticsBucket` 和 `displayLevel`。

`ReadModelQueryService` 是患者、医生、管理员读侧查询服务族的统称。它们可以按模块拆分为 `PatientMedicalRecordQueryService`、`PatientPrescriptionQueryService`、`DoctorPrescriptionHistoryQueryService`、`AdminAIRecordQueryService`、`AdminPrescriptionReviewTraceQueryService` 等，统一负责权限过滤、分页筛选、脱敏和 VO 组装。

### E09 微服务边界与远期 HIS

`RemoteAIServiceAdapter` 是核心服务调用 E09 AI 微服务的适配器族。它实现本地任务端口，对核心业务层保持与单体 AI Provider 相同的语义。核心服务不因 E09 拆分改变业务记录归属：`TriageRecord`、`MedicalRecord`、`DiagnosisSuggestionRecord`、`PrescriptionReview`、`AICallRecord` 仍由核心服务本地事务写入。

`AIInternalRequestEnvelope` 和 `AIInternalResponseEnvelope` 是四类 AI 微服务的统一内部消息外壳，包含 traceId、callerService、taskType、requestId、tenant 或院区占位、内部鉴权摘要、业务关联 ID、降级状态和错误响应。它们用于约束 Feign Client 和 Controller 的一致行为，而不是替代各任务自己的业务 DTO。

`AIServiceFallback` 是远程 AI 调用失败后的降级策略接口，负责把 Feign 超时、熔断、认证失败、远程 4xx/5xx、解析失败映射为核心服务可识别的 `AIFallbackStatus` 和业务默认输出。它适合接口形态，因为单体模式、模拟模式和微服务模式都可复用相同降级语义。

`HISGateway` 是 E10 远期 HIS 防腐层总接口，隔离收费、退费、检查检验、发药、退药、药库交易、外部患者主索引等复杂外部系统。默认 `DisabledHISGateway` 或 Mock 实现返回未启用/模拟状态，不阻断 P0/P1。`HISSyncRecord` 记录外部同步状态、幂等键、外部 ID、错误摘要和重试信息。

`ExamLabOrderBoundary` 是未来检查检验边界。E01 的 `SuggestedExamItem` 只有参考语义，医生若要开立检查检验，必须进入独立操作并通过该边界创建外部医嘱，不能把“采纳诊疗建议”解释为已开立检查检验。

## 关键行为契约

### P0 主链路协作

管理员维护基础数据时，基础数据模块负责校验科室、医生、排班、药品、规则和 AI 配置的有效性。业务查询只能读取启用且未过期的数据；历史记录读取快照。AI 配置写入时 API Key 只写不读，健康检查失败只影响 AI 调用可用性，不删除配置。

患者智能分诊时，`TriageCoordinator` 接收症状描述，先做基础校验，再调用 AI 分诊任务。AI 成功时，推荐结果必须匹配有效科室和医生，形成可进入挂号流程的推荐；AI 失败、超时、配置禁用或格式异常时，系统返回明确降级状态，患者仍可手动选择科室、医生和号源。无论成功或降级，系统都保存 `TriageRecord`。

患者创建挂号时，挂号服务以当前患者身份、有效号源和排班容量为边界完成事务。系统拒绝不可用号源、停用医生、重复挂号或已满号源。号源扣减与挂号创建在同一一致性边界内完成。取消挂号只允许取消自己的 `WAITING` 挂号；取消成功后释放对应号源，且同一挂号只能释放一次。

医生进入工作台时，队列服务根据 `ActorContext` 只返回当前医生相关挂号。开始接诊推进 `RegistrationStatus` 到 `IN_CONSULTATION`，并建立 `ConsultationWorkspace`。AI 病历生成只回填可编辑草稿；医生确认保存后才形成正式 `MedicalRecord`，并将挂号推进到 `MEDICAL_RECORD_SAVED`。AI 生成失败不影响医生手动填写并保存病历。

处方审核必须先执行本地规则，再执行 LLM 解释。LLM 不可用与规则不可用分开处理：LLM 超时或禁用时，只要本地规则成功，审核仍可基于规则结果继续；规则不可用时只能形成 `UNKNOWN` 或人工确认降级，不得伪装低风险。处方提交时后端重算或校验 `PrescriptionSnapshot`，确认提交内容与审核快照一致；内容变化必须重新审核。

处方提交成功后，挂号可进入 `PRESCRIPTION_SUBMITTED` 并由提交事务自动推进 `COMPLETED`，或保持 `PRESCRIPTION_SUBMITTED` 直到医生点击结束就诊。两种模式必须在目标文档中择一明确，不得再让保存病历承担完成语义。患者评价、就诊完成数和完整闭环验收均以 `COMPLETED` 为准。

患者查看病历和处方时，查询服务必须以当前患者身份过滤，只返回属于自己的记录。医生查看历史病历、历史处方和审核记录时，只能访问与自己接诊或授权范围相关的记录。管理员查看 AI 和审核记录时，展示调用状态、摘要、规则命中和风险等级，不展示明文 API Key 或过度隐私原文。

### E03 看板读侧消费契约

看板 API 建议统一支持 `scope=doctor|admin` 或按路径区分医生端与管理端。医生端默认使用当前医生身份，不接受前端伪造 doctorId；管理端可传日期范围、科室、医生、AI 任务类型等筛选项。统计周期默认包括今日、本月和自定义区间；区间为空时给出前端页面默认周期。

| VO | 关键字段语义 | 统计来源 | 医生端范围 | 管理端范围 | 零值与边界 |
|---|---|---|---|---|---|
| `DashboardOverviewVO` | 今日挂号数、今日就诊数、待接诊数、已完成数、今日处方数、今日 AI 调用数、更新时间 | `registration`、`prescription`、`ai_call_record` | 当前医生相关记录 | 全院，可按科室/医生过滤 | 无数据返回 0；更新时间返回服务端时间 |
| `TrendPointVO` | 日期/月、挂号数、就诊数、处方数、AI 调用数 | 聚合表或核心表实时聚合 | 当前医生 | 全院 | 缺失日期补 0，适配 ECharts 折线/柱状图 |
| `AIUsageStatsVO` | 四类 AI 使用次数、成功次数、失败/降级次数、使用率、平均耗时 | `ai_call_record.taskType/status` | 当前医生触发或关联的 AI 调用 | 全院，可按任务类型筛选 | 分诊、病历、处方审核、诊疗建议四类都必须覆盖 |
| `PrescriptionReviewRateVO` | 审核总数、通过/低风险数、中高风险数、人工确认数、未知数、审核通过率 | `prescription_review` | 当前医生审核记录 | 全院 | `UNKNOWN` 不计入低风险通过；人工确认率单独展示 |
| `RiskDistributionVO` | `LOW/MEDIUM/HIGH/UNKNOWN` 数量和比例，按风险等级分布 | `prescription_review.riskLevel`、通知统计桶 | 当前医生 | 全院 | `UNKNOWN` 单独桶；不并入 `HIGH` |
| `TriageAccuracyVO` | 反馈总数、准确数、不准确数、准确率、无反馈数 | `visit_feedback`、`triage_accuracy_feedback`、`triage_record` | 当前医生接诊后的反馈 | 全院 | 无反馈时准确率为空或 0 且标注样本数 |

看板刷新策略采用“前端手动刷新 + 可配置定时刷新”。定时刷新只重新请求读侧 API，不使用 WebSocket 增量直接修改复杂统计。收到 E04 通知时，`useNotificationStore` 可触发 `useDashboardStore` 重拉相关图表。后端聚合失败时返回可识别错误；局部无数据时返回零值结构，而不是 500。

看板 API 总览必须从单一 `/api/dashboard/**` 泛化说明升级为明确接口：`GET /api/dashboard/overview`、`GET /api/dashboard/trends`、`GET /api/dashboard/ai-usage`、`GET /api/dashboard/prescription-review-rate`、`GET /api/dashboard/risk-distribution`、`GET /api/dashboard/triage-accuracy`。鉴权至少支持 DOCTOR 和 ADMIN，且由 `MetricScopePolicy` 决定实际范围。

### E09 微服务内部接口矩阵

E09 拆分采用核心服务 + 四类 AI 微服务。核心服务仍对前端暴露原 P0/P1 API，AI 微服务只暴露内部接口，不直接给前端调用。每次远程调用由核心服务生成 traceId/requestId，并在本地 `AICallRecord` 保存远程响应摘要和降级状态。

| serviceId | 内部路径 | Feign Client | 入参 DTO | 出参 DTO | 本地业务落点 |
|---|---|---|---|---|---|
| `triage-ai-service` | `POST /internal/ai/v1/triage/recommend` | `TriageAIInternalClient` | `TriageAIRequestDTO`：症状摘要、患者年龄/性别可选、候选科室医生摘要、prompt 版本、traceId | `TriageAIResponseDTO`：推荐科室、推荐医生候选、推荐理由、置信/排序、模型摘要、降级状态 | 核心服务写 `AICallRecord` 与 `TriageRecord`；远程服务不写核心业务库 |
| `diagnosis-ai-service` | `POST /internal/ai/v1/diagnosis/suggest` | `DiagnosisAIInternalClient` | `DiagnosisAIRequestDTO`：问诊文本、主诉、初步诊断、患者摘要、科室、建议检查输出要求、traceId | `DiagnosisAIResponseDTO`：疾病建议列表、依据、建议检查项目、置信/排序、模型摘要、降级状态 | 核心服务写 `AICallRecord` 与 `DiagnosisSuggestionRecord` |
| `prescription-review-ai-service` | `POST /internal/ai/v1/prescription-review/explain` | `PrescriptionReviewAIInternalClient` | `PrescriptionReviewAIRequestDTO`：处方快照、本地规则命中、患者用药上下文、风险等级、法规依据摘要、traceId | `PrescriptionReviewAIResponseDTO`：解释建议、风险摘要、调整建议、模型风险补充、降级状态 | 核心服务写 `AICallRecord` 并合并到 `PrescriptionReview.llmExplanationSnapshot` |
| `medical-record-ai-service` | `POST /internal/ai/v1/medical-record/generate` | `MedicalRecordAIInternalClient` | `MedicalRecordAIRequestDTO`：问诊文本、诊断方向、患者摘要、科室模板、输出结构要求、traceId | `MedicalRecordAIResponseDTO`：结构化病历草稿、字段置信/缺失提示、模型摘要、降级状态 | 核心服务写 `AICallRecord` 并返回 `MedicalRecordDraft`，正式病历仍由医生保存 |

统一内部错误响应应包含 `errorCode`、`message`、`fallbackStatus`、`retryable`、`traceId`、`remoteServiceId` 和错误摘要。Feign 层将 HTTP 401/403 映射为内部鉴权失败，将 408/超时映射为 `AI_TIMEOUT`，将 5xx/连接失败/熔断映射为 `REMOTE_AI_UNAVAILABLE`，将业务解析错误映射为 `AI_PARSE_FAILED` 或 `REMOTE_AI_BAD_REQUEST`。

超时和重试策略以任务类型配置。分诊、病历生成、诊疗建议、处方解释默认超时建议 15 到 30 秒；重试只允许对网络瞬断或 5xx 做有限重试，不对非幂等或明显业务 4xx 重试。熔断打开后直接走 `AIServiceFallback`，并写入 `AICallRecord`。所有远程调用不得持有核心业务数据库长事务。

内部鉴权使用服务间凭据而不是用户 JWT 透传。请求头建议包含 `X-Internal-Service`、`X-Internal-Token`、`X-Trace-Id`、`X-Request-Id`、`X-Task-Type`。AI 微服务只信任网关或核心服务内部凭据；前端不可直接访问 `/internal/ai/v1/**`。

E09 目标文档同步应补充 Gateway 内部路由、Nacos serviceId、OpenFeign Client 名称、DTO 契约、统一错误响应、超时/熔断/重试、traceId 和内部鉴权字段。若一期仍以单体运行，也要在单体包内保持四类任务端口和适配器分组，避免未来拆分时重写业务层。

### 三端读侧追溯 VO 契约

读侧 VO 是架构契约的一部分，必须同步 API 总览和前端 `src/types/api.ts`。后端查询服务负责权限过滤、分页筛选、脱敏和关联查询，前端不拼接敏感筛选条件绕过权限。

| VO | 消费端 | 分页/筛选 | 核心返回字段语义 | 关联关系 | 权限与脱敏 |
|---|---|---|---|---|---|
| `AdminTriageRecordVO` | 管理端 AI 分诊记录 | 时间范围、患者姓名/手机号后缀、推荐科室、调用状态、来源 | 患者摘要、症状摘要、推荐科室/医生快照、推荐理由、推荐来源、AI 状态、降级状态、生成时间、挂号关联 | `TriageRecord` + `AICallRecord` + `Registration` | 患者联系方式脱敏；不展示完整 Prompt 或明文密钥 |
| `AdminMedicalRecordAIRecordVO` | 管理端病历生成记录 | 时间范围、医生、科室、调用状态、患者关键词 | 患者摘要、医生/科室、问诊输入摘要、生成字段摘要、调用状态、降级状态、生成时间、病历关联 | `AICallRecord` + `MedicalRecord`/草稿业务关联 | 问诊原文只展示摘要；敏感病史按管理端最小必要展示 |
| `AdminPrescriptionReviewRecordVO` | 管理端处方审核记录 | 时间范围、医生、科室、风险等级、规则类型、调用状态 | 患者摘要、医生/科室、处方药品摘要、风险等级、本地规则命中项、大模型建议摘要、人工确认状态、生成时间、处方关联 | `PrescriptionReview` + `AICallRecord` + `Prescription` | 规则依据可展示；患者联系方式脱敏；不展示密钥和完整 Prompt |
| `PatientPrescriptionListVO` | 患者端我的处方 | 当前患者、日期范围、状态 | 处方 ID、就诊日期、医生/科室快照、药品摘要、风险等级、处方状态、是否有审核详情 | `Prescription` + `PrescriptionReview` + `Registration` | 仅本人；不展示内部规则配置细节 |
| `PatientPrescriptionDetailVO` | 患者端处方详情 | 处方 ID | 药品明细、剂量/频次/疗程/用法、医生科室、风险等级、审核建议摘要、规则命中患者可读说明 | `Prescription` + `PrescriptionItem` + `PrescriptionReview` | 仅本人；医生内部人工说明可按患者可读摘要展示 |
| `PatientMedicalRecordListVO` | 患者端我的电子病历 | 当前患者、日期范围、科室 | 病历 ID、就诊日期、医生/科室、主诉摘要、诊断摘要、挂号状态 | `MedicalRecord` + `Registration` | 仅本人 |
| `PatientMedicalRecordDetailVO` | 患者端病历详情 | 病历 ID | 主诉、现病史、既往史、体格检查、初步诊断、治疗意见、医生科室、保存时间 | `MedicalRecord` + `Registration` | 仅本人；隐藏 AI 技术调用细节 |
| `DoctorPrescriptionHistoryVO` | 医生端历史处方 | 医生本人、患者姓名/ID、日期范围、风险等级 | 患者摘要、就诊日期、诊断方向、药品摘要、风险等级、提交状态、审核绑定状态 | `Prescription` + `PrescriptionReview` + `Registration` | 仅当前医生接诊范围；患者手机号等最小展示 |
| `DoctorReviewDetailVO` | 医生端审核详情 | 审核 ID 或处方 ID | 规则命中、风险等级、LLM 解释、上下文缺失项、人工确认说明、快照哈希摘要、绑定处方 | `PrescriptionReview` + `AICallRecord` | 仅当前医生相关审核；管理字段不回显 |
| `AdminAIConfigVO` | 管理端 AI 配置 | provider、任务类型、启停、健康状态 | provider、model、endpoint、任务范围、启停、默认、超时、健康检查、版本、API Key 掩码 | `AIConfig` | API Key 不明文回显 |
| `NotificationRecordVO` | 医生/管理端通知 | 未读、告警类型、日期范围 | 告警类型、统计桶、展示级别、患者摘要、风险摘要、已读状态、创建时间 | `NotificationRecord` + `PrescriptionReview` | 用户队列隔离；管理员看全院 |

管理端 AI 记录 API 必须分别覆盖 `GET /api/admin/ai-records/triage`、`GET /api/admin/ai-records/medical-record`、`GET /api/admin/ai-records/prescription-review`，并明确分页参数、筛选条件和 VO。患者端病历/处方、医生端历史处方/审核详情也必须同步 API 总览，不得只在页面中临时拼字段。

### 处方规则依据与内置演示规则

处方规则定义必须把“依据”结构化，而不是只有一个自由文本法规字段。每条规则可以包含一个或多个 `RuleBasis`。管理端规则列表展示依据标题、条款号/字段来源、依据摘要和是否内置；规则详情展示完整依据摘要、版本和适用范围；审核命中展示该规则当时的依据快照。

内置演示规则作为种子数据写入 `prescription_rule_definition`，`seeded=true`，带有版本和来源。种子脚本或初始化器应可重复执行，重复执行按规则编码和版本幂等处理。内置规则允许管理员停用或复制为自定义版本，但历史审核保留命中快照。

| 演示规则类别 | 规则依据来源 | 规则依据字段 | 示例落点 |
|---|---|---|---|
| 过敏冲突 | 药品说明书、患者过敏史 | `basisType=DRUG_LABEL`、`sourceDrugField=禁忌/成分`、患者过敏标签 | 青霉素类药品与青霉素过敏史命中高风险 |
| 禁忌症 | 药品说明书、处方审核规范 | `sourceDrugField=禁忌/疾病禁忌`、`basisClause` 可填规范章节 | 某药对严重肝肾功能异常或特定疾病禁用 |
| 特殊人群 | 药品说明书、处方管理办法 | `sourceDrugField=特殊人群/儿童/孕妇/老年人` | 儿童、孕妇、老年患者用药提醒或禁用 |
| 相互作用 | 药品说明书、处方点评管理规范 | `sourceDrugField=药物相互作用` | 两种药品合用触发相互作用风险 |
| 重复用药 | 处方审核规范、教学演示规则 | 药品分类、通用名、成分 | 同通用名或同类药重复开具 |
| 剂量/频次异常 | 药品说明书、处方审核规范 | `sourceDrugField=用法用量` | 单次剂量、日剂量、频次超出推荐范围 |
| 疾病禁忌 | 药品说明书、诊断方向 | `sourceDrugField=禁忌/注意事项`、诊断关键词 | 哮喘患者使用禁忌药品等 |
| 疗程异常 | 药品说明书、处方点评管理规范 | `sourceDrugField=疗程/用法用量` | 疗程过长或过短触发提醒 |

规则启用前必须通过 `RuleDefinitionCompiler` 校验。校验范围包括药品引用存在、药品组非空、上下文字段可从 `RuleEvaluationContext` 取得、条件表达可解释、风险等级合法、提示文案和处置建议可展示、依据结构满足管理端展示。校验失败的规则不能启用，并返回配置错误明细给管理端。

规则变更采用版本化策略。启用新版本后只影响新的审核；历史 `PrescriptionReview` 通过 `RuleHit` 快照保留当时规则 ID、版本、风险等级、提示和依据。停用规则不参与新审核，但管理端仍可查看历史版本和历史命中记录。规则被删除时建议软删除或归档，不应破坏历史审核追溯。

### 拓展能力协作契约

E01 诊疗建议返回的 `SuggestedExamItem` 只作为 `DiagnosisSuggestionRecord` 的结构化参考展示和追溯。医生采纳诊疗建议时，系统只写回采纳状态、最终诊断方向、采纳说明和采纳时间。建议检查项目作为参考摘要保留，不转成订单。

E02 评价通过 `registration_id` 唯一约束或等价校验保证同一次挂号最多一条有效评价。`TriageAccuracyFeedback` 从 `TriageRecord` 推荐快照读取推荐科室，以医生确认后的就诊科室或挂号科室快照作为实际科室，记录准确性和原因标签。

E04 WebSocket 连接通过 `/ws` 握手，JWT 可由 Authorization 头、受限场景的短期 token 或 SockJS 兼容方式传递。医生订阅 `/user/queue/risk-alerts`，管理员订阅管理员 topic 或用户队列。越权订阅和伪造 doctorId/topic 的连接必须拒绝。连接失败或推送失败不回滚处方审核；前端重连后通过未读 API 补拉。

E05 流式输出采用 Spring MVC `SseEmitter` 的“POST 建会话 + GET EventSource 订阅”。会话绑定任务类型、操作者和短期 token；超时、中断、取消和错误通过事件返回；任务结束后写 `AICallRecord` 和对应业务记录。普通 POST 保留为降级路径。

E06 Pinia Store 只维护前端交互状态和后端 VO 缓存，不承担权威业务状态推进。每个 Store 必须有 loading、error、degraded、lastLoadedAt 或等价状态；关键枚举来自后端类型契约，不使用松散字符串。

E07 Prompt 模板启用采用版本化策略。AI 调用使用渲染时确定的模板版本，不受管理员随后编辑影响。模板变量非法或缺失默认模板时，任务进入可识别降级或使用内置默认模板，并在 `AICallRecord` 中记录。

E08 部署以 Vite 构建产物 + Nginx、后端 Jar、`/api/` 反向代理和健康检查为交付契约。部署 profile 管理 CORS、日志级别、数据库连接和敏感配置来源；生产演示不允许在页面或日志中明文展示 API Key。

E10 HIS 同步采用防腐层和同步记录。外部患者主索引、收费、检查检验、发药、药库交易都通过边界接口表达。默认 `DisabledHISGateway` 或 Mock 实现返回“未启用/模拟”状态，不阻断挂号、病历、处方提交。外部失败记录为待重试、失败或待人工处理。

## 错误处理策略

输入校验错误、权限错误、状态流转错误和业务不变量冲突使用业务异常并映射到统一响应。症状过短、号源不可用、重复挂号、非归属医生接诊、保存病历状态不允许、处方内容与审核快照不一致、患者越权查看等场景应返回可被前端展示的明确错误码和中文提示。

状态机错误统一归类为状态冲突。前端按钮置灰只能改善体验，后端必须以 `RegistrationLifecyclePolicy` 兜底。重复开始接诊、重复取消、重复结束就诊、重复提交处方等幂等场景要么返回现有资源，要么返回明确“已处理”状态，不得重复扣号、释放号源或绑定审核。

AI 相关错误使用可识别降级状态，而不是未分类 500。`AI_DISABLED` 表示配置缺失、禁用、密钥缺失或配置校验失败；`AI_TIMEOUT` 表示超过配置超时；`REMOTE_AI_UNAVAILABLE` 表示供应商不可达、鉴权失败或远程异常；`AI_PARSE_FAILED` 表示输出格式无法解析。降级状态写入 `AICallRecord`，并投影到业务 VO。

处方规则错误分为配置校验失败、运行失败和上下文缺失。配置校验失败的规则不能启用；运行失败使本次审核进入 `UNKNOWN/MANUAL_REQUIRED`；上下文缺失应返回缺失项，允许医生补充并重审。规则不可用不得返回低风险。

看板错误分为查询参数错误、权限范围错误和聚合失败。查询参数错误返回明确提示；越权范围被拒绝；某指标无数据返回零值结构；数据库聚合异常返回整体错误并记录日志。`UNKNOWN`、人工确认、高风险三个统计语义必须分离。

E09 远程 AI 错误由 Feign/适配器统一转换，不向业务层泄露底层异常栈。内部鉴权失败、远程服务不可达、超时、熔断、远程解析失败分别映射到统一错误与降级状态。核心服务本地记录远程错误摘要和 traceId，前端只看到业务友好的降级提示。

WebSocket、SSE、看板、评价、Prompt、微服务、HIS 等拓展能力失败不应破坏 P0 主事务。WebSocket 推送失败保留通知记录；SSE 中断可退回普通 POST；看板无数据返回零值；评价重复提交返回已评价或幂等结果；Prompt 模板非法拒绝启用；远程 AI 失败写降级；HIS 失败写同步记录并等待补偿。

敏感字段错误按安全边界处理。API Key 不回显；密钥解密失败视为配置不可用，映射 `AI_DISABLED` 或配置校验失败；日志只记录配置 ID、供应商、模型和错误摘要，不记录明文密钥、完整 Prompt 中的敏感患者信息或 JWT。

## 并发设计

号源扣减使用数据库条件更新、乐观锁版本或等价事务约束，确保剩余号源不为负。挂号创建与号源扣减在同一事务内完成；重复挂号通过唯一约束或事务内检查保证。同一患者在同一 `schedule_id` 下的有效状态应包含 `WAITING`、`IN_CONSULTATION`、`MEDICAL_RECORD_SAVED`、`PRESCRIPTION_REVIEWED`、`PRESCRIPTION_SUBMITTED`、`COMPLETED`，不包含 `CANCELLED`。取消后允许按业务规则重新挂号。

取消挂号与释放号源在一个短事务内完成，并以状态和释放标记保证只释放一次。重复取消返回已取消或幂等成功，不得再次增加剩余号源。接诊开始后不允许患者取消，避免释放已占用诊疗资源。

医生开始接诊、保存病历、处方审核、提交处方和完成就诊都通过 `RegistrationLifecyclePolicy` 做状态条件更新。状态推进应使用版本号或条件更新避免两个浏览器窗口同时推进导致丢失更新。状态推进失败返回状态冲突和最新可执行动作。

AI 配置更新是短事务。默认 Provider 设置应通过唯一约束、状态锁或事务内互斥保证同一任务范围只有一个默认启用配置。运行中的 AI 调用使用解析时确定的配置版本，不受管理员随后修改影响；调用记录保存配置版本。

规则定义启用和版本变更是短事务。`RuleDefinitionCompiler` 可按版本缓存编译结果，但必须在规则启停或版本变化后失效。审核过程使用开始时确定的规则版本集合；`RuleHit` 快照保证历史稳定。规则执行组件应无状态或只持有只读缓存，避免并发审核互相污染。

处方审核与提交采用快照哈希绑定。审核记录创建后处于待绑定状态；提交时后端重算快照并比对。若医生修改处方项、诊断方向、正式病历或规则相关患者上下文，旧审核过期，必须重新审核。一个审核记录最多绑定一个处方。

WebSocket 通知在审核或提交事务成功后发布。`NotificationRecord` 写入与推送解耦，推送失败不回滚业务。已读状态更新是独立短事务。SSE 会话设置过期、空闲超时、最大输出限制和取消状态，避免浏览器连接长期占用线程。

E09 微服务模式不使用分布式事务。核心业务服务以本地数据库为事实来源；远程 AI 调用结果只作为输入。远程超时、熔断或失败进入降级记录。E10 HIS 同步以幂等键和 `HISSyncRecord` 管理外部一致性，重复同步应关联原记录或幂等忽略。

## 设计决策

第一，继续采用模块化单体作为 P0 基线。课程项目更需要可运行、可讲解、可本地联调的闭环；E09 通过任务端口、Feign Client、Nacos serviceId 和远程适配器落地，不冲击 P0 本地演示。

第二，以 `Registration` 作为诊疗闭环锚点，并用闭合状态机替代含混的“完成”语义。保存病历只表示病历已确认和可开方；就诊完成由处方提交后自动完成或医生结束就诊触发。该决策直接消解目标文档“保存病历即完成挂号”与 P0 后续处方/评价链路的冲突。

第三，把读侧 VO 作为架构契约。新增追溯字段如果只落数据库和写侧接口，患者端、医生端、管理端和看板仍无法稳定消费；因此管理端三类 AI 记录、患者处方/病历、医生历史处方/审核详情、看板 VO 必须与 OOD 同步定义。

第四，把 `AIConfig` 设计为 P0 管理端配置聚合，而不是简单环境变量。PRD 要求管理端配置供应商、模型、接口地址和启用状态，且 API Key 受控；聚合、应用服务和 VO/DTO 能让配置真正可编码、可审计、可降级。

第五，把 `PrescriptionRuleDefinition`、`RuleBasis` 与 `RuleDefinitionCompiler` 分离。管理端维护的是可读、可版本化、可校验、可解释的规则定义；规则引擎消费的是已编译、结构稳定的运行时规则。桥接层能避免 CRUD 字段与审核输入之间断裂。

第六，本地规则优先，LLM 解释增强。LLM 不可用不阻断本地规则结果；规则不可用不能伪装低风险。默认人工确认降级既保持演示闭环，又把安全边界展示给医生、患者和管理员。

第七，`UNKNOWN` 与 `HIGH` 是不同统计语义。高风险表示规则或审核明确识别出高风险；未知表示规则不可用、上下文缺失或需要人工确认。将二者混合会污染看板风险分布和审核通过率，因此通过 `RiskAlertType`、`statisticsBucket` 和看板读侧规则拆分。

第八，E03 看板采用读侧聚合而非写事务实时维护复杂统计。这样 P1 能力不会反向阻塞 P0 主链路，也便于医生端/管理端以同一组 VO 消费不同范围的数据。

第九，E09 以四类 AI 微服务为边界，而不是泛化为一个模糊 AI Service。PRD 明确拆分智能分诊、诊疗建议、处方审核、病历生成四类服务；内部接口矩阵、Feign Client、DTO、错误响应和降级映射能支撑独立编码与联调。

第十，E01 建议检查项目只作为诊疗建议记录参考。检查检验属于远期 HIS 边界，需要独立权限、状态、外部同步和医嘱生命周期。该边界可防止实现阶段把 AI 采纳动作误做成检查医嘱创建。

第十一，流式输出采用 Spring MVC `SseEmitter` 和“POST 建会话 + GET EventSource 订阅”。它适配当前 Spring Boot 3 MVC/REST 技术栈，能处理请求体、短期 token、取消、重试和普通 POST 降级；未来迁移 WebFlux 时可替换会话服务实现。

第十二，HIS 采用防腐层而非直接并入领域模型。收费、检查检验、发药、退费、药库交易和外部患者主索引属于真实医院系统复杂边界，通过网关和同步记录明确远期落点，同时不影响 P0 演示。

## 对目标详细设计的更新约束

系统概述和模块职责必须更新：模块二职责中“保存病历 -> 标记挂号完成”改为“保存病历 -> 病历已确认/可开方”；模块三职责增加“处方提交或医生结束就诊推进完成”；模块六职责补齐 E04 WebSocket、E08 部署、E09 微服务拆分和 E10 HIS 边界，不再只做概要提及。

数据库章节必须更新或补充：`registration.status` 枚举包含 `WAITING`、`IN_CONSULTATION`、`MEDICAL_RECORD_SAVED`、`PRESCRIPTION_REVIEWED`、`PRESCRIPTION_SUBMITTED`、`COMPLETED`、`CANCELLED` 或等价状态；`registration` 增加开始接诊时间、病历确认时间、处方提交时间、完成时间、取消时间、取消原因、号源释放标记、`triage_record_id`、快照字段和版本；`medical_record` 与 `registration` 保持正式病历关联但不负责完成状态；`prescription_review` 支持规则命中快照、规则引擎状态、风险等级、处方快照哈希、审核上下文哈希、上下文缺失项、人工确认和绑定状态。

数据库章节还必须补充或强化：`ai_config` 支持 provider、model、endpoint、enabled/default、task scope、timeout、encrypted API key、key version、health status、config version、审计字段和默认唯一性约束；`prescription_rule_definition` 支持规则类型、适用药品/药品组/疾病/人群/上下文字段、条件表达、风险等级、提示、处置建议、依据结构、是否内置、版本、启停、校验状态和历史版本；`triage_record`、`diagnosis_suggestion_record`、`visit_feedback`/`triage_accuracy_feedback`、`notification_record`、`his_sync_record` 补齐上述读侧和拓展需求所需字段。

类图章节必须更新：模块一增加 `RegistrationLifecyclePolicy`、`RegistrationStatus`、`TriageRecord`、`TriageRecommendation`；模块二增加 `ConsultationApplicationService`、`ConsultationWorkspace`、`ConsultationNote`，并把保存病历后的状态推进改为 `MEDICAL_RECORD_SAVED`；模块三增加 `PrescriptionSnapshot`、`PatientMedicationContext`、`PrescriptionReview` 绑定状态、`RuleBasis`、`RuleDefinitionCompiler`、`RuleEvaluationContext`、`RuleHit`；模块四增加 `AIConfig`、`AIConfigApplicationService`、`AIProviderResolver`、`SecretCipher`、`AdminAIConfigVO/WriteDTO`；模块六增加看板 VO、通知对象、流式会话、Prompt 模板、E09 远程适配器和 HIS 防腐层。

时序图章节必须更新：模块二保存病历时序删除 `updateStatus completed`，改为 `updateStatus MEDICAL_RECORD_SAVED`；模块三处方审核时序增加规则定义编译、上下文构造、规则快照、LLM 解释、人工确认和审核记录创建；模块三处方提交时序增加 `reviewId` 校验、快照哈希比对、审核记录绑定、处方提交和完成就诊状态推进；模块六看板时序扩展为医生端/管理端范围策略；E09 增加核心服务通过 Feign 调用四类 AI 微服务、远程失败降级和核心服务本地落库的时序。

API 总览必须补齐和修正：`POST /api/medical-record/save` 说明改为“保存正式病历并推进到病历已确认/可开方”；新增或明确 `POST /api/consultation/{registrationId}/begin`、`GET /api/consultation/{registrationId}/workspace`、`POST /api/consultation/{registrationId}/complete`；处方审核响应包含 `reviewId`、`reviewStatus`、`ruleEngineStatus`、`riskLevel`、`ruleHits`、`llmStatus`、`prescriptionSnapshotHash`、`reviewContextHash`、`contextMissingItems`、`degraded`；处方提交请求携带 `reviewId` 并由后端校验快照；管理端 AI 记录三类接口、患者端病历/处方 VO、医生端历史处方/审核详情、看板六类接口都必须在 API 总览中明示。

E03 API 总览必须补齐：`GET /api/dashboard/overview`、`GET /api/dashboard/trends`、`GET /api/dashboard/ai-usage`、`GET /api/dashboard/prescription-review-rate`、`GET /api/dashboard/risk-distribution`、`GET /api/dashboard/triage-accuracy`，支持 DOCTOR/ADMIN 鉴权和范围策略。目标文档中原有只面向医生端的简化看板描述需要升级为医生端/管理端双视角。

E09 部署和集成章节必须补齐：四个 serviceId、Gateway 内部路由、OpenFeign Client、内部路径、请求/响应 DTO、统一错误响应、超时、熔断、重试、traceId、内部鉴权字段，以及核心服务本地写 `AICallRecord` 和业务记录的归属。目标文档不得再只写“AI Service 独立为 Spring Boot 应用”。

前端 TypeScript 类型必须同步：`RegistrationStatus`、`AvailableAction`、`AIConfigStatus`、`AdminAIConfigVO`、`AdminAIConfigWriteDTO`、`AIFallbackStatus`、`RuleType`、`RuleBasisVO`、`RuleEngineStatus`、`PrescriptionRuleDefinitionVO`、`RuleConditionVO`、`RuleHitVO`、`PrescriptionReviewVO`、`DashboardOverviewVO`、`AIUsageStatsVO`、`PrescriptionReviewRateVO`、`RiskDistributionVO`、`TriageAccuracyVO`、`TrendPointVO`、`AdminTriageRecordVO`、`AdminMedicalRecordAIRecordVO`、`AdminPrescriptionReviewRecordVO`、`PatientPrescriptionListVO`、`PatientPrescriptionDetailVO`、`PatientMedicalRecordListVO`、`PatientMedicalRecordDetailVO`、`DoctorPrescriptionHistoryVO`、`DoctorReviewDetailVO`、`RiskAlertType`、`RiskAlertNotificationVO`、`DiagnosisSuggestionVO`、`SuggestedExamItemVO`、`HISSyncRecordVO` 等成为稳定类型。页面不得以 `any` 或松散字符串消费关键状态。

安全章节必须补充：API Key 加密存储、只写轮换、脱敏展示、日志脱敏；Prompt 模板变量白名单；WebSocket 握手鉴权与越权订阅拒绝；SSE 短期 token；E09 内部服务凭据；管理端规则和 AI 配置仅管理员可写；患者端和医生端不得读取 AI 密钥或管理端规则内部编辑信息。

## 修订说明（v7 design v1）

| 审查意见 | 修改措施 |
|---|---|
| 挂号/接诊/病历/处方状态机未闭合，且目标文档存在“保存病历即完成挂号”的冲突 | 新增完整 `RegistrationStatus` 状态机，覆盖 `WAITING`、`IN_CONSULTATION`、`MEDICAL_RECORD_SAVED`、`PRESCRIPTION_REVIEWED`、`PRESCRIPTION_SUBMITTED`、`COMPLETED`、`CANCELLED`；补充触发动作、前置条件、允许操作矩阵、可取消边界、评价开放条件和幂等规则；明确保存病历只推进到 `MEDICAL_RECORD_SAVED`，由处方提交或医生结束就诊推进 `COMPLETED`，并列出目标文档 API、模块职责和时序图必须同步修正的语义。 |
| E03 看板消费契约不足，未覆盖全部指标和医生/管理员双视角 | 新增 E03 看板读侧消费契约，定义 `DashboardOverviewVO`、`TrendPointVO`、`AIUsageStatsVO`、`PrescriptionReviewRateVO`、`RiskDistributionVO`、`TriageAccuracyVO` 的关键字段语义、统计来源、医生端/管理端范围、零值策略和刷新策略；补充看板 API 列表与 `UNKNOWN`/人工确认统计归属。 |
| E09 微服务拆分内部接口和 DTO 契约不足 | 新增 E09 微服务内部接口矩阵，分别定义 `triage-ai-service`、`diagnosis-ai-service`、`prescription-review-ai-service`、`medical-record-ai-service` 的内部路径、Feign Client、入参 DTO、出参 DTO、本地业务落点、统一错误响应、超时/重试/熔断、traceId 和内部鉴权字段；明确核心服务本地写 `AICallRecord` 与业务记录，不引入分布式事务。 |
| P0/P1 读侧追溯 VO 泛化，管理端三类 AI 记录与患者/医生端字段未完全落地 | 新增三端读侧追溯 VO 表，覆盖 `AdminTriageRecordVO`、`AdminMedicalRecordAIRecordVO`、`AdminPrescriptionReviewRecordVO`、`PatientPrescriptionListVO`、`PatientPrescriptionDetailVO`、`PatientMedicalRecordListVO`、`PatientMedicalRecordDetailVO`、`DoctorPrescriptionHistoryVO`、`DoctorReviewDetailVO`、`AdminAIConfigVO`、`NotificationRecordVO`，明确分页筛选、返回字段语义、关联关系、权限过滤和脱敏策略，并要求同步 API 总览与前端 TypeScript 类型。 |
| 处方规则依据和内置演示规则落地不明确 | 新增 `RuleBasis` 值对象，明确 `basisType`、`basisTitle`、`basisClause`、`basisSummary`、`sourceDrugField`、`sourceReference`、`seeded`、`version` 等语义；补充内置演示规则种子数据落点和幂等策略，覆盖过敏、禁忌、特殊人群、相互作用、重复用药、剂量/频次、疾病禁忌、疗程异常，并标注药品说明书、处方审核规范、处方管理办法、处方点评管理规范等来源与管理端展示方式。 |
| 用户强调 PRD 所有需求包括拓展需求均需实现设计 | 保留并扩展全 PRD 覆盖矩阵，将 P0、E01-E10、非功能和部署/微服务/HIS 边界全部列入设计范围，逐项给出模块归属、核心对象/接口、接口与数据落点、异常和边界策略，避免把拓展需求弱化为后续可选规划。 |
