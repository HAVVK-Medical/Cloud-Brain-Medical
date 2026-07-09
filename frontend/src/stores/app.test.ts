import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useAppStore } from './app';

vi.mock('@/api/system', () => ({
  getHealth: vi.fn(),
}));

const systemApi = await import('@/api/system');

describe('app store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  it('loads health and records a successful timestamp', async () => {
    vi.mocked(systemApi.getHealth).mockResolvedValue({
      service: 'cloud-brain-medical-backend',
      status: 'UP',
      javaVersion: '17',
    });

    const store = useAppStore();
    await store.refreshHealth();

    expect(store.health?.status).toBe('UP');
    expect(store.degraded).toBe(false);
    expect(store.error).toBe('');
    expect(store.loading).toBe(false);
    expect(store.lastLoadedAt).toEqual(expect.any(Number));
  });

  it('keeps the UI degraded when health check fails', async () => {
    vi.mocked(systemApi.getHealth).mockRejectedValue(new Error('health check failed'));

    const store = useAppStore();
    await store.refreshHealth();

    expect(store.health).toBeNull();
    expect(store.degraded).toBe(true);
    expect(store.loading).toBe(false);
    expect(store.error).not.toBe('');
  });
});
