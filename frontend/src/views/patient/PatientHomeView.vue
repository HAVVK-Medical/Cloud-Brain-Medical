<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { CalendarDays, FileText, ScanSearch, Ticket, UserRound } from 'lucide-vue-next';
import AiChatLauncher from '@/components/chat/AiChatLauncher.vue';
import ConfirmDialog from '@/components/shared/ConfirmDialog.vue';

import {
  cancelRegistration,
  confirmConversationTriage,
  createFeedback,
  createRegistration,
  getMedicalRecord,
  getPatientInfo,
  getPrescription,
  listDepartments,
  listDoctors,
  listPatientFeedback,
  listPatientMedicalRecords,
  listPatientPrescriptions,
  listRegistrations,
  listSchedules,
  listTriageHistory,
  triageConsult,
  updatePatientInfo,
} from '@/api/workflow';
import type {
  DepartmentOption,
  DoctorOption,
  FeedbackResponse,
  MedicalRecordSummary,
  PatientProfile,
  PrescriptionSummary,
  RegistrationSummary,
  ScheduleOption,
  TriageResponse,
  ConversationTriageConfirmRequest,
} from '@/api/workflow';
import { useAuthStore } from '@/stores/auth';
import { resolveUiErrorMessage } from '@/utils/zh';
import PhoneFrame from '@/components/layout/PhoneFrame.vue';
import LoadingSkeleton from '@/components/shared/LoadingSkeleton.vue';
import RecordPrescriptionDetailDialog from '@/components/shared/RecordPrescriptionDetailDialog.vue';

const authStore = useAuthStore();

const tabs = [
  { id: 'overview', label: '概览', icon: CalendarDays, path: '/patient/overview' },
  { id: 'triage', label: '分诊', icon: ScanSearch, path: '/patient/triage' },
  { id: 'registration', label: '挂号', icon: Ticket, path: '/patient/registration' },
  { id: 'records', label: '病历', icon: FileText, path: '/patient/records' },
  { id: 'profile', label: '我的', icon: UserRound, path: '/patient/profile' },
] as const;

const loading = ref(false);
const triaging = ref(false);
const registering = ref(false);
const savingProfile = ref(false);
const submittingFeedback = ref(false);
const canceling = ref(false);
const detailLoading = ref(false);
const error = ref('');

const patient = ref<PatientProfile | null>(null);
const departments = ref<DepartmentOption[]>([]);
const doctors = ref<DoctorOption[]>([]);
const schedules = ref<ScheduleOption[]>([]);
const triageHistory = ref<TriageResponse[]>([]);
const registrations = ref<RegistrationSummary[]>([]);
const medicalRecords = ref<MedicalRecordSummary[]>([]);
const prescriptions = ref<PrescriptionSummary[]>([]);
const feedbacks = ref<FeedbackResponse[]>([]);
const triageResult = ref<TriageResponse | null>(null);
const cancelRegistrationId = ref<number | null>(null);
const detailKind = ref<'record' | 'prescription' | null>(null);
const selectedMedicalRecord = ref<MedicalRecordSummary | null>(null);
const selectedPrescription = ref<PrescriptionSummary | null>(null);
const showRegistrationConfirm = ref(false);
const latestCreatedRegistration = ref<RegistrationSummary | null>(null);

const selectedDepartmentId = ref<number | null>(null);
const selectedDoctorId = ref<number | null>(null);
const selectedScheduleId = ref<number | null>(null);

const triageForm = reactive({ chiefComplaint: '' });
const profileForm = reactive({
  realName: '',
  gender: '',
  age: '',
  phone: '',
  idCardNumber: '',
  medicalHistory: '',
  remark: '',
});
const feedbackForm = reactive({
  registrationId: null as number | null,
  rating: 5,
  triageAccurate: true,
  comment: '',
});
const cancelReasons = reactive<Record<number, string>>({});

