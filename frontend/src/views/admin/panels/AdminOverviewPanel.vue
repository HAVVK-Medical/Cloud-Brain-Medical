<script setup lang="ts">
import { defineAsyncComponent } from 'vue';

const DashboardCharts = defineAsyncComponent(() => import('@/components/DashboardCharts.vue'));

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">后台总览</h3>
        <p class="section-copy">先看今日挂号、待接诊、AI 调用和高风险审方，再进入具体维护页。</p>
      </div>
      <span class="status-chip" :data-tone="workspace.activeTone">
        <span class="chip-dot" />
        <span>{{ workspace.authStore.sessionLabel }}</span>
      </span>
    </div>

    <div class="metric-grid workspace-metrics">
      <article class="metric workspace-metric">
        <div class="card-head">
          <h3>今日挂号</h3>
        </div>
        <div class="metric-value">{{ workspace.dashboard?.todayRegistrations ?? 0 }}</div>
        <p>今天的门诊挂号量。</p>
      </article>
      <article class="metric workspace-metric">
        <div class="card-head">
          <h3>待接诊</h3>
        </div>
        <div class="metric-value">{{ workspace.dashboard?.waitingRegistrations ?? 0 }}</div>
        <p>未进入接诊的挂号排队状态。</p>
      </article>
      <article class="metric workspace-metric">
        <div class="card-head">
          <h3>AI 调用</h3>
        </div>
        <div class="metric-value">{{ workspace.dashboard?.aiCallRecords ?? 0 }}</div>
        <p>分诊、病历和审方的 AI 相关调用总量。</p>
      </article>
      <article class="metric workspace-metric">
        <div class="card-head">
          <h3>高风险审方</h3>
        </div>
        <div class="metric-value">{{ workspace.dashboard?.highRiskReviews ?? 0 }}</div>
        <p>需要关注的风险处方数。</p>
      </article>
    </div>

    <div class="workspace-summary-grid">
      <div class="mini-item">
        <div class="mini-item-head">
          <div class="mini-item-title">数据更新时间</div>
          <span class="pill">{{ workspace.formatDateTime(workspace.dashboard?.updatedAt) }}</span>
        </div>
        <p class="mini-item-copy">看板、审计和告警会跟随刷新同步。</p>
      </div>
      <div class="mini-item">
        <div class="mini-item-head">
          <div class="mini-item-title">当前筛选</div>
          <span class="pill">{{ workspace.visibleSchedules.length }}</span>
        </div>
        <p class="mini-item-copy">科室和医生筛选会影响资源列表。</p>
      </div>
      <div class="mini-item">
        <div class="mini-item-head">
          <div class="mini-item-title">未读告警</div>
          <span class="pill" :data-tone="workspace.notificationSocketState === 'connected' ? 'healthy' : 'loading'">
            {{ workspace.unreadNotificationCount }}
          </span>
        </div>
        <p class="mini-item-copy">处方风险与人工复核提醒集中显示。</p>
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
      <button class="button-secondary" type="button" @click="workspace.setActiveAdminPanel('master')">基础资料</button>
      <button class="button-secondary" type="button" @click="workspace.setActiveAdminPanel('resources')">排班 / 药品</button>
      <button class="button-secondary" type="button" @click="workspace.setActiveAdminPanel('config')">规则 / 配置</button>
      <button class="button-ghost" type="button" @click="workspace.setActiveAdminPanel('audit')">审计记录</button>
    </div>
  </section>
</template>
