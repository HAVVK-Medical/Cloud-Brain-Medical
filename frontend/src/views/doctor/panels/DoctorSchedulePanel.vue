<script setup lang="ts">
const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">排班信息</h3>
        <p class="section-copy">查看本人的可用排班和号源。</p>
      </div>
    </div>

    <ul class="mini-list overflow-list">
      <li v-for="schedule in workspace.schedules" :key="schedule.id" class="mini-item">
        <div class="mini-item-head">
          <div class="mini-item-title">{{ workspace.formatDate(schedule.workDate) }} / {{ schedule.period }}</div>
          <span class="pill">{{ schedule.remainingSlots ?? 0 }}/{{ schedule.totalSlots ?? 0 }}</span>
        </div>
        <div class="mini-item-meta">
          <span>{{ schedule.departmentName || '未分科' }}</span>
          <span>{{ schedule.visitLevel || '普通门诊' }}</span>
        </div>
      </li>
    </ul>
  </section>
</template>
