<script setup lang="ts">
import { CalendarDays, FileText, ScanSearch } from 'lucide-vue-next';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">历史与建议</h3>
        <p class="section-copy">查看历史分诊和当前建议，方便随时回到上一次的工作流。</p>
      </div>
      <span class="pill" :data-tone="workspace.triageResult ? 'healthy' : 'loading'">
        <ScanSearch :size="14" />
        <span>{{ workspace.triageHistory.length }} 条</span>
      </span>
    </div>

    <div class="detail-grid two">
      <section class="section subtle-section">
        <div class="section-head">
          <div>
            <h4 class="section-title">分诊历史</h4>
            <p class="section-copy">最近一次本地规则分诊和 AI 推荐都在这里留痕。</p>
          </div>
        </div>

        <ul class="mini-list overflow-list">
          <li v-for="item in workspace.triageHistory" :key="item.triageRecordId" class="mini-item">
            <div class="mini-item-head">
              <div class="mini-item-title">{{ item.recommendedDept }}</div>
              <span class="pill">{{ item.callStatus }}</span>
            </div>
            <div class="mini-item-meta">
              <span>记录 #{{ item.triageRecordId }}</span>
              <span>{{ item.recommendationSource }}</span>
            </div>
            <p class="mini-item-copy">{{ workspace.truncate(item.reason, 96) }}</p>
          </li>
        </ul>
      </section>

      <section class="section subtle-section">
        <div class="section-head">
          <div>
            <h4 class="section-title">当前建议</h4>
            <p class="section-copy">把当前选中的科室、医生和号源放在一起确认。</p>
          </div>
        </div>

        <div class="mini-list">
          <div class="mini-item">
            <span class="label">科室</span>
            <span class="value">{{ workspace.selectedDepartment?.name || workspace.triageResult?.recommendedDept || '未选择' }}</span>
          </div>
          <div class="mini-item">
            <span class="label">医生</span>
            <span class="value">{{ workspace.selectedDoctor?.name || workspace.triageResult?.recommendedDoctors[0]?.name || '未选择' }}</span>
          </div>
          <div class="mini-item">
            <span class="label">号源</span>
            <span class="value">
              {{ workspace.selectedSchedule ? `${workspace.selectedSchedule.workDate} ${workspace.selectedSchedule.period}` : '未选择' }}
            </span>
          </div>
          <div class="mini-item">
            <span class="label">分诊理由</span>
            <span class="value">{{ workspace.triageResult?.reason || '等待分诊结果' }}</span>
          </div>
          <div class="mini-item">
            <span class="label">最近一次分诊</span>
            <span class="value">{{ workspace.latestTriage?.recommendedDept || '暂无记录' }} / {{ workspace.latestTriage?.callStatus || 'WAITING' }}</span>
          </div>
          <div class="mini-item">
            <span class="label">最近挂号</span>
            <span class="value">{{ workspace.latestRegistration?.departmentName || '暂无记录' }} / {{ workspace.latestRegistration?.status || 'NONE' }}</span>
          </div>
        </div>
      </section>
    </div>

    <div class="workspace-note">
      <CalendarDays :size="16" />
      <div>
        <strong>流程闭环</strong>
        <p>分诊、挂号、病历和反馈都在同一条工作流里，不再散在一个大页面里。</p>
      </div>
      <FileText :size="16" />
    </div>
  </section>
</template>
