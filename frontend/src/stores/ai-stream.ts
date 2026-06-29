import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import {
  cancelAiStreamSession,
  createAiStreamSession,
  diagnose,
  generateMedicalRecord,
} from '@/api/workflow';
import type {
  AiStreamTaskType,
  DiagnosisSuggestionResponse,
  MedicalRecordSummary,
} from '@/api/workflow';

type AiStreamResult = MedicalRecordSummary | DiagnosisSuggestionResponse;
type AiStreamDelivery = 'sse' | 'post-fallback' | 'cancelled';
type AiStreamResultHandler = (data: AiStreamResult, delivery: Exclude<AiStreamDelivery, 'cancelled'>) => void;

export const useAiStreamStore = defineStore('ai-stream', () => {
  const sessionId = ref<string | null>(null);
  const streamText = ref('');
  const thinkingText = ref('');
  const streaming = ref(false);
  const connected = computed(() => sessionId.value !== null && streaming.value);
  let currentSource: EventSource | null = null;
  let cancelRequested = false;
  let activeRunId = 0;
  let resolveCurrentStream: ((delivery: AiStreamDelivery) => void) | null = null;

  function parseSsePayload<T>(value: string): T | null {
    try { return JSON.parse(value) as T; } catch { return null; }
  }

  function supportsEventSource() {
    return typeof window !== 'undefined' && typeof window.EventSource !== 'undefined';
  }

  function resultToText(result: AiStreamResult) {
    if ('chiefComplaint' in result) {
      return [
        `chiefComplaint: ${result.chiefComplaint ?? ''}`,
        `presentIllness: ${result.presentIllness ?? ''}`,
        `pastHistory: ${result.pastHistory ?? ''}`,
        `physicalExam: ${result.physicalExam ?? ''}`,
        `preliminaryDiagnosis: ${result.preliminaryDiagnosis ?? ''}`,
        `treatmentPlan: ${result.treatmentPlan ?? ''}`,
        `docNote: ${result.docNote ?? ''}`,
      ].join('\n');
    }
    return [
      `suggestedDiagnoses: ${result.suggestedDiagnoses ?? ''}`,
      `suggestedExamItems: ${result.suggestedExamItems ?? ''}`,
      `summary: ${result.summary ?? ''}`,
    ].join('\n');
  }

  async function requestPlainPost(
    taskType: AiStreamTaskType,
    registrationId: number,
    conversationText: string,
    diagnosisDirection: string | null,
  ) {
    if (taskType === 'MEDICAL_RECORD') {
      return generateMedicalRecord({
        registrationId,
        conversationText,
        diagnosisDirection,
      });
    }
    return diagnose({
      registrationId,
      conversationText,
      diagnosisDirection,
    });
  }

  async function runPlainPostFallback(
    runId: number,
    taskType: AiStreamTaskType,
    registrationId: number,
    conversationText: string,
    diagnosisDirection: string | null,
    onResult: AiStreamResultHandler,
  ): Promise<AiStreamDelivery> {
    const result = await requestPlainPost(taskType, registrationId, conversationText, diagnosisDirection);
    if (activeRunId !== runId || cancelRequested) {
      return 'cancelled';
    }
    streamText.value = resultToText(result);
    onResult(result, 'post-fallback');
    return 'post-fallback';
  }

  function subscribeToEvents(
    runId: number,
    streamSessionId: string,
    streamToken: string,
    onResult: AiStreamResultHandler,
  ) {
    return new Promise<AiStreamDelivery>((resolve, reject) => {
      let completed = false;
      let resultReceived = false;
      let source: EventSource;

      const finish = (delivery: AiStreamDelivery) => {
        if (completed) {
          return;
        }
        completed = true;
        if (activeRunId === runId) {
          sessionId.value = null;
          currentSource = null;
          resolveCurrentStream = null;
        }
        source.close();
        resolve(delivery);
      };

      try {
        source = new EventSource(
          `/api/ai-stream-sessions/${streamSessionId}/events?token=${encodeURIComponent(streamToken)}`,
        );
      } catch (error) {
        reject(error);
        return;
      }

      currentSource = source;
      resolveCurrentStream = finish;

      source.addEventListener('thinking', (event) => {
        if (activeRunId !== runId || cancelRequested) {
          return;
        }
        const payload = parseSsePayload<{ text?: string }>(event.data);
        thinkingText.value += payload?.text ?? event.data;
      });
      source.addEventListener('chunk', (event) => {
        if (activeRunId !== runId || cancelRequested) {
          return;
        }
        const payload = parseSsePayload<{ text?: string }>(event.data);
        streamText.value += payload?.text ?? event.data;
      });
      source.addEventListener('result', (event) => {
        if (activeRunId !== runId || cancelRequested) {
          return;
        }
        const payload = parseSsePayload<AiStreamResult>(event.data);
        if (payload) {
          resultReceived = true;
          onResult(payload, 'sse');
        }
      });
      source.addEventListener('done', () => finish('sse'));
      source.addEventListener('cancelled', () => finish('cancelled'));
      source.onerror = () => {
        if (completed) {
          return;
        }
        if (resultReceived) {
          finish('sse');
          return;
        }
        if (activeRunId !== runId || cancelRequested) {
          finish('cancelled');
          return;
        }
        sessionId.value = null;
        currentSource = null;
        resolveCurrentStream = null;
        source.close();
        reject(new Error('stream failed'));
      };
    });
  }

  async function start(
    taskType: AiStreamTaskType,
    registrationId: number,
    conversationText: string,
    diagnosisDirection: string | null,
    onResult: AiStreamResultHandler,
  ) {
    if (streaming.value) {
      await cancel();
    }

    streamText.value = '';
    thinkingText.value = '';
    streaming.value = true;
    cancelRequested = false;
    const runId = ++activeRunId;

    try {
      if (!supportsEventSource()) {
        return await runPlainPostFallback(runId, taskType, registrationId, conversationText, diagnosisDirection, onResult);
      }

      let session;
      try {
        session = await createAiStreamSession({
          taskType,
          registrationId,
          conversationText,
          diagnosisDirection,
        });
      } catch {
        return await runPlainPostFallback(runId, taskType, registrationId, conversationText, diagnosisDirection, onResult);
      }

      sessionId.value = session.sessionId;
      try {
        const delivery = await subscribeToEvents(runId, session.sessionId, session.streamToken, onResult);
        return delivery;
      } catch {
        if (activeRunId !== runId || cancelRequested) {
          return 'cancelled';
        }
        try {
          await cancelAiStreamSession(session.sessionId);
        } catch {
          // The stream may already be gone on the backend.
        }
        return await runPlainPostFallback(runId, taskType, registrationId, conversationText, diagnosisDirection, onResult);
      }
    } finally {
      if (activeRunId === runId) {
        currentSource?.close();
        currentSource = null;
        resolveCurrentStream = null;
        sessionId.value = null;
        streaming.value = false;
        cancelRequested = false;
      }
    }
  }

  async function cancel() {
    const activeSessionId = sessionId.value;
    cancelRequested = true;
    activeRunId += 1;
    currentSource?.close();
    resolveCurrentStream?.('cancelled');
    if (activeSessionId) {
      try { await cancelAiStreamSession(activeSessionId); } catch { /* already ended */ }
    }
    currentSource = null;
    resolveCurrentStream = null;
    sessionId.value = null;
    streaming.value = false;
    streamText.value = '';
  }

  return { sessionId, streamText, thinkingText, streaming, connected, start, cancel };
});
