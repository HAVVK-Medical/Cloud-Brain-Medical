<script setup lang="ts">
import { ref, nextTick, watch } from 'vue';
import { useRouter } from 'vue-router';
import { MessageCircle, Send, Loader2, Bot, ChevronDown, ChevronUp, ArrowRight } from 'lucide-vue-next';
import { createSession, getMessages, buildStreamUrl } from '@/api/triage-conversation';
import { triageConsult } from '@/api/workflow';
import type { ChatMessage } from '@/types/chat';

interface ParsedResult {
  department: string;
  departmentCode: string;
  reason: string;
  urgencyLevel: string;
  suggestedQuestions: string[];
}

type ConversationMessage = { role: 'USER' | 'ASSISTANT' | 'SYSTEM'; content: string; thinkingContent?: string };

const { workspace } = defineProps<{ workspace: any }>();
const router = useRouter();

const expanded = ref(false);
const messages = ref<ConversationMessage[]>([]);
const inputText = ref('');
const streaming = ref(false);
const confirming = ref(false);
const sessionId = ref<number | null>(null);
const finalResult = ref<ParsedResult | null>(null);
const thinkingExpanded = ref(false);
const error = ref('');
const eventSource = ref<EventSource | null>(null);

function toggle() {
  expanded.value = !expanded.value;
}

function parseSuggestedQuestions(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value.map((item) => String(item)).filter(Boolean);
  }
  if (typeof value === 'string' && value.trim()) {
    return [value.trim()];
  }
  return [];
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function renderSimpleMarkdown(text: string): string {
  if (!text) return '';
  return escapeHtml(text)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\n\n/g, '</p><p>')
    .replace(/\n/g, '<br>');
}

function renderConversationText(text: string): string {
  const { cleanText } = parseResultMarker(text);
  return renderSimpleMarkdown(cleanText);
}

function parseChunkData(value: string): string {
  try {
    const parsed = JSON.parse(value) as { content?: string };
    if (typeof parsed.content === 'string') {
      return parsed.content;
    }
  } catch {
    // fallback to raw text
  }
  return value;
}

function decodeJsonString(value: string): string {
  try {
    return JSON.parse(`"${value}"`) as string;
  } catch {
    return value.replace(/\\"/g, '"').replace(/\\n/g, '\n');
  }
}

function extractStringField(raw: string, fieldNames: string[]): string {
  for (const fieldName of fieldNames) {
    const match = raw.match(new RegExp(`"${fieldName}"\\s*:\\s*"((?:\\\\.|[^"\\\\])*)"`, 'u'));
    if (match?.[1]) {
      return decodeJsonString(match[1]);
    }
  }
  return '';
}

function extractStringArrayField(raw: string, fieldNames: string[]): string[] {
  for (const fieldName of fieldNames) {
    const match = raw.match(new RegExp(`"${fieldName}"\\s*:\\s*\\[([\\s\\S]*?)\\]`, 'u'));
    if (!match?.[1]) {
      continue;
    }
    return Array.from(match[1].matchAll(/"((?:\\.|[^"\\])*)"/gu))
      .map((item) => decodeJsonString(item[1]))
      .filter(Boolean);
  }
  const textValue = extractStringField(raw, fieldNames);
  return textValue ? [textValue] : [];
}

function parseResultMarkerFallback(raw: string): ParsedResult | null {
  const department = extractStringField(raw, ['department', 'recommendedDepartment']);
  const reason = extractStringField(raw, ['reason']);
  if (!department && !reason) {
    return null;
  }
  return {
    department,
    departmentCode: extractStringField(raw, ['departmentCode', 'recommendedDepartmentCode']),
    reason,
    urgencyLevel: extractStringField(raw, ['urgencyLevel']) || 'normal',
    suggestedQuestions: extractStringArrayField(raw, ['suggestedQuestions']),
  };
}

async function fallbackToQuickTriage() {
  const chiefComplaint = messages.value
    .filter((msg) => msg.role === 'USER')
    .map((msg) => msg.content)
    .join('\n')
    .trim();
  if (!chiefComplaint) {
    return;
  }
  try {
    const result = await triageConsult({ chiefComplaint });
    finalResult.value = {
      department: result.recommendedDept,
      departmentCode: '',
      reason: result.reason,
      urgencyLevel: 'normal',
      suggestedQuestions: [],
    };
    if (!messages.value.some((msg) => msg.role === 'ASSISTANT' && msg.content.includes(result.recommendedDept))) {
      messages.value.push({
        role: 'ASSISTANT',
        content: `快速分诊已完成，建议前往 ${result.recommendedDept}。`,
      });
    }
    error.value = 'AI 服务暂时不可用，已切换为快速分诊。';
  } catch {
    error.value = 'AI 服务暂时不可用，快速分诊也未能完成，请稍后重试或手动选择科室挂号。';
  }
}

function parseResultMarker(text: string): { cleanText: string; result: ParsedResult | null } {
  const marker = /\[TRIAGE_RESULT\]([\s\S]*?)\[\/TRIAGE_RESULT\]/;
  const match = text.match(marker);
  if (!match) {
    return {
      cleanText: text.replace(/\[TRIAGE_RESULT\][\s\S]*$/u, '').trim(),
      result: null,
    };
  }

  const cleanText = text.replace(marker, '').trim();
  const rawResult = match[1].trim();
  try {
    const parsed = JSON.parse(rawResult) as Record<string, unknown>;
    return {
      cleanText,
      result: {
        department: String(parsed.department || parsed.recommendedDepartment || ''),
        departmentCode: String(parsed.departmentCode || parsed.recommendedDepartmentCode || ''),
        reason: String(parsed.reason || ''),
        urgencyLevel: String(parsed.urgencyLevel || 'normal'),
        suggestedQuestions: parseSuggestedQuestions(parsed.suggestedQuestions),
      },
    };
  } catch {
    return { cleanText, result: parseResultMarkerFallback(rawResult) };
  }
}

