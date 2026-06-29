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
import AiChatLauncher from '@/components/chat/AiChatLauncher.vue';
import RecordPrescriptionDetailDialog from '@/components/shared/RecordPrescriptionDetailDialog.vue';

import {
  adoptDiagnosisSuggestion,
  beginConsultation,
  completeConsultation,
  getDashboardAiUsage,
  getDashboardOverview,
  getDashboardPrescriptionReviewRate,
  getDashboardRiskDistribution,
  getDashboardTrends,
  getDashboardTriageAccuracy,
  getDoctor,
  getMedicalRecord,
  getPrescription,
  getWorkspace,
  ignoreDiagnosisSuggestion,
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
import { storeToRefs } from 'pinia';
import { useAuthStore } from '@/stores/auth';
import { useAiStreamStore } from '@/stores/ai-stream';
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

type StatusTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

const authStore = useAuthStore();
const aiStreamStore = useAiStreamStore();
const { streamText, sessionId: activeStreamSessionId, streaming: aiStreaming } = storeToRefs(aiStreamStore);

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
const recordDegraded = ref(false);
const diagnosisDegraded = ref(false);
const reviewingPrescription = ref(false);
const submittingPrescription = ref(false);
const diagnosisUpdating = ref(false);
const drugLoading = ref(false);
const showPrescriptionConfirm = ref(false);
const showCompleteConfirm = ref(false);
const startingConsultation = ref(false);
const completingConsultation = ref(false);
const notificationLoading = ref(false);
const detailLoading = ref(false);
const ackingNotificationId = ref<number | null>(null);
const notificationSocketState = ref<'idle' | 'connecting' | 'connected' | 'closed'>('idle');
let notificationSocket: WebSocket | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
let reconnectAttempt = 0;
const MAX_RECONNECT_DELAY = 30_000;

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
const detailKind = ref<'record' | 'prescription' | null>(null);
const selectedMedicalRecord = ref<MedicalRecordSummary | null>(null);
const selectedPrescription = ref<PrescriptionSummary | null>(null);

const selectedRegistration = computed<RegistrationSummary | null>(() => {
  if (workspace.value?.registration) {
    return workspace.value.registration;
  }
  return queue.value.find((item) => item.id === selectedRegistrationId.value) ?? null;
});
const selectedRegistrationStatus = computed(() => selectedRegistration.value?.status ?? null);
const canBeginSelectedConsultation = computed(() => selectedRegistrationStatus.value === 'WAITING');
const canReviewSelectedPrescription = computed(() =>
  ['MEDICAL_RECORD_SAVED', 'PRESCRIPTION_REVIEWED'].includes(selectedRegistrationStatus.value ?? ''),
);
const consultationStatusLabel = computed(() => registrationStatusLabel(selectedRegistrationStatus.value));
const consultationStatusTone = computed<StatusTone>(() => registrationStatusTone(selectedRegistrationStatus.value));

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
  recordDegraded,
  diagnosisDegraded,
  reviewingPrescription,
  submittingPrescription,
  diagnosisUpdating,
  drugLoading,
  startingConsultation,
  completingConsultation,
  notificationLoading,
  detailLoading,
  ackingNotificationId,
  notificationSocketState,
  streamText,
  activeStreamSessionId,
  consultationForm,
  recordForm,
  manualConfirmation,
  prescriptionItems,
  detailKind,
  selectedMedicalRecord,
  selectedPrescription,
  selectedRegistration,
  selectedRegistrationStatus,
  canBeginSelectedConsultation,
  canReviewSelectedPrescription,
  consultationStatusLabel,
  consultationStatusTone,
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
  choosePrescriptionDrug,
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
  adoptCurrentDiagnosis,
  ignoreCurrentDiagnosis,
  saveCurrentMedicalRecord,
  reviewCurrentPrescription,
  requestSubmitPrescription,
  submitCurrentPrescription,
  cancelPrescriptionSubmit,
  showPrescriptionConfirm,
  requestCompleteSelectedConsultation,
  confirmCompleteSelectedConsultation,
  cancelCompleteSelectedConsultation,
  showCompleteConfirm,
  viewMedicalRecordDetail,
  viewPrescriptionDetail,
  closeRecordPrescriptionDetail,
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

function registrationStatusLabel(status: string | null | undefined) {
  switch (status) {
    case 'WAITING':
      return '等待接诊';
    case 'IN_CONSULTATION':
      return '就诊中';
    case 'MEDICAL_RECORD_SAVED':
      return '病历已保存';
    case 'PRESCRIPTION_REVIEWED':
      return '处方已审核';
    case 'PRESCRIPTION_SUBMITTED':
      return '处方已提交';
    case 'COMPLETED':
      return '已完成';
    case 'CANCELLED':
      return '已取消';
    default:
      return '未选中';
  }
}

function registrationStatusTone(status: string | null | undefined): StatusTone {
  switch (status) {
    case 'WAITING':
      return 'info';
    case 'COMPLETED':
      return 'success';
    case 'CANCELLED':
      return 'danger';
    case null:
    case undefined:
      return 'neutral';
    default:
      return 'warning';
  }
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
  recordDegraded.value = draft.degraded ?? false;
  consultationForm.diagnosisDirection = draft.preliminaryDiagnosis || consultationForm.diagnosisDirection;
}

function applyDiagnosisResult(result: DiagnosisSuggestionResponse) {
  diagnosisSuggestion.value = result;
  diagnosisDegraded.value = result.degraded ?? false;
  recordForm.preliminaryDiagnosis = result.suggestedDiagnoses.split('\n')[0] || recordForm.preliminaryDiagnosis;
}

function resetPerPatientState() {
  if (activeStreamSessionId.value || aiStreaming.value) {
    void aiStreamStore.cancel();
  }
  recordDegraded.value = false;
  diagnosisDegraded.value = false;

  Object.assign(recordForm, emptyRecordForm());
  consultationForm.conversationText = '';
  consultationForm.diagnosisDirection = '';
  manualConfirmation.value = '医生已完成本地规则审方并确认。';
  diagnosisSuggestion.value = null;
  reviewResult.value = null;
  prescriptionItems.value = [createEmptyItem()];
}

function syncFormsFromWorkspace(snapshot: ConsultationWorkspace | null) {
  // Always reset per-patient transient state first
  resetPerPatientState();

  if (!snapshot) {
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

function applyRegistrationSummary(registration: RegistrationSummary) {
  queue.value = queue.value.map((item) => (item.id === registration.id ? registration : item));
  if (workspace.value?.registration.id === registration.id) {
    workspace.value = {
      ...workspace.value,
      registration,
    };
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
  drugLoading.value = true;
  try {
    availableDrugs.value = await searchDrugs(drugSearch.value.trim() || undefined);
  } finally {
    drugLoading.value = false;
  }
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

function scheduleReconnect() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
  }
  const delay = Math.min(1000 * Math.pow(2, reconnectAttempt), MAX_RECONNECT_DELAY);
  reconnectAttempt++;
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    if (notificationSocketState.value !== 'connected') {
      connectNotificationSocket();
    }
  }, delay);
}

function connectNotificationSocket() {
  if (!authStore.token) {
    return;
  }
  if (notificationSocket && notificationSocket.readyState === WebSocket.OPEN) {
    return;
  }
  notificationSocketState.value = 'connecting';
  notificationSocket = new WebSocket(buildWsUrl('/ws/notifications', authStore.token));
  notificationSocket.onopen = () => {
    notificationSocketState.value = 'connected';
    reconnectAttempt = 0;
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
    scheduleReconnect();
  };
  notificationSocket.onerror = () => {
    notificationSocket?.close();
  };
}

async function closeRealtimeChannels() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  await aiStreamStore.cancel();
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

function clearWorkspaceSelection() {
  selectedRegistrationId.value = null;
  workspace.value = null;
  syncFormsFromWorkspace(null);
}

function resolveQueueSelection(queueData: RegistrationSummary[], preferredRegistrationId?: number | null) {
  const preferredId = preferredRegistrationId === undefined
    ? selectedRegistrationId.value
    : preferredRegistrationId;
  if (preferredId !== null && preferredId !== undefined && queueData.some((item) => item.id === preferredId)) {
    return preferredId;
  }
  return queueData[0]?.id ?? null;
}

async function refreshAll(preferredRegistrationId?: number | null) {
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

    const nextRegistrationId = resolveQueueSelection(queueData, preferredRegistrationId);
    if (nextRegistrationId !== null) {
      await loadWorkspaceSnapshot(nextRegistrationId);
    } else {
      clearWorkspaceSelection();
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

function choosePrescriptionDrug(item: PrescriptionDraftItem, drugId: number) {
  item.drugId = drugId;
  applyDrugDefaults(item);
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
    .filter((item) => (
      Number.isFinite(item.drugId)
      && item.drugId > 0
      && Number.isFinite(item.dosage)
      && item.dosage > 0
      && Number.isFinite(item.quantity)
      && item.quantity > 0
      && item.frequency.length > 0
      && item.duration.length > 0
    ));
}

async function beginSelectedConsultation() {
  if (!selectedRegistrationId.value) {
    error.value = '请先选择一个待接诊号源';
    return false;
  }

  if (!canBeginSelectedConsultation.value) {
    return false;
  }

  startingConsultation.value = true;
  error.value = '';
  try {
    const registrationId = selectedRegistrationId.value;
    const registration = await beginConsultation(registrationId);
    applyRegistrationSummary(registration);
    await refreshAll(registrationId);
    return true;
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '开始接诊失败');
    return false;
  } finally {
    startingConsultation.value = false;
  }
}

async function diagnoseCurrentCase() {
  if (!selectedRegistrationId.value || !consultationForm.conversationText.trim()) {
    error.value = '请先填写问诊对话内容';
    return false;
  }

  diagnosingRecord.value = true;
  error.value = '';
  try {
    const streamRegistrationId = selectedRegistrationId.value;
    const delivery = await aiStreamStore.start(
      'DIAGNOSIS',
      streamRegistrationId,
      consultationForm.conversationText.trim(),
      consultationForm.diagnosisDirection.trim() || null,
      (result) => {
        if (selectedRegistrationId.value === streamRegistrationId) {
          applyDiagnosisResult(result as DiagnosisSuggestionResponse);
        }
      },
    );
    return delivery !== 'cancelled';
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '生成诊断建议失败');
    return false;
  } finally {
    diagnosingRecord.value = false;
  }
}

async function generateDraftMedicalRecord() {
  if (!selectedRegistrationId.value || !consultationForm.conversationText.trim()) {
    error.value = '请先填写问诊对话内容';
    return false;
  }

  generatingRecord.value = true;
  error.value = '';
  try {
    const streamRegistrationId = selectedRegistrationId.value;
    const delivery = await aiStreamStore.start(
      'MEDICAL_RECORD',
      streamRegistrationId,
      consultationForm.conversationText.trim(),
      consultationForm.diagnosisDirection.trim() || null,
      (result) => {
        if (selectedRegistrationId.value === streamRegistrationId) {
          applyMedicalRecordDraft(result as MedicalRecordSummary);
        }
      },
    );
    return delivery !== 'cancelled';
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '生成病历草稿失败');
    return false;
  } finally {
    generatingRecord.value = false;
  }
}

