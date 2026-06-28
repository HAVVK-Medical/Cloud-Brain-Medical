<script setup lang="ts">
import { ref, nextTick, watch } from 'vue';
import { MessageCircle, Send, Loader2, Bot, User, ChevronDown, ChevronUp, ArrowRight } from 'lucide-vue-next';
import { createSession, getMessages, buildStreamUrl } from '@/api/triage-conversation';
import type { ChatMessage } from '@/types/chat';

interface ParsedResult {
  department: string;
  departmentCode: string;
  reason: string;
  urgencyLevel: string;
  suggestedQuestions: string[];
}

const { workspace } = defineProps<{ workspace: any }>();

const expanded = ref(false);
const messages = ref<{ role: 'USER' | 'ASSISTANT' | 'SYSTEM'; content: string }[]>([]);
const inputText = ref('');
const streaming = ref(false);
const sessionId = ref<number | null>(null);
const finalResult = ref<ParsedResult | null>(null);
const error = ref('');
const eventSource = ref<EventSource | null>(null);

function toggle() {
  expanded.value = !expanded.value;
}

function parseResultMarker(text: string): { cleanText: string; result: ParsedResult | null } {
  const marker = /\[TRIAGE_RESULT\]([\s\S]*?)\[\/TRIAGE_RESULT\]/;
  const match = text.match(marker);
  if (!match) return { cleanText: text, result: null };

  const cleanText = text.replace(marker, '').trim();
  try {
    const parsed = JSON.parse(match[1]);
    return {
      cleanText,
      result: {
        department: parsed.department || '',
        departmentCode: parsed.departmentCode || '',
        reason: parsed.reason || '',
        urgencyLevel: parsed.urgencyLevel || 'normal',
        suggestedQuestions: parsed.suggestedQuestions || [],
      },
    };
  } catch {
    return { cleanText, result: null };
  }
}

function scrollToBottom() {
  nextTick(() => {
    const el = document.getElementById('conversation-messages');
    if (el) el.scrollTop = el.scrollHeight;
  });
}

async function startConversation() {
  error.value = '';
  finalResult.value = null;

  if (!inputText.value.trim()) return;

  const userText = inputText.value.trim();
  inputText.value = '';

  try {
    if (!sessionId.value) {
      const session = await createSession(userText);
      sessionId.value = session.id;
      const history = await getMessages(session.id);
      messages.value = history.map((m: ChatMessage) => ({
        role: m.role as 'USER' | 'ASSISTANT',
        content: m.content,
      }));
    } else {
      messages.value.push({ role: 'USER', content: userText });
    }
    scrollToBottom();
  } catch (e: any) {
    error.value = '创建对话失败，请重试';
    return;
  }

  // Start SSE stream
  streaming.value = true;
  const assistantMsg: { role: 'ASSISTANT' | 'SYSTEM'; content: string } = { role: 'ASSISTANT', content: '' };
  messages.value.push(assistantMsg);
  scrollToBottom();

  const url = buildStreamUrl(sessionId.value!, userText);
  const es = new EventSource(url);
  eventSource.value = es;

  es.addEventListener('chunk', (event) => {
    const data = JSON.parse(event.data);
    assistantMsg.content += data.content;
    scrollToBottom();
  });

  es.addEventListener('done', () => {
    streaming.value = false;
    es.close();
    eventSource.value = null;

    // Parse result marker
    const { cleanText, result } = parseResultMarker(assistantMsg.content);
    if (result) {
      finalResult.value = result;
      assistantMsg.content = cleanText;
      messages.value.push({ role: 'SYSTEM', content: '' }); // placeholder for result card
    }
    scrollToBottom();
  });

  es.addEventListener('error', () => {
    streaming.value = false;
    es.close();
    eventSource.value = null;
    if (!assistantMsg.content) {
      assistantMsg.content = 'AI 服务暂时不可用，请使用快速分诊或稍后重试。';
    }
    error.value = assistantMsg.content;
    scrollToBottom();
  });
}

function stopConversation() {
  eventSource.value?.close();
  eventSource.value = null;
  streaming.value = false;
}

function reset() {
  stopConversation();
  sessionId.value = null;
  messages.value = [];
  finalResult.value = null;
  error.value = '';
  inputText.value = '';
  expanded.value = false;
}

function goToRegistration() {
  workspace.selectedDepartmentId = null; // trigger re-select
  workspace.$router?.push('/patient/registration');
}

// Cleanup
watch(expanded, (val) => {
  if (!val) stopConversation();
});
</script>

