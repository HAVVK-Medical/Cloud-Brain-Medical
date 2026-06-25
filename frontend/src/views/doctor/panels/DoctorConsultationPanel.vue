<script setup lang="ts">
import { Plus, Search, Send, Sparkles, Stethoscope, Trash2 } from 'lucide-vue-next';

const { workspace } = defineProps<{ workspace: any }>();

// --- C3: Diagnosis helpers ---
interface DiagnosisEntry { name: string; confidence: number }

function parseDiagnoses(text: string): DiagnosisEntry[] {
  if (!text) return [];
  return text.split('\n').filter(Boolean).map(line => {
    const match = line.match(/^(.+?)\s*(\d{1,3})%?\s*$/);
    if (match) return { name: match[1].trim(), confidence: parseInt(match[2]) };
    return { name: line.replace(/^[-*\d.]+\s*/, '').trim(), confidence: 0 };
  });
}

function parseExamItems(text: string): string[] {
  if (!text) return [];
  return text.split('\n').filter(Boolean)
    .map(line => line.replace(/^[-*\d.]+\s*/, '').trim());
}

function confidenceBg(conf: number): string {
  if (conf >= 75) return '#d4edda';
  if (conf >= 50) return '#fff3cd';
  return '#f8f0ff';
}

function confidenceFg(conf: number): string {
  if (conf >= 75) return '#155724';
  if (conf >= 50) return '#856404';
  return '#6f42c1';
}

function adoptDiagnosis(name: string) {
  workspace.recordForm.preliminaryDiagnosis = name;
}

// --- C4: Prescription review helpers ---
interface RuleHitEntry {
  ruleName?: string;
  alertMessage?: string;
  suggestion?: string;
  riskLevel?: string;
}

function parseRuleHits(raw: string | Record<string, unknown>[]): RuleHitEntry[] {
  if (Array.isArray(raw)) return raw as RuleHitEntry[];
  if (typeof raw === 'string') {
    try { return JSON.parse(raw) as RuleHitEntry[]; }
    catch { return []; }
  }
  return [];
}

function riskLabel(level: string | null | undefined): string {
  switch ((level || '').toUpperCase()) {
    case 'HIGH': case 'DANGER': case 'CRITICAL': return '🔴 高风险';
    case 'MEDIUM': case 'WARNING': return '🟡 中风险';
    default: return '🟢 低风险';
  }
}

