<script setup lang="ts">
import { ref } from 'vue';
import { Battery, Signal, Wifi } from 'lucide-vue-next';

const time = ref(new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }));
setInterval(() => {
  time.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false });
}, 60000);
</script>

<template>
  <div class="flex items-center justify-center py-8">
    <div class="w-[375px] rounded-2xl border-[6px] border-gray-800 bg-white shadow-2xl overflow-hidden">
      <!-- Status bar -->
      <div class="flex items-center justify-between px-5 py-2 bg-white text-xs font-medium text-gray-900">
        <span>{{ time }}</span>
        <span class="flex items-center gap-1">
          <Signal :size="12" />
          <Wifi :size="12" />
          <Battery :size="12" />
        </span>
      </div>
      <!-- Content area -->
      <div class="h-[600px] overflow-y-auto bg-surface">
        <slot />
      </div>
    </div>
  </div>
</template>
