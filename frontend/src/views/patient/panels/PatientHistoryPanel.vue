<script setup lang="ts">
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <div class="p-4 space-y-4">
    <SectionCard title="分诊历史">
      <div v-if="workspace.triageHistory.length" class="space-y-3">
        <div v-for="item in workspace.triageHistory" :key="item.triageRecordId" class="py-2 border-b border-border last:border-b-0">
          <p class="text-sm font-medium">{{ workspace.truncate(item.chiefComplaint, 60) }}</p>
          <div class="flex items-center gap-2 mt-1">
            <StatusChip tone="success">{{ item.recommendedDept }}</StatusChip>
            <StatusChip tone="info">{{ item.recommendationSource }}</StatusChip>
          </div>
        </div>
      </div>
      <EmptyState v-else icon="search" title="暂无分诊历史" description="完成分诊后可在这里查看记录" />
    </SectionCard>

    <SectionCard title="挂号历史">
      <div v-if="workspace.registrations.length" class="space-y-2">
        <div v-for="reg in workspace.registrations" :key="reg.id" class="py-2 border-b border-border last:border-b-0 text-sm flex justify-between">
          <div>
            <span class="font-medium">{{ workspace.formatDate(reg.workDate) }}</span>
            <span class="text-text-secondary"> · {{ reg.doctorName }}</span>
          </div>
          <StatusChip :tone="reg.status === 'CANCELLED' ? 'danger' : reg.status === 'COMPLETED' ? 'success' : 'info'">
            {{ reg.status === 'WAITING' ? '待就诊' : reg.status === 'COMPLETED' ? '已完成' : reg.status === 'CANCELLED' ? '已取消' : reg.status }}
          </StatusChip>
        </div>
      </div>
      <EmptyState v-else icon="calendar" title="暂无挂号历史" />
    </SectionCard>

    <SectionCard title="反馈记录">
      <div v-if="workspace.feedbacks.length" class="space-y-2">
        <div v-for="fb in workspace.feedbacks" :key="fb.id" class="py-2 border-b border-border last:border-b-0 text-sm">
          <div class="flex items-center gap-1 text-warning">
            <span v-for="n in 5" :key="n">{{ n <= fb.rating ? '★' : '☆' }}</span>
          </div>
          <p v-if="fb.comment" class="text-text-secondary mt-0.5">{{ fb.comment }}</p>
        </div>
      </div>
      <EmptyState v-else icon="file" title="暂无反馈记录" />
    </SectionCard>
  </div>
</template>