function riskBarClass(level: string | null | undefined): string {
  switch ((level || '').toUpperCase()) {
    case 'HIGH': case 'DANGER': case 'CRITICAL': return 'risk--high';
    case 'MEDIUM': case 'WARNING': return 'risk--medium';
    default: return 'risk--low';
  }
}
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
              <Sparkles :size="16" />
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

        <div v-if="workspace.diagnosisSuggestion" class="diagnosis-suggestions">
          <h4 class="suggestion-title">🤖 AI 诊断建议</h4>
          <div v-if="workspace.diagnosisSuggestion.suggestedDiagnoses" class="diagnosis-tags">
            <span
              v-for="(diag, idx) in parseDiagnoses(workspace.diagnosisSuggestion.suggestedDiagnoses)"
              :key="idx"
              class="diagnosis-tag"
              :style="{ background: confidenceBg(diag.confidence), color: confidenceFg(diag.confidence) }"
            >
              {{ diag.name }}
              <span v-if="diag.confidence" class="diagnosis-tag__confidence">{{ diag.confidence }}%</span>
              <button class="diagnosis-tag__adopt" @click="adoptDiagnosis(diag.name)">采纳</button>
            </span>
          </div>
          <div v-if="workspace.diagnosisSuggestion.suggestedExamItems" class="exam-checklist">
            <h5>建议检查项目</h5>
            <label v-for="(exam, idx) in parseExamItems(workspace.diagnosisSuggestion.suggestedExamItems)"
                   :key="idx" class="exam-item">
              <input type="checkbox" />
              <span>{{ exam }}</span>
            </label>
          </div>
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
          <div class="record-field" :class="{ 'ai-field--streaming': workspace.generatingRecord }">
            <label class="field">
              <span>
                主诉
                <span v-if="workspace.generatingRecord" class="streaming-cursor">▌</span>
                <span v-else-if="workspace.recordForm.aiGenerated" class="ai-badge-inline">AI 生成</span>
              </span>
              <input v-model="workspace.recordForm.chiefComplaint" />
            </label>
          </div>
          <div class="record-field" :class="{ 'ai-field--streaming': workspace.generatingRecord }">
            <label class="field">
              <span>
                现病史
                <span v-if="workspace.generatingRecord" class="streaming-cursor">▌</span>
                <span v-else-if="workspace.recordForm.aiGenerated" class="ai-badge-inline">AI 生成</span>
              </span>
              <input v-model="workspace.recordForm.presentIllness" />
            </label>
          </div>
          <div class="record-field" :class="{ 'ai-field--streaming': workspace.generatingRecord }">
            <label class="field">
              <span>
                既往史
                <span v-if="workspace.generatingRecord" class="streaming-cursor">▌</span>
                <span v-else-if="workspace.recordForm.aiGenerated" class="ai-badge-inline">AI 生成</span>
              </span>
              <input v-model="workspace.recordForm.pastHistory" />
            </label>
          </div>
          <div class="record-field" :class="{ 'ai-field--streaming': workspace.generatingRecord }">
            <label class="field">
              <span>
                查体
                <span v-if="workspace.generatingRecord" class="streaming-cursor">▌</span>
                <span v-else-if="workspace.recordForm.aiGenerated" class="ai-badge-inline">AI 生成</span>
              </span>
              <input v-model="workspace.recordForm.physicalExam" />
            </label>
          </div>
        </div>

        <div class="record-field" :class="{ 'ai-field--streaming': workspace.generatingRecord }">
          <label class="field">
            <span>
              初步诊断
              <span v-if="workspace.generatingRecord" class="streaming-cursor">▌</span>
              <span v-else-if="workspace.recordForm.aiGenerated" class="ai-badge-inline">AI 生成</span>
            </span>
            <textarea v-model="workspace.recordForm.preliminaryDiagnosis" class="textarea" />
          </label>
        </div>
        <div class="record-field" :class="{ 'ai-field--streaming': workspace.generatingRecord }">
          <label class="field">
            <span>
              治疗计划
              <span v-if="workspace.generatingRecord" class="streaming-cursor">▌</span>
              <span v-else-if="workspace.recordForm.aiGenerated" class="ai-badge-inline">AI 生成</span>
            </span>
            <textarea v-model="workspace.recordForm.treatmentPlan" class="textarea" />
          </label>
        </div>
        <div class="record-field" :class="{ 'ai-field--streaming': workspace.generatingRecord }">
          <label class="field">
            <span>
              医生备注
              <span v-if="workspace.generatingRecord" class="streaming-cursor">▌</span>
              <span v-else-if="workspace.recordForm.aiGenerated" class="ai-badge-inline">AI 生成</span>
            </span>
            <textarea v-model="workspace.recordForm.docNote" class="textarea" />
          </label>
        </div>
        <label class="field">
          <span class="toolbar">
            <input v-model="workspace.recordForm.aiGenerated" type="checkbox" />
            <span>标记为 AI 辅助生成</span>
          </span>
        </label>

        <div class="record-ai-actions">
          <button class="btn btn--ai" @click="workspace.generateDraftMedicalRecord"
                  :disabled="workspace.generatingRecord">
            <Sparkles :size="14" />
            <span>{{ workspace.generatingRecord ? 'AI 生成中...' : 'AI 生成病历' }}</span>
          </button>
          <button class="btn btn--ghost" @click="workspace.saveCurrentMedicalRecord"
                  :disabled="!workspace.recordForm.chiefComplaint">
            采纳全部
          </button>
          <button class="btn btn--ghost" @click="workspace.generateDraftMedicalRecord">
            重新生成
          </button>
        </div>
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

        <div v-if="workspace.reviewResult" class="review-result">
          <div class="review-risk-bar" :class="riskBarClass(workspace.reviewResult.riskLevel)">
            <span class="review-risk-bar__label">风险等级: {{ workspace.reviewResult.riskLevel || '未知' }}</span>
          </div>

          <div v-if="workspace.reviewResult.localRuleHits" class="review-section">
            <h4 class="review-section__title">📋 本地规则引擎</h4>
            <div v-for="(hit, idx) in parseRuleHits(workspace.reviewResult.localRuleHits)"
                 :key="idx" class="rule-hit">
              <span class="rule-hit__risk" :data-level="hit.riskLevel">
                {{ riskLabel(hit.riskLevel) }}
              </span>
              <div class="rule-hit__body">
                <strong>{{ hit.ruleName || hit.alertMessage }}</strong>
                <p v-if="hit.alertMessage">{{ hit.alertMessage }}</p>
                <p v-if="hit.suggestion" class="rule-hit__suggestion">💡 {{ hit.suggestion }}</p>
              </div>
            </div>
          </div>

          <div v-if="workspace.reviewResult.llmSummary" class="review-section review-section--llm">
            <h4 class="review-section__title">
              🤖 AI 分析补充
              <span class="ai-badge">AI 生成</span>
            </h4>
            <p class="llm-summary">{{ workspace.reviewResult.llmSummary }}</p>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
