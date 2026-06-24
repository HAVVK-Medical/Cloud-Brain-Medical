<script setup lang="ts">
import { defineAsyncComponent } from 'vue';

const DashboardCharts = defineAsyncComponent(() => import('@/components/DashboardCharts.vue'));

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">工作台总览</h3>
        <p class="section-copy">先看今日接诊、队列和风险，再进入接诊区。</p>
      </div>
      <span class="status-chip" :data-tone="workspace.overviewTone">
        <span class="chip-dot" />
        <span>{{ workspace.doctor?.name || workspace.authStore.sessionLabel }}</span>
      </span>
    </div>

    <div class="metric-grid workspace-metrics">
      <article class="metric workspace-metric">
        <div class="card-head">
          <h3>待接诊</h3>
        </div>
        <div class="metric-value">{{ workspace.queue.length }}</div>
        <p>当前队列中的挂号可以直接点开进入接诊。</p>
      </article>
      <article class="metric workspace-metric">
        <div class="card-head">
          <h3>未读告警</h3>
        </div>
        <div class="metric-value">{{ workspace.unreadNotificationCount }}</div>
        <p>高风险处方与人工复核提醒会在这里汇总。</p>
      </article>
      <article class="metric workspace-metric">
        <div class="card-head">
          <h3>今天病历</h3>
        </div>
        <div class="metric-value">{{ workspace.medicalRecords.length }}</div>
        <p>已处理和待回看的病历都能在历史区找到。</p>
      </article>
      <article class="metric workspace-metric">
        <div class="card-head">
          <h3>最近更新时间</h3>
        </div>
        <div class="metric-value">{{ workspace.formatDateTime(workspace.dashboard?.updatedAt) }}</div>
        <p>看板、队列、审方和通知都在同步刷新。</p>
      </article>
    </div>

    <div class="workspace-summary-grid">
      <div class="mini-item">
        <div class="mini-item-head">
          <div class="mini-item-title">当前接诊</div>
          <span class="pill" :data-tone="workspace.workspaceTone">{{ workspace.selectedRegistration?.status || 'NONE' }}</span>
        </div>
        <p class="mini-item-copy">{{ workspace.selectedRegistration?.chiefComplaint || '请选择一个待接诊挂号' }}</p>
      </div>
      <div class="mini-item">
        <div class="mini-item-head">
          <div class="mini-item-title">病历草稿</div>
          <span class="pill">{{ workspace.workspace?.latestMedicalRecord ? '已加载' : '待生成' }}</span>
        </div>
        <p class="mini-item-copy">问诊结束后可直接生成草稿并人工修订。</p>
      </div>
      <div class="mini-item">
        <div class="mini-item-head">
          <div class="mini-item-title">处方状态</div>
          <span class="pill">{{ workspace.workspace?.latestPrescription?.review?.reviewStatus || '未审方' }}</span>
        </div>
        <p class="mini-item-copy">本地规则优先，模型解释作为辅助输出。</p>
      </div>
    </div>

    <DashboardCharts
      :overview="workspace.dashboard"
      :trends="workspace.dashboardTrends"
      :ai-usage="workspace.aiUsage"
      :prescription-review-rate="workspace.prescriptionReviewRate"
      :risk-distribution="workspace.riskDistribution"
      :triage-accuracy="workspace.triageAccuracy"
    />

    <div class="action-row">
      <button class="button-secondary" type="button" @click="workspace.setActiveDoctorPanel('consultation')">进入接诊</button>
      <button class="button-secondary" type="button" @click="workspace.setActiveDoctorPanel('history')">查看历史</button>
      <button class="button-ghost" type="button" @click="workspace.setActiveDoctorPanel('schedule')">查看排班</button>
    </div>
  </section>
</template>
