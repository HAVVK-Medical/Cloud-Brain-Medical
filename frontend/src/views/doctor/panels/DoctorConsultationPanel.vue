<script setup lang="ts">
import { Sparkles } from 'lucide-vue-next';
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';
import LoadingSkeleton from '@/components/shared/LoadingSkeleton.vue';

const { workspace } = defineProps<{ workspace: any }>();
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

          <!-- Medical record form -->
          <SectionCard title="病历草稿">
            <div class="grid grid-cols-2 gap-3">
              <label class="label-text">主诉 <textarea v-model="workspace.recordForm.chiefComplaint" class="input-field h-16 resize-none mt-1" /></label>
              <label class="label-text">现病史 <textarea v-model="workspace.recordForm.presentIllness" class="input-field h-16 resize-none mt-1" /></label>
              <label class="label-text">既往史 <textarea v-model="workspace.recordForm.pastHistory" class="input-field h-16 resize-none mt-1" /></label>
              <label class="label-text">体格检查 <textarea v-model="workspace.recordForm.physicalExam" class="input-field h-16 resize-none mt-1" /></label>
              <label class="label-text col-span-2">初步诊断 <textarea v-model="workspace.recordForm.preliminaryDiagnosis" class="input-field h-16 resize-none mt-1" /></label>
              <label class="label-text col-span-2">治疗方案 <textarea v-model="workspace.recordForm.treatmentPlan" class="input-field h-16 resize-none mt-1" /></label>
            </div>
            <button class="btn-primary mt-3" type="button" @click="workspace.saveCurrentMedicalRecord()" :disabled="workspace.savingRecord">
              {{ workspace.savingRecord ? '保存中...' : '保存病历' }}
            </button>
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

          <!-- Review result -->
          <SectionCard v-if="workspace.reviewResult" title="审方结果">
            <div class="space-y-2 text-sm">
              <div class="flex items-center gap-2">
                <span class="text-text-secondary">风险等级：</span>
                <StatusChip :tone="workspace.reviewResult.riskLevel === 'HIGH' ? 'danger' : workspace.reviewResult.riskLevel === 'MEDIUM' ? 'warning' : 'success'">
                  {{ workspace.reviewResult.riskLevel === 'HIGH' ? '高风险' : workspace.reviewResult.riskLevel === 'MEDIUM' ? '中风险' : '低风险' }}
                </StatusChip>
              </div>
              <div v-if="workspace.reviewResult.localRuleHits" class="p-3 rounded-md bg-red-50 text-danger text-xs whitespace-pre-wrap">{{ workspace.reviewResult.localRuleHits }}</div>
              <div v-if="workspace.reviewResult.llmSuggestion" class="p-3 rounded-md bg-blue-50 text-info text-xs whitespace-pre-wrap">{{ workspace.reviewResult.llmSuggestion }}</div>
            </div>
            <button class="btn-primary mt-3" type="button" @click="workspace.submitCurrentPrescription()" :disabled="workspace.submittingPrescription">
              {{ workspace.submittingPrescription ? '提交中...' : '确认提交处方' }}
            </button>
          </SectionCard>
        </div>

        <EmptyState v-else icon="inbox" title="请选择患者开始接诊" description="从左侧队列选择一个患者" />
      </div>
    </div>
  </div>
</template>
