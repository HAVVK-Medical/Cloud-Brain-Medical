<script setup lang="ts">
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <div class="p-4 space-y-4">
    <SectionCard title="就诊记录">
      <div v-if="workspace.medicalRecords.length" class="space-y-3">
        <div v-for="record in workspace.medicalRecords" :key="record.id" class="py-3 border-b border-border last:border-b-0">
          <p class="text-sm font-medium">{{ workspace.formatDateTime(record.createdAt) }}</p>
          <p class="text-sm text-text-secondary mt-0.5">{{ record.departmentName }} · {{ record.doctorName }}</p>
          <p v-if="record.preliminaryDiagnosis" class="text-sm mt-1">{{ record.preliminaryDiagnosis }}</p>
          <StatusChip :tone="record.aiGenerated ? 'info' : 'neutral'" class="mt-1.5">{{ record.aiGenerated ? 'AI辅助' : '手动' }}</StatusChip>
        </div>
      </div>
      <EmptyState v-else icon="file" title="暂无就诊记录" description="完成就诊后可在这里查看病历" />
    </SectionCard>

    <SectionCard title="处方记录">
      <div v-if="workspace.prescriptions.length" class="space-y-3">
        <div v-for="presc in workspace.prescriptions" :key="presc.id" class="py-3 border-b border-border last:border-b-0">
          <div class="flex items-center justify-between">
            <span class="text-sm font-medium">{{ workspace.formatDateTime(presc.createdAt) }}</span>
            <StatusChip :tone="presc.riskLevel === 'HIGH' ? 'danger' : presc.riskLevel === 'MEDIUM' ? 'warning' : 'success'">
              {{ presc.riskLevel === 'HIGH' ? '高风险' : presc.riskLevel === 'MEDIUM' ? '中风险' : '低风险' }}
            </StatusChip>
          </div>
          <p class="text-sm text-text-secondary mt-0.5">{{ presc.doctorName }} · {{ presc.departmentName }}</p>
        </div>
      </div>
      <EmptyState v-else icon="file" title="暂无处方记录" />
    </SectionCard>

    <SectionCard title="提交反馈">
      <div class="space-y-3">
        <label class="label-text">选择就诊记录</label>
        <select v-model="workspace.feedbackForm.registrationId" class="input-field">
          <option :value="null" disabled>请选择已完成的就诊</option>
          <option v-for="reg in workspace.completedRegistrations" :key="reg.id" :value="reg.id">
            {{ workspace.formatDate(reg.workDate) }} · {{ reg.doctorName }}
          </option>
        </select>
        <label class="label-text">评分</label>
        <div class="flex gap-1">
          <button v-for="n in 5" :key="n" type="button" class="text-2xl transition" :class="n <= workspace.feedbackForm.rating ? 'text-warning' : 'text-text-secondary/30'" @click="workspace.feedbackForm.rating = n">★</button>
        </div>
        <label class="label-text">评价</label>
        <textarea v-model="workspace.feedbackForm.comment" class="input-field h-20 resize-none" placeholder="写下您的就诊体验..." />
        <button class="btn-primary w-full" type="button" @click="workspace.submitFeedback()" :disabled="workspace.submittingFeedback || !workspace.feedbackForm.registrationId">
          {{ workspace.submittingFeedback ? '提交中...' : '提交反馈' }}
        </button>
      </div>
    </SectionCard>
  </div>
</template>
