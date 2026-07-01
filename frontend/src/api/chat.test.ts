import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { AUTH_STORAGE_KEY } from '@/constants/auth';
import type { Result } from '@/types/api';
import { installBrowserMocks, removeBrowserMocks } from '@/test-utils/browser';
import { buildStreamUrl, createSession, deleteSession, getMessages, listSessions } from './chat';

vi.mock('./http', () => ({
  http: {
    delete: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
  },
}));

const { http } = await import('./http');
const get = vi.mocked(http.get);
const post = vi.mocked(http.post);
const del = vi.mocked(http.delete);

function result<T>(data: T | null, message = 'success'): Result<T> {
  return { code: 200, message, data, timestamp: 1 };
}

describe('chat api', () => {
  beforeEach(() => {
    installBrowserMocks();
    vi.clearAllMocks();
  });

  afterEach(() => {
    removeBrowserMocks();
  });

  it('wraps chat session and message endpoints', async () => {
    get.mockResolvedValueOnce({ data: result([{ id: 1, userId: 10, userRole: 'PATIENT', title: 'T', createdAt: 'c', updatedAt: 'u' }]) });
    await expect(listSessions()).resolves.toHaveLength(1);
    expect(get).toHaveBeenLastCalledWith('/chat/sessions');

    post.mockResolvedValueOnce({ data: result({ id: 2, title: 'New' }) });
    await expect(createSession('hello')).resolves.toEqual({ id: 2, title: 'New' });
    expect(post).toHaveBeenLastCalledWith('/chat/sessions', { firstMessage: 'hello' });

    get.mockResolvedValueOnce({ data: result([{ id: 3, role: 'USER', content: 'hello', aiMeta: null, createdAt: 'c' }]) });
    await expect(getMessages(2)).resolves.toHaveLength(1);
    expect(get).toHaveBeenLastCalledWith('/chat/sessions/2/messages');

    del.mockResolvedValueOnce({});
    await expect(deleteSession(2)).resolves.toBeUndefined();
    expect(del).toHaveBeenLastCalledWith('/chat/sessions/2');
  });

  it('throws when wrapped chat responses contain no data', async () => {
    get.mockResolvedValueOnce({ data: result(null, 'missing') });
    await expect(listSessions()).rejects.toThrow('missing');
  });

  it('builds encoded stream URLs from stored tokens and tolerates plain/corrupt storage', () => {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({ token: 'jwt token' }));
    expect(buildStreamUrl(7, 'hello world')).toBe('/api/chat/stream?sessionId=7&message=hello%20world&token=jwt%20token');

    localStorage.setItem(AUTH_STORAGE_KEY, '{bad-json');
    expect(buildStreamUrl(8, 'a+b')).toBe('/api/chat/stream?sessionId=8&message=a%2Bb&token=');
  });
});