const selectedDepartment = computed(() => departments.value.find((item) => item.id === selectedDepartmentId.value) ?? null);
const selectedDoctor = computed(() => doctors.value.find((item) => item.id === selectedDoctorId.value) ?? null);
const visibleSchedules = computed(() =>
  selectedDoctorId.value ? schedules.value.filter((item) => item.doctorId === selectedDoctorId.value) : schedules.value,
);
const selectedSchedule = computed(() => visibleSchedules.value.find((item) => item.id === selectedScheduleId.value) ?? null);
const waitingRegistrations = computed(() => registrations.value.filter((item) => item.status === 'WAITING'));
const completedRegistrations = computed(() => registrations.value.filter((item) => item.status === 'COMPLETED'));
const latestRegistration = computed(() => registrations.value[0] ?? null);
const latestTriage = computed(() => triageHistory.value[0] ?? triageResult.value);
const cancelTargetRegistration = computed(() =>
  registrations.value.find((item) => item.id === cancelRegistrationId.value) ?? null,
);
const activeTone = computed(() => (error.value ? 'danger' : loading.value ? 'loading' : 'healthy'));
const displayName = computed(() => patient.value?.realName || patient.value?.username || authStore.sessionLabel);

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '未记录';
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
}

function formatDate(value: string | null | undefined) {
  return value || '未安排';
}

function truncate(value: string | null | undefined, length = 64) {
  if (!value) {
    return '暂无';
  }
  const compact = value.replace(/\s+/g, ' ').trim();
  return compact.length > length ? `${compact.slice(0, length)}...` : compact;
}

function syncDoctorSelection() {
  // Only keep the current selection if it is one of the triage-recommended doctors
  const recommended = triageResult.value?.recommendedDoctors ?? [];
  if (selectedDoctorId.value && recommended.some((item: DoctorOption) => item.id === selectedDoctorId.value)) {
    return;
  }
  // Otherwise, prefer the triage recommendation
  selectedDoctorId.value = recommended[0]?.id ?? doctors.value[0]?.id ?? null;
}

function syncScheduleSelection() {
  const items = visibleSchedules.value;
  if (selectedScheduleId.value && items.some((item) => item.id === selectedScheduleId.value)) {
    return;
  }
  selectedScheduleId.value = items[0]?.id ?? triageResult.value?.availableSchedules[0]?.id ?? null;
}

function applyTriageSelection(result: TriageResponse) {
  triageResult.value = result;
  // Clear old selections so the triage recommendation always takes priority
  selectedDepartmentId.value = result.recommendedDepartmentId ?? null;
  selectedDoctorId.value = null;
}

async function loadCatalog(departmentId: number | null) {
  const [departmentData, doctorData, scheduleData] = await Promise.all([
    listDepartments(),
    listDoctors(departmentId),
    listSchedules(departmentId),
  ]);

  const deptChanged = departmentId !== selectedDepartmentId.value;

  departments.value = departmentData;
  doctors.value = doctorData;
  schedules.value = scheduleData;

  // If the triage-recommended department is gone, fall back to first available
  if (departmentId !== null && !departmentData.some((item) => item.id === departmentId)) {
    departmentId = departmentData[0]?.id ?? null;
  }
  selectedDepartmentId.value = departmentId;

  // Clear doctor selection when department changes, then sync from triage or fallback
  if (deptChanged) {
    selectedDoctorId.value = null;
  }
  syncDoctorSelection();
  syncScheduleSelection();
}

async function loadPatientData() {
  const [profileData, triageData, registrationData, recordData, prescriptionData, feedbackData] = await Promise.all([
    getPatientInfo(),
    listTriageHistory(),
    listRegistrations(),
    listPatientMedicalRecords(),
    listPatientPrescriptions(),
    listPatientFeedback(),
  ]);

  patient.value = profileData;
  Object.assign(profileForm, {
    realName: profileData.realName ?? '',
    gender: profileData.gender ?? '',
    age: profileData.age === null || profileData.age === undefined ? '' : String(profileData.age),
    phone: profileData.phone ?? '',
    idCardNumber: profileData.idCardNumber ?? '',
    medicalHistory: profileData.medicalHistory ?? '',
    remark: profileData.remark ?? '',
  });

  triageHistory.value = triageData;
  registrations.value = registrationData;
  medicalRecords.value = recordData;
  prescriptions.value = prescriptionData;
  feedbacks.value = feedbackData;

  if (!triageResult.value && triageData[0]) {
    applyTriageSelection(triageData[0]);
  }

  if (!feedbackForm.registrationId) {
    feedbackForm.registrationId = completedRegistrations.value[0]?.id ?? latestRegistration.value?.id ?? null;
  }
}

async function refreshAll() {
  loading.value = true;
  error.value = '';
  try {
    await loadPatientData();
    await loadCatalog(selectedDepartmentId.value);
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '患者工作台加载失败');
  } finally {
    loading.value = false;
  }
}

