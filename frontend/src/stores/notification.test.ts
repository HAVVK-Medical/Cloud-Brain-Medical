import { createPinia, setActivePinia } from 'pinia';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { installBrowserMocks, removeBrowserMocks } from '@/test-utils/browser';
import type { NotificationRecordSummary } from '@/api/workflow';
import { useNotificationStore } from './notification';

vi.mock('@/api/workflow', () => ({
  listUnreadNotifications: vi.fn(),
  markNotificationRead: vi.fn(),
}));

const workflowApi = await import('@/api/workflow');

class FakeWebSocket {
  static readonly OPEN = 1;
  static instances: FakeWebSocket[] = [];
  readyState = FakeWebSocket.OPEN;
  onopen: (() => void) | null = null;
  onmessage: ((event: MessageEvent<string>) => void) | null = null;
  onclose: (() => void) | null = null;
  onerror: (() => void) | null = null;
  closed = false;

  constructor(readonly url: string) {
    FakeWebSocket.instances.push(this);
  }

  close() {
    this.closed = true;
    this.readyState = 3;
  }
}

function notice(overrides: Partial<NotificationRecordSummary> = {}): NotificationRecordSummary {
  return {
    id: 1,
    recipientId: 10,
    recipientRole: 'DOCTOR',
    alertType: 'HIGH_RISK',
    statisticsBucket: 'HIGH',
    displayLevel: 'HIGH',
    businessRecordId: 20,
    patientSummary: 'patient',
    riskSummary: 'risk',
    read: false,
    createdAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

describe('notification store', () => {
  beforeEach(() => {
    installBrowserMocks({ protocol: 'https:', host: 'medical.example' });
    setActivePinia(createPinia());
    FakeWebSocket.instances = [];
    Object.defineProperty(globalThis, 'WebSocket', {
      configurable: true,
      writable: true,
      value: FakeWebSocket,
    });
    vi.useFakeTimers();
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.useRealTimers();
    Reflect.deleteProperty(globalThis, 'WebSocket');
    removeBrowserMocks();
  });

  it('loads unread notifications and acknowledges them', async () => {
    vi.mocked(workflowApi.listUnreadNotifications).mockResolvedValue([notice(), notice({ id: 2, read: true })]);
    vi.mocked(workflowApi.markNotificationRead).mockResolvedValue(notice({ id: 1, read: true }));

    const store = useNotificationStore();
    await store.load();
    expect(store.loading).toBe(false);
    expect(store.unreadCount).toBe(1);

    await store.ack(1);
    expect(workflowApi.markNotificationRead).toHaveBeenCalledWith(1);
    expect(store.notifications.map((item) => item.id)).toEqual([2]);
  });

  it('connects websocket, merges incoming messages, ignores malformed events, reconnects, and disconnects', () => {
    const store = useNotificationStore();
    store.connect('token value');
    const socket = FakeWebSocket.instances[0];

    expect(socket.url).toBe('wss://medical.example/ws/notifications?token=token%20value');
    expect(store.socketState).toBe('connecting');

    socket.onopen?.();
    expect(store.connected).toBe(true);

    socket.onmessage?.({ data: JSON.stringify({ type: 'notification', payload: notice({ id: 9 }) }) } as MessageEvent<string>);
    socket.onmessage?.({ data: JSON.stringify({ type: 'notification', payload: notice({ id: 9, riskSummary: 'updated' }) }) } as MessageEvent<string>);
    socket.onmessage?.({ data: '{malformed' } as MessageEvent<string>);
    expect(store.notifications).toHaveLength(1);
    expect(store.notifications[0].riskSummary).toBe('updated');

    socket.onclose?.();
    expect(store.socketState).toBe('closed');
    vi.advanceTimersByTime(1000);
    expect(FakeWebSocket.instances).toHaveLength(2);

    const secondSocket = FakeWebSocket.instances[1];
    secondSocket.onerror?.();
    expect(secondSocket.closed).toBe(true);

    store.disconnect();
    expect(store.socketState).toBe('idle');
    expect(FakeWebSocket.instances.at(-1)?.closed).toBe(true);
  });

  it('does not open duplicate sockets and disconnects the active socket', () => {
    const store = useNotificationStore();
    store.connect('token');
    const socket = FakeWebSocket.instances[0];
    socket.onopen?.();

    store.connect('token');
    expect(FakeWebSocket.instances).toHaveLength(1);

    store.disconnect();

    expect(FakeWebSocket.instances).toHaveLength(1);
    expect(socket.closed).toBe(true);
    expect(store.socketState).toBe('idle');
  });
});
