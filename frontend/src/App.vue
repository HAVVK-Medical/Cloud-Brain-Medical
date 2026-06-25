<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import { Activity, LogIn, LogOut, RefreshCw, ShieldCheck, Stethoscope, UserRound } from 'lucide-vue-next';

import { useAppStore } from '@/stores/app';
import { useAuthStore } from '@/stores/auth';
import { formatZhTime, getHealthStatusLabel, getRoleLabel } from '@/utils/zh';

const appStore = useAppStore();
const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();

const healthLabel = computed(() => {
  if (appStore.loading) {
    return '检查中';
  }

  if (appStore.degraded) {
    return '异常';
  }

  return getHealthStatusLabel(appStore.health?.status ?? '待检查');
});

const healthTone = computed(() => {
  if (appStore.loading) {
    return 'loading';
  }

  if (appStore.degraded) {
    return 'danger';
  }

  if (appStore.health) {
    return 'healthy';
  }

  return 'neutral';
});

const sessionLabel = computed(() => {
  if (!authStore.isAuthenticated) {
    return '访客';
  }

  return `${getRoleLabel(authStore.role)} / ${authStore.sessionLabel}`;
});

function isActive(path: string) {
  return route.path === path;
}

async function signOut() {
  await authStore.logout();
  await router.push('/login');
}

onMounted(() => {
  void appStore.refreshHealth();
});
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">CB</span>
        <span class="brand-title">
          <span class="brand-kicker">云脑智慧诊疗平台</span>
          <span class="brand-name">工作台</span>
        </span>
      </RouterLink>

      <nav class="topnav" aria-label="主导航">
        <RouterLink class="nav-link" :class="{ active: isActive('/') }" to="/">
          <Activity :size="16" />
          <span>首页</span>
        </RouterLink>
        <RouterLink class="nav-link" :class="{ active: isActive('/patient') }" to="/patient">
          <UserRound :size="16" />
          <span>患者端</span>
        </RouterLink>
        <RouterLink class="nav-link" :class="{ active: isActive('/doctor') }" to="/doctor">
          <Stethoscope :size="16" />
          <span>医生端</span>
        </RouterLink>
        <RouterLink class="nav-link" :class="{ active: isActive('/admin') }" to="/admin">
          <ShieldCheck :size="16" />
          <span>管理端</span>
        </RouterLink>
      </nav>

      <div class="top-actions">
        <span class="status-chip session-chip">
          <span class="chip-dot" />
          <span>{{ sessionLabel }}</span>
        </span>
        <RouterLink v-if="!authStore.isAuthenticated" class="icon-button" to="/login">
          <LogIn :size="16" />
        </RouterLink>
        <button v-else class="icon-button" type="button" @click="signOut">
          <LogOut :size="16" />
        </button>
        <button class="icon-button" type="button" @click="appStore.refreshHealth()" :disabled="appStore.loading">
          <RefreshCw :size="16" :class="{ spinning: appStore.loading }" />
        </button>
      </div>
    </header>

    <main class="workspace">
      <section class="status-bar" aria-label="运行状态">
        <span class="status-chip" :data-tone="healthTone">
          <span class="chip-dot" />
          <span>{{ healthLabel }}</span>
        </span>
        <span class="status-chip">
          <span class="chip-label">JDK</span>
          <span>{{ appStore.health?.javaVersion ?? '17' }}</span>
        </span>
        <span class="status-chip">
          <span class="chip-label">路由</span>
          <span>{{ route.path }}</span>
        </span>
        <span class="status-chip">
          <span class="chip-label">更新时间</span>
          <span>{{ formatZhTime(appStore.lastLoadedAt) }}</span>
        </span>
      </section>

      <RouterView />
    </main>
  </div>
</template>
