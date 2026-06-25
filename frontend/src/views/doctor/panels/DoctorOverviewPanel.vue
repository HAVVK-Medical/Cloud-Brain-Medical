<script setup lang="ts">
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';
import DashboardCharts from '@/components/DashboardCharts.vue';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <div class="space-y-6">
    <div class="grid grid-cols-4 gap-4">
      <div class="card text-center">
        <div class="text-2xl font-bold text-brand">{{ workspace.dashboard?.todayVisits ?? 0 }}</div>
        <div class="text-xs text-text-secondary mt-1">今日接诊</div>
      </div>
      <div class="card text-center">
        <div class="text-2xl font-bold text-warning">{{ workspace.dashboard?.waitingRegistrations ?? 0 }}</div>
        <div class="text-xs text-text-secondary mt-1">待处理</div>
      </div>
      <div class="card text-center">
        <div class="text-2xl font-bold text-danger">{{ workspace.dashboard?.highRiskReviews ?? 0 }}</div>
        <div class="text-xs text-text-secondary mt-1">高风险</div>
      </div>
      <div class="card text-center">
        <div class="text-2xl font-bold text-info">{{ workspace.dashboard?.todayAiCallRecords ?? 0 }}</div>
        <div class="text-xs text-text-secondary mt-1">AI调用</div>
      </div>
    </div>

    <SectionCard title="待接诊队列">
      <div v-if="workspace.queue.length" class="space-y-2">
        <div v-for="reg in workspace.queue.slice(0, 10)" :key="reg.id" class="flex items-center justify-between py-2 border-b border-border last:border-b-0">
          <div>
            <p class="text-sm font-medium">{{ reg.patientName }}</p>
            <p class="text-xs text-text-secondary">{{ workspace.formatDate(reg.registrationTime) }}</p>
          </div>
          <StatusChip :tone="reg.status === 'WAITING' ? 'warning' : 'success'">
            {{ reg.status === 'WAITING' ? '等待接诊' : '就诊中' }}
          </StatusChip>
        </div>
      </div>
      <EmptyState v-else icon="calendar" title="暂无待接诊患者" />
    </SectionCard>

    <SectionCard title="趋势">
      <DashboardCharts
        :overview="workspace.dashboard"
        :trends="workspace.dashboardTrends ?? []"
        :ai-usage="workspace.aiUsage"
        :prescription-review-rate="workspace.prescriptionReviewRate"
        :risk-distribution="workspace.riskDistribution"
        :triage-accuracy="workspace.triageAccuracy"
      />
    </SectionCard>
  </div>
</template>
