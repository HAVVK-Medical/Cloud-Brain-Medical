import { createPinia, setActivePinia } from 'pinia';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { installBrowserMocks, removeBrowserMocks } from '@/test-utils/browser';
import type { DiagnosisSuggestionResponse, MedicalRecordSummary } from '@/api/workflow';
import { useAiStreamStore } from './ai-stream';

vi.mock('@/api/workflow', () => ({
  cancelAiStreamSession: vi.fn(),
  createAiStreamSession: vi.fn(),
  diagnose: vi.fn(),
  generateMedicalRecord: vi.fn(),
}));

const workflowApi = await import('@/api/workflow');

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  readonly listeners = new Map<string, Array<(event: MessageEvent<string>) => void>>();
  closed = false;
  onerror: (() => void) | null = null;

  constructor(readonly url: string) {
    FakeEventSource.instances.push(this);
  }

  addEventListener(type: string, handler: (event: MessageEvent<string>) => void) {
    const handlers = this.listeners.get(type) ?? [];
    handlers.push(handler);
    this.listeners.set(type, handlers);
  }

  emit(type: string, data: unknown) {
    const payload = typeof data === 'string' ? data : JSON.stringify(data);
    for (const handler of this.listeners.get(type) ?? []) {
      handler({ data: payload } as MessageEvent<string>);
    }
  }

  close() {
    this.closed = true;
  }
}

function recordResult(overrides: Partial<MedicalRecordSummary> = {}): MedicalRecordSummary {
  return {
    id: 1,
    registrationId: 40,
    patientId: 100,
    patientName: 'Alice',
    doctorId: 10,
    doctorName: 'Doctor',
    departmentName: 'Cardiology',
    chiefComplaint: 'cough',
    presentIllness: 'two days',
    pastHistory: 'none',
    physicalExam: 'normal',
    preliminaryDiagnosis: 'cold',
    treatmentPlan: 'rest',
    conversationText: 'chat',
    docNote: 'note',
    aiGenerated: true,
    version: 1,
    createdAt: '2026-01-01T00:00:00Z',
    degraded: false,
    ...overrides,
  };
}

function diagnosisResult(overrides: Partial<DiagnosisSuggestionResponse> = {}): DiagnosisSuggestionResponse {
  return {
    id: 2,
    registrationId: 40,
    suggestedDiagnoses: 'cold',
    suggestedExamItems: 'blood',
    adoptionStatus: 'SUGGESTED',
    summary: 'summary',
    degraded: false,
    ...overrides,
  };
}

function installEventSource() {
  Object.defineProperty(globalThis, 'EventSource', {
    configurable: true,
    writable: true,
    value: FakeEventSource,
  });
  (window as unknown as { EventSource: typeof FakeEventSource }).EventSource = FakeEventSource;
}

async function flushPromises() {
  await Promise.resolve();
  await Promise.resolve();
}