async function adoptCurrentDiagnosis(finalDiagnosis?: string) {
  if (!diagnosisSuggestion.value?.id) {
    error.value = '请先生成诊断建议';
    return false;
  }
  const diagnosis = (finalDiagnosis || diagnosisSuggestion.value.suggestedDiagnoses.split('\n')[0] || '').trim();
  if (!diagnosis) {
    error.value = '诊断建议为空，无法采纳';
    return false;
  }

  diagnosisUpdating.value = true;
  error.value = '';
  try {
    const updated = await adoptDiagnosisSuggestion(diagnosisSuggestion.value.id, { finalDiagnosis: diagnosis });
    applyDiagnosisResult(updated);
    recordForm.preliminaryDiagnosis = diagnosis;
    consultationForm.diagnosisDirection = diagnosis;
    return true;
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '采纳诊断建议失败');
    return false;
  } finally {
    diagnosisUpdating.value = false;
  }
}

async function ignoreCurrentDiagnosis(reason?: string) {
  if (!diagnosisSuggestion.value?.id) {
    error.value = '请先生成诊断建议';
    return false;
  }

  diagnosisUpdating.value = true;
  error.value = '';
  try {
    diagnosisSuggestion.value = await ignoreDiagnosisSuggestion(diagnosisSuggestion.value.id, {
      reason: reason?.trim() || null,
    });
    return true;
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '忽略诊断建议失败');
    return false;
  } finally {
    diagnosisUpdating.value = false;
  }
}

