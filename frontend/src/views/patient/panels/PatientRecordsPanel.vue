<script setup lang="ts">
import { FileText } from 'lucide-vue-next';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">病历与处方</h3>
        <p class="section-copy">完整查看自己的就诊记录、病历和处方结果。</p>
      </div>
    </div>

    <div class="detail-grid two">
      <div class="stack">
        <h4 class="section-title">病历</h4>
        <ul class="mini-list overflow-list">
          <li v-for="record in workspace.medicalRecords" :key="record.id" class="mini-item">
            <div class="mini-item-head">
              <div class="mini-item-title">{{ record.preliminaryDiagnosis || record.chiefComplaint || '病历记录' }}</div>
              <span class="pill">{{ workspace.formatDateTime(record.createdAt) }}</span>
            </div>
            <div class="mini-item-meta">
              <span>{{ record.departmentName || '未分科' }}</span>
              <span>{{ record.doctorName || '未知医生' }}</span>
              <span>版本 {{ record.version ?? 0 }}</span>
            </div>
            <p class="mini-item-copy">{{ workspace.truncate(record.treatmentPlan || record.presentIllness) }}</p>
          </li>
        </ul>
      </div>

      <div class="stack">
        <h4 class="section-title">处方</h4>
        <ul class="mini-list overflow-list">
          <li v-for="prescription in workspace.prescriptions" :key="prescription.id" class="mini-item">
            <div class="mini-item-head">
              <div class="mini-item-title">处方 #{{ prescription.id }}</div>
              <span class="pill" :data-tone="prescription.review?.reviewStatus === 'BOUND' ? 'healthy' : undefined">
                {{ prescription.review?.reviewStatus || prescription.status }}
              </span>
            </div>
            <div class="mini-item-meta">
              <span>{{ prescription.departmentName || '未分科' }}</span>
              <span>风险 {{ prescription.riskLevel || 'UNKNOWN' }}</span>
              <span>{{ workspace.formatDateTime(prescription.createdAt) }}</span>
            </div>
            <p class="mini-item-copy">{{ workspace.truncate(prescription.review?.llmSummary || prescription.review?.llmSuggestion) }}</p>
          </li>
        </ul>
      </div>
    </div>

    <div class="detail-grid two">
      <div class="section subtle-section">
        <div class="section-head">
          <div>
            <h4 class="section-title">个人信息</h4>
            <p class="section-copy">保持基本资料完整，便于医生接诊。</p>
          </div>
          <button class="button-secondary" type="button" @click="workspace.saveProfile" :disabled="workspace.savingProfile">
            <FileText :size="16" />
            <span>{{ workspace.savingProfile ? '保存中' : '保存资料' }}</span>
          </button>
        </div>

        <div class="field-grid">
          <label class="field">
            <span>姓名</span>
            <input v-model="workspace.profileForm.realName" placeholder="请输入真实姓名" />
          </label>
          <label class="field">
            <span>性别</span>
            <input v-model="workspace.profileForm.gender" placeholder="例如：男 / 女" />
          </label>
          <label class="field">
            <span>年龄</span>
            <input v-model="workspace.profileForm.age" type="number" min="0" step="1" placeholder="请输入年龄" />
          </label>
          <label class="field">
            <span>电话</span>
            <input v-model="workspace.profileForm.phone" placeholder="请输入手机号" />
          </label>
        </div>

        <label class="field">
          <span>身份证号</span>
          <input v-model="workspace.profileForm.idCardNumber" placeholder="请输入身份证号" />
        </label>
        <label class="field">
          <span>既往史</span>
          <textarea v-model="workspace.profileForm.medicalHistory" class="textarea" placeholder="例如：高血压、糖尿病、手术史等" />
        </label>
        <label class="field">
          <span>备注</span>
          <textarea v-model="workspace.profileForm.remark" class="textarea" placeholder="补充说明" />
        </label>
      </div>

      <div class="section subtle-section">
        <div class="section-head">
          <div>
            <h4 class="section-title">反馈</h4>
            <p class="section-copy">对分诊和就诊体验给出评分。</p>
          </div>
          <button class="button-secondary" type="button" @click="workspace.submitFeedback" :disabled="workspace.submittingFeedback">
            <FileText :size="16" />
            <span>{{ workspace.submittingFeedback ? '提交中' : '提交反馈' }}</span>
          </button>
        </div>

        <div class="field-grid">
          <label class="field">
            <span>选择挂号</span>
            <select v-model="workspace.feedbackForm.registrationId">
              <option :value="null">请选择已完成挂号</option>
              <option v-for="registration in workspace.completedRegistrations" :key="registration.id" :value="registration.id">
                #{{ registration.id }} / {{ registration.departmentName || '未分科' }} / {{ workspace.formatDateTime(registration.completedTime) }}
              </option>
            </select>
          </label>
          <label class="field">
            <span>评分</span>
            <select v-model.number="workspace.feedbackForm.rating">
              <option :value="5">5 分</option>
              <option :value="4">4 分</option>
              <option :value="3">3 分</option>
              <option :value="2">2 分</option>
              <option :value="1">1 分</option>
            </select>
          </label>
        </div>

        <label class="field">
          <span>分诊是否准确</span>
          <select v-model="workspace.feedbackForm.triageAccurate">
            <option :value="true">准确</option>
            <option :value="false">不准确</option>
          </select>
        </label>
        <label class="field">
          <span>反馈内容</span>
          <textarea v-model="workspace.feedbackForm.comment" class="textarea" placeholder="描述本次分诊、挂号或接诊体验" />
        </label>

        <ul class="mini-list overflow-list">
          <li v-for="feedback in workspace.feedbacks" :key="feedback.id" class="mini-item">
            <div class="mini-item-head">
              <div class="mini-item-title">评分 {{ feedback.rating }} / 5</div>
              <span class="pill" :data-tone="feedback.triageAccurate ? 'healthy' : 'danger'">
                {{ feedback.triageAccurate === null ? '未评价' : feedback.triageAccurate ? '分诊准确' : '分诊偏差' }}
              </span>
            </div>
            <div class="mini-item-meta">
              <span>挂号 #{{ feedback.registrationId }}</span>
              <span>{{ workspace.formatDateTime(feedback.createdAt) }}</span>
            </div>
            <p class="mini-item-copy">{{ feedback.comment || '暂无内容' }}</p>
          </li>
        </ul>
      </div>
    </div>

    <section class="section subtle-section">
      <div class="section-head">
        <div>
          <h4 class="section-title">分诊历史</h4>
          <p class="section-copy">最近一次本地规则分诊和 AI 推荐都在这里留痕。</p>
        </div>
        <span class="pill" :data-tone="workspace.triageResult ? 'healthy' : 'loading'">
          <span>{{ workspace.triageHistory.length }} 条</span>
        </span>
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

      <div class="workspace-note" v-if="workspace.selectedSchedule || workspace.selectedDoctor">
        <FileText :size="16" />
        <div>
          <strong>当前建议</strong>
          <p>
            {{ workspace.selectedDepartment?.name || workspace.triageResult?.recommendedDept || '未选择科室' }} /
            {{ workspace.selectedDoctor?.name || workspace.triageResult?.recommendedDoctors[0]?.name || '未选择医生' }} /
            {{ workspace.selectedSchedule ? `${workspace.selectedSchedule.workDate} ${workspace.selectedSchedule.period}` : '未选择号源' }}
          </p>
        </div>
      </div>
    </section>
  </section>
</template>
