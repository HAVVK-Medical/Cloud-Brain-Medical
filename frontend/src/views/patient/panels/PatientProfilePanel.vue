<script setup lang="ts">
import { CheckCircle2, Trash2 } from 'lucide-vue-next';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">个人资料</h3>
        <p class="section-copy">维护真实姓名、联系方式和既往史，方便后续接诊。</p>
      </div>
      <button class="button-secondary" type="button" @click="workspace.saveProfile" :disabled="workspace.savingProfile">
        <CheckCircle2 :size="16" />
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

    <section class="section subtle-section">
      <div class="section-head">
        <div>
          <h4 class="section-title">挂号列表</h4>
          <p class="section-copy">等待中的挂号可以直接取消。</p>
        </div>
      </div>

      <ul class="mini-list overflow-list">
        <li v-for="registration in workspace.registrations" :key="registration.id" class="mini-item">
          <div class="mini-item-head">
            <div class="mini-item-title">
              {{ registration.departmentName || '未分科' }} / {{ registration.doctorName || '未知医生' }}
            </div>
            <span class="pill" :data-tone="registration.status === 'WAITING' ? 'loading' : registration.status === 'COMPLETED' ? 'healthy' : undefined">
              {{ registration.status }}
            </span>
          </div>
          <div class="mini-item-meta">
            <span>{{ workspace.formatDate(registration.workDate) }} {{ registration.period || '' }}</span>
            <span>号源 #{{ registration.scheduleId }}</span>
            <span>病历 {{ registration.medicalRecordId ?? '未生成' }}</span>
          </div>
          <p class="mini-item-copy">{{ registration.chiefComplaint || '暂无主诉' }}</p>
          <div v-if="registration.status === 'WAITING'" class="stack">
            <label class="field">
              <span>取消原因</span>
              <input v-model="workspace.cancelReasons[registration.id]" placeholder="请输入取消原因" />
            </label>
            <button class="button-danger" type="button" @click="workspace.cancelWaitingRegistration(registration.id)" :disabled="workspace.canceling">
              <Trash2 :size="16" />
              <span>{{ workspace.canceling ? '取消中' : '取消挂号' }}</span>
            </button>
          </div>
        </li>
      </ul>

      <div class="empty-state" v-if="!workspace.registrations.length">还没有挂号记录。</div>
    </section>

    <section class="section subtle-section">
      <div class="section-head">
        <div>
          <h4 class="section-title">反馈</h4>
          <p class="section-copy">对分诊和就诊体验给出评分。</p>
        </div>
        <button class="button-secondary" type="button" @click="workspace.submitFeedback" :disabled="workspace.submittingFeedback">
          <CheckCircle2 :size="16" />
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
    </section>
  </section>
</template>
