import { describe, expect, it } from 'vitest';

import {
  formatZhTime,
  getHealthStatusLabel,
  getRoleLabel,
  getServiceLabel,
  getUiMessage,
  resolveUiErrorMessage,
} from './zh';

describe('zh utilities', () => {
  it('maps known role, health, service and business messages', () => {
    expect(getRoleLabel('doctor')).toBe('医生');
    expect(getRoleLabel(undefined)).toBe('访客');
    expect(getRoleLabel('unknown' as never)).toBe('未知角色');

    expect(getHealthStatusLabel('UP')).toBe('运行正常');
    expect(getHealthStatusLabel(' 自定义状态 ')).toBe('自定义状态');
    expect(getHealthStatusLabel(undefined)).toBe('待检查');

    expect(getServiceLabel('cloud-brain-medical-backend')).toBe('云脑医疗后端');
    expect(getServiceLabel('本地服务')).toBe('本地服务');

    expect(getUiMessage('doctor is unavailable', 'fallback')).toBe('医生已停用，无法挂号');
    expect(getUiMessage('success', 'fallback')).toBe('成功');
    expect(getUiMessage('未知中文消息', 'fallback')).toBe('未知中文消息');
    expect(getUiMessage('not mapped', 'fallback')).toBe('fallback');
  });

  it('resolves error messages from strings, Error objects and axios-like responses', () => {
    expect(resolveUiErrorMessage(' validation error ', 'fallback')).toBe('校验失败');
    expect(resolveUiErrorMessage(new Error('invalid username or password'), 'fallback'))
      .toBe('用户名或密码错误');
    expect(resolveUiErrorMessage({
      message: 'login failed',
      response: {
        data: {
          message: 'prescription permission required',
        },
      },
    }, 'fallback')).toBe('无权查看该处方');
    expect(resolveUiErrorMessage({}, 'fallback')).toBe('fallback');
  });

  it('formats timestamps and handles empty values', () => {
    expect(formatZhTime(null)).toBe('未更新');
    expect(formatZhTime(0)).toBe('未更新');
    expect(formatZhTime(Date.UTC(2026, 0, 1, 1, 2, 3))).toMatch(/^\d{2}:\d{2}:\d{2}$/);
  });
});
