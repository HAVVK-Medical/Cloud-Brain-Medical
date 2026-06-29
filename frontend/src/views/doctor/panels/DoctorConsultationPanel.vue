<script setup lang="ts">
import { ref, watch } from 'vue';
import { Sparkles, PanelRightOpen, CheckCircle2 } from 'lucide-vue-next';
import SectionCard from '@/components/shared/SectionCard.vue';
import EmptyState from '@/components/shared/EmptyState.vue';
import LoadingSkeleton from '@/components/shared/LoadingSkeleton.vue';
import ConfirmDialog from '@/components/shared/ConfirmDialog.vue';
import WorkflowSidebar from '@/components/workflow/WorkflowSidebar.vue';
import { useAiStreamStore } from '@/stores/ai-stream';

const { workspace } = defineProps<{ workspace: any }>();

const sidebarOpen = ref(false);
const sidebarRef = ref<InstanceType<typeof WorkflowSidebar> | null>(null);
const aiStreamStore = useAiStreamStore();

function toggleSidebar() {
  sidebarOpen.value = !sidebarOpen.value;
}

function openSidebarForStep(stepId: string) {
  sidebarOpen.value = true;
  sidebarRef.value?.triggerStep(stepId);
}

function runMedicalRecordWorkflow() {
  openSidebarForStep('MEDICAL_RECORD');
}

function runDiagnosisWorkflow() {
  openSidebarForStep('DIAGNOSIS');
}

function runPrescriptionReviewWorkflow() {
  openSidebarForStep('PRESCRIPTION_REVIEW');
}

async function runSidebarStep(stepId: string, action: () => Promise<boolean> | boolean, fallbackMessage: string) {
  const ok = await action();
  if (!ok && sidebarRef.value) {
    sidebarRef.value.setStepError(stepId, workspace.error || fallbackMessage);
  }
}

// Handle sidebar step trigger
function onSidebarTrigger(stepId: string) {
  switch (stepId) {
    case 'MEDICAL_RECORD':
      void runSidebarStep('MEDICAL_RECORD', workspace.generateDraftMedicalRecord, '请先填写问诊对话内容');
      break;
    case 'DIAGNOSIS':
      void runSidebarStep('DIAGNOSIS', workspace.diagnoseCurrentCase, '请先填写问诊对话内容');
      break;
    case 'PRESCRIPTION_REVIEW':
      void runSidebarStep(
        'PRESCRIPTION_REVIEW',
        () => workspace.reviewCurrentPrescription?.() ?? false,
        '请先至少添加一条处方项目',
      );
      break;
  }
}

// Handle sidebar adopt
function onSidebarAdopt(stepId: string) {
  switch (stepId) {
    case 'MEDICAL_RECORD':
      // Record is already populated by stream, save it
      void workspace.saveCurrentMedicalRecord();
      break;
    case 'DIAGNOSIS':
      // Parse the diagnosis text and adopt first diagnosis
      if (workspace.diagnosisSuggestion?.suggestedDiagnoses) {
        const first = String(workspace.diagnosisSuggestion.suggestedDiagnoses).split('\n')[0]?.trim();
        if (first) workspace.recordForm.preliminaryDiagnosis = first;
      }
      break;
    case 'PRESCRIPTION_REVIEW':
      // Review results are already displayed
      break;
  }
}

// Watch ai-stream store to sync content to sidebar
watch(() => aiStreamStore.streamText, (text) => {
  if (!sidebarRef.value || !text) return;
  // Determine which step is running
  if (aiStreamStore.streaming) {
    // Content is updated in real-time via the store
    if (workspace.generatingRecord) {
      sidebarRef.value.updateStepContent('MEDICAL_RECORD', text);
    } else if (workspace.diagnosingRecord) {
      sidebarRef.value.updateStepContent('DIAGNOSIS', text);
    }
  }
});

watch(() => workspace.selectedRegistrationId, () => {
  sidebarRef.value?.reset();
  sidebarOpen.value = false;
});

