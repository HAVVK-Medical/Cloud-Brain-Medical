<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import {
  LayoutDashboard,
  Stethoscope,
  Clock,
  CalendarDays,
  RefreshCw,
} from 'lucide-vue-next';
import SideNav from '@/components/layout/SideNav.vue';
import StatusChip from '@/components/shared/StatusChip.vue';

import {
  beginConsultation,
  cancelAiStreamSession,
  completeConsultation,
  createAiStreamSession,
  diagnose,
  generateMedicalRecord,
  getDashboardAiUsage,
  getDashboardOverview,
  getDashboardPrescriptionReviewRate,
  getDashboardRiskDistribution,
  getDashboardTrends,
  getDashboardTriageAccuracy,
  getDoctor,
  getWorkspace,
  listUnreadNotifications,
  listDoctorPrescriptions,
  listDoctorQueue,
  listDoctorSchedules,
  markNotificationRead,
  reviewPrescription,
  saveMedicalRecord,
  searchDoctorMedicalRecords,
  searchDrugs,
  submitPrescription,
} from '@/api/workflow';
import { useAuthStore } from '@/stores/auth';
import type {
  AiUsageStats,
  ConsultationWorkspace,
  DashboardOverview,
  DashboardTrendPoint,
  DiagnosisSuggestionResponse,
  DoctorOption,
  DrugOption,
  MedicalRecordSummary,
  NotificationRecordSummary,
  PrescriptionReviewRate,
  PrescriptionReviewResponse,
  PrescriptionSummary,
  RegistrationSummary,
  RiskDistribution,
  ScheduleOption,
  TriageAccuracyStats,
} from '@/api/workflow';
import { resolveUiErrorMessage } from '@/utils/zh';

interface PrescriptionDraftItem {
  key: number;
  drugId: number | null;
  dosage: string;
  frequency: string;
  duration: string;
  quantity: string;
  usageInstruction: string;
}

const authStore = useAuthStore();

const loading = ref(false);
const workspaceLoading = ref(false);
const error = ref('');
const doctor = ref<DoctorOption | null>(null);
const dashboard = ref<DashboardOverview | null>(null);
const dashboardTrends = ref<DashboardTrendPoint[]>([]);
const aiUsage = ref<AiUsageStats | null>(null);
const prescriptionReviewRate = ref<PrescriptionReviewRate | null>(null);
const riskDistribution = ref<RiskDistribution | null>(null);
const triageAccuracy = ref<TriageAccuracyStats | null>(null);
const queue = ref<RegistrationSummary[]>([]);
const schedules = ref<ScheduleOption[]>([]);
const workspace = ref<ConsultationWorkspace | null>(null);
const medicalRecords = ref<MedicalRecordSummary[]>([]);
const prescriptions = ref<PrescriptionSummary[]>([]);
const notifications = ref<NotificationRecordSummary[]>([]);
const availableDrugs = ref<DrugOption[]>([]);
const diagnosisSuggestion = ref<DiagnosisSuggestionResponse | null>(null);
const reviewResult = ref<PrescriptionReviewResponse | null>(null);
const selectedRegistrationId = ref<number | null>(null);
const recordSearch = ref('');
const drugSearch = ref('');
const itemSeed = ref(1);
const savingRecord = ref(false);
const generatingRecord = ref(false);
const diagnosingRecord = ref(false);
const reviewingPrescription = ref(false);
const submittingPrescription = ref(false);
const startingConsultation = ref(false);
const completingConsultation = ref(false);
const notificationLoading = ref(false);
const ackingNotificationId = ref<number | null>(null);
const notificationSocketState = ref<'idle' | 'connecting' | 'connected' | 'closed'>('idle');
const streamText = ref('');
const activeStreamSessionId = ref<string | null>(null);
let notificationSocket: WebSocket | null = null;

const consultationForm = reactive({
  conversationText: '',
  diagnosisDirection: '',
});

const recordForm = reactive({
  chiefComplaint: '',
  presentIllness: '',
  pastHistory: '',
  physicalExam: '',
  preliminaryDiagnosis: '',
  treatmentPlan: '',
  docNote: '',
  aiGenerated: true,
});

const manualConfirmation = ref('医生已完成本地规则审方并确认。');
const prescriptionItems = ref<PrescriptionDraftItem[]>([createEmptyItem()]);

const selectedRegistration = computed<RegistrationSummary | null>(() => {
  if (workspace.value?.registration) {
    return workspace.value.registration;
  }
  return queue.value.find((item) => item.id === selectedRegistrationId.value) ?? null;
});

