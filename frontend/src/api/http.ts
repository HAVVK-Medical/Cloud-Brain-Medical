import axios, { AxiosHeaders } from 'axios';

import { AUTH_STORAGE_KEY } from '@/constants/auth';
import type { LoginResponse, Result } from '@/types/api';

export const http = axios.create({
  baseURL: '/api',
  timeout: 15_000,
});

const refreshClient = axios.create({
  baseURL: '/api',
  timeout: 15_000,
});

function readSession() {
  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as Partial<LoginResponse> & { refreshToken?: string };
  } catch {
    return { token: raw };
  }
}

function persistSession(session: LoginResponse) {
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
}

http.interceptors.request.use((config) => {
  const session = readSession();
  const token = session?.token;

  if (token) {
    const headers = AxiosHeaders.from(config.headers);
    headers.set('Authorization', `Bearer ${token}`);
    config.headers = headers;
  }

  return config;
});

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error?.config;
    const session = readSession();

    if (error?.response?.status === 401 && originalRequest && !originalRequest.__retry) {
      originalRequest.__retry = true;
      if (session?.refreshToken && !String(originalRequest.url ?? '').includes('/auth/refresh')) {
        try {
          const refreshResponse = await refreshClient.post<Result<LoginResponse>>('/auth/refresh', {
            refreshToken: session.refreshToken,
          });
          if (refreshResponse.data.data) {
            persistSession(refreshResponse.data.data);
            const headers = AxiosHeaders.from(originalRequest.headers);
            headers.set('Authorization', `Bearer ${refreshResponse.data.data.token}`);
            originalRequest.headers = headers;
            return http.request(originalRequest);
          }
        } catch {
          window.localStorage.removeItem(AUTH_STORAGE_KEY);
        }
      }

      window.localStorage.removeItem(AUTH_STORAGE_KEY);
      if (window.location.pathname !== '/login') {
        window.location.assign('/login?reason=expired');
      }
    }

    return Promise.reject(error);
  },
);