async function chooseDepartment(departmentId: number | null) {
  selectedDepartmentId.value = departmentId;
  loading.value = true;
  error.value = '';
  try {
    await loadCatalog(departmentId);
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '加载科室失败');
  } finally {
    loading.value = false;
  }
}

function chooseDoctor(doctorId: number) {
  selectedDoctorId.value = doctorId;
  syncScheduleSelection();
}

function chooseSchedule(scheduleId: number) {
  selectedScheduleId.value = scheduleId;
}

async function runTriage() {
  if (!triageForm.chiefComplaint.trim()) {
    error.value = '请先填写主诉';
    return;
  }

  triaging.value = true;
  error.value = '';
  try {
    const result = await triageConsult({
      chiefComplaint: triageForm.chiefComplaint.trim(),
    });
    applyTriageSelection(result);
    triageHistory.value = [result, ...triageHistory.value.filter((item) => item.triageRecordId !== result.triageRecordId)];
    await loadCatalog(selectedDepartmentId.value);
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '分诊失败');
  } finally {
    triaging.value = false;
  }
}

async function confirmConversationTriageResult(payload: ConversationTriageConfirmRequest) {
  triaging.value = true;
  error.value = '';
  try {
    const result = await confirmConversationTriage(payload);
    applyTriageSelection(result);
    triageHistory.value = [result, ...triageHistory.value.filter((item) => item.triageRecordId !== result.triageRecordId)];
    await loadCatalog(selectedDepartmentId.value);
    return result;
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '对话分诊确认失败');
    throw cause;
  } finally {
    triaging.value = false;
  }
}

async function submitRegistration() {
  if (!selectedScheduleId.value) {
    error.value = '请先选择可用号源';
    return;
  }

  registering.value = true;
  error.value = '';
  try {
    const registration = await createRegistration({
      scheduleId: selectedScheduleId.value,
      triageRecordId: triageResult.value?.triageRecordId ?? null,
    });
    latestCreatedRegistration.value = registration;
    await refreshAll();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '挂号失败');
  } finally {
    registering.value = false;
    showRegistrationConfirm.value = false;
  }
}

function requestSubmitRegistration() {
  if (!selectedScheduleId.value) {
    error.value = '请先选择可用号源';
    return;
  }
  showRegistrationConfirm.value = true;
}

function closeRegistrationConfirm() {
  showRegistrationConfirm.value = false;
}

function dismissRegistrationSuccess() {
  latestCreatedRegistration.value = null;
}

async function saveProfile() {
  savingProfile.value = true;
  error.value = '';
  try {
    await updatePatientInfo({
      realName: profileForm.realName.trim() || null,
      gender: profileForm.gender.trim() || null,
      age: profileForm.age ? Number(profileForm.age) : null,
      phone: profileForm.phone.trim() || null,
      idCardNumber: profileForm.idCardNumber.trim() || null,
      medicalHistory: profileForm.medicalHistory.trim() || null,
      remark: profileForm.remark.trim() || null,
    });
    await refreshAll();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '保存患者信息失败');
  } finally {
    savingProfile.value = false;
  }
}

async function cancelWaitingRegistration(registrationId: number) {
  canceling.value = true;
  error.value = '';
  try {
    await cancelRegistration(registrationId, cancelReasons[registrationId]?.trim() || 'patient cancelled');
    delete cancelReasons[registrationId];
    await refreshAll();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '取消挂号失败');
  } finally {
    canceling.value = false;
  }
}

function requestCancelWaitingRegistration(registrationId: number) {
  const target = registrations.value.find((item) => item.id === registrationId);
  if (!target || target.status !== 'WAITING') {
    error.value = '只能取消待就诊的挂号';
    return;
  }
  cancelRegistrationId.value = registrationId;
}

async function confirmCancelWaitingRegistration() {
  if (!cancelRegistrationId.value) {
    return;
  }
  const registrationId = cancelRegistrationId.value;
  cancelRegistrationId.value = null;
  await cancelWaitingRegistration(registrationId);
}

