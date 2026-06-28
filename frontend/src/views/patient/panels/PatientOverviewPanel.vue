<script setup lang="ts">
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <div class="p-4 space-y-4">
    <p class="text-sm font-semibold text-text-main">你好，{{ workspace.displayName }}</p>

    <SectionCard v-if="workspace.latestRegistration" title="当前挂号">
      <div class="space-y-2 text-sm">
        <div class="flex justify-between">
          <span class="text-text-secondary">科室</span>
          <span class="font-medium">{{ workspace.latestRegistration.departmentName }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-text-secondary">医生</span>
          <span class="font-medium">{{ workspace.latestRegistration.doctorName }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-text-secondary">日期</span>
          <span class="font-medium">{{ workspace.formatDate(workspace.latestRegistration.workDate) }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-text-secondary">状态</span>
          <StatusChip :tone="workspace.latestRegistration.status === 'WAITING' ? 'info' : workspace.latestRegistration.status === 'COMPLETED' ? 'success' : 'neutral'">
            {{ workspace.latestRegistration.status === 'WAITING' ? '待就诊' : workspace.latestRegistration.status === 'COMPLETED' ? '已完成' : workspace.latestRegistration.status }}
          </StatusChip>
        </div>
      </div>
    </SectionCard>

    <EmptyState v-else icon="calendar" title="暂无挂号记录" description="完成分诊后可在这里查看和操作挂号" action-label="去分诊" @action="$router.push('/patient/triage')" />

    <SectionCard v-if="workspace.latestTriage" title="最近分诊结果">
      <div class="space-y-2 text-sm">
        <div>
          <span class="text-text-secondary">主诉</span>
          <p class="mt-0.5 text-text-main">{{ workspace.truncate(workspace.latestTriage.chiefComplaint, 100) }}</p>
        </div>
        <div class="flex justify-between">
          <span class="text-text-secondary">推荐科室</span>
          <span class="font-medium text-brand">{{ workspace.latestTriage.recommendedDept }}</span>
        </div>
      </div>
    </SectionCard>

    <div class="grid grid-cols-2 gap-3">
      <div class="card text-center">
        <div class="text-2xl font-bold text-brand">{{ workspace.waitingRegistrations.length }}</div>
        <div class="text-xs text-text-secondary mt-1">待就诊</div>
      </div>
      <div class="card text-center">
        <div class="text-2xl font-bold text-success">{{ workspace.completedRegistrations.length }}</div>
        <div class="text-xs text-text-secondary mt-1">已完成</div>
      </div>
    </div>
  </div>
</template>
