<script setup lang="ts">
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <SectionCard title="我的排班">
    <div v-if="workspace.schedules.length" class="space-y-2">
      <div v-for="slot in workspace.schedules" :key="slot.id" class="flex items-center justify-between py-3 px-3 rounded-lg border border-border">
        <div>
          <p class="text-sm font-medium">{{ slot.workDate }} · {{ slot.period }}</p>
          <p class="text-xs text-text-secondary">{{ slot.departmentName }} · {{ slot.visitLevel }}</p>
        </div>
        <div class="text-right">
          <p class="text-sm font-medium">{{ slot.remainingSlots }}/{{ slot.totalSlots }}</p>
          <StatusChip :tone="slot.remainingSlots && slot.remainingSlots > 0 ? 'success' : 'danger'">
            {{ slot.remainingSlots && slot.remainingSlots > 0 ? '有余号' : '已约满' }}
          </StatusChip>
        </div>
      </div>
    </div>
    <EmptyState v-else icon="calendar" title="暂无排班" description="暂无当前排班数据" />
  </SectionCard>
</template>
