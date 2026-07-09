import { afterEach, describe, expect, it, vi } from 'vitest';

import { AUTH_STORAGE_KEY } from '@/constants/auth';
import type { LoginResponse, Result } from '@/types/api';
import { installBrowserMocks, removeBrowserMocks } from '@/test-utils/browser';

type RequestInterceptor = (config: { headers?: unknown; url?: string; __retry?: boolean }) => unknown;
type ResponseInterceptor = (response: unknown) => unknown;
type ErrorInterceptor = (error: unknown) => Promise<unknown>;

class TestHeaders {
  private readonly values = new Map<string, string>();

  static from(input: unknown) {
    const headers = new TestHeaders();
    if (input instanceof TestHeaders) {
      for (const [key, value] of input.values) {
        headers.set(key, value);
      }
    }
    return headers;
  }

  set(key: string, value: string) {
    this.values.set(key, value);
  }

  get(key: string) {
    return this.values.get(key);
  }
}

function loginResponse(overrides: Partial<LoginResponse> = {}): LoginResponse {
  return {
    token: 'new-token',
    refreshToken: 'new-refresh',
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

function result<T>(data: T | null): Result<T> {
  return { code: 200, message: 'success', data, timestamp: 1 };
}

async function setup(pathname = '/') {
  const assign = vi.fn();
  installBrowserMocks({ pathname, assign });
  vi.resetModules();

  const requestHandlers: RequestInterceptor[] = [];
  const responseHandlers: ResponseInterceptor[] = [];
  const errorHandlers: ErrorInterceptor[] = [];
  const request = vi.fn(async (config: unknown) => ({ retried: true, config }));
  const refreshPost = vi.fn();
  const clients: unknown[] = [];

  const createClient = (isPrimary: boolean) => {
    const client = {
      interceptors: {
        request: {
          use: (handler: RequestInterceptor) => requestHandlers.push(handler),
        },
        response: {
          use: (handler: ResponseInterceptor, errorHandler: ErrorInterceptor) => {
            responseHandlers.push(handler);
            errorHandlers.push(errorHandler);
          },
        },
      },
      request,
      post: isPrimary ? vi.fn() : refreshPost,
    };
    clients.push(client);
    return client;
  };

  const create = vi.fn()
    .mockImplementationOnce(() => createClient(true))
    .mockImplementationOnce(() => createClient(false));

  vi.doMock('axios', () => ({
    default: { create },
    AxiosHeaders: TestHeaders,
  }));

  const module = await import('./http');
  return {
    ...module,
    assign,
    create,
    refreshPost,
    request,
    requestHandler: requestHandlers[0],
    responseHandler: responseHandlers[0],
    errorHandler: errorHandlers[0],
  };
}

describe('http client interceptors', () => {
  afterEach(() => {
    vi.doUnmock('axios');
    removeBrowserMocks();
  });

  it('attaches bearer tokens from JSON or raw stored sessions', async () => {
    const harness = await setup();
    expect(harness.create).toHaveBeenCalledWith({ baseURL: '/api', timeout: 15_000 });

    expect(harness.requestHandler({})).toEqual({});

    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({ token: 'json-token' }));
    const config = harness.requestHandler({ headers: undefined }) as { headers: TestHeaders };
    expect(config.headers.get('Authorization')).toBe('Bearer json-token');

    localStorage.setItem(AUTH_STORAGE_KEY, 'raw-token');
    const rawConfig = harness.requestHandler({}) as { headers: TestHeaders };
    expect(rawConfig.headers.get('Authorization')).toBe('Bearer raw-token');
  });

  it('passes through successful responses', async () => {
    const harness = await setup();
    const response = { data: { ok: true } };
    expect(harness.responseHandler(response)).toBe(response);
  });

  it('refreshes expired sessions, persists the new token, and retries the original request', async () => {
    const harness = await setup();
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({ token: 'old-token', refreshToken: 'refresh-token' }));
    harness.refreshPost.mockResolvedValueOnce({ data: result(loginResponse({ token: 'new-token' })) });
    const originalRequest = { url: '/patient/info', headers: new TestHeaders() };

    await expect(harness.errorHandler({ response: { status: 401 }, config: originalRequest })).resolves.toMatchObject({ retried: true });

    expect(harness.refreshPost).toHaveBeenCalledWith('/auth/refresh', { refreshToken: 'refresh-token' });
    expect(JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY)!).token).toBe('new-token');
    expect((originalRequest.headers as TestHeaders).get('Authorization')).toBe('Bearer new-token');
    expect(harness.request).toHaveBeenCalledWith(expect.objectContaining({ __retry: true }));
  });

  it('clears sessions and redirects when refresh is unavailable or fails', async () => {
    const noRefresh = await setup();
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({ token: 'old-token' }));
    await expect(noRefresh.errorHandler({ response: { status: 401 }, config: { url: '/patient/info' } })).rejects.toBeTruthy();
    expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull();
    expect(noRefresh.assign).toHaveBeenCalledWith('/login?reason=expired');

    const refreshFailure = await setup('/login');
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({ token: 'old-token', refreshToken: 'refresh-token' }));
    refreshFailure.refreshPost.mockRejectedValueOnce(new Error('refresh failed'));
    await expect(refreshFailure.errorHandler({ response: { status: 401 }, config: { url: '/patient/info' } })).rejects.toBeTruthy();
    expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull();
    expect(refreshFailure.assign).not.toHaveBeenCalled();
  });

  it('does not retry refresh requests, repeated retries, or non-401 errors', async () => {
    const harness = await setup();
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({ refreshToken: 'refresh-token' }));

    await expect(harness.errorHandler({ response: { status: 401 }, config: { url: '/auth/refresh' } })).rejects.toBeTruthy();
    expect(harness.refreshPost).not.toHaveBeenCalled();

    await expect(harness.errorHandler({ response: { status: 401 }, config: { url: '/patient/info', __retry: true } })).rejects.toBeTruthy();
    await expect(harness.errorHandler({ response: { status: 500 }, config: { url: '/patient/info' } })).rejects.toBeTruthy();
  });
});
