import type { WorkspaceRole } from '@/types/enums';

const ROLE_LABELS: Record<WorkspaceRole, string> = {
  patient: '患者',
  doctor: '医生',
  admin: '管理员',
};

const HEALTH_STATUS_LABELS: Record<string, string> = {
  UP: '运行正常',
  DOWN: '服务异常',
  DEGRADED: '服务异常',
  INIT: '初始化中',
  PENDING: '待检查',
};

const SERVICE_LABELS: Record<string, string> = {
  'cloud-brain-medical-backend': '云脑医疗后端',
};

const MESSAGE_LABELS: Record<string, string> = {
  success: '成功',
  registered: '注册成功',
  'invalid username or password': '用户名或密码错误',
  'age must be positive': '年龄不能小于 0',
  'validation error': '校验失败',
  'internal server error': '服务器内部错误',
  unauthorized: '未授权，请重新登录',
  'health check failed': '健康检查失败',
  'empty response': '响应为空',
  'login failed': '登录失败',
  'register failed': '注册失败',
};

const BUSINESS_MESSAGE_LABELS: Record<string, string> = {
  'please save medical record before prescription review': '请先保存正式病历，再进行处方审核',
  'prescription changed after review, please review again': '处方内容已变化，请重新审核',
  'doctor is unavailable': '医生已停用，无法挂号',
  'department is unavailable': '科室已停用，无法挂号',
  'active schedule requires active doctor and department': '启用排班需要选择启用状态的医生和科室',
  'remaining slots cannot exceed total slots': '剩余号源不能大于总号源',
  'schedule is unavailable': '该号源不可预约',
  'prescription review not found': '未找到处方审核结果',
  'prescription permission required': '无权查看该处方',
};

function hasChinese(text: string) {
  return /[\u4e00-\u9fff]/.test(text);
}

function extractMessage(error: unknown) {
  if (typeof error === 'string') {
    return error.trim();
  }

  if (!error || typeof error !== 'object') {
    return undefined;
  }

  const maybeError = error as {
    message?: unknown;
    response?: {
      data?: {
        message?: unknown;
      };
    };
  };

  const responseMessage = maybeError.response?.data?.message;
  if (typeof responseMessage === 'string' && responseMessage.trim()) {
    return responseMessage.trim();
  }

  if (typeof maybeError.message === 'string' && maybeError.message.trim()) {
    return maybeError.message.trim();
  }

  return undefined;
}

export function getRoleLabel(role: WorkspaceRole | null | undefined) {
  if (!role) {
    return '访客';
  }

  return ROLE_LABELS[role] ?? '未知角色';
}

export function getHealthStatusLabel(status: string | null | undefined) {
  if (!status) {
    return '待检查';
  }

  const normalized = status.trim();
  if (hasChinese(normalized)) {
    return normalized;
  }

  return HEALTH_STATUS_LABELS[normalized] ?? '未知状态';
}

export function getServiceLabel(service: string | null | undefined) {
  if (!service) {
    return '后端待启动';
  }

  const normalized = service.trim();
  if (hasChinese(normalized)) {
    return normalized;
  }

  return SERVICE_LABELS[normalized] ?? '未知服务';
}

export function getUiMessage(message: string | null | undefined, fallback: string) {
  if (!message) {
    return fallback;
  }

  const normalized = message.trim();
  if (hasChinese(normalized)) {
    return normalized;
  }

  return BUSINESS_MESSAGE_LABELS[normalized] ?? MESSAGE_LABELS[normalized] ?? fallback;
}

export function resolveUiErrorMessage(error: unknown, fallback: string) {
  return getUiMessage(extractMessage(error), fallback);
}

export function formatZhTime(value: number | null | undefined) {
  if (!value) {
    return '未更新';
  }

  return new Date(value).toLocaleTimeString('zh-CN', { hour12: false });
}