async function saveCurrentMedicalRecord() {
  if (!selectedRegistrationId.value) {
    error.value = '请先选择一个接诊号源';
    return;
  }

  savingRecord.value = true;
  error.value = '';
  try {
    const registrationId = selectedRegistrationId.value;
    await saveMedicalRecord({
      registrationId,
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
    await refreshAll(registrationId);
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '保存病历失败');
  } finally {
    savingRecord.value = false;
  }
}

async function reviewCurrentPrescription() {
  if (selectedRegistrationId.value && !canReviewSelectedPrescription.value) {
    error.value = '请先保存正式病历，再进行处方审核';
    return false;
  }

  if (!selectedRegistrationId.value) {
    error.value = '请先选择一个接诊号源';
    return false;
  }

  const items = buildPrescriptionPayload();
  if (!items.length) {
    error.value = '请先至少添加一条处方项目';
    return false;
  }

  reviewingPrescription.value = true;
  error.value = '';
  reviewResult.value = null;
  try {
    const result = await reviewPrescription({
      registrationId: selectedRegistrationId.value,
      items,
    });
    reviewResult.value = result;
    if (workspace.value?.registration.id === selectedRegistrationId.value) {
      workspace.value = {
        ...workspace.value,
        registration: {
          ...workspace.value.registration,
          status: 'PRESCRIPTION_REVIEWED',
          riskLevel: result.riskLevel,
        },
        recentReviews: [result, ...workspace.value.recentReviews.filter((item) => item.reviewId !== result.reviewId)],
      };
    }
    queue.value = queue.value.map((item) => item.id === selectedRegistrationId.value
      ? { ...item, status: 'PRESCRIPTION_REVIEWED', riskLevel: result.riskLevel }
      : item);
    await loadNotifications();
    return true;
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '处方审核失败');
    reviewResult.value = null;
    return false;
  } finally {
    reviewingPrescription.value = false;
  }
}

