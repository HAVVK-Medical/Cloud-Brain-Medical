<script setup lang="ts">
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <div class="space-y-6">
    <div class="flex gap-3">
      <input v-model="workspace.recordSearch" class="input-field flex-1" placeholder="搜索患者、诊断或病历..." />
      <button class="btn-primary" type="button" @click="workspace.loadHistory()">搜索</button>
    </div>

    <SectionCard title="病历历史">
      <div v-if="workspace.medicalRecords.length" class="space-y-3">
        <div v-for="record in workspace.medicalRecords" :key="record.id" class="py-3 border-b border-border last:border-b-0 text-sm">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <p class="font-medium">{{ record.patientName || '未记录患者' }}</p>
                <StatusChip :tone="record.aiGenerated ? 'info' : 'neutral'">{{ record.aiGenerated ? 'AI' : '手动' }}</StatusChip>
              </div>
              <p class="text-text-secondary text-xs mt-1">{{ workspace.formatDateTime(record.createdAt) }} · {{ record.departmentName || '未记录科室' }}</p>
              <p class="mt-1">{{ workspace.truncate(record.preliminaryDiagnosis, 80) }}</p>
            </div>
            <button class="btn-secondary shrink-0" type="button" @click="workspace.viewMedicalRecordDetail(record.id)">
              查看详情
            </button>
          </div>
        </div>
      </div>
      <EmptyState v-else icon="file" title="暂无病历记录" />
    </SectionCard>

    <SectionCard title="处方历史">
      <div v-if="workspace.filteredPrescriptions.length" class="space-y-3">
        <div v-for="presc in workspace.filteredPrescriptions" :key="presc.id" class="py-3 border-b border-border last:border-b-0 text-sm">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="font-medium">{{ presc.patientName || '未记录患者' }}</span>
                <StatusChip :tone="presc.riskLevel === 'HIGH' ? 'danger' : presc.riskLevel === 'MEDIUM' ? 'warning' : 'success'">
                  {{ presc.riskLevel === 'HIGH' ? '高风险' : presc.riskLevel === 'MEDIUM' ? '中风险' : '低风险' }}
                </StatusChip>
              </div>
              <p class="text-xs text-text-secondary mt-1">{{ presc.doctorName || '未记录医生' }} · {{ workspace.formatDateTime(presc.createdAt) }}</p>
              <p class="text-xs text-text-secondary mt-1">共 {{ presc.items?.length ?? 0 }} 项药品</p>
            </div>
            <button class="btn-secondary shrink-0" type="button" @click="workspace.viewPrescriptionDetail(presc.id)">
              查看详情
            </button>
          </div>
        </div>
      </div>
      <EmptyState v-else icon="file" title="暂无处方记录" />
    </SectionCard>
  </div>
</template>
