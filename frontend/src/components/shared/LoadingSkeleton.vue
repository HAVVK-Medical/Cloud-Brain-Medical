<script setup lang="ts">
withDefaults(defineProps<{
  rows?: number;
  type?: 'card' | 'table' | 'text';
}>(), {
  rows: 4,
  type: 'card',
});
</script>

<template>
  <div class="animate-pulse">
    <template v-if="type === 'card'">
      <div v-for="i in rows" :key="i" class="card mb-3">
        <div class="h-4 bg-gray-200 rounded w-2/3 mb-2" />
        <div class="h-3 bg-gray-100 rounded w-full mb-1" />
        <div class="h-3 bg-gray-100 rounded w-5/6" />
      </div>
    </template>
    <template v-else-if="type === 'table'">
      <div class="h-8 bg-gray-200 rounded w-full mb-2" />
      <div v-for="i in rows" :key="i" class="h-10 bg-gray-100 rounded w-full mb-1" />
    </template>
    <template v-else>
      <div v-for="i in rows" :key="i" class="flex gap-3 mb-3">
        <div class="h-3 bg-gray-200 rounded w-1/4" />
        <div class="h-3 bg-gray-100 rounded w-3/4" />
      </div>
    </template>
  </div>
</template>