const overviewTone = computed(() => (error.value ? 'danger' : loading.value ? 'loading' : 'healthy'));
const workspaceTone = computed(() => (workspaceLoading.value ? 'loading' : workspace.value ? 'healthy' : 'neutral'));
const unreadNotificationCount = computed(() => notifications.value.filter((item) => item.read !== true).length);
const filteredPrescriptions = computed(() => {
  const keyword = drugSearch.value.trim().toLowerCase();
  if (!keyword) {
    return prescriptions.value;
  }
  return prescriptions.value.filter((prescription) => {
    return [
      String(prescription.id),
      prescription.patientName,
      prescription.doctorName,
      prescription.departmentName,
      prescription.status,
      prescription.riskLevel,
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword));
  });
});

const doctorWorkspace = reactive({
  authStore,
  loading,
  workspaceLoading,
  error,
  doctor,
  dashboard,
  dashboardTrends,
  aiUsage,
  prescriptionReviewRate,
  riskDistribution,
  triageAccuracy,
  queue,
  schedules,
  workspace,
  medicalRecords,
  prescriptions,
  notifications,
  availableDrugs,
  diagnosisSuggestion,
  reviewResult,
  selectedRegistrationId,
  recordSearch,
  drugSearch,
  itemSeed,
  savingRecord,
  generatingRecord,
  diagnosingRecord,
  reviewingPrescription,
  submittingPrescription,
  startingConsultation,
  completingConsultation,
  notificationLoading,
  ackingNotificationId,
  notificationSocketState,
  streamText,
  activeStreamSessionId,
  consultationForm,
  recordForm,
  manualConfirmation,
  prescriptionItems,
  selectedRegistration,
  overviewTone,
  workspaceTone,
  unreadNotificationCount,
  filteredPrescriptions,
  formatDateTime,
  formatDate,
  truncate,
  buildWsUrl,
  riskTone,
  emptyRecordForm,
  applyMedicalRecordDraft,
  applyDiagnosisResult,
  syncFormsFromWorkspace,
  loadWorkspaceSnapshot,
  loadHistory,
  loadDrugCatalog,
  loadNotifications,
  upsertNotification,
  connectNotificationSocket,
  closeRealtimeChannels,
  loadDashboardBundle,
  refreshAll,
  selectRegistration,
  addPrescriptionItem,
  removePrescriptionItem,
  applyDrugDefaults,
  buildPrescriptionPayload,
  beginSelectedConsultation,
  diagnoseCurrentCase,
  generateDraftMedicalRecord,
  parseSsePayload,
  startAiStream,
  saveCurrentMedicalRecord,
  reviewCurrentPrescription,
  submitCurrentPrescription,
  ackNotification,
  completeSelectedConsultation,
});

function createEmptyItem(): PrescriptionDraftItem {
  return {
    key: itemSeed.value++,
    drugId: null,
    dosage: '1',
    frequency: 'bid',
    duration: '7天',
    quantity: '14',
    usageInstruction: '',
  };
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '未记录';
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
}

function formatDate(value: string | null | undefined) {
  return value || '未安排';
}

function truncate(value: string | null | undefined, length = 80) {
  if (!value) {
    return '暂无';
  }
  const compact = value.replace(/\s+/g, ' ').trim();
  return compact.length > length ? `${compact.slice(0, length)}...` : compact;
}

