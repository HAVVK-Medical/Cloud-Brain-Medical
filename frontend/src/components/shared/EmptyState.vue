<script setup lang="ts">
import { computed } from 'vue';
import { CalendarDays, FileText, Inbox, SearchX } from 'lucide-vue-next';

const props = withDefaults(defineProps<{
  icon?: 'inbox' | 'calendar' | 'file' | 'search';
  title: string;
  description?: string;
  actionLabel?: string;
}>(), {
  icon: 'inbox',
});

const emit = defineEmits<{
  action: [];
}>();

const iconComponent = computed(() => {
  switch (props.icon) {
    case 'calendar': return CalendarDays;
    case 'file': return FileText;
    case 'search': return SearchX;
    default: return Inbox;
  }
});
</script>

<template>
  <div class="flex flex-col items-center justify-center py-16 text-center">
    <component :is="iconComponent" :size="40" class="text-text-secondary/40 mb-4" />
    <h3 class="text-base font-semibold text-text-main mb-1">{{ title }}</h3>
    <p v-if="description" class="text-sm text-text-secondary max-w-sm mb-4">{{ description }}</p>
    <button v-if="actionLabel" class="btn-primary" type="button" @click="emit('action')">
      {{ actionLabel }}
    </button>
  </div>
</template>
