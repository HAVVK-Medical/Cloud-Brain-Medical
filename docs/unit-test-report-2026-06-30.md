# 东软智慧云脑诊疗平台单元测试文档

**文档日期**：2026-06-30  
**测试类型**：后端 JUnit 5 单元测试 + 前端 Vitest 单元测试  
**项目名称**：HAVVK-Medical / 东软智慧云脑诊疗平台  
**测试对象**：后端核心业务工具类、权限策略、前端工具函数与分页 composable  

---

## 1. 测试目标

本次单元测试按照“A4 - 单元测试 JUnit”任务要求进行，目标如下：

1. 使用 JUnit 5 为后端核心功能编写规范单元测试。
2. 使用 Vitest 为前端核心业务逻辑与公共模块编写单元测试。
3. 覆盖正常情况、异常情况和边界条件。
4. 接入覆盖率报告工具，便于检查测试充分性。
5. 确保后端测试、前端测试、前端类型检查均可在命令行稳定通过。

---

## 2. 测试环境

| 类别 | 工具 / 框架 | 说明 |
| --- | --- | --- |
| 后端语言 | Java 17 | Spring Boot 后端运行环境 |
| 后端测试框架 | JUnit 5 | 由 `spring-boot-starter-test` 提供 |
| 后端覆盖率 | JaCoCo | 通过 `jacoco-maven-plugin` 生成报告 |
| 后端构建工具 | Maven Wrapper | 使用项目根目录 `mvnw.cmd` |
| 前端语言 | TypeScript | Vue 3 前端代码 |
| 前端测试框架 | Vitest | 新增 `vitest run` 测试脚本 |
| 前端覆盖率 | Vitest Coverage V8 | 新增 `@vitest/coverage-v8` |
| 前端包管理 | npm | 使用 `package-lock.json` 锁定依赖 |

---

## 3. 测试代码位置

### 3.1 后端测试文件

| 测试文件 | 被测对象 | 主要验证内容 |
| --- | --- | --- |
| `backend/src/test/java/com/cloudbrain/application/ai/AITextParserTest.java` | `AITextParser` | AI 文本键值解析、空输入、非法输入、fallback |
| `backend/src/test/java/com/cloudbrain/application/admin/ConfigCipherTest.java` | `ConfigCipher` | 配置密文加密、解密、随机 IV、非法密文 |
| `backend/src/test/java/com/cloudbrain/security/DefaultRolePolicyTest.java` | `DefaultRolePolicy` | 患者、医生、管理员权限边界 |

### 3.2 前端测试文件

| 测试文件 | 被测对象 | 主要验证内容 |
| --- | --- | --- |
| `frontend/src/composables/usePagination.test.ts` | `usePagination` | 页数计算、页内容、页码边界、数据缩减 |
| `frontend/src/utils/zh.test.ts` | `zh.ts` | 角色/状态/服务/业务消息映射、错误解析、时间格式化 |

---

## 4. 测试用例说明

### 4.1 后端 JUnit 5 测试

#### AITextParserTest

| 用例 | 类型 | 预期结果 |
| --- | --- | --- |
| 解析多行 key-value 文本 | 正常情况 | 保持插入顺序，去除 key/value 两端空白 |
| value 中包含冒号 | 边界条件 | 只按第一个冒号分隔，后续冒号保留到 value |
| null、空白、无冒号文本 | 异常/边界 | 返回空 Map，不抛异常 |
| firstNonBlank 提取已有值 | 正常情况 | 返回 trim 后的业务值 |
| firstNonBlank 处理空白/空 Map | 边界条件 | 返回 fallback |

#### ConfigCipherTest

| 用例 | 类型 | 预期结果 |
| --- | --- | --- |
| 明文加密后再解密 | 正常情况 | 解密结果等于原始明文 |
| 密文不暴露明文 | 安全校验 | 加密结果不等于原始明文 |
| 同一明文重复加密 | 边界/安全 | 两次密文不同，均可正确解密 |
| null / 空白输入 | 边界条件 | encrypt/decrypt 返回 null |
| 非法密文 | 异常情况 | 抛出 `IllegalStateException` |

#### DefaultRolePolicyTest

| 用例 | 类型 | 预期结果 |
| --- | --- | --- |
| 患者查看本人病历 | 正常情况 | 允许访问 |
| 患者查看他人病历 | 权限边界 | 拒绝访问 |
| 管理员查看病历和医生工作台 | 正常情况 | 允许访问 |
| 医生保存病历/提交处方 | 正常情况 | 需医生身份且 registrationId 不为空 |
| 通知访问权限 | 权限边界 | 用户只能访问自身、患者/医生业务身份或管理员范围 |
| null 参数 | 边界条件 | 返回 false，不抛异常 |

### 4.2 前端 Vitest 测试

#### usePagination.test.ts

| 用例 | 类型 | 预期结果 |
| --- | --- | --- |
| 初始化分页 | 正常情况 | 正确计算 total、pageCount、pagedItems |
| 跳转到最后一页 | 正常情况 | 返回最后一页数据 |
| 页码过大/过小/小数 | 边界条件 | 自动限制在有效页码范围 |
| 数据源长度减少 | 边界条件 | 当前页自动回落到最大可用页 |
| resetPage | 正常情况 | 页码重置为 1 |