function buildWsUrl(path: string, token: string) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}${path}?token=${encodeURIComponent(token)}`;
}

function riskTone(level: string | null | undefined) {
  const normalized = (level || '').toUpperCase();
  if (normalized === 'HIGH' || normalized === 'DANGER' || normalized === 'CRITICAL') {
    return 'danger';
  }
  if (normalized === 'MEDIUM' || normalized === 'WARNING') {
    return 'loading';
  }
  return 'healthy';
}

function emptyRecordForm() {
  return {
    chiefComplaint: '',
    presentIllness: '',
    pastHistory: '',
    physicalExam: '',
    preliminaryDiagnosis: '',
    treatmentPlan: '',
    docNote: '',
    aiGenerated: true,
  };
}

function applyMedicalRecordDraft(draft: MedicalRecordSummary) {
  Object.assign(recordForm, {
    chiefComplaint: draft.chiefComplaint || recordForm.chiefComplaint,
    presentIllness: draft.presentIllness || consultationForm.conversationText,
    pastHistory: draft.pastHistory || recordForm.pastHistory,
    physicalExam: draft.physicalExam || recordForm.physicalExam,
    preliminaryDiagnosis: draft.preliminaryDiagnosis || recordForm.preliminaryDiagnosis,
    treatmentPlan: draft.treatmentPlan || recordForm.treatmentPlan,
    docNote: draft.docNote || recordForm.docNote,
    aiGenerated: draft.aiGenerated ?? true,
  });
  consultationForm.diagnosisDirection = draft.preliminaryDiagnosis || consultationForm.diagnosisDirection;
}

function applyDiagnosisResult(result: DiagnosisSuggestionResponse) {
  diagnosisSuggestion.value = result;
  recordForm.preliminaryDiagnosis = result.suggestedDiagnoses.split('\n')[0] || recordForm.preliminaryDiagnosis;
}

function syncFormsFromWorkspace(snapshot: ConsultationWorkspace | null) {
  if (!snapshot) {
    Object.assign(recordForm, emptyRecordForm());
    consultationForm.conversationText = '';
    consultationForm.diagnosisDirection = '';
    manualConfirmation.value = '医生已完成本地规则审方并确认。';
    diagnosisSuggestion.value = null;
    reviewResult.value = null;
    prescriptionItems.value = [createEmptyItem()];
    return;
  }

  const latestRecord = snapshot.latestMedicalRecord;
  const latestPrescription = snapshot.latestPrescription;
  consultationForm.conversationText = latestRecord?.conversationText || snapshot.registration.chiefComplaint || '';
  consultationForm.diagnosisDirection = latestRecord?.preliminaryDiagnosis || '';
  Object.assign(recordForm, {
    chiefComplaint: latestRecord?.chiefComplaint || snapshot.registration.chiefComplaint || '',
    presentIllness: latestRecord?.presentIllness || consultationForm.conversationText,
    pastHistory: latestRecord?.pastHistory || '',
    physicalExam: latestRecord?.physicalExam || '',
    preliminaryDiagnosis: latestRecord?.preliminaryDiagnosis || '',
    treatmentPlan: latestRecord?.treatmentPlan || '',
    docNote: latestRecord?.docNote || '',
    aiGenerated: latestRecord?.aiGenerated ?? true,
  });

  reviewResult.value = latestPrescription?.review ?? null;
  manualConfirmation.value = latestPrescription?.review?.bindStatus === 'BOUND'
    ? latestPrescription.review?.llmSummary || '已绑定审方结果。'
    : '医生已完成本地规则审方并确认。';

  if (latestPrescription?.items?.length) {
    const items = latestPrescription.items as Array<{
      drugId?: number | null;
      dosage?: number | string | null;
      frequency?: string | null;
      duration?: string | null;
      quantity?: number | string | null;
      usageInstruction?: string | null;
    }>;
    prescriptionItems.value = items.map((item) => ({
      key: itemSeed.value++,
      drugId: item.drugId ?? null,
      dosage: item.dosage == null ? '1' : String(item.dosage),
      frequency: item.frequency || 'bid',
      duration: item.duration || '7天',
      quantity: item.quantity == null ? '14' : String(item.quantity),
      usageInstruction: item.usageInstruction || '',
    }));
  } else {
    prescriptionItems.value = [createEmptyItem()];
  }
}

async function loadWorkspaceSnapshot(registrationId: number) {
  workspaceLoading.value = true;
  try {
    const snapshot = await getWorkspace(registrationId);
    workspace.value = snapshot;
    selectedRegistrationId.value = registrationId;
    syncFormsFromWorkspace(snapshot);
  } finally {
    workspaceLoading.value = false;
  }
}

async function loadHistory() {
  const [recordData, prescriptionData] = await Promise.all([
    searchDoctorMedicalRecords(recordSearch.value.trim() || undefined),
    listDoctorPrescriptions(),
  ]);
  medicalRecords.value = recordData;
  prescriptions.value = prescriptionData;
}

async function loadDrugCatalog() {
  availableDrugs.value = await searchDrugs(drugSearch.value.trim() || undefined);
}

async function loadNotifications() {
  notificationLoading.value = true;
  try {
    notifications.value = await listUnreadNotifications();
  } finally {
    notificationLoading.value = false;
  }
}

function upsertNotification(notification: NotificationRecordSummary) {
  notifications.value = [
    notification,
    ...notifications.value.filter((item) => item.id !== notification.id),
  ].slice(0, 20);
}

function connectNotificationSocket() {
  if (!authStore.token || notificationSocket) {
    return;
  }
  notificationSocketState.value = 'connecting';
  notificationSocket = new WebSocket(buildWsUrl('/ws/notifications', authStore.token));
  notificationSocket.onopen = () => {
    notificationSocketState.value = 'connected';
  };
  notificationSocket.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data) as { type?: string; payload?: NotificationRecordSummary };
      if (message.type === 'notification' && message.payload) {
        upsertNotification(message.payload);
        void loadDashboardBundle();
      }
    } catch {
      // ignore malformed realtime messages
    }
  };
  notificationSocket.onclose = () => {
    notificationSocket = null;
    notificationSocketState.value = 'closed';
  };
  notificationSocket.onerror = () => {
    notificationSocketState.value = 'closed';
  };
}

async function closeRealtimeChannels() {
  if (activeStreamSessionId.value) {
    try {
      await cancelAiStreamSession(activeStreamSessionId.value);
    } catch {
      // stream may already have ended
    }
    activeStreamSessionId.value = null;
  }
  notificationSocket?.close();
  notificationSocket = null;
}

async function loadDashboardBundle() {
  const [overviewData, trendData, aiUsageData, reviewRateData, riskData, triageData] = await Promise.all([
    getDashboardOverview(),
    getDashboardTrends(),
    getDashboardAiUsage(),
    getDashboardPrescriptionReviewRate(),
    getDashboardRiskDistribution(),
    getDashboardTriageAccuracy(),
  ]);
  dashboard.value = overviewData;
  dashboardTrends.value = trendData;
  aiUsage.value = aiUsageData;
  prescriptionReviewRate.value = reviewRateData;
  riskDistribution.value = riskData;
  triageAccuracy.value = triageData;
}

async function refreshAll() {
  loading.value = true;
  error.value = '';
  try {
    const doctorId = authStore.doctorId;
    const [doctorData, , queueData, scheduleData, notificationData] = await Promise.all([
      doctorId ? getDoctor(doctorId) : Promise.resolve(null),
      loadDashboardBundle(),
      listDoctorQueue(),
      doctorId ? listDoctorSchedules(doctorId) : Promise.resolve([] as ScheduleOption[]),
      listUnreadNotifications(),
    ]);

    doctor.value = doctorData;
    queue.value = queueData;
    schedules.value = scheduleData;
    notifications.value = notificationData;

    await loadHistory();
    await loadDrugCatalog();

    if (selectedRegistrationId.value === null) {
      selectedRegistrationId.value = queueData[0]?.id ?? null;
    }

    if (selectedRegistrationId.value !== null) {
      await loadWorkspaceSnapshot(selectedRegistrationId.value);
    } else {
      workspace.value = null;
      syncFormsFromWorkspace(null);
    }
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '医生工作台加载失败');
  } finally {
    loading.value = false;
  }
}

async function selectRegistration(registrationId: number) {
  selectedRegistrationId.value = registrationId;
  error.value = '';
  try {
    await loadWorkspaceSnapshot(registrationId);
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '切换接诊失败');
  }
}

function addPrescriptionItem() {
  prescriptionItems.value.push(createEmptyItem());
}

function removePrescriptionItem(key: number) {
  if (prescriptionItems.value.length === 1) {
    prescriptionItems.value = [createEmptyItem()];
    return;
  }
  prescriptionItems.value = prescriptionItems.value.filter((item) => item.key !== key);
}

function applyDrugDefaults(item: PrescriptionDraftItem) {
  const drug = availableDrugs.value.find((candidate) => candidate.id === item.drugId);
  if (drug && !item.usageInstruction) {
    item.usageInstruction = drug.defaultUsage || '';
  }
}

function buildPrescriptionPayload() {
  return prescriptionItems.value
    .filter((item) => item.drugId !== null)
    .map((item) => ({
      drugId: Number(item.drugId),
      dosage: Number(item.dosage || '0'),
      frequency: item.frequency.trim(),
      duration: item.duration.trim(),
      quantity: Number(item.quantity || '0'),
      usageInstruction: item.usageInstruction.trim() || null,
    }))
    .filter((item) => Number.isFinite(item.drugId) && Number.isFinite(item.dosage) && Number.isFinite(item.quantity));
}

async function beginSelectedConsultation() {
  if (!selectedRegistrationId.value) {
    error.value = '请先选择一个待接诊号源';
    return;
  }

  startingConsultation.value = true;
  error.value = '';
  try {
    await beginConsultation(selectedRegistrationId.value);
    await refreshAll();
    await loadWorkspaceSnapshot(selectedRegistrationId.value);
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '开始接诊失败');
  } finally {
    startingConsultation.value = false;
  }
}

async function diagnoseCurrentCase() {
  if (!selectedRegistrationId.value || !consultationForm.conversationText.trim()) {
    error.value = '请先填写问诊对话内容';
    return;
  }

  diagnosingRecord.value = true;
  error.value = '';
  try {
    try {
      await startAiStream('DIAGNOSIS');
    } catch {
      applyDiagnosisResult(await diagnose({
        registrationId: selectedRegistrationId.value,
        conversationText: consultationForm.conversationText.trim(),
        diagnosisDirection: consultationForm.diagnosisDirection.trim() || null,
      }));
    }
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '生成诊断建议失败');
  } finally {
    diagnosingRecord.value = false;
  }
}

async function generateDraftMedicalRecord() {
  if (!selectedRegistrationId.value || !consultationForm.conversationText.trim()) {
    error.value = '请先填写问诊对话内容';
    return;
  }

  generatingRecord.value = true;
  error.value = '';
  try {
    try {
      await startAiStream('MEDICAL_RECORD');
    } catch {
      const draft = await generateMedicalRecord({
        registrationId: selectedRegistrationId.value,
        conversationText: consultationForm.conversationText.trim(),
        diagnosisDirection: consultationForm.diagnosisDirection.trim() || null,
      });
      applyMedicalRecordDraft(draft);
    }
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '生成病历草稿失败');
  } finally {
    generatingRecord.value = false;
  }
}

function parseSsePayload<T>(value: string): T | null {
  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

async function startAiStream(taskType: 'MEDICAL_RECORD' | 'DIAGNOSIS') {
  if (!selectedRegistrationId.value || !consultationForm.conversationText.trim()) {
    throw new Error('missing stream context');
  }

  if (!window.EventSource) {
    throw new Error('event source unsupported');
  }

  streamText.value = '';
  const session = await createAiStreamSession({
    taskType,
    registrationId: selectedRegistrationId.value,
    conversationText: consultationForm.conversationText.trim(),
    diagnosisDirection: consultationForm.diagnosisDirection.trim() || null,
  });
  activeStreamSessionId.value = session.sessionId;

  await new Promise<void>((resolve, reject) => {
    let completed = false;
    const source = new EventSource(`/api/ai-stream-sessions/${session.sessionId}/events?token=${encodeURIComponent(session.streamToken)}`);
    const finish = () => {
      completed = true;
      activeStreamSessionId.value = null;
      source.close();
      resolve();
    };

    source.addEventListener('chunk', (event) => {
      const payload = parseSsePayload<{ text?: string }>(event.data);
      streamText.value += payload?.text ?? event.data;
    });
    source.addEventListener('result', (event) => {
      if (taskType === 'MEDICAL_RECORD') {
        const payload = parseSsePayload<MedicalRecordSummary>(event.data);
        if (payload) applyMedicalRecordDraft(payload);
      } else {
        const payload = parseSsePayload<DiagnosisSuggestionResponse>(event.data);
        if (payload) applyDiagnosisResult(payload);
      }
    });
    source.addEventListener('done', finish);
    source.addEventListener('cancelled', finish);
    source.onerror = () => {
      if (!completed) {
        activeStreamSessionId.value = null;
        source.close();
        reject(new Error('stream failed'));
      }
    };
  });
}

async function saveCurrentMedicalRecord() {
  if (!selectedRegistrationId.value) {
    error.value = '请先选择一个接诊号源';
    return;
  }

  savingRecord.value = true;
  error.value = '';
  try {
    await saveMedicalRecord({
      registrationId: selectedRegistrationId.value,
      conversationText: consultationForm.conversationText.trim() || null,
      chiefComplaint: recordForm.chiefComplaint.trim() || null,
      presentIllness: recordForm.presentIllness.trim() || null,
      pastHistory: recordForm.pastHistory.trim() || null,
      physicalExam: recordForm.physicalExam.trim() || null,
      preliminaryDiagnosis: recordForm.preliminaryDiagnosis.trim() || null,
      treatmentPlan: recordForm.treatmentPlan.trim() || null,
      docNote: recordForm.docNote.trim() || null,
      aiGenerated: recordForm.aiGenerated,
    });
    await refreshAll();
    if (selectedRegistrationId.value) {
      await loadWorkspaceSnapshot(selectedRegistrationId.value);
    }
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '保存病历失败');
  } finally {
    savingRecord.value = false;
  }
}

async function reviewCurrentPrescription() {
  if (!selectedRegistrationId.value) {
    error.value = '请先选择一个接诊号源';
    return;
  }

  const items = buildPrescriptionPayload();
  if (!items.length) {
    error.value = '请先至少添加一条处方项目';
    return;
  }

  reviewingPrescription.value = true;
  error.value = '';
  try {
    reviewResult.value = await reviewPrescription({
      registrationId: selectedRegistrationId.value,
      items,
    });
    await loadNotifications();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '处方审查失败');
  } finally {
    reviewingPrescription.value = false;
  }
}

async function submitCurrentPrescription() {
  if (!selectedRegistrationId.value || !reviewResult.value?.reviewId) {
    error.value = '请先完成审方';
    return;
  }

  const items = buildPrescriptionPayload();
  if (!items.length) {
    error.value = '请先至少添加一条处方项目';
    return;
  }

  submittingPrescription.value = true;
  error.value = '';
  try {
    await submitPrescription({
      registrationId: selectedRegistrationId.value,
      reviewId: reviewResult.value.reviewId,
      items,
      manualConfirmation: manualConfirmation.value.trim() || null,
    });
    await loadNotifications();
    await refreshAll();
    if (selectedRegistrationId.value) {
      await loadWorkspaceSnapshot(selectedRegistrationId.value);
    }
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '提交处方失败');
  } finally {
    submittingPrescription.value = false;
  }
}

async function ackNotification(id: number) {
  ackingNotificationId.value = id;
  error.value = '';
  try {
    await markNotificationRead(id);
    notifications.value = notifications.value.filter((item) => item.id !== id);
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '鏍囪鍛婅宸茶澶辫触');
  } finally {
    ackingNotificationId.value = null;
  }
}

async function completeSelectedConsultation() {
  if (!selectedRegistrationId.value) {
    error.value = '请先选择一个接诊号源';
    return;
  }

  completingConsultation.value = true;
  error.value = '';
  try {
    await completeConsultation(selectedRegistrationId.value);
    await refreshAll();
    if (selectedRegistrationId.value) {
      await loadWorkspaceSnapshot(selectedRegistrationId.value);
    }
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '结束就诊失败');
  } finally {
    completingConsultation.value = false;
  }
}

onMounted(() => {
  void refreshAll();
  connectNotificationSocket();
});

onBeforeUnmount(() => {
  void closeRealtimeChannels();
});
</script>

<template>
  <div class="flex flex-1 min-h-0">
    <SideNav
      title="医生工作台"
      :subtitle="doctor?.name || authStore.sessionLabel"
      :items="[
        { id: 'overview', label: '总览', path: '/doctor/overview', icon: LayoutDashboard },
        { id: 'consultation', label: '接诊', path: '/doctor/consultation', icon: Stethoscope },
        { id: 'history', label: '历史', path: '/doctor/history', icon: Clock },
        { id: 'schedule', label: '排班', path: '/doctor/schedule', icon: CalendarDays },
      ]"
    />

    <div class="flex-1 overflow-y-auto p-6">
      <p v-if="error" class="mb-4 p-3 rounded-md bg-red-50 text-danger text-sm">{{ error }}</p>

      <!-- Topline stats -->
      <div class="flex items-center gap-3 mb-6 flex-wrap">
        <StatusChip :tone="selectedRegistration?.status === 'WAITING' ? 'info' : 'success'">
          {{ selectedRegistration?.patientName || '未选中患者' }}
        </StatusChip>
        <StatusChip :tone="notificationSocketState === 'connected' ? 'success' : 'neutral'" :dot="true">
          通知 {{ notificationSocketState === 'connected' ? '已连接' : '未连接' }}
        </StatusChip>
        <span class="flex-1" />
        <button class="btn-ghost" type="button" @click="refreshAll" :disabled="loading">
          <RefreshCw :size="16" :class="{ 'animate-spin': loading }" />
          <span>{{ loading ? '加载中' : '刷新' }}</span>
        </button>
      </div>

      <RouterView v-slot="{ Component: PanelComp }">
        <Suspense>
          <component :is="PanelComp" :workspace="doctorWorkspace" v-if="PanelComp" />
        </Suspense>
      </RouterView>
    </div>
  </div>
</template>