function closeCancelRegistrationConfirm() {
  cancelRegistrationId.value = null;
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

async function submitFeedback() {
  if (!feedbackForm.registrationId) {
    error.value = '请先选择已完成的挂号';
    return;
  }

  submittingFeedback.value = true;
  error.value = '';
  try {
    await createFeedback({
      registrationId: feedbackForm.registrationId,
      rating: feedbackForm.rating,
      triageAccurate: feedbackForm.triageAccurate,
      comment: feedbackForm.comment.trim() || null,
    });
    feedbackForm.comment = '';
    await refreshAll();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '提交反馈失败');
  } finally {
    submittingFeedback.value = false;
  }
}

const workspace = reactive({
  loading,
  triaging,
  registering,
  savingProfile,
  submittingFeedback,
  canceling,
  detailLoading,
  error,
  patient,
  departments,
  doctors,
  schedules,
  triageHistory,
  registrations,
  medicalRecords,
  prescriptions,
  feedbacks,
  triageResult,
  cancelRegistrationId,
  detailKind,
  selectedMedicalRecord,
  selectedPrescription,
  showRegistrationConfirm,
  latestCreatedRegistration,
  selectedDepartmentId,
  selectedDoctorId,
  selectedScheduleId,
  triageForm,
  profileForm,
  feedbackForm,
  cancelReasons,
  selectedDepartment,
  selectedDoctor,
  visibleSchedules,
  selectedSchedule,
  waitingRegistrations,
  completedRegistrations,
  latestRegistration,
  latestTriage,
  cancelTargetRegistration,
  activeTone,
  displayName,
  chooseDepartment,
  chooseDoctor,
  chooseSchedule,
  runTriage,
  confirmConversationTriageResult,
  requestSubmitRegistration,
  closeRegistrationConfirm,
  dismissRegistrationSuccess,
  submitRegistration,
  saveProfile,
  cancelWaitingRegistration,
  requestCancelWaitingRegistration,
  confirmCancelWaitingRegistration,
  closeCancelRegistrationConfirm,
  viewMedicalRecordDetail,
  viewPrescriptionDetail,
  closeRecordPrescriptionDetail,
  submitFeedback,
  formatDateTime,
  formatDate,
  truncate,
});

onMounted(() => {
  void refreshAll();
});
</script>

<template>
  <PhoneFrame>
    <div class="flex flex-col h-full">
      <div class="flex-1 overflow-y-auto">
        <p v-if="error" class="mx-4 mt-3 p-2.5 rounded-md bg-red-50 text-danger text-xs">{{ error }}</p>

        <RouterView v-slot="{ Component: PanelComp }">
          <Suspense>
            <component :is="PanelComp" :workspace="workspace" v-if="PanelComp" />
          </Suspense>
        </RouterView>

        <LoadingSkeleton v-if="loading" :rows="3" class="p-4" />
      </div>

      <!-- Bottom tab bar -->
      <div class="shrink-0 bg-white border-t border-border flex">
        <RouterLink
          v-for="tab in tabs"
          :key="tab.id"
          :to="tab.path"
          class="flex-1 flex flex-col items-center gap-0.5 py-2 text-xs transition no-underline"
          :class="$route.path.startsWith(tab.path) ? 'text-brand' : 'text-text-secondary'"
        >
          <component :is="tab.icon" :size="18" />
          <span>{{ tab.label }}</span>
        </RouterLink>
      </div>
    </div>
  </PhoneFrame>
  <AiChatLauncher role="patient" />
  <ConfirmDialog
    :open="showRegistrationConfirm"
    title="确认挂号"
    :message="selectedSchedule ? `确认预约 ${selectedDoctor?.name || selectedSchedule.doctorName} ${formatDate(selectedSchedule.workDate)} ${selectedSchedule.period} 的号源吗？` : '确认提交当前挂号信息吗？'"
    level="info"
    confirm-label="确认挂号"
    :loading="registering"
    @confirm="void submitRegistration()"
    @cancel="closeRegistrationConfirm()"
  />
  <ConfirmDialog
    :open="cancelRegistrationId !== null"
    title="确认取消挂号"
    :message="cancelTargetRegistration ? `确认取消 ${cancelTargetRegistration.doctorName} ${formatDate(cancelTargetRegistration.workDate)} 的挂号吗？取消后号源将释放。` : '确认取消此挂号吗？取消后号源将释放。'"
    level="warning"
    confirm-label="确认取消"
    :loading="canceling"
    @confirm="void confirmCancelWaitingRegistration()"
    @cancel="closeCancelRegistrationConfirm()"
  />
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
