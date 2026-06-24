<script setup lang="ts">
import { BadgeCheck, CirclePlus } from 'lucide-vue-next';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">编辑面板</h3>
        <p class="section-copy">点击左侧资源即可编辑当前项，保存后自动刷新。</p>
      </div>
      <div class="action-row">
        <button class="button-secondary" type="button" @click="workspace.saveCurrent" :disabled="workspace.saving">
          <CirclePlus :size="16" />
          <span>{{ workspace.saving ? '保存中' : workspace.currentId ? '保存修改' : '新增保存' }}</span>
        </button>
        <button class="button-ghost" type="button" @click="workspace.toggleCurrent" :disabled="workspace.saving || !workspace.currentId">
          <BadgeCheck :size="16" />
          <span>启停切换</span>
        </button>
      </div>
    </div>

    <div class="mini-item">
      <div class="mini-item-head">
        <div class="mini-item-title">当前资源</div>
        <span class="pill">{{ workspace.currentKind }} {{ workspace.currentId ? `#${workspace.currentId}` : '新建' }}</span>
      </div>
      <p class="mini-item-copy">左侧列表选中后会在这里完成编辑。</p>
    </div>

    <div v-if="workspace.currentKind === 'department'" class="stack">
      <div class="field-grid">
        <label class="field"><span>编码</span><input v-model="workspace.departmentForm.code" /></label>
        <label class="field"><span>名称</span><input v-model="workspace.departmentForm.name" /></label>
      </div>
      <div class="field-grid">
        <label class="field"><span>类型</span><input v-model="workspace.departmentForm.type" /></label>
        <label class="field"><span>状态</span><input v-model="workspace.departmentForm.status" /></label>
      </div>
      <label class="field"><span>说明</span><textarea v-model="workspace.departmentForm.description" class="textarea" /></label>
    </div>

    <div v-else-if="workspace.currentKind === 'doctor'" class="stack">
      <div class="field-grid">
        <label class="field"><span>账号</span><input v-model="workspace.doctorForm.username" /></label>
        <label class="field"><span>密码</span><input v-model="workspace.doctorForm.password" type="password" placeholder="编辑时留空表示不修改" /></label>
        <label class="field"><span>姓名</span><input v-model="workspace.doctorForm.name" /></label>
        <label class="field"><span>科室 ID</span><input v-model.number="workspace.doctorForm.departmentId" type="number" min="1" /></label>
      </div>
      <div class="field-grid">
        <label class="field"><span>职称</span><input v-model="workspace.doctorForm.title" /></label>
        <label class="field"><span>专长</span><input v-model="workspace.doctorForm.specialty" /></label>
        <label class="field"><span>状态</span><input v-model="workspace.doctorForm.status" /></label>
      </div>
      <label class="field"><span>介绍</span><textarea v-model="workspace.doctorForm.introduction" class="textarea" /></label>
    </div>

    <div v-else-if="workspace.currentKind === 'schedule'" class="stack">
      <div class="field-grid">
        <label class="field"><span>医生 ID</span><input v-model.number="workspace.scheduleForm.doctorId" type="number" min="1" /></label>
        <label class="field"><span>科室 ID</span><input v-model.number="workspace.scheduleForm.departmentId" type="number" min="1" /></label>
        <label class="field"><span>日期</span><input v-model="workspace.scheduleForm.workDate" type="date" /></label>
        <label class="field"><span>时段</span><input v-model="workspace.scheduleForm.period" /></label>
        <label class="field"><span>总号源</span><input v-model.number="workspace.scheduleForm.totalSlots" type="number" min="1" /></label>
        <label class="field"><span>剩余号源</span><input v-model.number="workspace.scheduleForm.remainingSlots" type="number" min="0" /></label>
      </div>
      <div class="field-grid">
        <label class="field"><span>级别</span><input v-model="workspace.scheduleForm.visitLevel" /></label>
        <label class="field"><span>状态</span><input v-model="workspace.scheduleForm.status" /></label>
      </div>
    </div>

    <div v-else-if="workspace.currentKind === 'drug'" class="stack">
      <div class="field-grid">
        <label class="field"><span>编码</span><input v-model="workspace.drugForm.code" /></label>
        <label class="field"><span>名称</span><input v-model="workspace.drugForm.name" /></label>
        <label class="field"><span>拼音码</span><input v-model="workspace.drugForm.pinyinCode" /></label>
        <label class="field"><span>规格</span><input v-model="workspace.drugForm.specification" /></label>
        <label class="field"><span>剂型</span><input v-model="workspace.drugForm.dosageForm" /></label>
        <label class="field"><span>包装</span><input v-model="workspace.drugForm.packageUnit" /></label>
      </div>
      <div class="field-grid">
        <label class="field"><span>厂家</span><input v-model="workspace.drugForm.manufacturer" /></label>
        <label class="field"><span>单价</span><input v-model.number="workspace.drugForm.unitPrice" type="number" min="0" step="0.01" /></label>
        <label class="field"><span>状态</span><input v-model="workspace.drugForm.status" /></label>
      </div>
      <label class="field"><span>默认用法</span><textarea v-model="workspace.drugForm.defaultUsage" class="textarea" /></label>
      <label class="field"><span>禁忌</span><textarea v-model="workspace.drugForm.contraindications" class="textarea" /></label>
      <label class="field"><span>注意事项</span><textarea v-model="workspace.drugForm.precautions" class="textarea" /></label>
      <label class="field"><span>适应症</span><textarea v-model="workspace.drugForm.indications" class="textarea" /></label>
      <label class="field"><span>相互作用</span><textarea v-model="workspace.drugForm.interactionSummary" class="textarea" /></label>
    </div>

    <div v-else-if="workspace.currentKind === 'rule'" class="stack">
      <div class="field-grid">
        <label class="field"><span>规则码</span><input v-model="workspace.ruleForm.ruleCode" /></label>
        <label class="field"><span>类型</span><input v-model="workspace.ruleForm.ruleType" /></label>
        <label class="field"><span>风险级别</span><input v-model="workspace.ruleForm.riskLevel" /></label>
        <label class="field"><span>状态</span><input v-model="workspace.ruleForm.status" /></label>
      </div>
      <label class="field"><span>适用药品</span><textarea v-model="workspace.ruleForm.applicableDrugs" class="textarea" /></label>
      <label class="field"><span>适用病种</span><textarea v-model="workspace.ruleForm.applicableDiseases" class="textarea" /></label>
      <label class="field"><span>适用人群</span><textarea v-model="workspace.ruleForm.applicablePopulations" class="textarea" /></label>
      <label class="field"><span>条件表达式</span><textarea v-model="workspace.ruleForm.conditionExpression" class="textarea" /></label>
      <label class="field"><span>告警</span><textarea v-model="workspace.ruleForm.alertMessage" class="textarea" /></label>
      <label class="field"><span>建议</span><textarea v-model="workspace.ruleForm.suggestion" class="textarea" /></label>
      <label class="field"><span>依据</span><textarea v-model="workspace.ruleForm.basis" class="textarea" /></label>
    </div>

    <div v-else-if="workspace.currentKind === 'ai'" class="stack">
      <div class="field-grid">
        <label class="field"><span>Provider</span><input v-model="workspace.aiForm.provider" placeholder="先可用 LOCAL_RULE，后续再接豆包" /></label>
        <label class="field"><span>模型</span><input v-model="workspace.aiForm.modelName" /></label>
        <label class="field"><span>任务范围</span><input v-model="workspace.aiForm.taskScope" /></label>
        <label class="field"><span>状态</span><input v-model="workspace.aiForm.status" /></label>
        <label class="field"><span>超时秒数</span><input v-model.number="workspace.aiForm.timeoutSeconds" type="number" min="1" /></label>
        <label class="field"><span>优先级</span><input v-model.number="workspace.aiForm.priority" type="number" min="0" /></label>
      </div>
      <div class="field-grid">
        <label class="field"><span>默认配置</span><input v-model="workspace.aiForm.defaultConfig" type="checkbox" /></label>
        <label class="field"><span>启用</span><input v-model="workspace.aiForm.enabled" type="checkbox" /></label>
        <label class="field"><span>健康状态</span><input v-model="workspace.aiForm.healthStatus" /></label>
        <label class="field"><span>版本</span><input v-model="workspace.aiForm.configVersion" /></label>
      </div>
      <label class="field"><span>API URL</span><input v-model="workspace.aiForm.apiUrl" placeholder="后续接外部 AI 时再填" /></label>
      <label class="field"><span>API Key</span><input v-model="workspace.aiForm.apiKey" type="password" placeholder="仅保存，不展示" /></label>
      <label class="field"><span>Key Version</span><input v-model="workspace.aiForm.keyVersion" /></label>
    </div>

    <div v-else class="stack">
      <div class="field-grid">
        <label class="field"><span>模板编码</span><input v-model="workspace.promptForm.templateCode" /></label>
        <label class="field"><span>任务类型</span><input v-model="workspace.promptForm.taskType" /></label>
        <label class="field"><span>科室编码</span><input v-model="workspace.promptForm.deptCode" /></label>
        <label class="field"><span>状态</span><input v-model="workspace.promptForm.status" /></label>
        <label class="field"><span>版本</span><input v-model.number="workspace.promptForm.version" type="number" min="0" /></label>
      </div>
      <label class="field"><span>模板正文</span><textarea v-model="workspace.promptForm.templateBody" class="textarea" /></label>
      <label class="field"><span>变量白名单</span><textarea v-model="workspace.promptForm.variableWhitelist" class="textarea" /></label>
      <label class="field"><span>默认模板</span><input v-model="workspace.promptForm.defaultTemplate" type="checkbox" /></label>
    </div>
  </section>
</template>