describe('ai stream store', () => {
  beforeEach(() => {
    installBrowserMocks();
    setActivePinia(createPinia());
    FakeEventSource.instances = [];
    vi.clearAllMocks();
  });

  afterEach(() => {
    Reflect.deleteProperty(globalThis, 'EventSource');
    removeBrowserMocks();
  });

  it('falls back to plain POST when EventSource is unavailable', async () => {
    vi.mocked(workflowApi.generateMedicalRecord).mockResolvedValue(recordResult({
      chiefComplaint: null,
      presentIllness: null,
      pastHistory: null,
      physicalExam: null,
      preliminaryDiagnosis: null,
      treatmentPlan: null,
      docNote: null,
    }));
    const store = useAiStreamStore();
    const onResult = vi.fn();

    await expect(store.start('MEDICAL_RECORD', 40, 'conversation', 'cold', onResult)).resolves.toBe('post-fallback');

    expect(workflowApi.generateMedicalRecord).toHaveBeenCalledWith({
      registrationId: 40,
      conversationText: 'conversation',
      diagnosisDirection: 'cold',
    });
    expect(store.streamText).toContain('chiefComplaint: ');
    expect(onResult).toHaveBeenCalledWith(expect.objectContaining({ id: 1 }), 'post-fallback');
    expect(store.streaming).toBe(false);
    expect(store.connected).toBe(false);
  });

  it('consumes SSE thinking, chunks, result, and done events', async () => {
    installEventSource();
    vi.mocked(workflowApi.createAiStreamSession).mockResolvedValue({
      sessionId: 'session-1',
      streamToken: 'token 1',
      taskType: 'DIAGNOSIS',
      expiresAt: '2026-01-01T00:00:00Z',
    });
    const store = useAiStreamStore();
    const onResult = vi.fn();

    const run = store.start('DIAGNOSIS', 40, 'conversation', null, onResult);
    await flushPromises();
    const source = FakeEventSource.instances[0];

    expect(store.sessionId).toBe('session-1');
    expect(source.url).toBe('/api/ai-stream-sessions/session-1/events?token=token%201');
    expect(store.connected).toBe(true);

    source.emit('thinking', { text: 'think ' });
    source.emit('thinking', 'raw-think');
    source.emit('chunk', { text: 'hello ' });
    source.emit('chunk', 'raw');
    source.emit('result', diagnosisResult());
    source.emit('result', '{bad-json');
    source.emit('done', {});
    source.emit('done', {});

    await expect(run).resolves.toBe('sse');
    expect(store.thinkingText).toBe('think raw-think');
    expect(store.streamText).toBe('hello raw');
    expect(onResult).toHaveBeenCalledWith(expect.objectContaining({ suggestedDiagnoses: 'cold' }), 'sse');
    expect(source.closed).toBe(true);
    expect(store.streaming).toBe(false);
  });

  it('treats SSE error after a result as successful SSE completion', async () => {
    installEventSource();
    vi.mocked(workflowApi.createAiStreamSession).mockResolvedValue({
      sessionId: 'session-result-error',
      streamToken: 'token',
      taskType: 'DIAGNOSIS',
      expiresAt: '2026-01-01T00:00:00Z',
    });
    const store = useAiStreamStore();
    const onResult = vi.fn();

    const run = store.start('DIAGNOSIS', 40, 'conversation', null, onResult);
    await flushPromises();
    const source = FakeEventSource.instances[0];
    source.emit('result', diagnosisResult());
    source.onerror?.();

    await expect(run).resolves.toBe('sse');
    expect(onResult).toHaveBeenCalledWith(expect.objectContaining({ suggestedDiagnoses: 'cold' }), 'sse');
  });

  it('falls back to POST when SSE session creation fails', async () => {
    installEventSource();
    vi.mocked(workflowApi.createAiStreamSession).mockRejectedValue(new Error('create failed'));
    vi.mocked(workflowApi.diagnose).mockResolvedValue(diagnosisResult({
      suggestedDiagnoses: '',
      suggestedExamItems: '',
      summary: '',
    }));
    const store = useAiStreamStore();
    const onResult = vi.fn();

    await expect(store.start('DIAGNOSIS', 40, 'conversation', null, onResult)).resolves.toBe('post-fallback');

    expect(workflowApi.diagnose).toHaveBeenCalledWith({
      registrationId: 40,
      conversationText: 'conversation',
      diagnosisDirection: null,
    });
    expect(store.streamText).toContain('suggestedDiagnoses: ');
  });

  it('cancels failed SSE sessions and uses diagnosis POST fallback', async () => {
    installEventSource();
    vi.mocked(workflowApi.createAiStreamSession).mockResolvedValue({
      sessionId: 'session-2',
      streamToken: 'token-2',
      taskType: 'DIAGNOSIS',
      expiresAt: '2026-01-01T00:00:00Z',
    });
    vi.mocked(workflowApi.cancelAiStreamSession).mockResolvedValue(undefined);
    vi.mocked(workflowApi.diagnose).mockResolvedValue(diagnosisResult());
    const store = useAiStreamStore();
    const onResult = vi.fn();

    const run = store.start('DIAGNOSIS', 40, 'conversation', 'cold', onResult);
    await flushPromises();
    const source = FakeEventSource.instances[0];
    source.onerror?.();

    await expect(run).resolves.toBe('post-fallback');
    expect(workflowApi.cancelAiStreamSession).toHaveBeenCalledWith('session-2');
    expect(workflowApi.diagnose).toHaveBeenCalledWith({
      registrationId: 40,
      conversationText: 'conversation',
      diagnosisDirection: 'cold',
    });
    expect(store.streamText).toContain('suggestedDiagnoses: cold');
    expect(onResult).toHaveBeenCalledWith(expect.objectContaining({ summary: 'summary' }), 'post-fallback');
  });

  it('cancels active streams and clears transient output', async () => {
    installEventSource();
    vi.mocked(workflowApi.createAiStreamSession).mockResolvedValue({
      sessionId: 'session-3',
      streamToken: 'token-3',
      taskType: 'MEDICAL_RECORD',
      expiresAt: '2026-01-01T00:00:00Z',
    });
    vi.mocked(workflowApi.cancelAiStreamSession).mockResolvedValue(undefined);
    const store = useAiStreamStore();

    const run = store.start('MEDICAL_RECORD', 40, 'conversation', null, vi.fn());
    await flushPromises();
    const source = FakeEventSource.instances[0];
    source.emit('chunk', { text: 'partial' });

    await store.cancel();

    await expect(run).resolves.toBe('cancelled');
    expect(source.closed).toBe(true);
    expect(workflowApi.cancelAiStreamSession).toHaveBeenCalledWith('session-3');
    expect(store.sessionId).toBeNull();
    expect(store.streaming).toBe(false);
    expect(store.streamText).toBe('');
  });

  it('cancels an existing run before starting a new one and ignores stale events', async () => {
    installEventSource();
    vi.mocked(workflowApi.createAiStreamSession)
      .mockResolvedValueOnce({
        sessionId: 'session-old',
        streamToken: 'old',
        taskType: 'DIAGNOSIS',
        expiresAt: '2026-01-01T00:00:00Z',
      })
      .mockResolvedValueOnce({
        sessionId: 'session-new',
        streamToken: 'new',
        taskType: 'DIAGNOSIS',
        expiresAt: '2026-01-01T00:00:00Z',
      });
    vi.mocked(workflowApi.cancelAiStreamSession).mockResolvedValue(undefined);
    const store = useAiStreamStore();
    const oldResult = vi.fn();
    const newResult = vi.fn();

    const firstRun = store.start('DIAGNOSIS', 40, 'old', null, oldResult);
    await flushPromises();
    const oldSource = FakeEventSource.instances[0];
    const secondRun = store.start('DIAGNOSIS', 41, 'new', null, newResult);
    await flushPromises();
    const newSource = FakeEventSource.instances[1];

    oldSource.emit('thinking', { text: 'stale thinking' });
    oldSource.emit('chunk', { text: 'stale chunk' });
    oldSource.emit('result', diagnosisResult({ id: 99 }));
    newSource.emit('cancelled', {});

    await expect(firstRun).resolves.toBe('cancelled');
    await expect(secondRun).resolves.toBe('cancelled');
    expect(oldResult).not.toHaveBeenCalled();
    expect(newResult).not.toHaveBeenCalled();
    expect(workflowApi.cancelAiStreamSession).toHaveBeenCalledWith('session-old');
  });
});