function applyAssistantContent(message: ConversationMessage, rawContent: string) {
  const { cleanText, result } = parseResultMarker(rawContent);
  message.content = cleanText;
  if (result) {
    finalResult.value = result;
  }
}

function normalizeHistory(history: ChatMessage[]) {
  const normalized: ConversationMessage[] = [];
  for (const item of history) {
    const role = item.role as 'USER' | 'ASSISTANT';
    if (role !== 'ASSISTANT') {
      normalized.push({ role, content: item.content });
      continue;
    }
    const message: ConversationMessage = { role: 'ASSISTANT', content: item.content };
    applyAssistantContent(message, item.content);
    if (message.content || !finalResult.value) {
      normalized.push(message);
    }
  }
  return normalized;
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
      messages.value = normalizeHistory(history);
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
  const assistantMsg: ConversationMessage = { role: 'ASSISTANT', content: '' };
  let rawAssistantContent = '';
  messages.value.push(assistantMsg);
  scrollToBottom();

  const url = buildStreamUrl(sessionId.value!, userText);
  let es: EventSource;
  try {
    es = new EventSource(url);
  } catch {
    streaming.value = false;
    assistantMsg.content = 'AI 服务暂时不可用，请使用快速分诊或稍后重试。';
    error.value = assistantMsg.content;
    await fallbackToQuickTriage();
    scrollToBottom();
    return;
  }
  eventSource.value = es;

  es.addEventListener('thinking', (event) => {
    assistantMsg.thinkingContent = (assistantMsg.thinkingContent || '') + parseChunkData(event.data);
  });

  es.addEventListener('chunk', (event) => {
    rawAssistantContent += parseChunkData(event.data);
    applyAssistantContent(assistantMsg, rawAssistantContent);
    scrollToBottom();
  });

  es.addEventListener('done', () => {
    streaming.value = false;
    es.close();
    eventSource.value = null;

    applyAssistantContent(assistantMsg, rawAssistantContent || assistantMsg.content);
    scrollToBottom();
  });

  es.addEventListener('error', () => {
    streaming.value = false;
    es.close();
    eventSource.value = null;
    applyAssistantContent(assistantMsg, rawAssistantContent || assistantMsg.content);
    if (finalResult.value) {
      error.value = '';
      scrollToBottom();
      return;
    }
    if (!assistantMsg.content) {
      assistantMsg.content = 'AI 服务暂时不可用，请使用快速分诊或稍后重试。';
    }
    error.value = assistantMsg.content;
    void fallbackToQuickTriage().finally(scrollToBottom);
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

async function goToRegistration() {
  if (!finalResult.value) return;

  confirming.value = true;
  error.value = '';
  try {
    const chiefComplaint = messages.value
      .filter((msg) => msg.role === 'USER')
      .map((msg) => msg.content)
      .join('\n')
      .trim();
    await workspace.confirmConversationTriageResult({
      chiefComplaint: chiefComplaint || finalResult.value.reason || finalResult.value.department,
      department: finalResult.value.department,
      departmentCode: finalResult.value.departmentCode,
      reason: finalResult.value.reason,
      urgencyLevel: finalResult.value.urgencyLevel,
    });
    await router.push('/patient/registration');
  } catch {
    error.value = error.value || '对话分诊确认失败，请稍后重试。';
  } finally {
    confirming.value = false;
  }
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
          <div v-else-if="msg.role === 'ASSISTANT' && msg.content" class="flex gap-2">
            <div class="w-6 h-6 rounded-full bg-brand/20 flex items-center justify-center flex-shrink-0 mt-1">
              <Bot :size="12" class="text-brand" />
            </div>
            <div class="max-w-[85%] bg-gray-100 rounded-2xl rounded-bl-md px-3 py-2 text-sm whitespace-pre-wrap">
              <div v-if="msg.thinkingContent" class="mb-2">
                <button class="flex items-center gap-1 text-xs text-text-secondary hover:text-brand cursor-pointer" @click="thinkingExpanded = !thinkingExpanded">
                  <span>💭 AI 思考过程</span>
                  <ChevronDown v-if="!thinkingExpanded" :size="12" />
                  <ChevronUp v-else :size="12" />
                </button>
                <div v-if="thinkingExpanded" class="mt-1 p-2 bg-black/5 rounded text-xs text-text-secondary italic max-h-32 overflow-y-auto whitespace-pre-wrap">
                  {{ msg.thinkingContent }}
                </div>
              </div>
              <span v-html="renderConversationText(msg.content)" />
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
        <p class="text-xs text-green-700" v-html="renderSimpleMarkdown(finalResult.reason)" />
        <div v-if="finalResult.suggestedQuestions.length" class="space-y-1">
          <p class="text-xs font-medium text-green-800">可以继续咨询</p>
          <p v-for="question in finalResult.suggestedQuestions" :key="question" class="text-xs text-green-700">
            <span v-html="renderSimpleMarkdown(question)" />
          </p>
        </div>
        <button class="btn-primary w-full !py-2 !text-sm" :disabled="confirming" @click="goToRegistration">
          <Loader2 v-if="confirming" :size="14" class="animate-spin" />
          <span>{{ confirming ? '正在生成挂号推荐' : '去挂号' }}</span>
          <ArrowRight v-if="!confirming" :size="14" />
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
