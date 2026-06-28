<script setup lang="ts">
import { ref, computed } from 'vue';
import { X, ChevronRight, Stethoscope, Brain, ShieldCheck, Loader2, CheckCircle2, AlertCircle, Copy } from 'lucide-vue-next';

export interface WorkflowStep {
  id: string;
  label: string;
  icon: any;
  status: 'idle' | 'running' | 'completed' | 'error';
  content: string;
  errorMessage?: string;
}

const props = defineProps<{
  open: boolean;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'trigger', stepId: string): void;
  (e: 'adopt', stepId: string): void;
}>();

const steps = ref<WorkflowStep[]>([
  { id: 'MEDICAL_RECORD', label: 'AI 生成病历', icon: Stethoscope, status: 'idle', content: '' },
  { id: 'DIAGNOSIS', label: 'AI 诊断建议', icon: Brain, status: 'idle', content: '' },
  { id: 'PRESCRIPTION_REVIEW', label: 'AI 处方审核', icon: ShieldCheck, status: 'idle', content: '' },
]);

const activeStepId = ref<string | null>(null);

const activeStep = computed(() => steps.value.find(s => s.id === activeStepId.value) ?? null);

function selectStep(stepId: string) {
  activeStepId.value = stepId;
}

function triggerStep(stepId: string) {
  const step = steps.value.find(s => s.id === stepId);
  if (!step) return;
  step.status = 'running';
  step.content = '';
  step.errorMessage = '';
  activeStepId.value = stepId;
  emit('trigger', stepId);
}

function updateStepContent(stepId: string, content: string) {
  const step = steps.value.find(s => s.id === stepId);
  if (step) step.content = content;
}

function setStepCompleted(stepId: string) {
  const step = steps.value.find(s => s.id === stepId);
  if (step) step.status = 'completed';
}

function setStepError(stepId: string, message: string) {
  const step = steps.value.find(s => s.id === stepId);
  if (step) {
    step.status = 'error';
    step.errorMessage = message;
  }
}

function adoptStep(stepId: string) {
  emit('adopt', stepId);
}

function statusBadge(status: WorkflowStep['status']) {
  switch (status) {
    case 'running': return { color: 'text-brand bg-brand/10', label: '生成中', icon: Loader2 };
    case 'completed': return { color: 'text-green-600 bg-green-50', label: '已完成', icon: CheckCircle2 };
    case 'error': return { color: 'text-red-600 bg-red-50', label: '失败', icon: AlertCircle };
    default: return { color: 'text-gray-400 bg-gray-100', label: '待触发', icon: null };
  }
}

defineExpose({ steps, activeStepId, updateStepContent, setStepCompleted, setStepError });
</script>

<template>
  <Teleport to="body">
    <!-- Backdrop -->
    <div
      v-if="open"
      class="fixed inset-0 z-40 bg-black/20 transition-opacity"
      @click="emit('close')"
    />

    <!-- Sidebar -->
    <div
      class="fixed top-0 right-0 z-50 h-full w-[380px] bg-white shadow-2xl border-l border-border flex flex-col transition-transform duration-300"
      :class="open ? 'translate-x-0' : 'translate-x-full'"
    >
      <!-- Header -->
      <div class="flex items-center justify-between px-4 py-3 border-b border-border bg-gray-50">
        <div class="flex items-center gap-2">
          <span class="text-base font-semibold">AI 工作台</span>
          <span class="w-2 h-2 rounded-full bg-green-500" title="AI 已连接" />
        </div>
        <button class="p-1 hover:bg-gray-200 rounded cursor-pointer" @click="emit('close')">
          <X :size="18" />
        </button>
      </div>

      <!-- Step list -->
      <div class="px-4 py-3 border-b border-border space-y-1">
        <div
          v-for="step in steps" :key="step.id"
          class="flex items-center gap-3 p-2.5 rounded-lg cursor-pointer transition"
          :class="activeStepId === step.id ? 'bg-brand-soft border border-brand/30' : 'hover:bg-gray-50 border border-transparent'"
          @click="selectStep(step.id)"
        >
          <component :is="step.icon" :size="18" class="flex-shrink-0" :class="activeStepId === step.id ? 'text-brand' : 'text-text-secondary'" />
          <span class="text-sm flex-1" :class="activeStepId === step.id ? 'font-medium text-brand' : ''">{{ step.label }}</span>
          <span v-if="statusBadge(step.status).icon" class="flex items-center gap-1 text-xs px-1.5 py-0.5 rounded" :class="statusBadge(step.status).color">
            <component :is="statusBadge(step.status).icon" :size="12" :class="step.status === 'running' ? 'animate-spin' : ''" />
            {{ statusBadge(step.status).label }}
          </span>
        </div>
      </div>

      <!-- Detail area -->
      <div class="flex-1 overflow-y-auto p-4">
        <div v-if="!activeStep" class="flex items-center justify-center h-full text-sm text-text-secondary">
          选择左侧步骤开始 AI 辅助
        </div>

        <div v-else class="space-y-3">
          <!-- Actions -->
          <div class="flex items-center gap-2">
            <button
              class="btn-primary !py-1.5 !px-3 !text-sm flex-1"
              :disabled="activeStep.status === 'running'"
              @click="triggerStep(activeStep.id)"
            >
              <component :is="activeStep.icon" :size="14" />
              {{ activeStep.status === 'completed' ? '重新生成' : activeStep.status === 'running' ? '生成中...' : '开始生成' }}
            </button>
            <button
              v-if="activeStep.status === 'completed'"
              class="btn-secondary !py-1.5 !px-3 !text-sm"
              @click="adoptStep(activeStep.id)"
            >
              采纳
            </button>
          </div>

          <!-- Error message -->
          <div v-if="activeStep.status === 'error'" class="p-2 bg-red-50 border border-red-200 rounded text-xs text-red-700">
            {{ activeStep.errorMessage || '生成失败' }}
          </div>

          <!-- Streamed content -->
          <div v-if="activeStep.content" class="text-sm text-text-secondary whitespace-pre-wrap leading-relaxed bg-gray-50 rounded-lg p-3 border border-border">
            {{ activeStep.content }}
            <span v-if="activeStep.status === 'running'" class="inline-block w-1.5 h-4 bg-brand animate-pulse ml-0.5 align-middle" />
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
