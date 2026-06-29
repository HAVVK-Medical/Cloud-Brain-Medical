<script setup lang="ts">
import { CheckCircle2, Ticket, X } from 'lucide-vue-next';
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <div class="p-4 space-y-4">
    <section v-if="workspace.latestCreatedRegistration" class="rounded-lg border border-green-100 bg-green-50 p-4">
      <div class="flex items-start gap-3">
        <CheckCircle2 :size="22" class="mt-0.5 shrink-0 text-success" />
        <div class="min-w-0 flex-1">
          <div class="flex items-start justify-between gap-2">
            <div>
              <h3 class="text-sm font-semibold text-text-main">挂号成功</h3>
              <p class="mt-1 text-xs text-text-secondary">
                {{ workspace.latestCreatedRegistration.departmentName || '未记录科室' }} ·
                {{ workspace.latestCreatedRegistration.doctorName || '未记录医生' }}
              </p>
            </div>
            <button type="button" class="btn-ghost !p-1" @click="workspace.dismissRegistrationSuccess()">
              <X :size="14" />
            </button>
          </div>
          <div class="mt-3 grid grid-cols-2 gap-2 text-xs">
            <div class="rounded-md bg-white/80 p-2">
              <p class="text-text-secondary">就诊日期</p>
              <p class="mt-0.5 font-medium text-text-main">{{ workspace.formatDate(workspace.latestCreatedRegistration.workDate) }}</p>
            </div>
            <div class="rounded-md bg-white/80 p-2">
              <p class="text-text-secondary">时段</p>
              <p class="mt-0.5 font-medium text-text-main">{{ workspace.latestCreatedRegistration.period || '未安排' }}</p>
            </div>
          </div>
          <RouterLink to="/patient/history" class="inline-flex items-center gap-1 mt-2 text-xs text-brand hover:underline">查看全部记录 →</RouterLink>
        </div>
      </div>
    </section>

    <!-- My existing registrations -->
    <SectionCard v-if="workspace.registrations?.length" title="我的挂号">
      <div class="space-y-2">
        <div v-for="reg in workspace.registrations" :key="reg.id" class="py-2 border-b border-border last:border-b-0 text-sm flex justify-between gap-3">
          <div class="min-w-0">
            <span class="font-medium">{{ workspace.formatDate(reg.workDate) }}</span>
            <span class="text-text-secondary"> · {{ reg.doctorName }}</span>
            <p class="text-xs text-text-secondary mt-0.5">{{ reg.departmentName }} · {{ reg.period || '未排时段' }}</p>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <StatusChip :tone="reg.status === 'CANCELLED' ? 'danger' : reg.status === 'COMPLETED' ? 'success' : 'info'">
              {{ reg.status === 'WAITING' ? '待就诊' : reg.status === 'COMPLETED' ? '已完成' : reg.status === 'CANCELLED' ? '已取消' : reg.status }}
            </StatusChip>
            <button
              v-if="reg.status === 'WAITING'"
              type="button"
              class="btn-ghost !px-2 !py-1 !text-xs !text-danger"
              :disabled="workspace.canceling"
              @click="workspace.requestCancelWaitingRegistration(reg.id)"
            >
              取消
            </button>
          </div>
        </div>
      </div>
    </SectionCard>

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

    <SectionCard title="选择科室">
      <div class="grid grid-cols-2 gap-2">
        <button
          v-for="dept in workspace.departments"
          :key="dept.id"
          type="button"
          class="rounded-lg border px-3 py-2.5 text-sm text-left transition"
          :class="workspace.selectedDepartmentId === dept.id ? 'border-brand bg-brand-soft text-brand font-medium' : 'border-border hover:border-brand'"
          @click="workspace.chooseDepartment(dept.id)"
        >
          {{ dept.name }}
        </button>
      </div>
    </SectionCard>

    <SectionCard title="选择医生">
      <div v-if="!workspace.doctors.length" class="text-center text-sm text-text-secondary py-4">该科室暂无可用医生</div>
      <div class="space-y-2">
        <button
          v-for="doc in workspace.doctors"
          :key="doc.id"
          type="button"
          class="flex w-full items-center justify-between py-3 px-3 rounded-lg border text-left transition"
          :class="workspace.selectedDoctorId === doc.id ? 'border-brand bg-brand-soft' : 'border-border hover:border-brand'"
          @click="workspace.chooseDoctor(doc.id)"
        >
          <div>
            <p class="text-sm font-medium">{{ doc.name }}</p>
            <p class="text-xs text-text-secondary">{{ doc.title || '医生' }} · {{ doc.specialty || '门诊' }}</p>
          </div>
          <CheckCircle2 v-if="workspace.selectedDoctorId === doc.id" :size="18" class="text-brand" />
        </button>
      </div>
    </SectionCard>

    <SectionCard v-if="workspace.visibleSchedules.length" title="选择时段">
      <div class="space-y-2">
        <button
          v-for="slot in workspace.visibleSchedules"
          :key="slot.id"
          type="button"
          class="flex w-full items-center justify-between py-2 px-3 rounded-lg border text-left transition"
          :class="workspace.selectedScheduleId === slot.id ? 'border-brand bg-brand-soft' : 'border-border hover:border-brand'"
          @click="workspace.chooseSchedule(slot.id)"
        >
          <div>
            <p class="text-sm font-medium">{{ slot.workDate }} · {{ slot.period }}</p>
            <p class="text-xs text-text-secondary">{{ slot.doctorName }} · {{ slot.visitLevel || '普通门诊' }}</p>
          </div>
          <StatusChip :tone="slot.remainingSlots && slot.remainingSlots > 0 ? 'success' : 'danger'">
            {{ slot.remainingSlots ? `余 ${slot.remainingSlots} 号` : '约满' }}
          </StatusChip>
        </button>
      </div>
    </SectionCard>

    <SectionCard v-if="workspace.selectedSchedule" title="挂号确认">
      <div class="space-y-2 text-sm mb-4">
        <div class="flex justify-between gap-3"><span class="text-text-secondary">科室</span><span>{{ workspace.selectedDepartment?.name }}</span></div>
        <div class="flex justify-between gap-3"><span class="text-text-secondary">医生</span><span>{{ workspace.selectedDoctor?.name }}</span></div>
        <div class="flex justify-between gap-3"><span class="text-text-secondary">时段</span><span>{{ workspace.selectedSchedule.workDate }} {{ workspace.selectedSchedule.period }}</span></div>
      </div>
      <button class="btn-primary w-full" type="button" @click="workspace.requestSubmitRegistration()" :disabled="workspace.registering">
        <Ticket :size="16" />
        <span>{{ workspace.registering ? '挂号中...' : '确认挂号' }}</span>
      </button>
    </SectionCard>

    <EmptyState
      v-if="!workspace.departments.length && !workspace.loading"
      icon="calendar"
      title="暂无可用科室"
      description="当前没有可挂号的科室，请稍后再试"
    />
  </div>
</template>
