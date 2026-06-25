<script setup lang="ts">
import { CheckCircle2, XCircle, AlertTriangle } from 'lucide-vue-next';
import { useToast } from '@/composables/useToast';

const { toasts } = useToast();

const iconMap: Record<string, typeof CheckCircle2> = {
  success: CheckCircle2,
  error: XCircle,
  warning: AlertTriangle,
};

const bgMap: Record<string, string> = {
  success: 'bg-success text-white',
  error: 'bg-danger text-white',
  warning: 'bg-warning text-white',
};
</script>

<template>
  <div class="fixed top-4 right-4 z-[100] flex flex-col gap-2">
    <TransitionGroup name="toast">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        :class="bgMap[toast.tone]"
        class="flex items-center gap-2 rounded-lg px-4 py-2.5 shadow-lg text-sm font-medium"
      >
        <component :is="iconMap[toast.tone]" :size="16" />
        <span>{{ toast.text }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-enter-active { transition: all 0.25s ease-out; }
.toast-leave-active { transition: all 0.2s ease-in; }
.toast-enter-from { opacity: 0; transform: translateX(50px); }
.toast-leave-to { opacity: 0; transform: translateX(50px); }
</style>
