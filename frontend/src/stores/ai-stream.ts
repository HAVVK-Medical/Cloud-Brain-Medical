import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { createAiStreamSession, cancelAiStreamSession } from '@/api/workflow';
import type { MedicalRecordSummary, DiagnosisSuggestionResponse } from '@/api/workflow';

export const useAiStreamStore = defineStore('ai-stream', () => {
  const sessionId = ref<string | null>(null);
  const streamText = ref('');
  const streaming = ref(false);
  const connected = computed(() => sessionId.value !== null && streaming.value);

  function parseSsePayload<T>(value: string): T | null {
    try { return JSON.parse(value) as T; } catch { return null; }
  }

  async function start(
    taskType: 'MEDICAL_RECORD' | 'DIAGNOSIS',
    registrationId: number,
    conversationText: string,
    diagnosisDirection: string | null,
    onResult: (data: MedicalRecordSummary | DiagnosisSuggestionResponse) => void,
  ) {
    streamText.value = '';
    streaming.value = true;
    const session = await createAiStreamSession({
      taskType,
      registrationId,
      conversationText,
      diagnosisDirection,
    });
    sessionId.value = session.sessionId;

    return new Promise<void>((resolve, reject) => {
      let completed = false;
      const source = new EventSource(
        `/api/ai-stream-sessions/${session.sessionId}/events?token=${encodeURIComponent(session.streamToken)}`,
      );

      const finish = () => {
        completed = true;
        sessionId.value = null;
        streaming.value = false;
        source.close();
        resolve();
      };

      source.addEventListener('chunk', (event) => {
        const payload = parseSsePayload<{ text?: string }>(event.data);
        streamText.value += payload?.text ?? event.data;
      });
      source.addEventListener('result', (event) => {
        const payload = parseSsePayload<any>(event.data);
        if (payload) onResult(payload);
      });
      source.addEventListener('done', finish);
      source.addEventListener('cancelled', finish);
      source.onerror = () => {
        if (!completed) {
          sessionId.value = null;
          streaming.value = false;
          source.close();
          reject(new Error('stream failed'));
        }
      };
    });
  }

  async function cancel() {
    if (sessionId.value) {
      try { await cancelAiStreamSession(sessionId.value); } catch { /* already ended */ }
    }
    sessionId.value = null;
    streaming.value = false;
    streamText.value = '';
  }

  return { sessionId, streamText, streaming, connected, start, cancel };
});
