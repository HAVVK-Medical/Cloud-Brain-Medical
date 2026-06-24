<script setup lang="ts">
const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">审计记录</h3>
        <p class="section-copy">AI 调用、审计日志和当前上下文集中展示。</p>
      </div>
    </div>

    <div class="detail-grid two">
      <section class="section subtle-section">
        <div class="section-head">
          <div>
            <h4 class="section-title">AI 调用</h4>
            <p class="section-copy">最近几条调用记录。</p>
          </div>
        </div>

        <div class="mini-list overflow-list">
          <div class="mini-item" v-for="record in workspace.aiRecords.slice(0, 6)" :key="record.id">
            <div class="mini-item-head">
              <div class="mini-item-title">{{ record.taskType }}</div>
              <span class="pill">{{ record.callStatus }}</span>
            </div>
            <div class="mini-item-meta">
              <span>{{ record.provider || 'LOCAL' }}</span>
              <span>{{ record.operatorRole || 'unknown' }}</span>
              <span>{{ workspace.formatDateTime(record.createdAt) }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="section subtle-section">
        <div class="section-head">
          <div>
            <h4 class="section-title">审计日志</h4>
            <p class="section-copy">追溯操作链路和结果。</p>
          </div>
        </div>

        <div class="mini-list overflow-list">
          <div class="mini-item" v-for="audit in workspace.auditLogs.slice(0, 6)" :key="audit.id">
            <div class="mini-item-head">
              <div class="mini-item-title">{{ audit.action }}</div>
              <span class="pill" :data-tone="audit.success ? 'healthy' : 'danger'">{{ audit.success ? '成功' : '失败' }}</span>
            </div>
            <div class="mini-item-meta">
              <span>{{ audit.actorRole || 'unknown' }}</span>
              <span>{{ audit.resourceType || 'NO_RESOURCE' }}</span>
              <span>{{ workspace.formatDateTime(audit.occurredAt) }}</span>
            </div>
          </div>
        </div>
      </section>
    </div>

    <div class="mini-list">
      <div class="mini-item">
        <span class="label">当前资源</span>
        <span class="value">{{ workspace.currentKind }} {{ workspace.currentId ? `#${workspace.currentId}` : '新建' }}</span>
      </div>
      <div class="mini-item">
        <span class="label">科室筛选</span>
        <span class="value">{{ workspace.selectedDepartment?.name || '全部科室' }}</span>
      </div>
      <div class="mini-item">
        <span class="label">医生筛选</span>
        <span class="value">{{ workspace.selectedDoctor?.name || '全部医生' }}</span>
      </div>
    </div>
  </section>
</template>