#### zh.test.ts

| 用例 | 类型 | 预期结果 |
| --- | --- | --- |
| 角色标签映射 | 正常情况 | doctor/patient/admin 映射为中文 |
| 未登录或未知角色 | 边界条件 | 返回“访客”或“未知角色” |
| 健康状态和服务名称映射 | 正常情况 | 英文状态映射为中文，中文输入原样返回 |
| 业务错误消息映射 | 正常情况 | 后端英文错误转换为中文提示 |
| axios-like 错误对象 | 异常情况 | 优先读取 `response.data.message` |
| 空时间戳 | 边界条件 | 返回“未更新” |

---

## 5. 测试运行方式

### 5.1 后端单元测试

在项目根目录执行：

```powershell
.\mvnw.cmd test
```

执行后会同时生成 JaCoCo 覆盖率报告：

```text
backend/target/site/jacoco/index.html
```

### 5.2 前端单元测试

在 `frontend` 目录执行：

```powershell
npm run test
```

### 5.3 前端覆盖率

在 `frontend` 目录执行：

```powershell
npm run test:coverage
```

执行后会生成覆盖率报告：

```text
frontend/coverage/index.html
```

### 5.4 前端类型检查

在 `frontend` 目录执行：

```powershell
npm run typecheck
```

---

## 6. 测试执行结果

### 6.1 后端测试结果

执行命令：

```powershell
.\mvnw.cmd test
```

执行结果：

```text
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

结论：后端测试全部通过，JaCoCo 报告已生成。

### 6.2 前端测试结果

执行命令：

```powershell
npm run test
```

执行结果：

```text
Test Files  2 passed (2)
Tests       6 passed (6)
```

结论：前端 Vitest 单元测试全部通过。

### 6.3 前端覆盖率结果

执行命令：

```powershell
npm run test:coverage
```

覆盖率结果：

| 指标 | 覆盖率 |
| --- | ---: |
| Statements | 94.82% |
| Branches | 88.63% |
| Functions | 100% |
| Lines | 94.64% |

分模块覆盖率：

| 文件 | Statements | Branches | Functions | Lines |
| --- | ---: | ---: | ---: | ---: |
| `frontend/src/composables/usePagination.ts` | 93.75% | 80.00% | 100% | 92.85% |
| `frontend/src/utils/zh.ts` | 95.23% | 89.74% | 100% | 95.23% |

### 6.4 后端重点类覆盖率

本次新增测试重点覆盖的后端类 JaCoCo 结果如下：

| 类 | 指令覆盖率 | 分支覆盖率 | 行覆盖率 |
| --- | ---: | ---: | ---: |
| `AITextParser` | 100.00% | 87.50% | 100.00% |
| `ConfigCipher` | 88.89% | 90.00% | 86.11% |
| `DefaultRolePolicy` | 97.27% | 76.92% | 94.44% |

说明：JaCoCo 全项目报告覆盖 157 个后端类，本次任务重点补充的是核心工具与权限策略类的单元测试，因此表格列出本次新增测试直接覆盖的重点类。

### 6.5 类型检查结果

执行命令：

```powershell
npm run typecheck
```

执行结果：通过，无 TypeScript 编译错误。

---

## 7. 本次配置变更

### 7.1 后端配置

在 `backend/pom.xml` 中新增 JaCoCo 插件，用于在 `mvn test` 阶段生成覆盖率报告。

### 7.2 前端配置

在 `frontend/package.json` 中新增脚本：

```json
{
  "test": "vitest run",
  "test:coverage": "vitest run --coverage"
}
```

新增开发依赖：

```text
vitest
@vitest/coverage-v8
```

### 7.3 忽略覆盖率生成物

在 `.gitignore` 中新增：

```gitignore
/frontend/coverage/
```

避免前端覆盖率 HTML 报告被误提交。覆盖率报告可通过 `npm run test:coverage` 随时重新生成。

---

## 8. 质量结论

本次单元测试已完成后端 JUnit 5 与前端 Vitest 的基础建设和核心用例补充，覆盖了正常输入、异常输入和边界场景。测试结果显示：

1. 后端现有测试与新增测试共 19 个，全部通过。
2. 前端新增 2 个测试文件、6 个测试用例，全部通过。
3. 前端核心工具模块覆盖率达到 80% 以上，满足任务要求。
4. 后端本次重点覆盖类中，`AITextParser` 达到行覆盖率 100%，`ConfigCipher` 和 `DefaultRolePolicy` 行覆盖率均超过 80%。
5. 前端类型检查通过，说明新增测试配置未破坏 TypeScript 工程约束。

综合判断：本次单元测试实现满足任务要求，可以作为团队作业提交材料的一部分。

---

## 9. 后续建议

1. 继续为 `AuthService`、`WorkflowService`、`PatientService` 等业务服务补充 Mock 型单元测试。
2. 前端可继续为 Pinia store、API 封装和关键 Vue 组件补充测试。
3. 若课程要求提交截图，可打开以下报告页面截图：
   - `backend/target/site/jacoco/index.html`
   - `frontend/coverage/index.html`
4. 后续可以在 CI 中加入 `mvn test`、`npm run test`、`npm run typecheck`，保证提交前自动验证。