watch(() => workspace.generatingRecord, (val) => {
  if (!val && sidebarRef.value) {
    if (workspace.recordDegraded || workspace.error) {
      sidebarRef.value.setStepError('MEDICAL_RECORD', workspace.error || '生成病历失败');
    } else {
      sidebarRef.value.setStepCompleted('MEDICAL_RECORD');
    }
  }
});

watch(() => workspace.diagnosingRecord, (val) => {
  if (!val && sidebarRef.value) {
    if (workspace.diagnosisDegraded || workspace.error) {
      sidebarRef.value.setStepError('DIAGNOSIS', workspace.error || '生成诊断建议失败');
      return;
    }
    if (workspace.diagnosisSuggestion) {
      sidebarRef.value.setStepCompleted('DIAGNOSIS');
      const text = workspace.diagnosisSuggestion.suggestedDiagnoses || '';
      sidebarRef.value.updateStepContent('DIAGNOSIS', text);
    }
  }
});

watch(() => workspace.reviewingPrescription, (val) => {
  if (!val && sidebarRef.value) {
    if (workspace.error || !workspace.reviewResult) {
      sidebarRef.value.setStepError('PRESCRIPTION_REVIEW', workspace.error || '处方审查失败');
      return;
    }
    sidebarRef.value.setStepCompleted('PRESCRIPTION_REVIEW');
    const r = workspace.reviewResult;
    const content = [
      r.riskLevel ? `风险等级: ${r.riskLevel}` : '',
      r.localRuleHits ? `本地规则命中: ${r.localRuleHits}` : '',
      r.llmSuggestion || '',
    ].filter(Boolean).join('\n\n');
    sidebarRef.value.updateStepContent('PRESCRIPTION_REVIEW', content);
  }
});

interface DiagnosisEntry { name: string; confidence: number }
interface RuleHitEntry { ruleName?: string; alertMessage?: string; suggestion?: string; riskLevel?: string }
interface DrugEntry {
  id: number;
  name: string;
  specification?: string | null;
  dosageForm?: string | null;
  packageUnit?: string | null;
  unitPrice?: number | null;
  defaultUsage?: string | null;
  contraindications?: string | null;
}

function parseDiagnoses(text: string): DiagnosisEntry[] {
  if (!text) return [];
  return text.split('\n').filter(Boolean).map(line => {
    const match = line.match(/^(.+?)\s*(\d{1,3})%?\s*$/);
    if (match) return { name: match[1].trim(), confidence: parseInt(match[2]) };
    return { name: line.replace(/^[-*\d.]+\s*/, '').trim(), confidence: 0 };
  });
}

function parseExamItems(text: string): string[] {
  if (!text) return [];
  return text.split('\n').filter(Boolean).map(line => line.replace(/^[-*\d.]+\s*/, '').trim());
}

function confidenceBg(conf: number): string {
  if (conf >= 75) return '#d4edda';
  if (conf >= 50) return '#fff3cd';
  return '#f8f0ff';
}

function confidenceFg(conf: number): string {
  if (conf >= 75) return '#155724';
  if (conf >= 50) return '#856404';
  return '#6f42c1';
}

function adoptDiagnosis(name: string) {
  void workspace.adoptCurrentDiagnosis(name);
}

function drugMeta(drug: DrugEntry): string {
  return [drug.specification, drug.dosageForm, drug.packageUnit].filter(Boolean).join(' · ') || '暂无规格信息';
}

function selectedDrug(item: { drugId: number | null }) {
  return workspace.availableDrugs.find((drug: DrugEntry) => drug.id === item.drugId) as DrugEntry | undefined;
}

function drugPrice(drug: DrugEntry | undefined) {
  if (!drug || drug.unitPrice === null || drug.unitPrice === undefined) {
    return '暂无单价';
  }
  return `¥${Number(drug.unitPrice).toFixed(2)}`;
}

