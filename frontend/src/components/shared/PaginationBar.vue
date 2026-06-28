<script setup lang="ts">
import { ChevronLeft, ChevronRight } from 'lucide-vue-next';

const props = defineProps<{
  page: number;
  pageCount: number;
  total: number;
  pageSize: number;
}>();

const emit = defineEmits<{
  (e: 'update:page', value: number): void;
}>();

function go(delta: number) {
  const nextPage = Math.min(Math.max(1, props.page + delta), Math.max(1, props.pageCount));
  emit('update:page', nextPage);
}
</script>

<template>
  <div v-if="pageCount > 1" class="mt-3 flex items-center justify-between gap-3 border-t border-border pt-3">
    <div class="text-xs text-text-secondary">
      共 {{ total }} 条，每页 {{ pageSize }} 条
    </div>
    <div class="flex items-center gap-2">
      <button
        class="inline-flex h-8 w-8 items-center justify-center rounded-md border border-border hover:bg-gray-50 disabled:opacity-40"
        type="button"
        :disabled="page <= 1"
        title="上一页"
        @click="go(-1)"
      >
        <ChevronLeft :size="16" />
      </button>
      <span class="min-w-20 text-center text-xs text-text-secondary">
        {{ page }} / {{ pageCount }}
      </span>
      <button
        class="inline-flex h-8 w-8 items-center justify-center rounded-md border border-border hover:bg-gray-50 disabled:opacity-40"
        type="button"
        :disabled="page >= pageCount"
        title="下一页"
        @click="go(1)"
      >
        <ChevronRight :size="16" />
      </button>
    </div>
  </div>
</template>
