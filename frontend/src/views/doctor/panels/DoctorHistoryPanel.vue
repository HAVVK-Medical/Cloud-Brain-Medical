<script setup lang="ts">
const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">历史与告警</h3>
        <p class="section-copy">病历、处方、告警和工作摘要集中放在一起。</p>
      </div>
      <button class="button-ghost" type="button" @click="workspace.loadNotifications" :disabled="workspace.notificationLoading">
        <span>{{ workspace.unreadNotificationCount }} 条未读</span>
      </button>
    </div>

    <div class="detail-grid two">
      <section class="section subtle-section">
        <div class="section-head">
          <div>
            <h4 class="section-title">告警通知</h4>
            <p class="section-copy">未读提醒与高风险审方记录。</p>
          </div>
        </div>

        <ul v-if="workspace.notifications.length" class="mini-list overflow-list">
          <li v-for="notice in workspace.notifications" :key="notice.id" class="mini-item">
            <div class="mini-item-head">
              <div class="mini-item-title">{{ notice.patientSummary || notice.alertType }}</div>
              <span class="pill" :data-tone="workspace.riskTone(notice.displayLevel)">{{ notice.displayLevel || notice.alertType }}</span>
            </div>
            <div class="mini-item-meta">
              <span>{{ notice.alertType }}</span>
              <span>{{ notice.statisticsBucket || 'NO_BUCKET' }}</span>
              <span>{{ workspace.formatDateTime(notice.createdAt) }}</span>
            </div>
            <p class="mini-item-copy">{{ workspace.truncate(notice.riskSummary, 120) }}</p>
            <div class="action-row">
              <button class="button-ghost" type="button" @click="workspace.ackNotification(notice.id)" :disabled="workspace.ackingNotificationId === notice.id">
                <span>{{ workspace.ackingNotificationId === notice.id ? '标记中' : '标记已读' }}</span>
              </button>
            </div>
          </li>
        </ul>
        <div v-else class="empty-state">暂无未读告警。</div>
      </section>

      <section class="section subtle-section">
        <div class="section-head">
          <div>
            <h4 class="section-title">工作摘要</h4>
            <p class="section-copy">当前接诊和下一步动作。</p>
          </div>
          <button class="button-secondary" type="button" @click="workspace.completeSelectedConsultation" :disabled="workspace.completingConsultation">
            <span>{{ workspace.completingConsultation ? '结束中' : '结束就诊' }}</span>
          </button>
        </div>

        <div class="mini-list">
          <div class="mini-item">
            <span class="label">患者</span>
            <span class="value">{{ workspace.workspace?.registration.patientName || workspace.selectedRegistration?.patientName || '未选择' }}</span>
          </div>
          <div class="mini-item">
            <span class="label">主诉</span>
            <span class="value">{{ workspace.workspace?.registration.chiefComplaint || workspace.selectedRegistration?.chiefComplaint || '未记录' }}</span>
          </div>
          <div class="mini-item">
            <span class="label">下一步</span>
            <span class="value">{{ workspace.workspace?.nextActions.join(' / ') || '请选择号源' }}</span>
          </div>
          <div class="mini-item">
            <span class="label">接诊时间</span>
            <span class="value">{{ workspace.formatDateTime(workspace.workspace?.registration.consultationStartTime) }}</span>
          </div>
        </div>
      </section>
    </div>

    <div class="detail-grid two">
      <section class="section subtle-section">
        <div class="section-head">
          <div>
            <h4 class="section-title">病历与处方历史</h4>
            <p class="section-copy">支持本地搜索。</p>
          </div>
        </div>

        <div class="field-grid">
          <label class="field"><span>病历搜索</span><input v-model="workspace.recordSearch" placeholder="按患者、主诉或诊断搜索" /></label>
          <div class="action-row" style="align-self: end;">
            <button class="button-secondary" type="button" @click="workspace.loadHistory">
              <span>查询病历</span>
            </button>
          </div>
        </div>

        <ul class="mini-list overflow-list">
          <li v-for="record in workspace.medicalRecords" :key="record.id" class="mini-item">
            <div class="mini-item-head">
              <div class="mini-item-title">{{ record.patientName || '匿名患者' }}</div>
              <span class="pill">{{ workspace.formatDateTime(record.createdAt) }}</span>
            </div>
            <div class="mini-item-meta">
              <span>{{ record.departmentName || '未分科' }}</span>
              <span>{{ record.preliminaryDiagnosis || '待明确' }}</span>
            </div>
            <p class="mini-item-copy">{{ workspace.truncate(record.treatmentPlan || record.presentIllness) }}</p>
          </li>
        </ul>
      </section>

      <section class="section subtle-section">
        <div class="section-head">
          <div>
            <h4 class="section-title">处方历史</h4>
            <p class="section-copy">支持搜索和风险回看。</p>
          </div>
        </div>

        <div class="field-grid">
          <label class="field"><span>处方搜索</span><input v-model="workspace.drugSearch" placeholder="按处方、患者或风险等级搜索" /></label>
          <div class="action-row" style="align-self: end;">
            <button class="button-secondary" type="button" @click="workspace.loadHistory">
              <span>查询处方</span>
            </button>
          </div>
        </div>

        <ul class="mini-list overflow-list">
          <li v-for="prescription in workspace.filteredPrescriptions" :key="prescription.id" class="mini-item">
            <div class="mini-item-head">
              <div class="mini-item-title">处方 #{{ prescription.id }}</div>
              <span class="pill" :data-tone="prescription.review?.reviewStatus === 'BOUND' ? 'healthy' : 'loading'">
                {{ prescription.review?.reviewStatus || prescription.status }}
              </span>
            </div>
            <div class="mini-item-meta">
              <span>{{ prescription.patientName || '匿名患者' }}</span>
              <span>{{ prescription.departmentName || '未分科' }}</span>
              <span>{{ prescription.riskLevel || 'UNKNOWN' }}</span>
            </div>
            <p class="mini-item-copy">{{ workspace.truncate(prescription.review?.llmSummary || prescription.review?.llmSuggestion) }}</p>
          </li>
        </ul>
      </section>
    </div>
  </section>
</template>
