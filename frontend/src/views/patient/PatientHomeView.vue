<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { CalendarDays, FileText, RefreshCw, Stethoscope, Ticket, UserRound } from 'lucide-vue-next';

import {
  cancelRegistration,
  createFeedback,
  createRegistration,
  getPatientInfo,
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
} from '@/api/workflow';
import { useAuthStore } from '@/stores/auth';
import { resolveUiErrorMessage } from '@/utils/zh';
import PatientHistoryPanel from '@/views/patient/panels/PatientHistoryPanel.vue';
import PatientOverviewPanel from '@/views/patient/panels/PatientOverviewPanel.vue';
import PatientProfilePanel from '@/views/patient/panels/PatientProfilePanel.vue';
import PatientRecordsPanel from '@/views/patient/panels/PatientRecordsPanel.vue';
import PatientRegistrationPanel from '@/views/patient/panels/PatientRegistrationPanel.vue';
import PatientTriagePanel from '@/views/patient/panels/PatientTriagePanel.vue';

const authStore = useAuthStore();

const loading = ref(false);
const triaging = ref(false);
const registering = ref(false);
const savingProfile = ref(false);
const submittingFeedback = ref(false);
const canceling = ref(false);
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
const activeTone = computed(() => (error.value ? 'danger' : loading.value ? 'loading' : 'healthy'));
const displayName = computed(() => patient.value?.realName || patient.value?.username || authStore.sessionLabel);

const patientPanels = [
  { id: 'overview', label: '总览' },
  { id: 'triage', label: '分诊' },
  { id: 'registration', label: '挂号' },
  { id: 'records', label: '病历' },
  { id: 'profile', label: '资料' },
  { id: 'history', label: '历史' },
] as const;

const activePatientPanel = ref<(typeof patientPanels)[number]['id']>('overview');
const patientPanelComponent = computed(() => {
  switch (activePatientPanel.value) {
    case 'triage':
      return PatientTriagePanel;
    case 'registration':
      return PatientRegistrationPanel;
    case 'records':
      return PatientRecordsPanel;
    case 'profile':
      return PatientProfilePanel;
    case 'history':
      return PatientHistoryPanel;
    default:
      return PatientOverviewPanel;
  }
});

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

function syncScheduleSelection() {
  const items = visibleSchedules.value;
  if (selectedScheduleId.value && items.some((item) => item.id === selectedScheduleId.value)) {
    return;
  }
  selectedScheduleId.value = items[0]?.id ?? triageResult.value?.availableSchedules[0]?.id ?? null;
}

function syncDoctorSelection() {
  if (selectedDoctorId.value && doctors.value.some((item) => item.id === selectedDoctorId.value)) {
    return;
  }
  selectedDoctorId.value = triageResult.value?.recommendedDoctors[0]?.id ?? doctors.value[0]?.id ?? null;
}

function applyTriageSelection(result: TriageResponse) {
  triageResult.value = result;
  if (result.recommendedDepartmentId !== null) {
    selectedDepartmentId.value = result.recommendedDepartmentId;
  }
  syncDoctorSelection();
  selectedScheduleId.value = result.availableSchedules[0]?.id ?? selectedScheduleId.value;
}

async function loadCatalog() {
  const [departmentData, doctorData, scheduleData] = await Promise.all([
    listDepartments(),
    listDoctors(selectedDepartmentId.value),
    listSchedules(selectedDepartmentId.value),
  ]);

  departments.value = departmentData;
  doctors.value = doctorData;
  schedules.value = scheduleData;

  if (selectedDepartmentId.value !== null && !departmentData.some((item) => item.id === selectedDepartmentId.value)) {
    selectedDepartmentId.value = departmentData[0]?.id ?? null;
  }

  if (selectedDoctorId.value !== null && !doctorData.some((item) => item.id === selectedDoctorId.value)) {
    selectedDoctorId.value = triageResult.value?.recommendedDoctors[0]?.id ?? doctorData[0]?.id ?? null;
  } else if (selectedDoctorId.value === null) {
    syncDoctorSelection();
  }

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
    await loadCatalog();
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
    await loadCatalog();
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
    await loadCatalog();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '分诊失败');
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
    await createRegistration({
      scheduleId: selectedScheduleId.value,
      triageRecordId: triageResult.value?.triageRecordId ?? null,
    });
    await refreshAll();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '挂号失败');
  } finally {
    registering.value = false;
  }
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
  activeTone,
  displayName,
  setActivePatientPanel: (panel: (typeof patientPanels)[number]['id']) => {
    activePatientPanel.value = panel;
  },
  chooseDepartment,
  chooseDoctor,
  chooseSchedule,
  runTriage,
  submitRegistration,
  saveProfile,
  cancelWaitingRegistration,
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
  <section class="page patient-page">
    <div class="band workspace-shell">
      <div class="band-header">
        <div>
          <h2 class="band-title">患者工作区</h2>
          <p class="band-copy">分诊、挂号、病历、资料和历史分区展示，手机端也能顺着流程操作。</p>
        </div>
        <span class="status-chip" :data-tone="activeTone">
          <span class="chip-dot" />
          <span>{{ displayName }}</span>
        </span>
      </div>

      <div class="toolbar workspace-topline">
        <span class="pill" :data-tone="triageResult ? 'healthy' : 'loading'">
          <FileText :size="14" />
          <span>{{ triageResult ? '已分诊' : '待分诊' }}</span>
        </span>
        <span class="pill">
          <Stethoscope :size="14" />
          <span>{{ triageResult?.recommendedDoctors.length ?? 0 }} 位推荐医生</span>
        </span>
        <span class="pill">
          <Ticket :size="14" />
          <span>{{ waitingRegistrations.length }} 个待处理挂号</span>
        </span>
        <span class="pill">
          <CalendarDays :size="14" />
          <span>{{ completedRegistrations.length }} 个已完成就诊</span>
        </span>
        <span class="pill">
          <UserRound :size="14" />
          <span>{{ patient?.patientId ?? authStore.patientId ?? '未知患者' }}</span>
        </span>
        <button class="button-ghost" type="button" @click="refreshAll" :disabled="loading">
          <RefreshCw :size="16" :class="{ spinning: loading }" />
          <span>刷新</span>
        </button>
      </div>

      <p class="auth-error" v-if="error">{{ error }}</p>

      <div class="segmented workspace-tabs">
        <button
          v-for="panel in patientPanels"
          :key="panel.id"
          type="button"
          class="segment"
          :class="{ active: activePatientPanel === panel.id }"
          @click="activePatientPanel = panel.id"
        >
          <span>{{ panel.label }}</span>
        </button>
      </div>

      <component :is="patientPanelComponent" :workspace="workspace" />
    </div>
  </section>
</template>
