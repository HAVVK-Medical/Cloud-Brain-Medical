<script setup lang="ts">
import { Wifi, WifiOff } from 'lucide-vue-next';

withDefaults(defineProps<{
  connected: boolean;
  label?: string;
}>(), {
  label: '实时连接',
});
</script>

<template>
  <span class="inline-flex items-center gap-1 text-xs" :class="connected ? 'text-success' : 'text-text-secondary'">
    <Wifi v-if="connected" :size="14" />
    <WifiOff v-else :size="14" />
    <span v-if="label">{{ connected ? label : `${label}已断开` }}</span>
  </span>
</template>
