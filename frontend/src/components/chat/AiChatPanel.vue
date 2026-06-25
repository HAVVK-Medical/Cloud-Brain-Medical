<script setup lang="ts">
import { onMounted, watch, ref } from 'vue';
import { X, Minimize2, Maximize2 } from 'lucide-vue-next';
import { useChatStore } from '@/stores/chat';
import AiChatMessage from './AiChatMessage.vue';
import AiChatInput from './AiChatInput.vue';
import AiChatSessionList from './AiChatSessionList.vue';

const props = defineProps<{ visible: boolean }>();
const emit = defineEmits<{ close: [] }>();

const store = useChatStore();
const minimized = ref(false);
const sessionIdInput = ref('');

onMounted(() => { store.fetchSessions(); });
watch(() => props.visible, (v) => { if (v) store.fetchSessions(); });

async function handleNewSession() {
  const text = sessionIdInput.value.trim() || '你好';
  await store.createNewSession(text);
}

function handleSend(text: string) { store.sendMessage(text); }
async function handleSelectSession(id: number) { await store.selectSession(id); }
async function handleDeleteSession(id: number) { await store.removeSession(id); }
</script>

<template>
  <Transition name="panel">
    <div v-if="visible" class="chat-panel" :class="{ minimized }">
      <div class="chat-panel__header">
        <span class="chat-panel__title">🧠 AI 助手</span>
        <div class="chat-panel__actions">
          <button class="chat-panel__action-btn" @click="minimized = !minimized" :title="minimized ? '展开' : '最小化'">
            <Maximize2 v-if="minimized" :size="14" />
            <Minimize2 v-else :size="14" />
          </button>
          <button class="chat-panel__action-btn" @click="emit('close')" title="关闭">
            <X :size="14" />
          </button>
        </div>
      </div>

      <div v-if="!minimized" class="chat-panel__body">
        <AiChatSessionList :sessions="store.sessions" :current-id="store.currentSessionId"
          @select="handleSelectSession" @delete="handleDeleteSession" @new="handleNewSession" />

        <div class="chat-panel__main">
          <div v-if="!store.currentSessionId" class="chat-panel__empty">
            <p>👋 你好！我是云脑医疗助手。</p>
            <p>选择左侧对话或发送消息开始新对话。</p>
            <div class="chat-panel__quick-start">
              <input v-model="sessionIdInput" class="chat-panel__quick-input"
                     placeholder="输入第一条消息..." @keydown.enter="handleNewSession" />
              <button class="chat-panel__quick-btn" @click="handleNewSession">开始</button>
            </div>
          </div>

          <template v-else>
            <div class="chat-panel__messages">
              <AiChatMessage v-for="msg in store.messages" :key="msg.id"
                             :message="msg" :is-streaming="store.isStreaming" />
            </div>
            <AiChatInput :is-streaming="store.isStreaming" :disabled="!store.currentSessionId"
                         @send="handleSend" @stop="store.stopStreaming()" />
          </template>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.chat-panel {
  position: fixed; bottom: 80px; right: 20px; width: 680px; height: 520px;
  background: var(--surface); border-radius: 12px; box-shadow: 0 8px 40px rgba(0,0,0,.15);
  display: flex; flex-direction: column; z-index: 1000; overflow: hidden;
}
.chat-panel.minimized { height: auto; width: 260px; }
.chat-panel__header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px; background: var(--primary); color: #fff; user-select: none;
}
.chat-panel__title { font-size: 14px; font-weight: 600; }
.chat-panel__actions { display: flex; gap: 4px; }
.chat-panel__action-btn {
  width: 28px; height: 28px; border: none; border-radius: 6px;
  background: rgba(255,255,255,.15); color: #fff; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
}
.chat-panel__action-btn:hover { background: rgba(255,255,255,.3); }
.chat-panel__body { flex: 1; display: flex; overflow: hidden; }
.chat-panel__main { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.chat-panel__empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; color: var(--muted); padding: 24px; }
.chat-panel__empty p { margin: 0; font-size: 14px; }
.chat-panel__quick-start { display: flex; gap: 8px; margin-top: 12px; width: 100%; max-width: 320px; }
.chat-panel__quick-input { flex: 1; border: 1px solid var(--border); border-radius: 8px; padding: 8px 12px; font-size: 13px; outline: none; }
.chat-panel__quick-input:focus { border-color: var(--primary); }
.chat-panel__quick-btn { padding: 8px 16px; border: none; border-radius: 8px; background: var(--primary); color: #fff; cursor: pointer; font-size: 13px; }
.chat-panel__messages { flex: 1; overflow-y: auto; padding: 16px; }
.panel-enter-active, .panel-leave-active { transition: all .25s ease; }
.panel-enter-from, .panel-leave-to { opacity: 0; transform: translateY(20px) scale(.96); }
</style>
