<script setup lang="ts">
import { Building2, ScanSearch, Stethoscope } from 'lucide-vue-next';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">智能分诊</h3>
        <p class="section-copy">输入主诉后，会优先返回本地规则推荐的科室、医生和号源。</p>
      </div>
      <button class="button-secondary" type="button" @click="workspace.runTriage" :disabled="workspace.triaging">
        <ScanSearch :size="16" />
        <span>{{ workspace.triaging ? '分诊中' : '开始分诊' }}</span>
      </button>
    </div>

    <label class="field">
      <span>主诉</span>
      <textarea
        v-model="workspace.triageForm.chiefComplaint"
        class="textarea"
        placeholder="例如：咳嗽三天伴发热、胸闷心慌、腹痛腹泻等"
      />
    </label>

    <div class="candidate-list" v-if="workspace.triageResult">
      <article class="candidate-item active">
        <h4>推荐科室：{{ workspace.triageResult.recommendedDept }}</h4>
        <p>{{ workspace.triageResult.reason }}</p>
      </article>
      <button
        v-for="doctor in workspace.triageResult.recommendedDoctors"
        :key="doctor.id"
        type="button"
        class="candidate-item"
        :class="{ active: workspace.selectedDoctorId === doctor.id }"
        @click="workspace.chooseDoctor(doctor.id)"
      >
        <h4>{{ doctor.name }}</h4>
        <p>{{ doctor.title || '门诊医生' }} / {{ doctor.specialty || '常规诊疗' }}</p>
      </button>
    </div>

    <div class="empty-state" v-else>
      先填写主诉，再点击开始分诊，系统会给出推荐科室和医生。
    </div>

    <div class="workspace-note" v-if="workspace.triageResult">
      <Building2 :size="16" />
      <div>
        <strong>{{ workspace.triageResult.recommendedDept }}</strong>
        <p>推荐理由：{{ workspace.triageResult.reason }}</p>
      </div>
      <Stethoscope :size="16" />
    </div>
  </section>
</template>
