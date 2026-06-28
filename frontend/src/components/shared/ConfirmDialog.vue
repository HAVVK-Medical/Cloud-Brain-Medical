<script setup lang="ts">
import { AlertTriangle, Info, X } from 'lucide-vue-next';

export type DialogLevel = 'info' | 'warning' | 'danger';

const props = withDefaults(defineProps<{
  open: boolean;
  title: string;
  message: string;
  level?: DialogLevel;
  confirmLabel?: string;
  cancelLabel?: string;
  loading?: boolean;
}>(), {
  level: 'info',
  confirmLabel: '确认',
  cancelLabel: '取消',
  loading: false,
});

const emit = defineEmits<{
  confirm: [];
  cancel: [];
}>();

const iconColor: Record<DialogLevel, string> = {
  info: 'text-info',
  warning: 'text-warning',
  danger: 'text-danger',
};

const confirmBtnClass: Record<DialogLevel, string> = {
  info: 'btn-primary',
  warning: 'bg-warning text-white rounded-md px-4 py-2 text-sm font-medium hover:opacity-90 disabled:opacity-50 inline-flex items-center gap-1.5',
  danger: 'btn-danger',
};
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="absolute inset-0 bg-black/40" @click="!loading && emit('cancel')" />
      <div class="relative bg-white rounded-xl shadow-xl max-w-sm w-full mx-4 p-6">
        <div class="flex items-start gap-3 mb-4">
          <AlertTriangle v-if="level === 'danger' || level === 'warning'" :size="20" :class="iconColor[level]" class="shrink-0 mt-0.5" />
          <Info v-else :size="20" :class="iconColor[level]" class="shrink-0 mt-0.5" />
          <div class="flex-1">
            <h3 class="text-base font-semibold text-text-main">{{ title }}</h3>
            <p class="text-sm text-text-secondary mt-1">{{ message }}</p>
          </div>
          <button type="button" class="btn-ghost !p-1" @click="!loading && emit('cancel')" :disabled="loading">
            <X :size="16" />
          </button>
        </div>
        <div class="flex justify-end gap-2">
          <button type="button" class="btn-secondary" @click="emit('cancel')" :disabled="loading">
            {{ cancelLabel }}
          </button>
          <button type="button" :class="confirmBtnClass[level]" @click="emit('confirm')" :disabled="loading">
            {{ loading ? '处理中...' : confirmLabel }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
