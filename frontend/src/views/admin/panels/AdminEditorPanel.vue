<script setup lang="ts">
import { computed } from 'vue';
import SectionCard from '@/components/shared/SectionCard.vue';

const { workspace } = defineProps<{ workspace: any }>();

const kindLabels: Record<string, string> = {
  department: '科室',
  doctor: '医生',
  schedule: '排班',
  drug: '药品',
  rule: '处方规则',
  ai: 'AI 配置',
  prompt: 'Prompt 模板',
};

type ScheduleDoctorOption = {
  id: number;
  name: string;
  title?: string | null;
  departmentId: number | null;
};

const editorTitle = computed(() => {
  const action = workspace.currentId ? '编辑' : '新增';
  return `${action}${kindLabels[workspace.currentKind] ?? '资料'}`;
});

const scheduleDoctors = computed<ScheduleDoctorOption[]>(() =>
  (workspace.doctors as ScheduleDoctorOption[]).filter((doctor) =>
    !workspace.scheduleForm.departmentId || doctor.departmentId === workspace.scheduleForm.departmentId,
  ),
);

function onScheduleDepartmentChange() {
  const hasCurrentDoctor = scheduleDoctors.value.some((doctor) => doctor.id === workspace.scheduleForm.doctorId);
  if (!hasCurrentDoctor) {
    workspace.scheduleForm.doctorId = scheduleDoctors.value[0]?.id ?? 0;
  }
}

function onScheduleDoctorChange() {
  const doctor = (workspace.doctors as ScheduleDoctorOption[]).find((item) => item.id === workspace.scheduleForm.doctorId);
  if (doctor?.departmentId) {
    workspace.scheduleForm.departmentId = doctor.departmentId;
  }
}
</script>

