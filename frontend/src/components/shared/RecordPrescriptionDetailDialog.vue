<script setup lang="ts">
import { FileText, Pill, X } from 'lucide-vue-next';
import StatusChip from '@/components/shared/StatusChip.vue';
import type { MedicalRecordSummary, PrescriptionItemSummary, PrescriptionSummary } from '@/api/workflow';

const props = withDefaults(defineProps<{
  open: boolean;
  loading?: boolean;
  kind: 'record' | 'prescription' | null;
  medicalRecord?: MedicalRecordSummary | null;
  prescription?: PrescriptionSummary | null;
  formatDateTime: (value: string | null | undefined) => string;
}>(), {
  loading: false,
  medicalRecord: null,
  prescription: null,
});

const emit = defineEmits<{
  close: [];
}>();

function text(value: string | number | null | undefined, fallback = '暂无') {
  if (value === null || value === undefined || value === '') {
    return fallback;
  }
  return String(value);
}

function riskTone(level: string | null | undefined) {
  const normalized = (level || '').toUpperCase();
  if (normalized === 'HIGH' || normalized === 'DANGER' || normalized === 'CRITICAL') {
    return 'danger';
  }
  if (normalized === 'MEDIUM' || normalized === 'WARNING') {
    return 'warning';
  }
  if (!normalized || normalized === 'UNKNOWN') {
    return 'neutral';
  }
  return 'success';
}

function riskLabel(level: string | null | undefined) {
  const normalized = (level || '').toUpperCase();
  if (normalized === 'HIGH') return '高风险';
  if (normalized === 'MEDIUM') return '中风险';
  if (normalized === 'LOW') return '低风险';
  if (normalized === 'MANUAL_REQUIRED') return '需人工确认';
  return text(level, '未评估');
}

function money(value: number | null | undefined) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '暂无';
  }
  return `¥${Number(value).toFixed(2)}`;
}

