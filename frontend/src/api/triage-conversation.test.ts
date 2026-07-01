import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { AUTH_STORAGE_KEY } from '@/constants/auth';
import type { Result } from '@/types/api';
import { installBrowserMocks, removeBrowserMocks } from '@/test-utils/browser';
import { buildStreamUrl, createSession, getMessages, listSessions } from './triage-conversation';

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

const { http } = await import('./http');
const get = vi.mocked(http.get);
const post = vi.mocked(http.post);

function result<T>(data: T | null, message = 'success'): Result<T> {
  return { code: 200, message, data, timestamp: 1 };
}

describe('triage conversation api', () => {
  beforeEach(() => {
    installBrowserMocks();
    vi.clearAllMocks();
  });

  afterEach(() => {
    removeBrowserMocks();
  });

  it('wraps triage conversation session and message endpoints', async () => {
    get.mockResolvedValueOnce({ data: result([{ id: 1, userId: 10, userRole: 'PATIENT', title: 'T', createdAt: 'c', updatedAt: 'u' }]) });
    await expect(listSessions()).resolves.toHaveLength(1);
    expect(get).toHaveBeenLastCalledWith('/triage/conversation/sessions');

    post.mockResolvedValueOnce({ data: result({ id: 2, title: 'New' }) });
    await expect(createSession('hello')).resolves.toEqual({ id: 2, title: 'New' });
    expect(post).toHaveBeenLastCalledWith('/triage/conversation/sessions', { firstMessage: 'hello' });

    get.mockResolvedValueOnce({ data: result([{ id: 3, role: 'ASSISTANT', content: 'reply', aiMeta: null, createdAt: 'c' }]) });
    await expect(getMessages(2)).resolves.toHaveLength(1);
    expect(get).toHaveBeenLastCalledWith('/triage/conversation/sessions/2/messages');
  });

  it('throws on empty responses and builds tokenized stream URLs', async () => {
    get.mockResolvedValueOnce({ data: result(null, 'missing') });
    await expect(listSessions()).rejects.toThrow('missing');

    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({ token: 'triage token' }));
    expect(buildStreamUrl(9, 'chest pain')).toBe('/api/triage/conversation/stream?sessionId=9&message=chest%20pain&token=triage%20token');
  });
});
