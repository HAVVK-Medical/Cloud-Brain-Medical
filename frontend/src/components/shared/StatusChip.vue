<script setup lang="ts">
export type StatusTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

const props = withDefaults(defineProps<{
  tone?: StatusTone;
  dot?: boolean;
}>(), {
  tone: 'neutral',
  dot: true,
});

const toneClasses: Record<StatusTone, string> = {
  success: 'bg-green-50 text-success',
  warning: 'bg-amber-50 text-warning',
  danger: 'bg-red-50 text-danger',
  info: 'bg-blue-50 text-info',
  neutral: 'bg-gray-100 text-text-secondary',
};

const dotColor: Record<StatusTone, string> = {
  success: 'bg-success',
  warning: 'bg-warning',
  danger: 'bg-danger',
  info: 'bg-info',
  neutral: 'bg-gray-400',
};
</script>

<template>
  <span class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium" :class="toneClasses[tone]">
    <span v-if="dot" class="inline-block h-1.5 w-1.5 rounded-full" :class="dotColor[tone]" />
    <slot />
  </span>
</template>
