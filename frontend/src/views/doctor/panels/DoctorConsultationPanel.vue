<script setup lang="ts">
import { Plus, Search, Send, Stethoscope, Trash2 } from 'lucide-vue-next';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">接诊工作区</h3>
        <p class="section-copy">先选患者，再完成问诊、病历和审方。</p>
      </div>
      <button class="button-secondary" type="button" @click="workspace.beginSelectedConsultation" :disabled="workspace.startingConsultation">
        <Stethoscope :size="16" />
        <span>{{ workspace.startingConsultation ? '启动中' : '开始接诊' }}</span>
      </button>
    </div>

    <div class="detail-grid two">
      <section class="section subtle-section">
        <div class="section-head">
          <div>
            <h4 class="section-title">待接诊队列</h4>
            <p class="section-copy">点击患者后，会加载病历工作区。</p>
          </div>
        </div>

        <ul class="mini-list overflow-list">
          <li v-for="registration in workspace.queue" :key="registration.id" class="mini-item" :class="{ active: workspace.selectedRegistrationId === registration.id }">
            <button class="candidate-item" type="button" @click="workspace.selectRegistration(registration.id)">
              <div class="mini-item-head">
                <div class="mini-item-title">{{ registration.patientName || '匿名患者' }} / {{ registration.departmentName || '未分科' }}</div>
                <span class="pill" :data-tone="registration.status === 'WAITING' ? 'loading' : 'healthy'">{{ registration.status }}</span>
              </div>
              <div class="mini-item-meta">
                <span>{{ workspace.formatDate(registration.workDate) }} {{ registration.period || '' }}</span>
                <span>号源 #{{ registration.scheduleId }}</span>
                <span>病历 {{ registration.medicalRecordId ?? '未生成' }}</span>
              </div>
              <p class="mini-item-copy">{{ registration.chiefComplaint || '暂无主诉' }}</p>
            </button>
          </li>
        </ul>
      </section>

      <section class="section subtle-section">
        <div class="section-head">
          <div>
            <h4 class="section-title">问诊与分诊建议</h4>
            <p class="section-copy">SSE 流式输出和本地/AI 建议都在这里。</p>
          </div>
          <div class="action-row">
            <button class="button-secondary" type="button" @click="workspace.diagnoseCurrentCase" :disabled="workspace.diagnosingRecord">
              <Search :size="16" />
              <span>{{ workspace.diagnosingRecord ? '生成中' : '生成诊断' }}</span>
            </button>
            <button class="button-secondary" type="button" @click="workspace.generateDraftMedicalRecord" :disabled="workspace.generatingRecord">
              <Search :size="16" />
              <span>{{ workspace.generatingRecord ? '生成中' : '生成病历草稿' }}</span>
            </button>
          </div>
        </div>

        <label class="field">
          <span>问诊文本</span>
          <textarea v-model="workspace.consultationForm.conversationText" class="textarea" placeholder="记录患者主诉、现病史、查体要点等" />
        </label>
        <label class="field">
          <span>诊疗方向</span>
          <input v-model="workspace.consultationForm.diagnosisDirection" placeholder="可不填，系统会用本地规则推断" />
        </label>

        <div v-if="workspace.streamText || workspace.activeStreamSessionId" class="stream-output">
          {{ workspace.streamText || 'SSE streaming...' }}
        </div>

        <div v-if="workspace.diagnosisSuggestion" class="section" style="padding: 0.85rem;">
          <div class="section-head">
            <div>
              <h4 class="section-title">诊断建议</h4>
              <p class="section-copy">{{ workspace.diagnosisSuggestion.summary }}</p>
            </div>
            <span class="pill" data-tone="healthy">{{ workspace.diagnosisSuggestion.adoptionStatus }}</span>
          </div>
          <p class="mini-item-copy">候选诊断：{{ workspace.diagnosisSuggestion.suggestedDiagnoses }}</p>
          <p class="mini-item-copy">建议检查：{{ workspace.diagnosisSuggestion.suggestedExamItems }}</p>
        </div>
      </section>
    </div>

    <div class="detail-grid two">
      <section class="section subtle-section">
        <div class="section-head">
          <div>
            <h4 class="section-title">病历草稿</h4>
            <p class="section-copy">可由 AI 辅助生成，再人工修订保存。</p>
          </div>
          <button class="button-secondary" type="button" @click="workspace.saveCurrentMedicalRecord" :disabled="workspace.savingRecord">
            <Send :size="16" />
            <span>{{ workspace.savingRecord ? '保存中' : '保存病历' }}</span>
          </button>
        </div>

        <div class="field-grid">
          <label class="field"><span>主诉</span><input v-model="workspace.recordForm.chiefComplaint" /></label>
          <label class="field"><span>现病史</span><input v-model="workspace.recordForm.presentIllness" /></label>
          <label class="field"><span>既往史</span><input v-model="workspace.recordForm.pastHistory" /></label>
          <label class="field"><span>查体</span><input v-model="workspace.recordForm.physicalExam" /></label>
        </div>

        <label class="field"><span>初步诊断</span><textarea v-model="workspace.recordForm.preliminaryDiagnosis" class="textarea" /></label>
        <label class="field"><span>治疗计划</span><textarea v-model="workspace.recordForm.treatmentPlan" class="textarea" /></label>
        <label class="field"><span>医生备注</span><textarea v-model="workspace.recordForm.docNote" class="textarea" /></label>
        <label class="field">
          <span class="toolbar">
            <input v-model="workspace.recordForm.aiGenerated" type="checkbox" />
            <span>标记为 AI 辅助生成</span>
          </span>
        </label>
      </section>

      <section class="section subtle-section">
        <div class="section-head">
          <div>
            <h4 class="section-title">处方审方</h4>
            <p class="section-copy">先审方，再提交。</p>
          </div>
          <div class="action-row">
            <button class="button-secondary" type="button" @click="workspace.reviewCurrentPrescription" :disabled="workspace.reviewingPrescription">
              <Search :size="16" />
              <span>{{ workspace.reviewingPrescription ? '审方中' : '审方' }}</span>
            </button>
            <button class="button-secondary" type="button" @click="workspace.submitCurrentPrescription" :disabled="workspace.submittingPrescription">
              <Send :size="16" />
              <span>{{ workspace.submittingPrescription ? '提交中' : '提交处方' }}</span>
            </button>
          </div>
        </div>

        <div class="field-grid">
          <label class="field"><span>药品搜索</span><input v-model="workspace.drugSearch" placeholder="按名称或拼音搜索药品" /></label>
          <div class="action-row" style="align-self: end;">
            <button class="button-secondary" type="button" @click="workspace.loadDrugCatalog">
              <Search :size="16" />
              <span>加载药品</span>
            </button>
            <button class="button-ghost" type="button" @click="workspace.addPrescriptionItem">
              <Plus :size="16" />
              <span>添加项目</span>
            </button>
          </div>
        </div>

        <ul class="mini-list">
          <li v-for="item in workspace.prescriptionItems" :key="item.key" class="mini-item">
            <div class="mini-item-head">
              <div class="mini-item-title">处方项目</div>
              <button class="button-ghost" type="button" @click="workspace.removePrescriptionItem(item.key)">
                <Trash2 :size="16" />
                <span>删除</span>
              </button>
            </div>
            <div class="field-grid">
              <label class="field">
                <span>药品</span>
                <select v-model="item.drugId" @change="workspace.applyDrugDefaults(item)">
                  <option :value="null">请选择药品</option>
                  <option v-for="drug in workspace.availableDrugs" :key="drug.id" :value="drug.id">
                    {{ drug.name }}{{ drug.specification ? ` / ${drug.specification}` : '' }}
                  </option>
                </select>
              </label>
              <label class="field"><span>剂量</span><input v-model="item.dosage" /></label>
              <label class="field"><span>频次</span><input v-model="item.frequency" /></label>
              <label class="field"><span>疗程</span><input v-model="item.duration" /></label>
              <label class="field"><span>数量</span><input v-model="item.quantity" /></label>
            </div>
            <label class="field"><span>用法说明</span><input v-model="item.usageInstruction" /></label>
          </li>
        </ul>

        <label class="field">
          <span>医生确认</span>
          <textarea v-model="workspace.manualConfirmation" class="textarea" placeholder="说明本次审方确认内容" />
        </label>

        <div v-if="workspace.reviewResult" class="section" style="padding: 0.85rem;">
          <div class="section-head">
            <div>
              <h4 class="section-title">审方结果</h4>
              <p class="section-copy">{{ workspace.reviewResult.llmSummary || workspace.reviewResult.llmSuggestion }}</p>
            </div>
            <span class="pill" :data-tone="workspace.reviewResult.riskLevel === 'HIGH' ? 'danger' : workspace.reviewResult.riskLevel === 'MEDIUM' ? 'loading' : 'healthy'">
              {{ workspace.reviewResult.riskLevel || 'UNKNOWN' }}
            </span>
          </div>
          <p class="mini-item-copy">规则命中：{{ workspace.reviewResult.localRuleHits || '无' }}</p>
          <p class="mini-item-copy">上下文缺失：{{ workspace.reviewResult.contextMissingItems || '无' }}</p>
        </div>
      </section>
    </div>
  </section>
</template>