function itemSubtotal(item: PrescriptionItemSummary) {
  if (item.unitPrice === null || item.unitPrice === undefined || item.quantity === null || item.quantity === undefined) {
    return null;
  }
  return Number(item.unitPrice) * Number(item.quantity);
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center px-4 py-6">
      <div class="absolute inset-0 bg-black/40" @click="!loading && emit('close')" />
      <section class="relative z-10 flex max-h-[90vh] w-full max-w-3xl flex-col overflow-hidden rounded-lg bg-white shadow-xl">
        <header class="flex items-start gap-3 border-b border-border px-5 py-4">
          <div class="mt-0.5 rounded-md bg-blue-50 p-2 text-info">
            <FileText v-if="kind === 'record'" :size="18" />
            <Pill v-else :size="18" />
          </div>
          <div class="min-w-0 flex-1">
            <h2 class="text-base font-semibold text-text-main">
              {{ kind === 'record' ? '病历详情' : '处方详情' }}
            </h2>
            <p class="mt-0.5 text-xs text-text-secondary">
              <template v-if="kind === 'record' && medicalRecord">
                {{ text(medicalRecord.patientName) }} · {{ text(medicalRecord.departmentName) }} · {{ formatDateTime(medicalRecord.createdAt) }}
              </template>
              <template v-else-if="kind === 'prescription' && prescription">
                {{ text(prescription.patientName) }} · {{ text(prescription.departmentName) }} · {{ formatDateTime(prescription.createdAt) }}
              </template>
              <template v-else>
                正在读取详情
              </template>
            </p>
          </div>
          <button type="button" class="btn-ghost !p-1" @click="emit('close')" :disabled="loading">
            <X :size="18" />
          </button>
        </header>

        <div class="overflow-y-auto px-5 py-4">
          <div v-if="loading" class="space-y-3">
            <div class="h-4 w-2/3 rounded bg-gray-100" />
            <div class="h-24 rounded bg-gray-100" />
            <div class="h-24 rounded bg-gray-100" />
          </div>

          <div v-else-if="kind === 'record' && medicalRecord" class="space-y-5 text-sm">
            <div class="grid gap-3 sm:grid-cols-2">
              <div>
                <p class="label-text">患者</p>
                <p class="mt-1 text-text-main">{{ text(medicalRecord.patientName) }}</p>
              </div>
              <div>
                <p class="label-text">医生</p>
                <p class="mt-1 text-text-main">{{ text(medicalRecord.doctorName) }}</p>
              </div>
              <div>
                <p class="label-text">科室</p>
                <p class="mt-1 text-text-main">{{ text(medicalRecord.departmentName) }}</p>
              </div>
              <div>
                <p class="label-text">生成方式</p>
                <StatusChip class="mt-1" :tone="medicalRecord.aiGenerated ? 'info' : 'neutral'">
                  {{ medicalRecord.aiGenerated ? 'AI 辅助' : '手动录入' }}
                </StatusChip>
              </div>
            </div>

            <div class="grid gap-4">
              <div>
                <p class="label-text">主诉</p>
                <p class="mt-1 whitespace-pre-wrap rounded-md bg-gray-50 p-3 text-text-main">{{ text(medicalRecord.chiefComplaint) }}</p>
              </div>
              <div>
                <p class="label-text">现病史</p>
                <p class="mt-1 whitespace-pre-wrap rounded-md bg-gray-50 p-3 text-text-main">{{ text(medicalRecord.presentIllness) }}</p>
              </div>
              <div>
                <p class="label-text">既往史</p>
                <p class="mt-1 whitespace-pre-wrap rounded-md bg-gray-50 p-3 text-text-main">{{ text(medicalRecord.pastHistory) }}</p>
              </div>
              <div>
                <p class="label-text">体格检查</p>
                <p class="mt-1 whitespace-pre-wrap rounded-md bg-gray-50 p-3 text-text-main">{{ text(medicalRecord.physicalExam) }}</p>
              </div>
              <div>
                <p class="label-text">初步诊断</p>
                <p class="mt-1 whitespace-pre-wrap rounded-md bg-gray-50 p-3 text-text-main">{{ text(medicalRecord.preliminaryDiagnosis) }}</p>
              </div>
              <div>
                <p class="label-text">处理方案</p>
                <p class="mt-1 whitespace-pre-wrap rounded-md bg-gray-50 p-3 text-text-main">{{ text(medicalRecord.treatmentPlan) }}</p>
              </div>
              <div>
                <p class="label-text">问诊对话</p>
                <p class="mt-1 max-h-48 overflow-y-auto whitespace-pre-wrap rounded-md bg-gray-50 p-3 text-text-main">{{ text(medicalRecord.conversationText) }}</p>
              </div>
              <div>
                <p class="label-text">医生备注</p>
                <p class="mt-1 whitespace-pre-wrap rounded-md bg-gray-50 p-3 text-text-main">{{ text(medicalRecord.docNote) }}</p>
              </div>
            </div>
          </div>

          <div v-else-if="kind === 'prescription' && prescription" class="space-y-5 text-sm">
            <div class="grid gap-3 sm:grid-cols-3">
              <div>
                <p class="label-text">患者</p>
                <p class="mt-1 text-text-main">{{ text(prescription.patientName) }}</p>
              </div>
              <div>
                <p class="label-text">医生</p>
                <p class="mt-1 text-text-main">{{ text(prescription.doctorName) }}</p>
              </div>
              <div>
                <p class="label-text">状态</p>
                <p class="mt-1 text-text-main">{{ text(prescription.status) }}</p>
              </div>
              <div>
                <p class="label-text">风险等级</p>
                <StatusChip class="mt-1" :tone="riskTone(prescription.riskLevel)">
                  {{ riskLabel(prescription.riskLevel) }}
                </StatusChip>
              </div>
              <div>
                <p class="label-text">处方编号</p>
                <p class="mt-1 text-text-main">#{{ prescription.id }}</p>
              </div>
              <div>
                <p class="label-text">提交时间</p>
                <p class="mt-1 text-text-main">{{ formatDateTime(prescription.createdAt) }}</p>
              </div>
            </div>

            <div>
              <p class="label-text mb-2">药品明细</p>
              <div class="overflow-x-auto rounded-md border border-border">
                <table class="min-w-full text-left text-xs">
                  <thead class="bg-gray-50 text-text-secondary">
                    <tr>
                      <th class="px-3 py-2 font-medium">药品</th>
                      <th class="px-3 py-2 font-medium">规格</th>
                      <th class="px-3 py-2 font-medium">剂量</th>
                      <th class="px-3 py-2 font-medium">频次</th>
                      <th class="px-3 py-2 font-medium">疗程</th>
                      <th class="px-3 py-2 font-medium">数量</th>
                      <th class="px-3 py-2 font-medium">单价</th>
                      <th class="px-3 py-2 font-medium">小计</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in prescription.items" :key="item.id ?? `${item.drugId}-${item.drugName}`" class="border-t border-border">
                      <td class="px-3 py-2 text-text-main">{{ text(item.drugName) }}</td>
                      <td class="px-3 py-2 text-text-secondary">{{ text(item.specification) }}</td>
                      <td class="px-3 py-2 text-text-main">{{ text(item.dosage) }}</td>
                      <td class="px-3 py-2 text-text-main">{{ text(item.frequency) }}</td>
                      <td class="px-3 py-2 text-text-main">{{ text(item.duration) }}</td>
                      <td class="px-3 py-2 text-text-main">{{ text(item.quantity) }} {{ text(item.packageUnit, '') }}</td>
                      <td class="px-3 py-2 text-text-main">{{ money(item.unitPrice) }}</td>
                      <td class="px-3 py-2 text-text-main">{{ itemSubtotal(item) === null ? '暂无' : money(itemSubtotal(item)) }}</td>
                    </tr>
                    <tr v-if="!prescription.items.length">
                      <td colspan="8" class="px-3 py-4 text-center text-text-secondary">暂无药品明细</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <div v-if="prescription.items.some((item) => item.usageInstruction)" class="space-y-2">
              <p class="label-text">用药说明</p>
              <div v-for="item in prescription.items.filter((entry) => entry.usageInstruction)" :key="`usage-${item.id ?? item.drugId}`" class="rounded-md bg-gray-50 p-3">
                <p class="font-medium text-text-main">{{ text(item.drugName) }}</p>
                <p class="mt-1 whitespace-pre-wrap text-text-secondary">{{ item.usageInstruction }}</p>
              </div>
            </div>

            <div v-if="prescription.review" class="space-y-3">
              <p class="label-text">AI 审方结果</p>
              <div class="grid gap-3 sm:grid-cols-2">
                <div class="rounded-md bg-gray-50 p-3">
                  <p class="label-text">规则命中</p>
                  <p class="mt-1 whitespace-pre-wrap text-text-main">{{ text(prescription.review.localRuleHits) }}</p>
                </div>
                <div class="rounded-md bg-gray-50 p-3">
                  <p class="label-text">缺失上下文</p>
                  <p class="mt-1 whitespace-pre-wrap text-text-main">{{ text(prescription.review.contextMissingItems) }}</p>
                </div>
              </div>
              <div class="rounded-md bg-gray-50 p-3">
                <p class="label-text">审方摘要</p>
                <p class="mt-1 whitespace-pre-wrap text-text-main">{{ text(prescription.review.llmSummary) }}</p>
              </div>
              <div class="rounded-md bg-gray-50 p-3">
                <p class="label-text">用药建议</p>
                <p class="mt-1 whitespace-pre-wrap text-text-main">{{ text(prescription.review.llmSuggestion) }}</p>
              </div>
            </div>
          </div>

          <p v-else class="py-8 text-center text-sm text-text-secondary">未找到详情数据</p>
        </div>
      </section>
    </div>
  </Teleport>
</template>