/* C2: Streaming and AI styles */
.ai-field--streaming {
  border-left: 3px solid var(--primary);
  padding-left: 10px;
  background: var(--primary-soft);
  transition: border-color .3s;
}
.ai-badge-inline {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 8px;
  background: var(--primary-soft);
  color: var(--primary);
  margin-left: 8px;
  font-weight: 500;
}
.streaming-cursor {
  animation: blink 1s step-end infinite;
  color: var(--primary);
  margin-left: 4px;
}
@keyframes blink { 50% { opacity: 0; } }
.record-ai-actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
  flex-wrap: wrap;
}
.btn--ai {
  background: var(--primary-soft);
  color: var(--primary);
  border: 1px solid var(--primary);
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.btn--ai:disabled { opacity: .5; cursor: not-allowed; }
.btn--ghost {
  background: transparent;
  color: var(--muted);
  border: 1px solid var(--border);
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
}
.btn--ghost:disabled { opacity: .4; cursor: not-allowed; }

/* C3: Diagnosis styles */
.diagnosis-suggestions {
  margin-top: 20px;
  padding: 16px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  border-left: 4px solid var(--primary);
}
.suggestion-title { margin: 0 0 12px; font-size: 14px; }
.diagnosis-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.diagnosis-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}
.diagnosis-tag__confidence {
  font-size: 11px;
  opacity: .7;
}
.diagnosis-tag__adopt {
  border: none;
  background: rgba(0,0,0,.08);
  border-radius: 4px;
  padding: 1px 6px;
  font-size: 11px;
  cursor: pointer;
}
.exam-checklist {
  margin-top: 14px;
}
.exam-checklist h5 { margin: 0 0 8px; font-size: 13px; color: var(--muted); }
.exam-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
}

/* C4: Review styles */
.review-result { margin-top: 20px; }
.review-risk-bar {
  padding: 10px 16px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 14px;
}
.risk--high { background: #f8d7da; color: #721c24; border-left: 4px solid var(--danger); }
.risk--medium { background: #fff3cd; color: #856404; border-left: 4px solid var(--accent); }
.risk--low { background: #d4edda; color: #155724; border-left: 4px solid var(--success); }
.review-section {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 12px;
}
.review-section--llm {
  border-left: 4px solid var(--primary);
  background: var(--primary-soft);
}
.review-section__title { margin: 0 0 10px; font-size: 14px; }
.rule-hit {
  display: flex;
  gap: 12px;
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 8px;
  background: var(--bg);
}
.rule-hit__risk {
  flex-shrink: 0;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
}
.rule-hit__risk[data-level="HIGH"],
.rule-hit__risk[data-level="DANGER"],
.rule-hit__risk[data-level="CRITICAL"] { background: #f8d7da; color: #721c24; }
.rule-hit__risk[data-level="MEDIUM"],
.rule-hit__risk[data-level="WARNING"] { background: #fff3cd; color: #856404; }
.rule-hit__risk[data-level="LOW"],
.rule-hit__risk[data-level="INFO"] { background: #d4edda; color: #155724; }
.rule-hit__body p { margin: 4px 0 0; font-size: 13px; color: var(--muted); }
.rule-hit__suggestion { color: var(--primary) !important; }
.llm-summary { font-size: 14px; line-height: 1.6; margin: 0; }

/* AI badge for review section */
.ai-badge {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 8px;
  background: var(--primary-soft);
  color: var(--primary);
  margin-left: 8px;
  font-weight: 500;
}
</style>
