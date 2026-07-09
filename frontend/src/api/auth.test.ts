import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { LoginResponse, Result } from '@/types/api';
import {
  adminLogin,
  doctorLogin,
  logoutSession,
  patientLogin,
  patientRegister,
  refreshSession,
} from './auth';

vi.mock('./http', () => ({
  http: {
    post: vi.fn(),
  },
}));

const { http } = await import('./http');
const post = vi.mocked(http.post);

function result<T>(data: T | null, message = 'success'): Result<T> {
  return { code: 200, message, data, timestamp: 1 };
}

function loginResponse(overrides: Partial<LoginResponse> = {}): LoginResponse {
  return {
    token: 'token',
    refreshToken: 'refresh',
    tokenType: 'Bearer',
    userId: 1,
    role: 'patient',
    patientId: 2,
    doctorId: null,
    username: 'alice',
    displayName: 'Alice',
    expiresAt: 1_800_000_000_000,
    ...overrides,
  };
}

describe('auth api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('posts login requests to role-specific endpoints and requires response data', async () => {
    post.mockResolvedValueOnce({ data: result(loginResponse({ role: 'patient' })) });
    await expect(patientLogin({ username: 'p', password: 'pw' })).resolves.toMatchObject({ role: 'patient' });
    expect(post).toHaveBeenLastCalledWith('/patient/login', { username: 'p', password: 'pw' });

    post.mockResolvedValueOnce({ data: result(loginResponse({ role: 'doctor', patientId: null, doctorId: 9 })) });
    await expect(doctorLogin({ username: 'd', password: 'pw' })).resolves.toMatchObject({ role: 'doctor' });
    expect(post).toHaveBeenLastCalledWith('/doctor/login', { username: 'd', password: 'pw' });

    post.mockResolvedValueOnce({ data: result(loginResponse({ role: 'admin', patientId: null })) });
    await expect(adminLogin({ username: 'a', password: 'pw' })).resolves.toMatchObject({ role: 'admin' });
    expect(post).toHaveBeenLastCalledWith('/admin/login', { username: 'a', password: 'pw' });

    post.mockResolvedValueOnce({ data: result<LoginResponse>(null, 'empty response') });
    await expect(patientLogin({ username: 'bad', password: 'pw' })).rejects.toThrow();
  });

  it('posts register, refresh, and logout payloads', async () => {
    post.mockResolvedValueOnce({ data: result<null>(null, 'registered') });
    const registerPayload = {
      username: 'p',
      password: 'pw',
      realName: 'Alice',
      phone: '10086',
      gender: 'F',
      age: 20,
    };
    await expect(patientRegister(registerPayload)).resolves.toMatchObject({ message: 'registered' });
    expect(post).toHaveBeenLastCalledWith('/patient/register', registerPayload);

    post.mockResolvedValueOnce({ data: result(loginResponse({ token: 'token-2' })) });
    await expect(refreshSession({ refreshToken: 'refresh' })).resolves.toMatchObject({ token: 'token-2' });
    expect(post).toHaveBeenLastCalledWith('/auth/refresh', { refreshToken: 'refresh' });

    post.mockResolvedValueOnce({ data: result<null>(null) });
    await expect(logoutSession({ refreshToken: 'refresh', reason: 'manual' })).resolves.toMatchObject({ code: 200 });
    expect(post).toHaveBeenLastCalledWith('/auth/logout', { refreshToken: 'refresh', reason: 'manual' });
  });
});
