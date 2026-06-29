<script setup lang="ts">
import { computed, ref } from 'vue';
import { ChevronDown, ChevronUp } from 'lucide-vue-next';
import type { ChatMessage, ChatMeta } from '@/types/chat';

const props = defineProps<{
  message: ChatMessage;
  isStreaming: boolean;
}>();

const meta = computed<ChatMeta | null>(() => {
  if (!props.message.aiMeta) return null;
  try {
    return JSON.parse(props.message.aiMeta);
  } catch {
    return null;
  }
});

const showCursor = computed(() => {
  return props.isStreaming && props.message.role === 'ASSISTANT' && !props.message.content;
});

const thinkingExpanded = ref(false);

const renderedContent = computed(() => {
  return simpleMarkdown(props.message.content);
});

const renderedThinking = computed(() => {
  return simpleMarkdown(props.message.thinkingContent || '');
});

function simpleMarkdown(text: string): string {
  if (!text) return '';
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\n\n/g, '</p><p>')
    .replace(/\n/g, '<br>')
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    .replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>');
}
</script>

<template>
  <div class="chat-msg" :class="message.role === 'USER' ? 'chat-msg--user' : 'chat-msg--ai'">
    <div class="chat-msg__avatar">
      <span v-if="message.role === 'USER'">👤</span>
      <span v-else>🧠</span>
    </div>
    <div class="chat-msg__body">
      <div v-if="message.role === 'ASSISTANT' && message.thinkingContent" class="chat-msg__thinking">
        <button class="chat-msg__thinking-toggle" @click="thinkingExpanded = !thinkingExpanded">
          <span>💭 AI 思考过程</span>
          <ChevronDown v-if="!thinkingExpanded" :size="14" />
          <ChevronUp v-else :size="14" />
        </button>
        <div v-if="thinkingExpanded" class="chat-msg__thinking-content" v-html="renderedThinking" />
      </div>
      <div class="chat-msg__content" v-html="renderedContent" />
      <span v-if="showCursor" class="chat-msg__cursor">▌</span>
      <div v-if="meta" class="chat-msg__meta">
        <span class="meta-tag">{{ meta.model }}</span>
        <span class="meta-tag" v-if="meta.degraded" style="background:var(--danger);color:#fff">降级</span>
        <span class="meta-time">{{ meta.durationMs }}ms</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-msg { display: flex; gap: 10px; margin-bottom: 16px; }
.chat-msg--user { flex-direction: row-reverse; }
.chat-msg__avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--surface-soft); display: flex;
  align-items: center; justify-content: center; font-size: 16px; flex-shrink: 0;
}
.chat-msg__body { max-width: 75%; }
.chat-msg--user .chat-msg__body {
  background: var(--primary); color: #fff;
  border-radius: 12px 12px 4px 12px; padding: 10px 14px;
}
.chat-msg--ai .chat-msg__body {
  background: var(--surface); border: 1px solid var(--border);
  border-radius: 12px 12px 12px 4px; padding: 10px 14px; border-left: 3px solid var(--primary);
}
.chat-msg__content :deep(p) { margin: 0 0 8px; }
.chat-msg__content :deep(p:last-child) { margin-bottom: 0; }
.chat-msg__content :deep(code) { background: rgba(0,0,0,.06); padding: 1px 4px; border-radius: 3px; font-size: .9em; }
.chat-msg__content :deep(ul) { margin: 4px 0; padding-left: 18px; }
.chat-msg__cursor { animation: blink 1s step-end infinite; color: var(--primary); }
@keyframes blink { 50% { opacity: 0; } }
.chat-msg__meta { display: flex; gap: 6px; margin-top: 8px; flex-wrap: wrap; }
.meta-tag { font-size: 11px; padding: 1px 6px; border-radius: 4px; background: var(--surface-soft); color: var(--muted); }
.meta-time { font-size: 11px; color: var(--muted); }
.chat-msg__thinking { margin-bottom: 10px; }
.chat-msg__thinking-toggle {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: var(--muted); cursor: pointer;
  background: none; border: none; padding: 2px 0;
  width: 100%; text-align: left;
}
.chat-msg__thinking-toggle:hover { color: var(--primary); }
.chat-msg__thinking-content {
  margin-top: 6px; padding: 8px 10px;
  background: rgba(0,0,0,.03); border-radius: 6px;
  font-size: 12px; color: var(--muted); font-style: italic;
  border-left: 2px solid var(--border);
  max-height: 200px; overflow-y: auto;
  white-space: pre-wrap;
}
</style>
