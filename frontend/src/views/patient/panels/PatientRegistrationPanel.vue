<script setup lang="ts">
import { Building2, CalendarDays, Ticket } from 'lucide-vue-next';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">挂号与排班</h3>
        <p class="section-copy">先筛选科室和医生，再从可用号源里选择合适时段。</p>
      </div>
      <button class="button-secondary" type="button" @click="workspace.submitRegistration" :disabled="workspace.registering">
        <Ticket :size="16" />
        <span>{{ workspace.registering ? '提交中' : '确认挂号' }}</span>
      </button>
    </div>

    <div class="toolbar">
      <button
        v-for="department in workspace.departments"
        :key="department.id"
        type="button"
        class="pill"
        :data-tone="workspace.selectedDepartmentId === department.id ? 'healthy' : undefined"
        @click="workspace.chooseDepartment(department.id)"
      >
        <Building2 :size="14" />
        <span>{{ department.name }}</span>
      </button>
      <button type="button" class="pill" @click="workspace.chooseDepartment(null)">
        <span>全部科室</span>
      </button>
    </div>

    <div class="candidate-list">
      <button
        v-for="doctor in workspace.doctors"
        :key="doctor.id"
        type="button"
        class="candidate-item"
        :class="{ active: workspace.selectedDoctorId === doctor.id }"
        @click="workspace.chooseDoctor(doctor.id)"
      >
        <h4>{{ doctor.name }}</h4>
        <p>{{ doctor.departmentName || '未分科' }} / {{ doctor.specialty || doctor.title || '普通门诊' }}</p>
      </button>
    </div>

    <div class="candidate-list">
      <button
        v-for="schedule in workspace.visibleSchedules"
        :key="schedule.id"
        type="button"
        class="candidate-item"
        :class="{ active: workspace.selectedScheduleId === schedule.id }"
        @click="workspace.chooseSchedule(schedule.id)"
      >
        <h4>{{ schedule.workDate }} / {{ schedule.period }}</h4>
        <p>
          {{ schedule.doctorName || '未知医生' }} / {{ schedule.departmentName || '未分科' }} /
          剩余 {{ schedule.remainingSlots ?? 0 }}/{{ schedule.totalSlots ?? 0 }}
        </p>
      </button>
    </div>

    <div class="empty-state" v-if="!workspace.visibleSchedules.length">
      当前筛选下没有可挂号号源。
    </div>

    <div v-if="workspace.selectedSchedule" class="workspace-note">
      <CalendarDays :size="16" />
      <div>
        <strong>{{ workspace.selectedSchedule.doctorName }} / {{ workspace.selectedSchedule.departmentName }}</strong>
        <p>日期：{{ workspace.formatDate(workspace.selectedSchedule.workDate) }}，时段：{{ workspace.selectedSchedule.period }}</p>
      </div>
    </div>
  </section>
</template>
