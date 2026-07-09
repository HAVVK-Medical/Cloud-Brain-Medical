# HAVVK Medical HarmonyOS App

这是在现有项目基础上新增的 HarmonyOS 端应用目录，只复用现有后端 `/api` 接口，不改动原有 `backend/` 和 `frontend/`。

## 已实现范围

- 患者登录页：对接 `POST /api/patient/login`
- 患者注册：对接 `POST /api/patient/register`
- 挂号页：对接科室、医生、排班、创建挂号、取消挂号和挂号记录接口
- 本地会话：保存 `token`、`refreshToken`、用户信息，并自动注入 `Authorization: Bearer <token>`

## 目录说明

```text
harmonyos/
  AppScope/                         # 应用级配置
  entry/src/main/ets/
    common/                         # 常量与工具
    entryability/                   # Ability 入口
    models/                         # 后端契约模型
    pages/                          # 登录与挂号页面
    services/                       # API 与会话服务
  entry/src/main/module.json5       # 模块配置
```

## 后端地址配置

默认后端地址在 `entry/src/main/ets/common/Config.ets`：

```ts
export const API_BASE_URL = 'http://127.0.0.1:8080/api';
```

真机调试时请改成电脑局域网 IP，例如：

```ts
export const API_BASE_URL = 'http://192.168.1.10:8080/api';
```

同时确保后端 CORS 允许该来源访问。

## DevEco Studio 运行

1. 使用 DevEco Studio 打开 `harmonyos/` 目录。
2. 等待 Gradle/Hvigor 同步完成。
3. 确认 `Config.ets` 中后端地址正确。
4. 运行 `entry` 模块到模拟器或真机。

## 接口复用说明

鸿蒙端当前按现有 Web 端契约实现：

- `POST /api/patient/login`
- `POST /api/patient/register`
- `GET /api/departments`
- `GET /api/doctors?departmentId=`
- `GET /api/schedules/available?departmentId=`
- `GET /api/registration/list`
- `POST /api/registration/create`
- `POST /api/registration/cancel/{id}`

如后续要拓展医生端、管理端或 AI 分诊页，可以继续在 `pages/` 和 `services/` 下增量追加。
