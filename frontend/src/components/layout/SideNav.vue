<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { Component } from 'vue';

export interface NavItem {
  id: string;
  label: string;
  path: string;
  icon: Component;
}

const props = defineProps<{
  items: NavItem[];
  title?: string;
  subtitle?: string;
}>();

const route = useRoute();
const router = useRouter();

const activeId = computed(() => {
  const current = props.items.find((item) => route.path.startsWith(item.path));
  return current?.id ?? props.items[0]?.id;
});

function navigate(item: NavItem) {
  router.push(item.path);
}
</script>

<template>
  <nav class="w-52 shrink-0 bg-white border-r border-border flex flex-col min-h-0">
    <div v-if="title" class="px-5 py-4 border-b border-border">
      <p class="text-sm font-semibold text-text-main">{{ title }}</p>
      <p v-if="subtitle" class="text-xs text-text-secondary mt-0.5">{{ subtitle }}</p>
    </div>
    <div class="flex-1 py-2 overflow-y-auto">
      <button
        v-for="item in items"
        :key="item.id"
        type="button"
        class="w-full flex items-center gap-2.5 px-5 py-2.5 text-sm transition text-left border-l-2 -ml-px"
        :class="activeId === item.id
          ? 'text-brand bg-brand-soft border-brand font-medium'
          : 'text-text-secondary border-transparent hover:bg-gray-50'"
        @click="navigate(item)"
      >
        <component :is="item.icon" :size="16" />
        <span>{{ item.label }}</span>
      </button>
    </div>
  </nav>
</template>
