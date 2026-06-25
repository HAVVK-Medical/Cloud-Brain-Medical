<script setup lang="ts">
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <div class="space-y-6">
    <div class="flex gap-3">
      <input v-model="workspace.recordSearch" class="input-field flex-1" placeholder="搜索病历..." />
      <button class="btn-primary" type="button" @click="workspace.loadHistory()">搜索</button>
    </div>

    <SectionCard title="病历历史">
      <div v-if="workspace.medicalRecords.length" class="space-y-3">
        <div v-for="record in workspace.medicalRecords" :key="record.id" class="py-3 border-b border-border last:border-b-0 text-sm">
          <div class="flex justify-between items-start">
            <div>
              <p class="font-medium">{{ record.patientName }}</p>
              <p class="text-text-secondary text-xs">{{ workspace.formatDateTime(record.createdAt) }} · {{ record.departmentName }}</p>
            </div>
            <StatusChip :tone="record.aiGenerated ? 'info' : 'neutral'">{{ record.aiGenerated ? 'AI' : '手动' }}</StatusChip>
          </div>
          <p class="mt-1">{{ workspace.truncate(record.preliminaryDiagnosis, 80) }}</p>
        </div>
      </div>
      <EmptyState v-else icon="file" title="暂无病历记录" />
    </SectionCard>

    <SectionCard title="处方历史">
      <div v-if="workspace.prescriptions.length" class="space-y-3">
        <div v-for="presc in workspace.prescriptions" :key="presc.id" class="py-3 border-b border-border last:border-b-0 text-sm">
          <div class="flex justify-between">
            <span class="font-medium">{{ presc.patientName }}</span>
            <StatusChip :tone="presc.riskLevel === 'HIGH' ? 'danger' : presc.riskLevel === 'MEDIUM' ? 'warning' : 'success'">
              {{ presc.riskLevel === 'HIGH' ? '高风险' : presc.riskLevel === 'MEDIUM' ? '中风险' : '低风险' }}
            </StatusChip>
          </div>
          <p class="text-xs text-text-secondary">{{ presc.doctorName }} · {{ workspace.formatDateTime(presc.createdAt) }}</p>
        </div>
      </div>
      <EmptyState v-else icon="file" title="暂无处方记录" />
    </SectionCard>
  </div>
</template>