<template>
  <div class="border-t border-border pt-3">
    <!-- Entry card (collapsed) -->
    <div v-if="!expanded" class="space-y-2">
      <button
        class="w-full flex items-center gap-3 p-3 rounded-lg border border-dashed border-brand/40 bg-brand-soft/50 hover:bg-brand-soft transition cursor-pointer"
        @click="toggle"
      >
        <div class="w-8 h-8 rounded-full bg-brand flex items-center justify-center flex-shrink-0">
          <MessageCircle :size="14" class="text-white" />
        </div>
        <div class="text-left flex-1 min-w-0">
          <p class="text-sm font-medium text-brand">需要更详细的 AI 问诊？</p>
          <p class="text-xs text-text-secondary">AI 会追问症状细节，提供更精准的科室推荐</p>
        </div>
        <ChevronDown :size="16" class="text-text-secondary flex-shrink-0" />
      </button>
      <p v-if="workspace.triageResult" class="text-xs text-text-secondary text-center">
        已有快筛结果，AI 详细问诊可帮助更精确匹配
      </p>
    </div>

    <!-- Expanded conversation -->
    <div v-else class="space-y-3">
      <!-- Header -->
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <Bot :size="16" class="text-brand" />
          <span class="text-sm font-medium">AI 分诊助手</span>
          <span v-if="streaming" class="flex items-center gap-1 text-xs text-brand">
            <Loader2 :size="12" class="animate-spin" />
            正在输入...
          </span>
        </div>
        <button class="text-text-secondary hover:text-text-primary p-1 cursor-pointer" @click="toggle">
          <ChevronUp :size="16" />
        </button>
      </div>

      <!-- Error -->
      <div v-if="error && !streaming" class="text-xs text-red-600 bg-red-50 rounded-lg p-2">
        {{ error }}
        <button class="ml-2 underline cursor-pointer" @click="reset">重新开始</button>
      </div>

      <!-- Messages -->
      <div id="conversation-messages" class="space-y-3 max-h-64 overflow-y-auto">
        <div v-for="(msg, i) in messages" :key="i">
          <!-- User message -->
          <div v-if="msg.role === 'USER'" class="flex justify-end">
            <div class="max-w-[80%] bg-brand text-white rounded-2xl rounded-br-md px-3 py-2 text-sm">
              {{ msg.content }}
            </div>
          </div>

          <!-- Assistant message -->
          <div v-else-if="msg.role === 'ASSISTANT'" class="flex gap-2">
            <div class="w-6 h-6 rounded-full bg-brand/20 flex items-center justify-center flex-shrink-0 mt-1">
              <Bot :size="12" class="text-brand" />
            </div>
            <div class="max-w-[85%] bg-gray-100 rounded-2xl rounded-bl-md px-3 py-2 text-sm">
              {{ msg.content }}
              <span v-if="streaming && i === messages.length - 1" class="inline-block w-1.5 h-4 bg-brand animate-pulse ml-0.5 align-middle" />
            </div>
          </div>

          <!-- System / result card (placeholder) -->
          <div v-else-if="msg.role === 'SYSTEM' && i === messages.length - 1">
            <!-- Result card rendered separately below -->
          </div>
        </div>
      </div>

      <!-- Final result card -->
      <div v-if="finalResult" class="p-3 bg-green-50 rounded-lg border border-green-200 space-y-2">
        <div class="flex items-center gap-2">
          <span class="text-sm">🏥</span>
          <span class="font-semibold text-green-800">{{ finalResult.department }}</span>
          <span v-if="finalResult.urgencyLevel === 'urgent'" class="text-xs bg-red-100 text-red-700 px-1.5 py-0.5 rounded">紧急</span>
        </div>
        <p class="text-xs text-green-700">{{ finalResult.reason }}</p>
        <button class="btn-primary w-full !py-2 !text-sm" @click="goToRegistration">
          去挂号 <ArrowRight :size="14" />
        </button>
      </div>

      <!-- Input area -->
      <div v-if="!finalResult" class="flex gap-2">
        <input
          v-model="inputText"
          class="input-field phone-input flex-1 !py-2 !text-sm"
          placeholder="输入您的症状..."
          :disabled="streaming"
          @keyup.enter="startConversation"
        />
        <button
          v-if="!streaming"
          class="btn-primary !p-2 flex-shrink-0"
          :disabled="!inputText.trim()"
          @click="startConversation"
        >
          <Send :size="16" />
        </button>
        <button
          v-else
          class="btn-secondary !p-2 flex-shrink-0"
          @click="stopConversation"
        >
          <Loader2 :size="16" class="animate-spin" />
        </button>
      </div>
    </div>
  </div>
</template>
