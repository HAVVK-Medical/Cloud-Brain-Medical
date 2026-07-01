<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { LogOut } from 'lucide-vue-next';

import { useAppStore } from '@/stores/app';
import { useAuthStore } from '@/stores/auth';
import { getRoleLabel } from '@/utils/zh';

const appStore = useAppStore();
const authStore = useAuthStore();
const router = useRouter();

const roleLabel = computed(() => authStore.isAuthenticated ? getRoleLabel(authStore.role) : '访客');
defineEmits<{
  refresh: [];
}>();
</script>

<template>
  <header class="h-12 bg-brand text-white flex items-center px-4 gap-4 shrink-0 z-50">
    <RouterLink to="/" class="flex items-center gap-2 font-semibold text-sm hover:opacity-80 transition">
      <img src="/logo.jpg" alt="HAVVK" class="h-6 w-auto rounded" />
      <span class="hidden sm:inline">智慧云脑诊疗平台</span>
    </RouterLink>

    <div class="flex-1" />

    <span class="text-xs bg-white/15 rounded-full px-2.5 py-0.5 hidden sm:block">
      {{ roleLabel }} · {{ authStore.sessionLabel }}
    </span>

    <button
      v-if="authStore.isAuthenticated"
      type="button"
      class="p-1 rounded hover:bg-white/10 transition"
      aria-label="退出登录"
      title="退出登录"
      @click="async () => { await authStore.logout(); await router.push('/login'); }"
    >
      <LogOut :size="16" />
    </button>
  </header>
</template>
