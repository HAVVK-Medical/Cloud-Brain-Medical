<script setup lang="ts">
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <div class="p-4 space-y-4">
    <p class="text-sm font-semibold text-text-main">你好，{{ workspace.displayName }}</p>

    <SectionCard v-if="workspace.activeRegistrations?.length" title="我的挂号">
      <div class="space-y-3">
        <div v-for="reg in workspace.activeRegistrations" :key="reg.id" class="py-2 border-b border-border last:border-b-0">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <p class="text-sm font-medium">{{ workspace.formatDate(reg.workDate) }} · {{ reg.period || '未安排' }}</p>
              <p class="text-xs text-text-secondary mt-0.5">{{ reg.departmentName }} · {{ reg.doctorName }}</p>
            </div>
            <StatusChip :tone="'info'">待就诊</StatusChip>
          </div>
          <button
            type="button"
            class="btn-ghost !px-2 !py-1 !text-xs !text-danger mt-1"
            :disabled="workspace.canceling"
            @click="workspace.requestCancelWaitingRegistration(reg.id)"
          >
            {{ workspace.canceling ? '取消中...' : '取消挂号' }}
          </button>
        </div>
      </div>
    </SectionCard>

    <EmptyState v-if="!workspace.activeRegistrations?.length" icon="calendar" title="暂无挂号记录" description="完成分诊后可在这里查看和操作挂号" action-label="去分诊" @action="$router.push('/patient/triage')" />

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
