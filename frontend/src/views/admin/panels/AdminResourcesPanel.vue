<script setup lang="ts">
import { ArrowDownUp, Copy } from 'lucide-vue-next';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">排班 / 药品</h3>
        <p class="section-copy">医生排班和药品库是挂号与开方的前置数据。</p>
      </div>
      <div class="action-row">
        <button class="button-secondary" type="button" @click="workspace.createNew('schedule')"><ArrowDownUp :size="16" /><span>新排班</span></button>
        <button class="button-secondary" type="button" @click="workspace.createNew('drug')"><Copy :size="16" /><span>新药品</span></button>
      </div>
    </div>

    <div class="detail-grid two">
      <ul class="mini-list overflow-list">
        <li v-for="schedule in workspace.visibleSchedules" :key="schedule.id" class="mini-item" @click="workspace.selectSchedule(schedule)">
          <div class="mini-item-head">
            <div class="mini-item-title">{{ schedule.workDate }} · {{ schedule.period }}</div>
            <span class="pill">{{ schedule.remainingSlots ?? 0 }}/{{ schedule.totalSlots ?? 0 }}</span>
          </div>
          <div class="mini-item-meta">
            <span>{{ schedule.departmentName || '未分科' }}</span>
            <span>{{ schedule.doctorName || '未知医生' }}</span>
            <span>{{ workspace.formatStatus(schedule.status) }}</span>
          </div>
        </li>
      </ul>

      <ul class="mini-list overflow-list">
        <li v-for="drug in workspace.drugs" :key="drug.id" class="mini-item" @click="workspace.selectDrug(drug)">
          <div class="mini-item-head">
            <div class="mini-item-title">{{ drug.name }}</div>
            <span class="pill">{{ drug.code }}</span>
          </div>
          <div class="mini-item-meta">
            <span>{{ drug.specification || '无规格' }}</span>
            <span>{{ drug.dosageForm || '无剂型' }}</span>
            <span>{{ workspace.formatStatus(drug.status) }}</span>
          </div>
        </li>
      </ul>
    </div>
  </section>
</template>
