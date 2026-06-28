import { defineStore } from 'pinia';

import { adminLogin, doctorLogin, logoutSession, patientLogin, patientRegister, refreshSession } from '@/api/auth';
import { AUTH_STORAGE_KEY } from '@/constants/auth';
import type { LoginRequest, LoginResponse, RegisterRequest } from '@/types/api';
import type { WorkspaceRole } from '@/types/enums';
import { resolveUiErrorMessage } from '@/utils/zh';

interface SessionState {
  token: string;
  refreshToken: string;
  tokenType: string;
  userId: number | null;
  role: WorkspaceRole | null;
  patientId: number | null;
  doctorId: number | null;
  username: string;
  displayName: string;
  expiresAt: number | null;
}

interface AuthState extends SessionState {
  loading: boolean;
  error: string;
  degraded: boolean;
  lastLoadedAt: number | null;
  hydrated: boolean;
}

const emptySession = (): SessionState => ({
  token: '',
  refreshToken: '',
  tokenType: 'Bearer',
  userId: null,
  role: null,
  patientId: null,
  doctorId: null,
  username: '',
  displayName: '',
  expiresAt: null,
});

function toSession(response: LoginResponse): SessionState {
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

function isBrowserStorageAvailable() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined';
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    ...emptySession(),
    loading: false,
    error: '',
    degraded: false,
    lastLoadedAt: null,
    hydrated: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    sessionLabel: (state) => state.displayName || state.username || '访客',
    isExpired: (state) => {
      if (!state.expiresAt) {
        return false;
      }

      return Date.now() >= state.expiresAt;
    },
  },
  actions: {
    hydrateFromStorage() {
      if (!isBrowserStorageAvailable()) {
        this.hydrated = true;
        return;
      }

      const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);
      if (!raw) {
        this.hydrated = true;
        return;
      }

      try {
        const parsed = JSON.parse(raw) as SessionState;
        Object.assign(this, parsed);
      } catch {
        window.localStorage.removeItem(AUTH_STORAGE_KEY);
      } finally {
        this.hydrated = true;
      }
    },
    persistSession(session: SessionState) {
      Object.assign(this, session);
      this.lastLoadedAt = Date.now();
      this.degraded = false;
      this.error = '';

      if (isBrowserStorageAvailable()) {
        window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
      }
    },
    clearSession() {
      Object.assign(this, emptySession());
      this.degraded = false;
      this.error = '';
      this.lastLoadedAt = Date.now();

      if (isBrowserStorageAvailable()) {
        window.localStorage.removeItem(AUTH_STORAGE_KEY);
      }
    },
    async login(role: WorkspaceRole, payload: LoginRequest) {
      this.loading = true;
      this.error = '';

      try {
        const response =
          role === 'patient'
            ? await patientLogin(payload)
            : role === 'doctor'
              ? await doctorLogin(payload)
              : await adminLogin(payload);
        this.persistSession(toSession(response));
        return response;
      } catch (error) {
        this.degraded = true;
        this.error = resolveUiErrorMessage(error, '登录失败');
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async register(payload: RegisterRequest) {
      this.loading = true;
      this.error = '';

      try {
        await patientRegister(payload);
        this.lastLoadedAt = Date.now();
      } catch (error) {
        this.degraded = true;
        this.error = resolveUiErrorMessage(error, '注册失败');
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async refreshSession() {
      if (!this.refreshToken) {
        throw new Error('login expired please re-login');
      }

      try {
        const response = await refreshSession({ refreshToken: this.refreshToken });
        this.persistSession(toSession(response));
        return response;
      } catch (error) {
        this.degraded = true;
        this.error = resolveUiErrorMessage(error, '登录已过期，请重新登录');
        this.clearSession();
        throw error;
      }
    },
    async logout(reason = 'user logout') {
      try {
        if (this.refreshToken) {
          await logoutSession({ refreshToken: this.refreshToken, reason });
        }
      } catch {
        // local sign out still happens below
      } finally {
        this.clearSession();
      }
    },
  },
});
