import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { listSessions, createSession, getMessages, deleteSession, buildStreamUrl } from '@/api/chat';
import type { ChatSession, ChatMessage, ChatMeta } from '@/types/chat';

export const useChatStore = defineStore('chat', () => {
  const sessions = ref<ChatSession[]>([]);
  const currentSessionId = ref<number | null>(null);
  const messages = ref<ChatMessage[]>([]);
  const isStreaming = ref(false);
  const streamAbortController = ref<AbortController | null>(null);
  const streamBuffer = ref('');
  const thinkingBuffer = ref('');
  const error = ref('');

  const currentSession = computed(() =>
    sessions.value.find(s => s.id === currentSessionId.value) ?? null
  );

  function clearError() { error.value = ''; }

  async function fetchSessions() {
    try {
      sessions.value = await listSessions();
      error.value = '';
    } catch {
      error.value = '加载对话列表失败';
    }
  }

  async function selectSession(id: number) {
    try {
      currentSessionId.value = id;
      messages.value = await getMessages(id);
      error.value = '';
    } catch {
      error.value = '加载消息失败';
    }
  }

  async function createNewSession(firstMessage: string): Promise<ChatSession | null> {
    try {
      const result = await createSession(firstMessage);
      await fetchSessions();
      const session = sessions.value.find(s => s.id === result.id);
      if (session) {
        currentSessionId.value = session.id;
        messages.value = [];
      }
      error.value = '';
      return session ?? null;
    } catch {
      error.value = '创建对话失败';
      return null;
    }
  }

  async function removeSession(id: number) {
    try {
      await deleteSession(id);
      if (currentSessionId.value === id) {
        currentSessionId.value = null;
        messages.value = [];
      }
      sessions.value = sessions.value.filter(s => s.id !== id);
      error.value = '';
    } catch {
      error.value = '删除对话失败';
    }
  }

  function sendMessage(text: string): AbortController {
    const controller = new AbortController();
    streamAbortController.value = controller;

    // Add user message optimistically
    const userMsg: ChatMessage = {
      id: Date.now(),
      role: 'USER',
      content: text,
      aiMeta: null,
      createdAt: new Date().toISOString(),
    };
    messages.value.push(userMsg);

    // Add assistant placeholder
    const assistantMsg: ChatMessage = {
      id: Date.now() + 1,
      role: 'ASSISTANT',
      content: '',
      aiMeta: null,
      createdAt: new Date().toISOString(),
    };
    messages.value.push(assistantMsg);
    streamBuffer.value = '';
    thinkingBuffer.value = '';

    isStreaming.value = true;

    const url = buildStreamUrl(currentSessionId.value!, text);
    const eventSource = new EventSource(url);

    eventSource.addEventListener('thinking', (event) => {
      const data = JSON.parse(event.data);
      thinkingBuffer.value += data.content;
      assistantMsg.thinkingContent = thinkingBuffer.value;
    });

    eventSource.addEventListener('chunk', (event) => {
      const data = JSON.parse(event.data);
      streamBuffer.value += data.content;
      assistantMsg.content = streamBuffer.value;
    });

    eventSource.addEventListener('done', (event) => {
      const data = JSON.parse(event.data);
      assistantMsg.id = data.messageId;
      assistantMsg.content = streamBuffer.value;
      assistantMsg.thinkingContent = data.thinkingContent || thinkingBuffer.value;
      assistantMsg.aiMeta = JSON.stringify(data.meta as ChatMeta);
      isStreaming.value = false;
      eventSource.close();
      // Refresh sessions to get updated title
      fetchSessions();
    });

    eventSource.addEventListener('error', () => {
      if (isStreaming.value) {
        assistantMsg.content = streamBuffer.value || 'AI 服务暂时不可用，请稍后重试。';
        isStreaming.value = false;
      }
      eventSource.close();
    });

    controller.signal.addEventListener('abort', () => {
      eventSource.close();
      isStreaming.value = false;
      if (!streamBuffer.value) {
        assistantMsg.content = '已停止生成。';
      }
    });

    return controller;
  }

  function stopStreaming() {
    streamAbortController.value?.abort();
    streamAbortController.value = null;
    isStreaming.value = false;
  }

  return {
    sessions,
    currentSessionId,
    messages,
    isStreaming,
    streamBuffer,
    thinkingBuffer,
    error,
    currentSession,
    clearError,
    fetchSessions,
    selectSession,
    createNewSession,
    removeSession,
    sendMessage,
    stopStreaming,
  };
});
