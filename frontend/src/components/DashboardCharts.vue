<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts/core';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
  TitleComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { EChartsCoreOption } from 'echarts/core';

import type {
  AiUsageStats,
  DashboardOverview,
  DashboardTrendPoint,
  PrescriptionReviewRate,
  RiskDistribution,
  TriageAccuracyStats,
} from '@/api/workflow';

echarts.use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, TitleComponent, CanvasRenderer]);

const props = defineProps<{
  overview: DashboardOverview | null;
  trends: DashboardTrendPoint[];
  aiUsage: AiUsageStats | null;
  prescriptionReviewRate: PrescriptionReviewRate | null;
  riskDistribution: RiskDistribution | null;
  triageAccuracy: TriageAccuracyStats | null;
}>();

const trendEl = ref<HTMLDivElement | null>(null);
const aiEl = ref<HTMLDivElement | null>(null);
const riskEl = ref<HTMLDivElement | null>(null);
const triageEl = ref<HTMLDivElement | null>(null);

let trendChart: echarts.ECharts | null = null;
let aiChart: echarts.ECharts | null = null;
let riskChart: echarts.ECharts | null = null;
let triageChart: echarts.ECharts | null = null;

const reviewRateItems = computed(() => {
  const rate = props.prescriptionReviewRate;
  return [
    { label: '低风险通过', value: rate?.lowRiskReviews ?? 0 },
    { label: '中风险', value: rate?.mediumRiskReviews ?? 0 },
    { label: '高风险', value: rate?.highRiskReviews ?? 0 },
    { label: '需人工确认', value: rate?.manualRequiredReviews ?? 0 },
    { label: '未知', value: rate?.unknownReviews ?? 0 },
  ];
});

function initCharts() {
  if (trendEl.value && !trendChart) trendChart = echarts.init(trendEl.value);
  if (aiEl.value && !aiChart) aiChart = echarts.init(aiEl.value);
  if (riskEl.value && !riskChart) riskChart = echarts.init(riskEl.value);
  if (triageEl.value && !triageChart) triageChart = echarts.init(triageEl.value);
}

function renderCharts() {
  initCharts();
  trendChart?.setOption(trendOption());
  aiChart?.setOption(aiOption());
  riskChart?.setOption(riskOption());
  triageChart?.setOption(triageOption());
}

function trendOption(): EChartsCoreOption {
  const dates = props.trends.map((item) => item.date.slice(5));
  return {
    color: ['#116b66', '#1f7a3d', '#b46a12', '#4f6f9f'],
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, textStyle: { color: '#5d6c69' } },
    grid: { top: 28, left: 36, right: 16, bottom: 56 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '挂号', type: 'line', smooth: true, data: props.trends.map((item) => item.registrations) },
      { name: '就诊', type: 'line', smooth: true, data: props.trends.map((item) => item.visits) },
      { name: '处方', type: 'bar', data: props.trends.map((item) => item.prescriptions) },
      { name: 'AI 调用', type: 'bar', data: props.trends.map((item) => item.aiCalls) },
    ],
  };
}

function aiOption(): EChartsCoreOption {
  const buckets = props.aiUsage?.buckets ?? [];
  return {
    color: ['#116b66', '#b46a12', '#4f6f9f', '#8a5a44'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#5d6c69' } },
    series: [
      {
        name: 'AI 使用',
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '44%'],
        data: buckets.length
          ? buckets.map((item) => ({ name: item.taskType, value: item.calls }))
          : [{ name: '暂无调用', value: 0 }],
      },
    ],
  };
}

function riskOption(): EChartsCoreOption {
  const buckets = props.riskDistribution?.buckets ?? [];
  return {
    color: ['#1f7a3d', '#b46a12', '#b3261e', '#6f7775'],
    tooltip: { trigger: 'axis' },
    grid: { top: 28, left: 36, right: 16, bottom: 32 },
    xAxis: { type: 'category', data: buckets.map((item) => item.riskLevel) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ name: '风险分布', type: 'bar', data: buckets.map((item) => item.count) }],
  };
}

function triageOption(): EChartsCoreOption {
  const stats = props.triageAccuracy;
  return {
    color: ['#1f7a3d', '#b3261e', '#6f7775'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#5d6c69' } },
    series: [
      {
        name: '分诊反馈',
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '44%'],
        data: [
          { name: '准确', value: stats?.accurateCount ?? 0 },
          { name: '不准确', value: stats?.inaccurateCount ?? 0 },
          { name: '无反馈', value: stats?.noFeedbackCount ?? 0 },
        ],
      },
    ],
  };
}

function resizeCharts() {
  trendChart?.resize();
  aiChart?.resize();
  riskChart?.resize();
  triageChart?.resize();
}

onMounted(() => {
  void nextTick(renderCharts);
  window.addEventListener('resize', resizeCharts);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts);
  trendChart?.dispose();
  aiChart?.dispose();
  riskChart?.dispose();
  triageChart?.dispose();
});

watch(() => props, () => void nextTick(renderCharts), { deep: true });
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <div>
        <h3 class="text-base font-semibold text-text-main">数据看板</h3>
        <p class="text-xs text-text-secondary mt-0.5">就诊趋势、AI 使用、处方风险和分诊反馈按当前账号视角汇总。</p>
      </div>
      <span class="text-xs text-text-secondary">
        更新 {{ overview?.updatedAt ? new Date(overview.updatedAt).toLocaleTimeString('zh-CN', { hour12: false }) : '待刷新' }}
      </span>
    </div>

    <div class="grid grid-cols-2 gap-4">
      <article class="col-span-2 border border-border rounded-lg p-4 bg-white">
        <div class="text-sm font-medium text-text-main mb-2">近 14 日趋势</div>
        <div ref="trendEl" style="height: 280px" />
      </article>
      <article class="border border-border rounded-lg p-4 bg-white">
        <div class="text-sm font-medium text-text-main mb-2">AI 使用率</div>
        <div ref="aiEl" style="height: 240px" />
      </article>
      <article class="border border-border rounded-lg p-4 bg-white">
        <div class="text-sm font-medium text-text-main mb-2">风险分布</div>
        <div ref="riskEl" style="height: 240px" />
      </article>
      <article class="col-span-2 border border-border rounded-lg p-4 bg-white">
        <div class="text-sm font-medium text-text-main mb-2">分诊准确率 {{ triageAccuracy?.accuracyRate ? Math.round(triageAccuracy.accuracyRate * 100) : 0 }}%</div>
        <div ref="triageEl" style="height: 260px" />
      </article>
    </div>

    <div class="grid grid-cols-6 gap-3">
      <div v-for="item in reviewRateItems" :key="item.label" class="card !p-3 text-center">
        <span class="text-xs text-text-secondary">{{ item.label }}</span>
        <span class="block text-lg font-bold text-text-main">{{ item.value }}</span>
      </div>
      <div class="card !p-3 text-center">
        <span class="text-xs text-text-secondary">审核通过率</span>
        <span class="block text-lg font-bold text-text-main">{{ prescriptionReviewRate?.passRate ?? 0 }}%</span>
      </div>
    </div>
  </div>
</template>