function requestSubmitPrescription() {
  if (!selectedRegistrationId.value || !reviewResult.value?.reviewId) {
    error.value = '请先完成审方';
    return;
  }
  const items = buildPrescriptionPayload();
  if (!items.length) {
    error.value = '请先至少添加一条处方项目';
    return false;
  }
  showPrescriptionConfirm.value = true;
}

async function submitCurrentPrescription() {
  showPrescriptionConfirm.value = false;

  if (!selectedRegistrationId.value || !reviewResult.value?.reviewId) {
    return false;
  }

  const items = buildPrescriptionPayload();
  if (!items.length) {
    error.value = '请先至少添加一条处方项目';
    return false;
  }

  submittingPrescription.value = true;
  error.value = '';
  try {
    const completedRegistrationId = selectedRegistrationId.value;
    await submitPrescription({
      registrationId: completedRegistrationId,
      reviewId: reviewResult.value.reviewId,
      items,
      manualConfirmation: manualConfirmation.value.trim() || null,
    });
    await loadNotifications();
    await refreshAll(null);
    return true;
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '提交处方失败');
    return false;
  } finally {
    submittingPrescription.value = false;
  }
}

function cancelPrescriptionSubmit() {
  showPrescriptionConfirm.value = false;
}

function requestCompleteSelectedConsultation() {
  if (!selectedRegistrationId.value) {
    error.value = '请先选择一个接诊号源';
    return;
  }
  showCompleteConfirm.value = true;
}

async function confirmCompleteSelectedConsultation() {
  showCompleteConfirm.value = false;
  return completeSelectedConsultation();
}

function cancelCompleteSelectedConsultation() {
  showCompleteConfirm.value = false;
}

async function viewMedicalRecordDetail(recordId: number) {
  const summary = medicalRecords.value.find((item) => item.id === recordId) ?? null;
  detailKind.value = 'record';
  selectedMedicalRecord.value = summary;
  selectedPrescription.value = null;
  detailLoading.value = true;
  error.value = '';
  try {
    selectedMedicalRecord.value = await getMedicalRecord(recordId);
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '加载病历详情失败');
    if (!summary) {
      detailKind.value = null;
    }
  } finally {
    detailLoading.value = false;
  }
}

async function viewPrescriptionDetail(prescriptionId: number) {
  const summary = prescriptions.value.find((item) => item.id === prescriptionId) ?? null;
  detailKind.value = 'prescription';
  selectedMedicalRecord.value = null;
  selectedPrescription.value = summary;
  detailLoading.value = true;
  error.value = '';
  try {
    selectedPrescription.value = await getPrescription(prescriptionId);
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '加载处方详情失败');
    if (!summary) {
      detailKind.value = null;
    }
  } finally {
    detailLoading.value = false;
  }
}

function closeRecordPrescriptionDetail() {
  detailKind.value = null;
  selectedMedicalRecord.value = null;
  selectedPrescription.value = null;
  detailLoading.value = false;
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
    return false;
  }

  completingConsultation.value = true;
  error.value = '';
  try {
    await completeConsultation(selectedRegistrationId.value);
    await refreshAll(null);
    return true;
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '结束就诊失败');
    return false;
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
        <StatusChip :tone="consultationStatusTone">
          {{ selectedRegistration?.patientName || '未选中患者' }} · {{ consultationStatusLabel }}
        </StatusChip>
        <StatusChip :tone="notificationSocketState === 'connected' ? 'success' : 'neutral'" :dot="true">
          通知 {{ notificationSocketState === 'connected' ? '已连接' : '未连接' }}
        </StatusChip>
        <span class="flex-1" />
        <button class="btn-ghost" type="button" @click="refreshAll()" :disabled="loading">
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
  <AiChatLauncher role="doctor" />
  <RecordPrescriptionDetailDialog
    :open="detailKind !== null"
    :kind="detailKind"
    :loading="detailLoading"
    :medical-record="selectedMedicalRecord"
    :prescription="selectedPrescription"
    :format-date-time="formatDateTime"
    @close="closeRecordPrescriptionDetail()"
  />
</template>
