<script setup lang="ts">
import { ref, nextTick } from 'vue';
import { Send, Square } from 'lucide-vue-next';

const props = defineProps<{
  isStreaming: boolean;
  disabled: boolean;
}>();

const emit = defineEmits<{
  send: [text: string];
  stop: [];
}>();

const input = ref('');
const textareaRef = ref<HTMLTextAreaElement | null>(null);

function handleSend() {
  const text = input.value.trim();
  if (!text || props.isStreaming) return;
  emit('send', text);
  input.value = '';
  nextTick(() => textareaRef.value?.focus());
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    handleSend();
  }
}
</script>

<template>
  <div class="chat-input">
    <textarea
      ref="textareaRef" v-model="input" class="chat-input__field"
      :disabled="isStreaming || disabled" placeholder="输入您的问题..." rows="2"
      @keydown="handleKeydown"
    />
    <button v-if="!isStreaming" class="chat-input__btn chat-input__btn--send"
            :disabled="disabled || !input.trim()" @click="handleSend" title="发送">
      <Send :size="16" />
    </button>
    <button v-else class="chat-input__btn chat-input__btn--stop"
            @click="emit('stop')" title="停止生成">
      <Square :size="14" />
    </button>
  </div>
</template>

<style scoped>
.chat-input { display: flex; gap: 8px; align-items: flex-end; padding: 12px; border-top: 1px solid var(--border); background: var(--surface); }
.chat-input__field { flex: 1; border: 1px solid var(--border); border-radius: 8px; padding: 10px 12px; font-size: 14px; font-family: inherit; resize: none; outline: none; background: var(--bg); color: var(--text); }
.chat-input__field:focus { border-color: var(--primary); }
.chat-input__btn { width: 40px; height: 40px; border-radius: 50%; border: none; cursor: pointer; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.chat-input__btn--send { background: var(--primary); color: #fff; }
.chat-input__btn--send:disabled { opacity: .4; cursor: not-allowed; }
.chat-input__btn--stop { background: var(--danger); color: #fff; }
</style>