function parseRuleHits(raw: string | Record<string, unknown>[]): RuleHitEntry[] {
  if (Array.isArray(raw)) return raw as RuleHitEntry[];
  if (typeof raw === 'string') {
    try { return JSON.parse(raw) as RuleHitEntry[]; } catch { return []; }
  }
  return [];
}

function riskLabel(level: string | null | undefined): string {
  switch ((level || '').toUpperCase()) {
    case 'HIGH': case 'DANGER': case 'CRITICAL': return '高风险';
    case 'MEDIUM': case 'WARNING': return '中风险';
    default: return '低风险';
  }
}

function riskBarClass(level: string | null | undefined): string {
  switch ((level || '').toUpperCase()) {
    case 'HIGH': case 'DANGER': case 'CRITICAL': return 'bg-red-50 text-danger border-l-2 border-danger';
    case 'MEDIUM': case 'WARNING': return 'bg-yellow-50 text-warning border-l-2 border-warning';
    default: return 'bg-green-50 text-success border-l-2 border-success';
  }
}

function ruleRiskClass(level: string | null | undefined): string {
  switch ((level || '').toUpperCase()) {
    case 'HIGH': case 'DANGER': case 'CRITICAL': return 'bg-red-100 text-danger';
    case 'MEDIUM': case 'WARNING': return 'bg-yellow-100 text-warning';
    default: return 'bg-green-100 text-success';
  }
}

function beginButtonLabel() {
  if (workspace.startingConsultation) return '接诊中...';
  if (workspace.canBeginSelectedConsultation) return '开始接诊';
  return workspace.consultationStatusLabel || '已开始';
}
</script>

