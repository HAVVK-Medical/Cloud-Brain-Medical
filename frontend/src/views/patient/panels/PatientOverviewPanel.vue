<script setup lang="ts">
import { CalendarDays, FileText, ScanSearch, Stethoscope, Ticket } from 'lucide-vue-next';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">工作台概览</h3>
        <p class="section-copy">先看当前分诊、挂号和近期记录，再进入具体操作。</p>
      </div>
      <span class="status-chip" :data-tone="workspace.activeTone">
        <span class="chip-dot" />
        <span>{{ workspace.displayName }}</span>
      </span>
    </div>

    <div class="workspace-metrics">
      <article class="metric workspace-metric">
        <div class="card-head">
          <h3>当前分诊</h3>
          <ScanSearch :size="18" />
        </div>
        <div class="metric-value">{{ workspace.triageResult ? '已生成' : '待分诊' }}</div>
        <p>{{ workspace.triageResult?.recommendedDept || '输入主诉后即可生成推荐科室' }}</p>
      </article>

      <article class="metric workspace-metric">
        <div class="card-head">
          <h3>当前挂号</h3>
          <Ticket :size="18" />
        </div>
        <div class="metric-value">{{ workspace.latestRegistration?.status || 'NONE' }}</div>
        <p>{{ workspace.latestRegistration ? `${workspace.latestRegistration.doctorName || '未选择医生'} / ${workspace.latestRegistration.departmentName || '未选择科室'}` : '暂无挂号记录' }}</p>
      </article>

      <article class="metric workspace-metric">
        <div class="card-head">
          <h3>待处理</h3>
          <Stethoscope :size="18" />
        </div>
        <div class="metric-value">{{ workspace.waitingRegistrations.length }}</div>
        <p>未就诊的挂号可以直接取消或重新选择号源。</p>
      </article>

      <article class="metric workspace-metric">
        <div class="card-head">
          <h3>最近更新</h3>
          <CalendarDays :size="18" />
        </div>
        <div class="metric-value">{{ workspace.formatDateTime(workspace.registrations[0]?.registrationTime) }}</div>
        <p>分诊、挂号和反馈会在这里持续同步。</p>
      </article>
    </div>

    <div class="workspace-summary-grid">
      <div class="mini-item">
        <div class="mini-item-head">
          <div class="mini-item-title">推荐科室</div>
          <span class="pill" :data-tone="workspace.triageResult ? 'healthy' : 'loading'">
            {{ workspace.triageResult ? '已生成' : '待输入' }}
          </span>
        </div>
        <p class="mini-item-copy">{{ workspace.triageResult?.reason || '输入主诉后会给出本地规则分诊建议。' }}</p>
      </div>

      <div class="mini-item">
        <div class="mini-item-head">
          <div class="mini-item-title">我的病历</div>
          <span class="pill">{{ workspace.medicalRecords.length }}</span>
        </div>
        <p class="mini-item-copy">可查看最近就诊记录、处方和审核结果。</p>
      </div>

      <div class="mini-item">
        <div class="mini-item-head">
          <div class="mini-item-title">完成就诊</div>
          <span class="pill" data-tone="healthy">{{ workspace.completedRegistrations.length }}</span>
        </div>
        <p class="mini-item-copy">反馈入口和历史数据都在这个工作区里。</p>
      </div>
    </div>

    <div class="action-row">
      <button class="button-secondary" type="button" @click="workspace.setActivePatientPanel('triage')">
        <ScanSearch :size="16" />
        <span>去分诊</span>
      </button>
      <button class="button-secondary" type="button" @click="workspace.setActivePatientPanel('registration')">
        <Ticket :size="16" />
        <span>去挂号</span>
      </button>
      <button class="button-secondary" type="button" @click="workspace.setActivePatientPanel('records')">
        <FileText :size="16" />
        <span>看病历</span>
      </button>
      <button class="button-ghost" type="button" @click="workspace.setActivePatientPanel('history')">
        <CalendarDays :size="16" />
        <span>看历史</span>
      </button>
    </div>
  </section>
</template>
