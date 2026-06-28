<script setup lang="ts">
import { onMounted } from 'vue';
import { RefreshCw } from 'lucide-vue-next';

import { useAppStore } from '@/stores/app';
import { getHealthStatusLabel, getServiceLabel } from '@/utils/zh';

const appStore = useAppStore();

onMounted(() => {
  if (!appStore.health && !appStore.loading) {
    void appStore.refreshHealth();
  }
});
</script>

<template>
  <div class="max-w-lg mx-auto py-12 px-6">
    <div class="card">
      <div class="flex items-center justify-between mb-4">
        <div>
          <h2 class="text-base font-semibold text-text-main">系统健康</h2>
          <p class="text-xs text-text-secondary mt-0.5">后端状态、运行版本与探针状态。</p>
        </div>
        <button class="btn-ghost" type="button" @click="appStore.refreshHealth()" :disabled="appStore.loading">
          <RefreshCw :size="16" :class="{ 'animate-spin': appStore.loading }" />
          <span>刷新</span>
        </button>
      </div>

      <div class="space-y-2 text-sm">
        <div class="flex justify-between py-2 border-b border-border">
          <span class="text-text-secondary">服务</span>
          <span class="font-medium text-text-main">{{ getServiceLabel(appStore.health?.service ?? '后端待启动') }}</span>
        </div>
        <div class="flex justify-between py-2 border-b border-border">
          <span class="text-text-secondary">状态</span>
          <span class="font-medium text-text-main">{{ getHealthStatusLabel(appStore.health?.status ?? 'DEGRADED') }}</span>
        </div>
        <div class="flex justify-between py-2 border-b border-border">
          <span class="text-text-secondary">JDK</span>
          <span class="font-medium text-text-main">{{ appStore.health?.javaVersion ?? '17' }}</span>
        </div>
        <div class="flex justify-between py-2">
          <span class="text-text-secondary">最近错误</span>
          <span class="font-medium text-text-main">{{ appStore.error || '无' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