<template>
  <div class="space-y-6">
    <div class="grid grid-cols-[240px_1fr] gap-6">
      <!-- Left: Patient queue -->
      <SectionCard title="患者队列" class="h-fit">
        <div v-if="workspace.queue.length" class="space-y-1">
          <button
            v-for="reg in workspace.queue"
            :key="reg.id"
            type="button"
            class="w-full text-left px-3 py-2 rounded-md text-sm transition"
            :class="workspace.selectedRegistrationId === reg.id ? 'bg-brand-soft text-brand font-medium' : 'hover:bg-gray-50'"
            @click="workspace.selectRegistration(reg.id)"
          >
            <p>{{ reg.patientName }}</p>
            <p class="text-xs text-text-secondary">{{ workspace.truncate(reg.chiefComplaint, 20) }}</p>
          </button>
        </div>
        <EmptyState v-else icon="calendar" title="暂无患者" />
      </SectionCard>

      <!-- Right: Workspace -->
      <div class="space-y-4">
        <LoadingSkeleton v-if="workspace.workspaceLoading" :rows="3" />

        <div v-else-if="workspace.workspace" class="space-y-4">
          <!-- Quick actions -->
          <div class="flex items-center gap-2 flex-wrap">
            <button
              class="btn-primary"
              type="button"
              @click="workspace.beginSelectedConsultation()"
              :disabled="workspace.startingConsultation || !workspace.canBeginSelectedConsultation"
              :class="{ 'opacity-75 cursor-default': !workspace.canBeginSelectedConsultation }"
            >
              <CheckCircle2 v-if="!workspace.canBeginSelectedConsultation" :size="16" />
              {{ beginButtonLabel() }}
            </button>
              <button class="btn-danger" type="button" @click="workspace.requestCompleteSelectedConsultation()" :disabled="workspace.completingConsultation">
                {{ workspace.completingConsultation ? '处理中...' : '结束就诊' }}
              </button>
            <span
              class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium"
              :class="{
                'bg-blue-50 text-info': workspace.consultationStatusTone === 'info',
                'bg-amber-50 text-warning': workspace.consultationStatusTone === 'warning',
                'bg-green-50 text-success': workspace.consultationStatusTone === 'success',
                'bg-red-50 text-danger': workspace.consultationStatusTone === 'danger',
                'bg-gray-100 text-text-secondary': workspace.consultationStatusTone === 'neutral',
              }"
            >
              {{ workspace.consultationStatusLabel }}
            </span>
          </div>

          <!-- Conversation -->
          <SectionCard title="问诊记录">
            <textarea v-model="workspace.consultationForm.conversationText" class="input-field h-32 resize-none" placeholder="输入问诊对话内容..." />
            <div class="flex gap-2 mt-3">
              <button class="btn-primary" type="button" @click="runMedicalRecordWorkflow()" :disabled="workspace.generatingRecord">
                <Sparkles :size="16" /><span>{{ workspace.generatingRecord ? '生成中...' : 'AI生成病历' }}</span>
              </button>
              <button class="btn-secondary" type="button" @click="runDiagnosisWorkflow()" :disabled="workspace.diagnosingRecord">
                <Sparkles :size="16" /><span>{{ workspace.diagnosingRecord ? '诊断中...' : 'AI诊断建议' }}</span>
              </button>
            </div>
          </SectionCard>

          <!-- Degraded notice for medical record -->
          <div v-if="workspace.recordDegraded" class="ai-degraded-notice mb-3">
            ⚠️ AI 服务生成病历失败，当前显示的是本地规则生成的草稿，建议医生仔细核对。
          </div>

          <!-- Medical record form -->
          <SectionCard title="病历草稿">
            <div class="grid grid-cols-2 gap-3">
              <label class="label-text" :class="{ 'ai-field--streaming': workspace.generatingRecord }">
                主诉
                <span v-if="workspace.generatingRecord" class="ai-cursor" />
                <span v-else-if="workspace.recordForm.aiGenerated" class="ai-badge-inline">AI 生成</span>
                <textarea v-model="workspace.recordForm.chiefComplaint" class="input-field h-16 resize-none mt-1" />
              </label>
              <label class="label-text" :class="{ 'ai-field--streaming': workspace.generatingRecord }">
                现病史
                <span v-if="workspace.generatingRecord" class="ai-cursor" />
                <span v-else-if="workspace.recordForm.aiGenerated" class="ai-badge-inline">AI 生成</span>
                <textarea v-model="workspace.recordForm.presentIllness" class="input-field h-16 resize-none mt-1" />
              </label>
              <label class="label-text" :class="{ 'ai-field--streaming': workspace.generatingRecord }">
                既往史
                <span v-if="workspace.generatingRecord" class="ai-cursor" />
                <span v-else-if="workspace.recordForm.aiGenerated" class="ai-badge-inline">AI 生成</span>
                <textarea v-model="workspace.recordForm.pastHistory" class="input-field h-16 resize-none mt-1" />
              </label>
              <label class="label-text" :class="{ 'ai-field--streaming': workspace.generatingRecord }">
                体格检查
                <span v-if="workspace.generatingRecord" class="ai-cursor" />
                <span v-else-if="workspace.recordForm.aiGenerated" class="ai-badge-inline">AI 生成</span>
                <textarea v-model="workspace.recordForm.physicalExam" class="input-field h-16 resize-none mt-1" />
              </label>
              <label class="label-text col-span-2" :class="{ 'ai-field--streaming': workspace.generatingRecord }">
                初步诊断
                <span v-if="workspace.generatingRecord" class="ai-cursor" />
                <span v-else-if="workspace.recordForm.aiGenerated" class="ai-badge-inline">AI 生成</span>
                <textarea v-model="workspace.recordForm.preliminaryDiagnosis" class="input-field h-16 resize-none mt-1" />
              </label>
              <label class="label-text col-span-2" :class="{ 'ai-field--streaming': workspace.generatingRecord }">
                治疗方案
                <span v-if="workspace.generatingRecord" class="ai-cursor" />
                <span v-else-if="workspace.recordForm.aiGenerated" class="ai-badge-inline">AI 生成</span>
                <textarea v-model="workspace.recordForm.treatmentPlan" class="input-field h-16 resize-none mt-1" />
              </label>
            </div>
            <div class="flex gap-2 mt-3">
              <button class="btn-primary" type="button" @click="workspace.saveCurrentMedicalRecord()" :disabled="workspace.savingRecord">
                {{ workspace.savingRecord ? '保存中...' : '保存病历' }}
              </button>
              <button class="btn-ghost" type="button" @click="workspace.saveCurrentMedicalRecord()" :disabled="!workspace.recordForm.chiefComplaint">
                采纳全部
              </button>
              <button class="btn-ghost" type="button" @click="runMedicalRecordWorkflow()" :disabled="workspace.generatingRecord">
                重新生成
              </button>
            </div>
          </SectionCard>

          <!-- Prescription -->
          <SectionCard title="处方编辑">
            <div class="space-y-4">
              <div class="flex gap-2">
                <input
                  v-model="workspace.drugSearch"
                  class="input-field flex-1"
                  placeholder="搜索药品名称、拼音码..."
                  @keyup.enter="workspace.loadDrugCatalog()"
                />
                <button class="btn-secondary" type="button" @click="workspace.loadDrugCatalog()" :disabled="workspace.drugLoading">
                  {{ workspace.drugLoading ? '搜索中...' : '搜索药品' }}
                </button>
              </div>

              <div class="grid gap-3">
                <div v-for="item in workspace.prescriptionItems" :key="item.key" class="rounded-lg border border-border p-3">
                  <div class="grid gap-2 lg:grid-cols-[minmax(220px,1.4fr)_72px_92px_92px_40px]">
                    <select v-model="item.drugId" class="input-field" @change="workspace.applyDrugDefaults(item)">
                      <option :value="null" disabled>选择药品</option>
                      <option v-for="drug in workspace.availableDrugs" :key="drug.id" :value="drug.id">
                        {{ drug.name }} · {{ drug.specification || '无规格' }}
                      </option>
                    </select>
                    <input v-model="item.dosage" class="input-field" placeholder="剂量" />
                    <input v-model="item.frequency" class="input-field" placeholder="频次" />
                    <input v-model="item.quantity" class="input-field" placeholder="数量" />
                    <button
                      v-if="workspace.prescriptionItems.length > 1"
                      class="btn-ghost !p-1 !text-danger"
                      type="button"
                      @click="workspace.removePrescriptionItem(item.key)"
                    >
                      ✕
                    </button>
                  </div>

                  <div v-if="selectedDrug(item)" class="mt-2 rounded-md bg-gray-50 p-2 text-xs text-text-secondary">
                    <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
                      <span class="font-medium text-text-main">{{ selectedDrug(item)?.name }}</span>
                      <span>{{ drugMeta(selectedDrug(item)!) }}</span>
                      <span>{{ drugPrice(selectedDrug(item)) }}</span>
                    </div>
                    <p v-if="selectedDrug(item)?.defaultUsage" class="mt-1">默认用法：{{ selectedDrug(item)?.defaultUsage }}</p>
                    <p v-if="selectedDrug(item)?.contraindications" class="mt-1 text-danger">禁忌：{{ selectedDrug(item)?.contraindications }}</p>
                  </div>

                  <label class="mt-2 block text-xs text-text-secondary">
                    用药说明
                    <input v-model="item.usageInstruction" class="input-field mt-1" placeholder="可填写餐前/餐后、特殊注意事项等" />
                  </label>
                </div>
              </div>

              <div v-if="workspace.availableDrugs.length" class="rounded-lg bg-blue-50 p-3">
                <p class="text-xs font-medium text-info mb-2">快速选择药品</p>
                <div class="flex flex-wrap gap-2">
                  <button
                    v-for="drug in workspace.availableDrugs.slice(0, 8)"
                    :key="drug.id"
                    class="rounded-full bg-white px-3 py-1 text-xs text-text-main shadow-sm hover:text-brand"
                    type="button"
                    @click="workspace.choosePrescriptionDrug(workspace.prescriptionItems[workspace.prescriptionItems.length - 1], drug.id)"
                  >
                    {{ drug.name }}
                  </button>
                </div>
              </div>

              <div class="flex gap-2">
                <button class="btn-secondary" type="button" @click="workspace.addPrescriptionItem()">+ 添加药品</button>
                <button class="btn-primary" type="button" @click="runPrescriptionReviewWorkflow()" :disabled="workspace.reviewingPrescription">
                  {{ workspace.reviewingPrescription ? '审方中...' : '提交审方' }}
                </button>
              </div>
            </div>
          </SectionCard>

          <!-- Degraded notice for diagnosis -->
          <div v-if="workspace.diagnosisDegraded" class="ai-degraded-notice mb-3">
            ⚠️ AI 服务生成诊断建议失败，当前显示的是本地规则生成的建议，供医生参考。
          </div>

          <!-- Diagnosis suggestions -->
          <SectionCard v-if="workspace.diagnosisSuggestion" title="AI 诊断建议">
            <div class="space-y-3">
              <div class="flex items-center justify-between gap-2">
                <span class="inline-flex rounded-full bg-blue-50 px-2.5 py-1 text-xs font-medium text-info">
                  {{ workspace.diagnosisSuggestion.adoptionStatus === 'ADOPTED' ? '已采纳' : workspace.diagnosisSuggestion.adoptionStatus === 'IGNORED' ? '已忽略' : '待处理' }}
                </span>
                <button
                  v-if="workspace.diagnosisSuggestion.adoptionStatus !== 'IGNORED'"
                  class="btn-ghost !py-1 text-xs"
                  type="button"
                  :disabled="workspace.diagnosisUpdating"
                  @click="workspace.ignoreCurrentDiagnosis('医生选择不采用该诊断建议')"
                >
                  忽略建议
                </button>
              </div>
              <div class="flex flex-wrap gap-2">
                <span v-for="(diag, idx) in parseDiagnoses(workspace.diagnosisSuggestion.suggestedDiagnoses)" :key="idx"
                      class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium"
                      :style="{ background: confidenceBg(diag.confidence), color: confidenceFg(diag.confidence) }">
                  {{ diag.name }}
                  <span v-if="diag.confidence" class="opacity-70">{{ diag.confidence }}%</span>
                  <button
                    class="ml-1 px-1.5 py-0.5 rounded text-[10px] bg-black/10 hover:bg-black/20 disabled:opacity-50"
                    type="button"
                    :disabled="workspace.diagnosisUpdating || workspace.diagnosisSuggestion.adoptionStatus === 'ADOPTED'"
                    @click="adoptDiagnosis(diag.name)"
                  >
                    采纳
                  </button>
                </span>
              </div>
              <div v-if="workspace.diagnosisSuggestion.suggestedExamItems" class="space-y-1">
                <p class="text-xs font-medium text-text-secondary">建议检查项目</p>
                <label v-for="(exam, idx) in parseExamItems(workspace.diagnosisSuggestion.suggestedExamItems)" :key="idx"
                       class="flex items-center gap-2 text-sm py-1">
                  <input type="checkbox" class="rounded" />
                  <span>{{ exam }}</span>
                </label>
              </div>
            </div>
          </SectionCard>

          <!-- Review result -->
          <SectionCard v-if="workspace.reviewResult" title="审方结果">
            <!-- Risk bar -->
            <div class="px-4 py-2.5 rounded-lg font-semibold text-sm mb-3" :class="riskBarClass(workspace.reviewResult.riskLevel)">
              风险等级：{{ workspace.reviewResult.riskLevel === 'HIGH' ? '高风险' : workspace.reviewResult.riskLevel === 'MEDIUM' ? '中风险' : '低风险' }}
            </div>

            <!-- Local Rule Engine -->
            <div v-if="workspace.reviewResult.localRuleHits" class="mb-3 p-3 rounded-lg bg-gray-50 border border-border">
              <h4 class="text-sm font-medium mb-2">📋 本地规则引擎</h4>
              <div v-for="(hit, idx) in parseRuleHits(workspace.reviewResult.localRuleHits)" :key="idx"
                   class="flex gap-3 p-2 rounded-md bg-white mb-2 last:mb-0">
                <span class="flex-shrink-0 text-xs px-2 py-0.5 rounded font-semibold"
                      :class="ruleRiskClass(hit.riskLevel)">
                  {{ riskLabel(hit.riskLevel) }}
                </span>
                <div class="text-xs">
                  <strong>{{ hit.ruleName || hit.alertMessage }}</strong>
                  <p v-if="hit.alertMessage" class="text-text-secondary mt-0.5">{{ hit.alertMessage }}</p>
                  <p v-if="hit.suggestion" class="text-brand mt-0.5">💡 {{ hit.suggestion }}</p>
                </div>
              </div>
            </div>

            <!-- LLM Analysis -->
            <div v-if="workspace.reviewResult.llmSuggestion" class="p-3 rounded-lg bg-brand-soft border-l-2 border-brand">
              <h4 class="text-sm font-medium mb-1">🤖 AI 分析补充 <span class="ai-badge">AI 生成</span></h4>
              <p class="text-xs text-text-secondary whitespace-pre-wrap">{{ workspace.reviewResult.llmSuggestion }}</p>
            </div>

            <button class="btn-primary mt-3" type="button" @click="workspace.requestSubmitPrescription()" :disabled="workspace.submittingPrescription">
              {{ workspace.submittingPrescription ? '提交中...' : '确认提交处方' }}
            </button>
          </SectionCard>
        </div>

        <EmptyState v-else icon="inbox" title="请选择患者开始接诊" description="从左侧队列选择一个患者" />
      </div>
    </div>

    <!-- Thinking content indicator during streaming -->
    <div v-if="aiStreamStore.thinkingText && aiStreamStore.streaming" class="fixed right-12 top-1/2 -translate-y-1/2 z-30 bg-white border border-brand/30 rounded-lg shadow-lg p-3 max-w-xs text-xs text-text-secondary max-h-48 overflow-y-auto">
      <p class="font-medium text-brand mb-1">💭 AI 思考中...</p>
      <p class="whitespace-pre-wrap">{{ aiStreamStore.thinkingText }}</p>
    </div>

    <!-- AI Workflow Sidebar toggle -->
    <button
      class="fixed right-0 top-1/2 -translate-y-1/2 z-30 w-9 h-20 bg-brand text-white rounded-l-lg flex items-center justify-center shadow-lg hover:bg-brand-dark transition cursor-pointer"
      @click="toggleSidebar"
      title="AI 工作台"
    >
      <PanelRightOpen :size="18" />
    </button>

    <!-- AI Workflow Sidebar -->
    <WorkflowSidebar
      ref="sidebarRef"
      :open="sidebarOpen"
      @close="sidebarOpen = false"
      @trigger="onSidebarTrigger"
      @adopt="onSidebarAdopt"
    />

    <ConfirmDialog
      :open="workspace.showPrescriptionConfirm"
      title="确认提交处方"
      message="提交后将生成正式处方并锁定当前审方结果。"
      level="warning"
      confirm-label="确认提交"
      :loading="workspace.submittingPrescription"
      @confirm="void workspace.submitCurrentPrescription()"
      @cancel="workspace.cancelPrescriptionSubmit()"
    />

    <ConfirmDialog
      :open="workspace.showCompleteConfirm"
      title="确认结束就诊"
      message="结束后当前接诊将进入完成状态。"
      level="danger"
      confirm-label="确认结束"
      :loading="workspace.completingConsultation"
      @confirm="void workspace.confirmCompleteSelectedConsultation()"
      @cancel="workspace.cancelCompleteSelectedConsultation()"
    />
  </div>
</template>
