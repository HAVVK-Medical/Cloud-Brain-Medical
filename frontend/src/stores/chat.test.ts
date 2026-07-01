import { createPinia, setActivePinia } from 'pinia';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { ChatMessage, ChatSession } from '@/types/chat';
import { installBrowserMocks, removeBrowserMocks } from '@/test-utils/browser';
import { useChatStore } from './chat';

vi.mock('@/api/chat', () => ({
  buildStreamUrl: vi.fn(),
  createSession: vi.fn(),
  deleteSession: vi.fn(),
  getMessages: vi.fn(),
  listSessions: vi.fn(),
}));

const chatApi = await import('@/api/chat');

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  readonly listeners = new Map<string, Array<(event: MessageEvent<string>) => void>>();
  closed = false;
  onerror: (() => void) | null = null;

  constructor(readonly url: string) {
    FakeEventSource.instances.push(this);
  }

  addEventListener(type: string, handler: (event: MessageEvent<string>) => void) {
    const handlers = this.listeners.get(type) ?? [];
    handlers.push(handler);
    this.listeners.set(type, handlers);
  }

  emit(type: string, data: unknown) {
    const payload = typeof data === 'string' ? data : JSON.stringify(data);
    for (const handler of this.listeners.get(type) ?? []) {
      handler({ data: payload } as MessageEvent<string>);
    }
  }

  close() {
    this.closed = true;
  }
}

function session(overrides: Partial<ChatSession> = {}): ChatSession {
  return {
    id: 1,
    userId: 10,
    userRole: 'PATIENT',
    title: 'Initial',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

function message(overrides: Partial<ChatMessage> = {}): ChatMessage {
  return {
    id: 11,
    role: 'USER',
    content: 'hello',
    aiMeta: null,
    createdAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

describe('chat store', () => {
  beforeEach(() => {
    installBrowserMocks();
    setActivePinia(createPinia());
    FakeEventSource.instances = [];
    Object.defineProperty(globalThis, 'EventSource', {
      configurable: true,
      writable: true,
      value: FakeEventSource,
    });
    vi.clearAllMocks();
  });

  afterEach(() => {
    Reflect.deleteProperty(globalThis, 'EventSource');
    removeBrowserMocks();
  });

  it('loads, selects, creates, and removes sessions', async () => {
    vi.mocked(chatApi.listSessions)
      .mockResolvedValueOnce([session({ id: 1 }), session({ id: 2, title: 'Second' })])
      .mockResolvedValueOnce([session({ id: 3, title: 'New' })]);
    vi.mocked(chatApi.getMessages).mockResolvedValue([message()]);
    vi.mocked(chatApi.createSession).mockResolvedValue({ id: 3, title: 'New' });
    vi.mocked(chatApi.deleteSession).mockResolvedValue(undefined);

    const store = useChatStore();
    await store.fetchSessions();
    expect(store.sessions).toHaveLength(2);

    await store.selectSession(1);
    expect(store.currentSessionId).toBe(1);
    expect(store.currentSession?.title).toBe('Initial');
    expect(store.messages).toHaveLength(1);

    await expect(store.createNewSession('first')).resolves.toMatchObject({ id: 3 });
    expect(store.currentSessionId).toBe(3);
    expect(store.messages).toEqual([]);

    await store.removeSession(3);
    expect(store.currentSessionId).toBeNull();
    expect(store.sessions).toEqual([]);
  });

  it('sets readable error states when session operations fail', async () => {
    vi.mocked(chatApi.listSessions).mockRejectedValueOnce(new Error('network'));
    vi.mocked(chatApi.getMessages).mockRejectedValueOnce(new Error('network'));
    vi.mocked(chatApi.createSession).mockRejectedValueOnce(new Error('network'));
    vi.mocked(chatApi.deleteSession).mockRejectedValueOnce(new Error('network'));

    const store = useChatStore();
    await store.fetchSessions();
    expect(store.error).not.toBe('');

    await store.selectSession(99);
    expect(store.error).not.toBe('');

    await expect(store.createNewSession('first')).resolves.toBeNull();
    expect(store.error).not.toBe('');

    await store.removeSession(99);
    expect(store.error).not.toBe('');

    store.clearError();
    expect(store.error).toBe('');
  });

  it('streams assistant chunks, thinking content, done metadata, error fallback, and aborts', async () => {
    vi.mocked(chatApi.buildStreamUrl).mockReturnValue('/api/chat/stream?sessionId=1');
    vi.mocked(chatApi.listSessions).mockResolvedValue([session({ id: 1, title: 'Updated' })]);

    const store = useChatStore();
    store.currentSessionId = 1;
    const controller = store.sendMessage('hello');
    const source = FakeEventSource.instances[0];

    expect(source.url).toBe('/api/chat/stream?sessionId=1');
    expect(store.isStreaming).toBe(true);
    expect(store.messages[0]).toMatchObject({ role: 'USER', content: 'hello' });

    source.emit('thinking', { content: 'plan ' });
    source.emit('chunk', { content: 'hi ' });
    source.emit('chunk', { content: 'there' });
    expect(store.thinkingBuffer).toBe('plan ');
    expect(store.streamBuffer).toBe('hi there');

    source.emit('done', { messageId: 99, thinkingContent: 'final thinking', meta: { provider: 'mock' } });
    expect(store.isStreaming).toBe(false);
    expect(source.closed).toBe(true);
    expect(store.messages[1]).toMatchObject({
      id: 99,
      role: 'ASSISTANT',
      content: 'hi there',
      thinkingContent: 'final thinking',
      aiMeta: JSON.stringify({ provider: 'mock' }),
    });

    store.currentSessionId = 1;
    store.sendMessage('again');
    const errorSource = FakeEventSource.instances[1];
    errorSource.listeners.get('error')?.[0]({ data: '{}' } as MessageEvent<string>);
    expect(store.isStreaming).toBe(false);
    expect(store.messages.at(-1)?.content).toContain('AI');

    store.currentSessionId = 1;
    store.sendMessage('partial error');
    const partialErrorSource = FakeEventSource.instances[2];
    partialErrorSource.emit('chunk', { content: 'partial' });
    partialErrorSource.listeners.get('error')?.[0]({ data: '{}' } as MessageEvent<string>);
    expect(store.messages.at(-1)?.content).toBe('partial');

    store.currentSessionId = 1;
    const abortController = store.sendMessage('stop');
    const abortSource = FakeEventSource.instances[3];
    abortController.abort();
    expect(abortSource.closed).toBe(true);
    expect(store.messages.at(-1)?.content).not.toBe('');

    store.currentSessionId = 1;
    store.sendMessage('toolbar stop');
    const toolbarSource = FakeEventSource.instances[4];
    toolbarSource.emit('chunk', { content: 'keep partial' });
    store.stopStreaming();
    expect(toolbarSource.closed).toBe(true);
    expect(store.isStreaming).toBe(false);
    expect(store.messages.at(-1)?.content).toBe('keep partial');
    controller.abort();
  });
});
