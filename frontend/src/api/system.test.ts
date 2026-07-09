import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { HealthResponse, Result } from '@/types/api';
import { getHealth } from './system';

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
  },
}));

const { http } = await import('./http');
const get = vi.mocked(http.get);

function result<T>(data: T | null, message = 'success'): Result<T> {
  return { code: 200, message, data, timestamp: 1 };
}

describe('system api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns health data and throws on empty health payloads', async () => {
    const health: HealthResponse = { service: 'backend', status: 'UP', javaVersion: '17' };
    get.mockResolvedValueOnce({ data: result(health) });
    await expect(getHealth()).resolves.toEqual(health);
    expect(get).toHaveBeenLastCalledWith('/health');

    get.mockResolvedValueOnce({ data: result<HealthResponse>(null, 'health check failed') });
    await expect(getHealth()).rejects.toThrow();
  });
});
