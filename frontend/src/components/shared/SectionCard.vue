<script setup lang="ts">
import { ref } from 'vue';
import { ChevronDown } from 'lucide-vue-next';

withDefaults(defineProps<{
  title?: string;
  collapsible?: boolean;
}>(), {
  collapsible: false,
});

const collapsed = ref(false);
</script>

<template>
  <div class="card">
    <div v-if="title" class="flex items-center justify-between mb-4">
      <h3 class="text-base font-semibold text-text-main">{{ title }}</h3>
      <button v-if="collapsible" type="button" class="btn-ghost !p-1" :aria-label="collapsed ? '展开' : '收起'" @click="collapsed = !collapsed">
        <ChevronDown :size="16" class="transition-transform" :class="{ 'rotate-180': collapsed }" />
      </button>
    </div>
    <div v-show="!collapsed || !collapsible">
      <slot />
    </div>
  </div>
</template>