<template>
  <SectionCard v-if="workspace.currentKind" :title="editorTitle">
    <div class="grid grid-cols-2 gap-3 text-sm">
      <template v-if="workspace.currentKind === 'department'">
        <label class="label-text">编码 <input v-model="workspace.departmentForm.code" class="input-field mt-1" /></label>
        <label class="label-text">名称 <input v-model="workspace.departmentForm.name" class="input-field mt-1" /></label>
        <label class="label-text col-span-2">类型 <input v-model="workspace.departmentForm.type" class="input-field mt-1" /></label>
        <label class="label-text col-span-2">描述 <textarea v-model="workspace.departmentForm.description" class="input-field h-20 resize-none mt-1" /></label>
      </template>

      <template v-else-if="workspace.currentKind === 'doctor'">
        <label class="label-text">账号 <input v-model="workspace.doctorForm.username" class="input-field mt-1" /></label>
        <label class="label-text">密码 <input v-model="workspace.doctorForm.password" type="password" class="input-field mt-1" placeholder="编辑时留空表示不修改" /></label>
        <label class="label-text">姓名 <input v-model="workspace.doctorForm.name" class="input-field mt-1" /></label>
        <label class="label-text">科室 ID <input v-model.number="workspace.doctorForm.departmentId" type="number" min="1" class="input-field mt-1" /></label>
        <label class="label-text">职称 <input v-model="workspace.doctorForm.title" class="input-field mt-1" /></label>
        <label class="label-text">专长 <input v-model="workspace.doctorForm.specialty" class="input-field mt-1" /></label>
        <label class="label-text">状态 <input v-model="workspace.doctorForm.status" class="input-field mt-1" /></label>
        <label class="label-text col-span-2">介绍 <textarea v-model="workspace.doctorForm.introduction" class="input-field h-20 resize-none mt-1" /></label>
      </template>

      <template v-else-if="workspace.currentKind === 'schedule'">
        <label class="label-text">
          科室
          <select v-model.number="workspace.scheduleForm.departmentId" class="input-field mt-1" @change="onScheduleDepartmentChange">
            <option v-for="department in workspace.departments" :key="department.id" :value="department.id">
              {{ department.name }}
            </option>
          </select>
        </label>
        <label class="label-text">
          医生
          <select v-model.number="workspace.scheduleForm.doctorId" class="input-field mt-1" @change="onScheduleDoctorChange">
            <option v-if="!scheduleDoctors.length" :value="0">暂无可选医生</option>
            <option v-for="doctor in scheduleDoctors" :key="doctor.id" :value="doctor.id">
              {{ doctor.name }}{{ doctor.title ? ` · ${doctor.title}` : '' }}
            </option>
          </select>
        </label>
        <label class="label-text">日期 <input v-model="workspace.scheduleForm.workDate" type="date" class="input-field mt-1" /></label>
        <label class="label-text">
          时段
          <select v-model="workspace.scheduleForm.period" class="input-field mt-1">
            <option value="上午">上午</option>
            <option value="下午">下午</option>
            <option value="夜诊">夜诊</option>
          </select>
        </label>
        <label class="label-text">总号源 <input v-model.number="workspace.scheduleForm.totalSlots" type="number" min="1" class="input-field mt-1" /></label>
        <label class="label-text">剩余号源 <input v-model.number="workspace.scheduleForm.remainingSlots" type="number" min="0" class="input-field mt-1" /></label>
        <label class="label-text">级别 <input v-model="workspace.scheduleForm.visitLevel" class="input-field mt-1" /></label>
        <label class="label-text">
          状态
          <select v-model="workspace.scheduleForm.status" class="input-field mt-1">
            <option value="ACTIVE">启用</option>
            <option value="INACTIVE">停用</option>
          </select>
        </label>
      </template>

      <template v-else-if="workspace.currentKind === 'drug'">
        <label class="label-text">编码 <input v-model="workspace.drugForm.code" class="input-field mt-1" /></label>
        <label class="label-text">名称 <input v-model="workspace.drugForm.name" class="input-field mt-1" /></label>
        <label class="label-text">拼音码 <input v-model="workspace.drugForm.pinyinCode" class="input-field mt-1" /></label>
        <label class="label-text">规格 <input v-model="workspace.drugForm.specification" class="input-field mt-1" /></label>
        <label class="label-text">剂型 <input v-model="workspace.drugForm.dosageForm" class="input-field mt-1" /></label>
        <label class="label-text">包装 <input v-model="workspace.drugForm.packageUnit" class="input-field mt-1" /></label>
        <label class="label-text">厂家 <input v-model="workspace.drugForm.manufacturer" class="input-field mt-1" /></label>
        <label class="label-text">单价 <input v-model.number="workspace.drugForm.unitPrice" type="number" min="0" step="0.01" class="input-field mt-1" /></label>
        <label class="label-text">状态 <input v-model="workspace.drugForm.status" class="input-field mt-1" /></label>
        <label class="label-text col-span-2">默认用法 <textarea v-model="workspace.drugForm.defaultUsage" class="input-field h-20 resize-none mt-1" /></label>
        <label class="label-text col-span-2">禁忌 <textarea v-model="workspace.drugForm.contraindications" class="input-field h-20 resize-none mt-1" /></label>
        <label class="label-text col-span-2">注意事项 <textarea v-model="workspace.drugForm.precautions" class="input-field h-20 resize-none mt-1" /></label>
        <label class="label-text col-span-2">适应症 <textarea v-model="workspace.drugForm.indications" class="input-field h-20 resize-none mt-1" /></label>
        <label class="label-text col-span-2">相互作用 <textarea v-model="workspace.drugForm.interactionSummary" class="input-field h-20 resize-none mt-1" /></label>
      </template>

      <template v-else-if="workspace.currentKind === 'rule'">
        <label class="label-text">规则码 <input v-model="workspace.ruleForm.ruleCode" class="input-field mt-1" /></label>
        <label class="label-text">类型 <input v-model="workspace.ruleForm.ruleType" class="input-field mt-1" /></label>
        <label class="label-text">风险级别 <input v-model="workspace.ruleForm.riskLevel" class="input-field mt-1" /></label>
        <label class="label-text">状态 <input v-model="workspace.ruleForm.status" class="input-field mt-1" /></label>
        <label class="label-text col-span-2">适用药品 <textarea v-model="workspace.ruleForm.applicableDrugs" class="input-field h-20 resize-none mt-1" /></label>
        <label class="label-text col-span-2">适用病种 <textarea v-model="workspace.ruleForm.applicableDiseases" class="input-field h-20 resize-none mt-1" /></label>
        <label class="label-text col-span-2">适用人群 <textarea v-model="workspace.ruleForm.applicablePopulations" class="input-field h-20 resize-none mt-1" /></label>
        <label class="label-text col-span-2">条件表达式 <textarea v-model="workspace.ruleForm.conditionExpression" class="input-field h-20 resize-none mt-1" /></label>
        <label class="label-text col-span-2">告警 <textarea v-model="workspace.ruleForm.alertMessage" class="input-field h-20 resize-none mt-1" /></label>
        <label class="label-text col-span-2">建议 <textarea v-model="workspace.ruleForm.suggestion" class="input-field h-20 resize-none mt-1" /></label>
        <label class="label-text col-span-2">依据 <textarea v-model="workspace.ruleForm.basis" class="input-field h-20 resize-none mt-1" /></label>
      </template>

      <template v-else-if="workspace.currentKind === 'ai'">
        <label class="label-text">Provider <input v-model="workspace.aiForm.provider" class="input-field mt-1" placeholder="先可用 LOCAL_RULE，后续再接豆包" /></label>
        <label class="label-text">模型 <input v-model="workspace.aiForm.modelName" class="input-field mt-1" /></label>
        <label class="label-text">任务范围 <input v-model="workspace.aiForm.taskScope" class="input-field mt-1" /></label>
        <label class="label-text">状态 <input v-model="workspace.aiForm.status" class="input-field mt-1" /></label>
        <label class="label-text">超时秒数 <input v-model.number="workspace.aiForm.timeoutSeconds" type="number" min="1" class="input-field mt-1" /></label>
        <label class="label-text">优先级 <input v-model.number="workspace.aiForm.priority" type="number" min="0" class="input-field mt-1" /></label>
        <label class="label-text flex items-center gap-2">默认配置 <input v-model="workspace.aiForm.defaultConfig" type="checkbox" /></label>
        <label class="label-text flex items-center gap-2">启用 <input v-model="workspace.aiForm.enabled" type="checkbox" /></label>
        <label class="label-text">健康状态 <input v-model="workspace.aiForm.healthStatus" class="input-field mt-1" /></label>
        <label class="label-text">版本 <input v-model="workspace.aiForm.configVersion" class="input-field mt-1" /></label>
        <label class="label-text col-span-2">API URL <input v-model="workspace.aiForm.apiUrl" class="input-field mt-1" placeholder="后续接外部 AI 时再填" /></label>
        <label class="label-text col-span-2">API Key <input v-model="workspace.aiForm.apiKey" type="password" class="input-field mt-1" placeholder="仅保存，不展示" /></label>
        <label class="label-text col-span-2">Key Version <input v-model="workspace.aiForm.keyVersion" class="input-field mt-1" /></label>
      </template>

      <template v-else>
        <label class="label-text">模板编码 <input v-model="workspace.promptForm.templateCode" class="input-field mt-1" /></label>
        <label class="label-text">任务类型 <input v-model="workspace.promptForm.taskType" class="input-field mt-1" /></label>
        <label class="label-text">
          科室编码
          <input
            v-model.trim="workspace.promptForm.deptCode"
            class="input-field mt-1"
            placeholder="留空表示全局模板"
          />
        </label>
        <label class="label-text">状态 <input v-model="workspace.promptForm.status" class="input-field mt-1" /></label>
        <label class="label-text">版本 <input v-model.number="workspace.promptForm.version" type="number" min="0" class="input-field mt-1" /></label>
        <label class="label-text col-span-2">模板正文 <textarea v-model="workspace.promptForm.templateBody" class="input-field h-20 resize-none mt-1" /></label>
        <label class="label-text col-span-2">变量白名单 <textarea v-model="workspace.promptForm.variableWhitelist" class="input-field h-20 resize-none mt-1" /></label>
        <label class="label-text flex items-center gap-2">默认模板 <input v-model="workspace.promptForm.defaultTemplate" type="checkbox" /></label>
      </template>
    </div>
    <div class="flex gap-2 mt-4">
      <button class="btn-primary" type="button" @click="workspace.saveCurrent()" :disabled="workspace.saving">{{ workspace.saving ? '保存中...' : '保存' }}</button>
      <button class="btn-secondary" type="button" @click="workspace.closeEditor()">取消</button>
    </div>
  </SectionCard>
</template>
