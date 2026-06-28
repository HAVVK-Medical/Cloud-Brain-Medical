<script setup lang="ts">
import { ref, watch } from 'vue';
import { Sparkles, PanelRightOpen } from 'lucide-vue-next';
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';
import LoadingSkeleton from '@/components/shared/LoadingSkeleton.vue';
import WorkflowSidebar from '@/components/workflow/WorkflowSidebar.vue';
import { useAiStreamStore } from '@/stores/ai-stream';

const { workspace } = defineProps<{ workspace: any }>();

const sidebarOpen = ref(false);
const sidebarRef = ref<InstanceType<typeof WorkflowSidebar> | null>(null);
const aiStreamStore = useAiStreamStore();

function toggleSidebar() {
  sidebarOpen.value = !sidebarOpen.value;
}

// Handle sidebar step trigger
function onSidebarTrigger(stepId: string) {
  switch (stepId) {
    case 'MEDICAL_RECORD':
      workspace.generateDraftMedicalRecord();
      break;
    case 'DIAGNOSIS':
      workspace.diagnoseCurrentCase();
      break;
    case 'PRESCRIPTION_REVIEW':
      workspace.reviewCurrentPrescription?.();
      break;
  }
}

// Handle sidebar adopt
function onSidebarAdopt(stepId: string) {
  switch (stepId) {
    case 'MEDICAL_RECORD':
      // Record is already populated by stream, save it
      workspace.saveCurrentMedicalRecord();
      break;
    case 'DIAGNOSIS':
      // Parse the diagnosis text and adopt first diagnosis
      if (workspace.diagnosisSuggestions?.length) {
        const first = workspace.diagnosisSuggestions[0];
        if (first.name) workspace.recordForm.preliminaryDiagnosis = first.name;
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

watch(() => workspace.generatingRecord, (val) => {
  if (!val && sidebarRef.value) {
    sidebarRef.value.setStepCompleted('MEDICAL_RECORD');
  }
});

watch(() => workspace.diagnosingRecord, (val) => {
  if (!val && sidebarRef.value && workspace.diagnosisSuggestion) {
    sidebarRef.value.setStepCompleted('DIAGNOSIS');
    const text = workspace.diagnosisSuggestion.suggestedDiagnoses || '';
    sidebarRef.value.updateStepContent('DIAGNOSIS', text);
  }
});

watch(() => workspace.reviewingPrescription, (val) => {
  if (!val && sidebarRef.value && workspace.reviewResult) {
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
  workspace.recordForm.preliminaryDiagnosis = name;
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
          <div class="flex items-center gap-2">
            <button class="btn-primary" type="button" @click="workspace.beginSelectedConsultation()" :disabled="workspace.startingConsultation">
              {{ workspace.startingConsultation ? '接诊中...' : '开始接诊' }}
            </button>
            <button class="btn-danger" type="button" @click="workspace.completeSelectedConsultation()" :disabled="workspace.completingConsultation">
              {{ workspace.completingConsultation ? '处理中...' : '结束就诊' }}
            </button>
          </div>

          <!-- Conversation -->
          <SectionCard title="问诊记录">
            <textarea v-model="workspace.consultationForm.conversationText" class="input-field h-32 resize-none" placeholder="输入问诊对话内容..." />
            <div class="flex gap-2 mt-3">
              <button class="btn-primary" type="button" @click="workspace.generateDraftMedicalRecord()" :disabled="workspace.generatingRecord">
                <Sparkles :size="16" /><span>{{ workspace.generatingRecord ? '生成中...' : 'AI生成病历' }}</span>
              </button>
              <button class="btn-secondary" type="button" @click="workspace.diagnoseCurrentCase()" :disabled="workspace.diagnosingRecord">
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
              <button class="btn-ghost" type="button" @click="workspace.generateDraftMedicalRecord()" :disabled="workspace.generatingRecord">
                重新生成
              </button>
            </div>
          </SectionCard>

          <!-- Prescription -->
          <SectionCard title="处方编辑">
            <div class="space-y-3">
              <div v-for="item in workspace.prescriptionItems" :key="item.key" class="flex items-center gap-2">
                <select v-model="item.drugId" class="input-field flex-1" @change="workspace.applyDrugDefaults(item)">
                  <option :value="null" disabled>选择药品</option>
                  <option v-for="drug in workspace.availableDrugs" :key="drug.id" :value="drug.id">{{ drug.name }}</option>
                </select>
                <input v-model="item.dosage" class="input-field w-16" placeholder="用量" />
                <input v-model="item.frequency" class="input-field w-20" placeholder="频次" />
                <input v-model="item.quantity" class="input-field w-16" placeholder="数量" />
                <button v-if="workspace.prescriptionItems.length > 1" class="btn-ghost !p-1 !text-danger" type="button" @click="workspace.removePrescriptionItem(item.key)">✕</button>
              </div>
              <div class="flex gap-2">
                <button class="btn-secondary" type="button" @click="workspace.addPrescriptionItem()">+ 添加药品</button>
                <button class="btn-primary" type="button" @click="workspace.reviewCurrentPrescription()" :disabled="workspace.reviewingPrescription">
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
              <div class="flex flex-wrap gap-2">
                <span v-for="(diag, idx) in parseDiagnoses(workspace.diagnosisSuggestion.suggestedDiagnoses)" :key="idx"
                      class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium"
                      :style="{ background: confidenceBg(diag.confidence), color: confidenceFg(diag.confidence) }">
                  {{ diag.name }}
                  <span v-if="diag.confidence" class="opacity-70">{{ diag.confidence }}%</span>
                  <button class="ml-1 px-1.5 py-0.5 rounded text-[10px] bg-black/10 hover:bg-black/20" @click="adoptDiagnosis(diag.name)">采纳</button>
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

            <button class="btn-primary mt-3" type="button" @click="workspace.submitCurrentPrescription()" :disabled="workspace.submittingPrescription">
              {{ workspace.submittingPrescription ? '提交中...' : '确认提交处方' }}
            </button>
          </SectionCard>
        </div>

        <EmptyState v-else icon="inbox" title="请选择患者开始接诊" description="从左侧队列选择一个患者" />
      </div>
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
  </div>
</template>
