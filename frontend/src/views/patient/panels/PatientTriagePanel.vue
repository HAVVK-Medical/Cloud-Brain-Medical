<script setup lang="ts">
import { Sparkles, ArrowRight } from 'lucide-vue-next';
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';

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
      <SectionCard title="分诊结果">
        <div class="space-y-3 text-sm">
          <div>
            <span class="text-text-secondary">主诉</span>
            <p class="mt-0.5 font-medium">{{ workspace.triageResult.chiefComplaint }}</p>
          </div>
          <div class="flex items-center gap-2">
            <StatusChip tone="success">{{ workspace.triageResult.recommendedDept }}</StatusChip>
            <StatusChip tone="info">{{ workspace.triageResult.recommendationSource }}</StatusChip>
          </div>
          <p class="text-text-secondary text-xs">{{ workspace.triageResult.reason }}</p>
        </div>
      </SectionCard>

      <SectionCard v-if="workspace.triageResult.recommendedDoctors.length" title="推荐医生">
        <div class="space-y-2">
          <div v-for="doc in workspace.triageResult.recommendedDoctors" :key="doc.id" class="flex items-center justify-between py-2 border-b border-border last:border-b-0">
            <div>
              <p class="text-sm font-medium">{{ doc.name }}</p>
              <p class="text-xs text-text-secondary">{{ doc.title }} · {{ doc.departmentName }}</p>
            </div>
            <button class="btn-secondary !py-1 !px-3 !text-xs" type="button" @click="workspace.chooseDoctor(doc.id)">选择</button>
          </div>
        </div>
      </SectionCard>

      <button class="btn-primary w-full" type="button" @click="$router.push('/patient/registration')">
        去挂号 <ArrowRight :size="16" />
      </button>
    </div>

    <EmptyState v-else icon="search" title="输入症状开始分诊" description="AI 会根据您的症状推荐合适的科室和医生" />
  </div>
</template>
