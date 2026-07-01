import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { listUnreadNotifications, markNotificationRead } from '@/api/workflow';
import type { NotificationRecordSummary } from '@/api/workflow';

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<NotificationRecordSummary[]>([]);
  const loading = ref(false);
  const socketState = ref<'idle' | 'connecting' | 'connected' | 'closed'>('idle');
  let socket: WebSocket | null = null;

  const unreadCount = computed(() => notifications.value.filter((n) => n.read !== true).length);
  const connected = computed(() => socketState.value === 'connected');

  function buildWsUrl(path: string, token: string) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}${path}?token=${encodeURIComponent(token)}`;
  }

  async function load() {
    loading.value = true;
    try {
      notifications.value = await listUnreadNotifications();
    } finally {
      loading.value = false;
    }
  }

  async function ack(id: number) {
    await markNotificationRead(id);
    notifications.value = notifications.value.filter((n) => n.id !== id);
  }

  let reconnectAttempt = 0;
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  const MAX_RECONNECT_DELAY = 30_000;

  function scheduleReconnect(token: string) {
    if (reconnectTimer) clearTimeout(reconnectTimer);
    const delay = Math.min(1000 * Math.pow(2, reconnectAttempt), MAX_RECONNECT_DELAY);
    reconnectAttempt++;
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null;
      connect(token);
    }, delay);
  }

  function connect(token: string) {
    if (socket && socket.readyState === WebSocket.OPEN) return;
    socketState.value = 'connecting';
    socket = new WebSocket(buildWsUrl('/ws/notifications', token));
    socket.onopen = () => {
      socketState.value = 'connected';
      reconnectAttempt = 0;
    };
    socket.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data) as { type?: string; payload?: NotificationRecordSummary };
        if (msg.type === 'notification' && msg.payload) {
          notifications.value = [
            msg.payload,
            ...notifications.value.filter((n) => n.id !== msg.payload!.id),
          ].slice(0, 20);
        }
      } catch { /* ignore malformed */ }
    };
    socket.onclose = () => {
      socket = null;
      socketState.value = 'closed';
      scheduleReconnect(token);
    };
    socket.onerror = () => {
      socket?.close();
    };
  }

  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    reconnectAttempt = 0;
    socket?.close();
    socket = null;
    socketState.value = 'idle';
  }

  function disconnect() {
    socket?.close();
    socket = null;
    socketState.value = 'idle';
  }

  return { notifications, loading, socketState, unreadCount, connected, load, ack, connect, disconnect };
});
