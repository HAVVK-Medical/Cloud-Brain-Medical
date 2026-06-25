<script setup lang="ts">
import { ref } from 'vue';
import { MessageCircle, Stethoscope } from 'lucide-vue-next';
import AiChatPanel from './AiChatPanel.vue';

const props = defineProps<{ role: 'doctor' | 'patient' }>();
const panelVisible = ref(false);
</script>

<template>
  <div class="chat-launcher">
    <button class="chat-launcher__fab" @click="panelVisible = !panelVisible"
            :title="role === 'doctor' ? 'AI 助手' : '智能分诊'">
      <MessageCircle v-if="role === 'doctor'" :size="22" />
      <Stethoscope v-else :size="22" />
    </button>
    <AiChatPanel :visible="panelVisible" @close="panelVisible = false" />
  </div>
</template>

<style scoped>
.chat-launcher { position: fixed; bottom: 24px; right: 24px; z-index: 999; }
.chat-launcher__fab {
  width: 52px; height: 52px; border-radius: 50%; border: none;
  background: var(--primary); color: #fff; cursor: pointer;
  box-shadow: 0 4px 16px rgba(0,0,0,.2);
  display: flex; align-items: center; justify-content: center;
  transition: transform .2s, box-shadow .2s;
}
.chat-launcher__fab:hover { transform: scale(1.08); box-shadow: 0 6px 24px rgba(0,0,0,.3); }
</style>
