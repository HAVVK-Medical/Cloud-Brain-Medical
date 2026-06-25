<script setup lang="ts">
import { Ticket } from 'lucide-vue-next';
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <div class="p-4 space-y-4">
    <!-- Step indicators -->
    <div class="flex items-center gap-1 text-xs font-medium justify-center">
      <span class="flex items-center gap-1" :class="workspace.selectedDepartmentId ? 'text-success' : 'text-brand'">
        <span class="w-5 h-5 rounded-full flex items-center justify-center text-white text-xs" :class="workspace.selectedDepartmentId ? 'bg-success' : 'bg-brand'">1</span>
        选科室
      </span>
      <span class="text-text-secondary mx-1">→</span>
      <span class="flex items-center gap-1" :class="workspace.selectedDoctorId ? 'text-success' : 'text-text-secondary'">
        <span class="w-5 h-5 rounded-full flex items-center justify-center text-white text-xs" :class="workspace.selectedDoctorId ? 'bg-success' : 'bg-gray-300'">2</span>
        选医生
      </span>
      <span class="text-text-secondary mx-1">→</span>
      <span class="flex items-center gap-1" :class="workspace.selectedScheduleId ? 'text-success' : 'text-text-secondary'">
        <span class="w-5 h-5 rounded-full flex items-center justify-center text-white text-xs" :class="workspace.selectedScheduleId ? 'bg-success' : 'bg-gray-300'">3</span>
        选时段
      </span>
    </div>

    <!-- Department selection -->
    <SectionCard title="选择科室">
      <div class="grid grid-cols-2 gap-2">
        <button v-for="dept in workspace.departments" :key="dept.id" type="button" class="rounded-lg border px-3 py-2.5 text-sm text-left transition" :class="workspace.selectedDepartmentId === dept.id ? 'border-brand bg-brand-soft text-brand font-medium' : 'border-border hover:border-brand'" @click="workspace.chooseDepartment(dept.id)">
          {{ dept.name }}
        </button>
      </div>
    </SectionCard>

    <!-- Doctor selection -->
    <SectionCard title="选择医生">
      <div v-if="!workspace.doctors.length" class="text-center text-sm text-text-secondary py-4">该科室暂无可用医生</div>
      <div class="space-y-2">
        <div v-for="doc in workspace.doctors" :key="doc.id" class="flex items-center justify-between py-3 px-3 rounded-lg border cursor-pointer transition" :class="workspace.selectedDoctorId === doc.id ? 'border-brand bg-brand-soft' : 'border-border hover:border-brand'" @click="workspace.chooseDoctor(doc.id)">
          <div><p class="text-sm font-medium">{{ doc.name }}</p><p class="text-xs text-text-secondary">{{ doc.title }} · {{ doc.specialty }}</p></div>
          <span v-if="workspace.selectedDoctorId === doc.id" class="text-brand font-bold text-lg">✓</span>
        </div>
      </div>
    </SectionCard>

    <!-- Schedule and confirm -->
    <SectionCard v-if="workspace.visibleSchedules.length" title="选择时段">
      <div class="space-y-2">
        <div v-for="slot in workspace.visibleSchedules" :key="slot.id" class="flex items-center justify-between py-2 px-3 rounded-lg border cursor-pointer transition" :class="workspace.selectedScheduleId === slot.id ? 'border-brand bg-brand-soft' : 'border-border hover:border-brand'" @click="workspace.chooseSchedule(slot.id)">
          <div><p class="text-sm font-medium">{{ slot.workDate }} · {{ slot.period }}</p><p class="text-xs text-text-secondary">{{ slot.doctorName }} · {{ slot.visitLevel }}</p></div>
          <StatusChip :tone="slot.remainingSlots && slot.remainingSlots > 0 ? 'success' : 'danger'">{{ slot.remainingSlots ? `余${slot.remainingSlots}号` : '约满' }}</StatusChip>
        </div>
      </div>
    </SectionCard>

    <!-- Confirm -->
    <SectionCard v-if="workspace.selectedSchedule" title="挂号确认">
      <div class="space-y-2 text-sm mb-4">
        <div class="flex justify-between"><span class="text-text-secondary">科室</span><span>{{ workspace.selectedDepartment?.name }}</span></div>
        <div class="flex justify-between"><span class="text-text-secondary">医生</span><span>{{ workspace.selectedDoctor?.name }}</span></div>
        <div class="flex justify-between"><span class="text-text-secondary">时段</span><span>{{ workspace.selectedSchedule.workDate }} {{ workspace.selectedSchedule.period }}</span></div>
      </div>
      <button class="btn-primary w-full" type="button" @click="workspace.submitRegistration()" :disabled="workspace.registering">
        <Ticket :size="16" />
        <span>{{ workspace.registering ? '挂号中...' : '确认挂号' }}</span>
      </button>
    </SectionCard>

    <EmptyState v-if="!workspace.departments.length && !workspace.loading" icon="calendar" title="暂无可用科室" description="当前没有可挂号的科室，请稍后再试" />
  </div>
</template>
