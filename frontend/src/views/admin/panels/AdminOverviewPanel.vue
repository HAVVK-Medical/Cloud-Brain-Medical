<script setup lang="ts">
import SectionCard from '@/components/shared/SectionCard.vue';
import DashboardCharts from '@/components/DashboardCharts.vue';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <div class="space-y-6">
    <div class="grid grid-cols-5 gap-4">
      <div class="card text-center">
        <div class="text-2xl font-bold text-brand">{{ workspace.dashboard?.todayRegistrations ?? 0 }}</div>
        <div class="text-xs text-text-secondary mt-1">今日挂号</div>
      </div>
      <div class="card text-center">
        <div class="text-2xl font-bold text-info">{{ workspace.dashboard?.todayVisits ?? 0 }}</div>
        <div class="text-xs text-text-secondary mt-1">今日接诊</div>
      </div>
      <div class="card text-center">
        <div class="text-2xl font-bold text-warning">{{ workspace.dashboard?.todayPrescriptions ?? 0 }}</div>
        <div class="text-xs text-text-secondary mt-1">处方数</div>
      </div>
      <div class="card text-center">
        <div class="text-2xl font-bold text-info">{{ workspace.aiUsage?.totalCalls ?? 0 }}</div>
        <div class="text-xs text-text-secondary mt-1">AI调用</div>
      </div>
      <div class="card text-center">
        <div class="text-2xl font-bold text-success">{{ workspace.triageAccuracy ? Math.round(workspace.triageAccuracy.accuracyRate * 100) + '%' : '-' }}</div>
        <div class="text-xs text-text-secondary mt-1">分诊准确率</div>
      </div>
    </div>

    <SectionCard title="趋势">
      <DashboardCharts
        :overview="workspace.dashboard"
        :trends="workspace.dashboardTrends"
        :ai-usage="workspace.aiUsage"
        :prescription-review-rate="workspace.prescriptionReviewRate"
        :risk-distribution="workspace.riskDistribution"
        :triage-accuracy="workspace.triageAccuracy"
      />
    </SectionCard>
  </div>
</template>
