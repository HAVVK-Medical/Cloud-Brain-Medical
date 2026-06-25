<script setup lang="ts">
import { onMounted } from 'vue';
import { RouterView } from 'vue-router';
import { useAppStore } from '@/stores/app';
import GlobalTopbar from '@/components/layout/GlobalTopbar.vue';
import ToastProvider from '@/components/shared/ToastProvider.vue';

const appStore = useAppStore();

onMounted(() => {
  void appStore.refreshHealth();
});
</script>

<template>
  <div class="flex flex-col h-screen">
    <GlobalTopbar @refresh="appStore.refreshHealth()" />
    <div class="flex-1 min-h-0">
      <RouterView />
    </div>
    <ToastProvider />
  </div>
</template>
