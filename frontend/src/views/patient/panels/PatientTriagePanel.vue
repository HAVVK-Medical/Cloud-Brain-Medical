<script setup lang="ts">
import { Sparkles, ArrowRight } from 'lucide-vue-next';
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';
import ConversationalTriageSection from './ConversationalTriageSection.vue';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <div class="p-4 space-y-4">
    <div class="space-y-3">
      <textarea
        v-model="workspace.triageForm.chiefComplaint"
        class="input-field phone-input h-28 resize-none"
        placeholder="请描述您的不适症状，例如：头痛、发热、咳嗽等..."
      />
      <button class="btn-primary w-full" type="button" @click="workspace.runTriage()" :disabled="workspace.triaging || !workspace.triageForm.chiefComplaint.trim()">
        <Sparkles :size="16" />
        <span>{{ workspace.triaging ? '分析中...' : '智能分诊' }}</span>
      </button>
    </div>

    <div v-if="workspace.triageResult" class="space-y-4">
      <!-- AI / Local badge -->
      <div class="flex items-center gap-2 flex-wrap">
        <span v-if="workspace.triageResult.degraded" class="degraded-badge">本地规则匹配</span>
        <span v-else class="ai-badge">AI 推荐</span>
        <span v-if="workspace.triageResult.aiRecommendedDept && workspace.triageResult.aiRecommendedDept !== workspace.triageResult.recommendedDept" class="text-xs text-warning">
          (AI 建议: {{ workspace.triageResult.aiRecommendedDept }}, 已采纳)
        </span>
      </div>

      <!-- Degraded notice -->
      <div v-if="workspace.triageResult.degraded" class="ai-degraded-notice">
        ⚠️ AI 服务不可用，当前使用本地规则引擎匹配，建议结合人工判断。
      </div>

      <SectionCard title="推荐科室">
        <div class="flex items-center gap-3">
          <span class="text-2xl">🏥</span>
          <div>
            <p class="text-lg font-semibold text-brand">{{ workspace.triageResult.recommendedDept }}</p>
            <p class="text-xs text-text-secondary">{{ workspace.triageResult.degraded ? '本地规则根据关键词匹配' : 'AI 根据您的症状智能匹配' }}</p>
          </div>
        </div>
      </SectionCard>

      <SectionCard v-if="workspace.triageResult.recommendedDoctors.length" title="推荐医生">
        <div class="space-y-2">
          <div v-for="doc in workspace.triageResult.recommendedDoctors" :key="doc.id"
               class="flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition hover:border-brand"
               :class="workspace.selectedDoctor?.id === doc.id ? 'border-brand bg-brand-soft' : 'border-border'"
               @click="workspace.chooseDoctor(doc.id)">
            <div class="w-10 h-10 rounded-full bg-brand flex items-center justify-center text-white font-bold text-sm flex-shrink-0">
              {{ doc.name[0] }}
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium">{{ doc.name }}</p>
              <p class="text-xs text-text-secondary">{{ doc.title }} · {{ doc.specialty }}</p>
            </div>
            <button class="btn-secondary !py-1 !px-3 !text-xs" type="button" @click.stop="workspace.chooseDoctor(doc.id)">
              {{ workspace.selectedDoctor?.id === doc.id ? '已选' : '选择' }}
            </button>
          </div>
        </div>
      </SectionCard>

      <!-- AI Reasoning (collapsible) -->
      <details v-if="workspace.triageResult.reason" class="text-sm cursor-pointer">
        <summary class="text-brand font-medium">查看 AI 分析依据 ▼</summary>
        <p class="mt-2 p-3 bg-gray-50 rounded-lg text-text-secondary text-xs border-l-2 border-brand">{{ workspace.triageResult.reason }}</p>
      </details>

      <button class="btn-primary w-full" type="button" @click="$router.push('/patient/registration')">
        去挂号 <ArrowRight :size="16" />
      </button>
    </div>

    <EmptyState v-else icon="search" title="输入症状开始分诊" description="AI 会根据您的症状推荐合适的科室和医生" />

    <!-- Conversational AI Triage -->
    <ConversationalTriageSection :workspace="workspace" />
  </div>
</template>
