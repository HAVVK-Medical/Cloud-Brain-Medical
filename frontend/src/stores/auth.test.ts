import { createPinia, setActivePinia } from 'pinia';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { AUTH_STORAGE_KEY } from '@/constants/auth';
import type { LoginResponse } from '@/types/api';
import { installBrowserMocks, removeBrowserMocks } from '@/test-utils/browser';
import { useAuthStore } from './auth';

vi.mock('@/api/auth', () => ({
  adminLogin: vi.fn(),
  doctorLogin: vi.fn(),
  logoutSession: vi.fn(),
  patientLogin: vi.fn(),
  patientRegister: vi.fn(),
  refreshSession: vi.fn(),
}));

const authApi = await import('@/api/auth');

function loginResponse(overrides: Partial<LoginResponse> = {}): LoginResponse {
  return {
    token: 'token-1',
    refreshToken: 'refresh-1',
    tokenType: 'Bearer',
    userId: 7,
    role: 'patient',
    patientId: 17,
    doctorId: null,
    username: 'alice',
    displayName: 'Alice',
    expiresAt: Date.now() + 60_000,
    ...overrides,
  };
}

function session(overrides: Partial<ReturnType<typeof loginResponse>> = {}) {
  const response = loginResponse(overrides);
  return {
    token: response.token,
    refreshToken: response.refreshToken,
    tokenType: response.tokenType,
    userId: response.userId,
    role: response.role,
    patientId: response.patientId,
    doctorId: response.doctorId,
    username: response.username,
    displayName: response.displayName ?? '',
    expiresAt: response.expiresAt,
  };
}

describe('auth store', () => {
  beforeEach(() => {
    installBrowserMocks();
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  afterEach(() => {
    removeBrowserMocks();
  });

  it('hydrates valid stored sessions and clears corrupt storage', () => {
    const stored = loginResponse({ token: 'stored-token' });
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(stored));

    const store = useAuthStore();
    store.hydrateFromStorage();

    expect(store.hydrated).toBe(true);
    expect(store.token).toBe('stored-token');
    expect(store.sessionLabel).toBe('Alice');
    expect(store.isAuthenticated).toBe(true);

    localStorage.setItem(AUTH_STORAGE_KEY, '{broken-json');
    setActivePinia(createPinia());
    const freshStore = useAuthStore();
    freshStore.hydrated = false;
    freshStore.hydrateFromStorage();

    expect(freshStore.hydrated).toBe(true);
    expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull();
  });

  it('handles empty storage, unavailable browser storage, and anonymous labels', () => {
    const store = useAuthStore();
    store.hydrateFromStorage();
    expect(store.hydrated).toBe(true);
    expect(store.isExpired).toBe(false);
    expect(store.sessionLabel).not.toBe('');

    removeBrowserMocks();
    setActivePinia(createPinia());
    const serverSideStore = useAuthStore();
    serverSideStore.hydrateFromStorage();
    serverSideStore.persistSession(session({ displayName: null, username: 'server-user' }));
    serverSideStore.clearSession();

    expect(serverSideStore.hydrated).toBe(true);
    expect(serverSideStore.token).toBe('');
  });

  it('routes login by role, persists sessions, and exposes expiry state', async () => {
    vi.mocked(authApi.patientLogin).mockResolvedValue(loginResponse({ expiresAt: Date.now() - 1 }));
    vi.mocked(authApi.doctorLogin).mockResolvedValue(loginResponse({ role: 'doctor', doctorId: 9, patientId: null }));
    vi.mocked(authApi.adminLogin).mockResolvedValue(loginResponse({ role: 'admin', patientId: null }));

    const store = useAuthStore();

    await expect(store.login('patient', { username: 'p', password: 'pw' })).resolves.toMatchObject({ role: 'patient' });
    expect(authApi.patientLogin).toHaveBeenCalledWith({ username: 'p', password: 'pw' });
    expect(JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY)!).token).toBe('token-1');
    expect(store.isExpired).toBe(true);

    await store.login('doctor', { username: 'd', password: 'pw' });
    expect(authApi.doctorLogin).toHaveBeenCalledTimes(1);
    expect(store.role).toBe('doctor');

    await store.login('admin', { username: 'a', password: 'pw' });
    expect(authApi.adminLogin).toHaveBeenCalledTimes(1);
    expect(store.loading).toBe(false);
    expect(store.error).toBe('');
    expect(store.degraded).toBe(false);
  });

  it('records login and register failures without leaving loading stuck', async () => {
    vi.mocked(authApi.patientLogin).mockRejectedValueOnce(new Error('invalid username or password'));
    vi.mocked(authApi.patientRegister).mockRejectedValueOnce({ response: { data: { message: 'validation error' } } });
    const store = useAuthStore();

    await expect(store.login('patient', { username: 'bad', password: 'bad' })).rejects.toThrow();
    expect(store.degraded).toBe(true);
    expect(store.loading).toBe(false);
    expect(store.error).not.toBe('');

    await expect(store.register({
      username: 'p',
      password: 'pw',
      realName: 'Alice',
      phone: '10086',
      gender: 'F',
      age: 20,
    })).rejects.toBeTruthy();
    expect(store.loading).toBe(false);
    expect(store.error).not.toBe('');
  });

  it('records successful registration timestamps', async () => {
    vi.mocked(authApi.patientRegister).mockResolvedValue({ code: 200, message: 'registered', data: null, timestamp: 1 });
    const store = useAuthStore();

    await store.register({
      username: 'p',
      password: 'pw',
      realName: 'Alice',
      phone: '10086',
      gender: 'F',
      age: 20,
    });

    expect(store.loading).toBe(false);
    expect(store.degraded).toBe(false);
    expect(store.lastLoadedAt).toEqual(expect.any(Number));
  });

  it('refreshes sessions, clears expired sessions on refresh failure, and always logs out locally', async () => {
    vi.mocked(authApi.refreshSession).mockResolvedValueOnce(loginResponse({ token: 'token-2', refreshToken: 'refresh-2' }));
    vi.mocked(authApi.logoutSession).mockRejectedValueOnce(new Error('network'));
    const store = useAuthStore();
    store.persistSession(session());

    await expect(store.refreshSession()).resolves.toMatchObject({ token: 'token-2' });
    expect(authApi.refreshSession).toHaveBeenCalledWith({ refreshToken: 'refresh-1' });
    expect(store.token).toBe('token-2');

    vi.mocked(authApi.refreshSession).mockRejectedValueOnce(new Error('unauthorized'));
    await expect(store.refreshSession()).rejects.toThrow();
    expect(store.token).toBe('');
    expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull();

    store.persistSession(session({ refreshToken: 'refresh-logout' }));
    await store.logout('manual');
    expect(authApi.logoutSession).toHaveBeenCalledWith({ refreshToken: 'refresh-logout', reason: 'manual' });
    expect(store.isAuthenticated).toBe(false);
  });

  it('rejects refresh when no refresh token exists', async () => {
    const store = useAuthStore();
    await expect(store.refreshSession()).rejects.toThrow('login expired please re-login');
  });
});
