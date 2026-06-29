<script setup lang="ts">
import { Home } from 'lucide-vue-next';
</script>

<template>
  <div class="flex flex-col items-center justify-center min-h-[60vh] gap-4">
    <p class="text-6xl font-bold text-text-secondary/30">404</p>
    <h1 class="text-lg font-semibold text-text-main">页面未找到</h1>
    <p class="text-sm text-text-secondary">您访问的页面不存在或已被移除</p>
    <RouterLink to="/" class="btn-primary mt-2">
      <Home :size="16" />
      <span>返回首页</span>
    </RouterLink>
  </div>
</template>
