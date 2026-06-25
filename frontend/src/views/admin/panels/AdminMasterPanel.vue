<script setup lang="ts">
import { BellRing, BadgeCheck } from 'lucide-vue-next';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">基础数据</h3>
        <p class="section-copy">科室、医生和告警放在一起，便于先把主数据维护完整。</p>
      </div>
    </div>

    <div class="detail-grid two">
      <ul class="mini-list overflow-list">
        <li v-for="department in workspace.departments" :key="department.id" class="mini-item" @click="workspace.selectDepartment(department)">
          <div class="mini-item-head">
            <div class="mini-item-title">{{ department.name }}</div>
            <span class="pill">{{ department.code }}</span>
          </div>
          <div class="mini-item-meta">
            <span>{{ department.type || '未分类' }}</span>
            <span>{{ workspace.formatStatus(department.status) }}</span>
            <span>{{ department.doctorCount ?? 0 }} 医生</span>
          </div>
        </li>
      </ul>

      <ul class="mini-list overflow-list">
        <li v-for="doctor in workspace.doctors" :key="doctor.id" class="mini-item" @click="workspace.selectDoctor(doctor)">
          <div class="mini-item-head">
            <div class="mini-item-title">{{ doctor.name }}</div>
            <span class="pill">{{ doctor.title || '门诊医生' }}</span>
          </div>
          <div class="mini-item-meta">
            <span>{{ doctor.departmentName || '未分科' }}</span>
            <span>{{ workspace.formatStatus(doctor.status) }}</span>
            <span>{{ doctor.scheduleCount ?? 0 }} 排班</span>
          </div>
        </li>
      </ul>
    </div>

    <div class="section subtle-section">
      <div class="section-head">
        <div>
          <h4 class="section-title">风险告警</h4>
          <p class="section-copy">WebSocket 接入前先通过补拉机制兜底。</p>
        </div>
        <button class="button-ghost" type="button" @click="workspace.loadNotifications" :disabled="workspace.notificationLoading">
          <BellRing :size="16" :class="{ spinning: workspace.notificationLoading }" />
          <span>{{ workspace.unreadNotificationCount }} 条</span>
        </button>
      </div>

      <ul v-if="workspace.notifications.length" class="mini-list overflow-list">
        <li v-for="notice in workspace.notifications" :key="notice.id" class="mini-item">
          <div class="mini-item-head">
            <div class="mini-item-title">{{ notice.patientSummary || notice.alertType }}</div>
            <span class="pill" :data-tone="workspace.riskTone(notice.displayLevel)">{{ notice.displayLevel || notice.alertType }}</span>
          </div>
          <div class="mini-item-meta">
            <span>{{ notice.recipientRole }}</span>
            <span>{{ notice.alertType }}</span>
            <span>{{ workspace.formatDateTime(notice.createdAt) }}</span>
          </div>
          <p class="mini-item-copy">{{ workspace.truncate(notice.riskSummary, 120) }}</p>
          <div class="action-row">
            <button class="button-ghost" type="button" @click="workspace.ackNotification(notice.id)" :disabled="workspace.ackingNotificationId === notice.id">
              <BadgeCheck :size="16" />
              <span>{{ workspace.ackingNotificationId === notice.id ? '标记中' : '标记已读' }}</span>
            </button>
          </div>
        </li>
      </ul>
      <div v-else class="empty-state">暂无未读告警。</div>
    </div>
  </section>
</template>
