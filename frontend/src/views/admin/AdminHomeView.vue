<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import {
  BellRing,
  CirclePlus,
  RefreshCw,
} from 'lucide-vue-next';

import {
  adminListDepartments,
  adminListDoctors,
  adminListDrugs,
  adminCreateAiConfig,
  adminCreateDepartment,
  adminCreateDoctor,
  adminCreateDrug,
  adminCreatePromptTemplate,
  adminCreatePrescriptionRule,
  adminCreateSchedule,
  adminToggleAiConfig,
  adminToggleDepartment,
  adminToggleDoctor,
  adminToggleDrug,
  adminTogglePromptTemplate,
  adminTogglePrescriptionRule,
  adminToggleSchedule,
  adminUpdateAiConfig,
  adminUpdateDepartment,
  adminUpdateDoctor,
  adminUpdateDrug,
  adminUpdatePromptTemplate,
  adminUpdatePrescriptionRule,
  adminUpdateSchedule,
  getDashboardAiUsage,
  getDashboardOverview,
  getDashboardPrescriptionReviewRate,
  getDashboardRiskDistribution,
  getDashboardTrends,
  getDashboardTriageAccuracy,
  listAiCallRecords,
  listAiConfig,
  listAllSchedules,
  listAuditLogs,
  listUnreadNotifications,
  listPrescriptionRules,
  listPromptTemplates,
  markNotificationRead,
} from '@/api/workflow';
import { useAuthStore } from '@/stores/auth';
import AdminAuditPanel from '@/views/admin/panels/AdminAuditPanel.vue';
import AdminConfigPanel from '@/views/admin/panels/AdminConfigPanel.vue';
import AdminEditorPanel from '@/views/admin/panels/AdminEditorPanel.vue';
import AdminMasterPanel from '@/views/admin/panels/AdminMasterPanel.vue';
import AdminOverviewPanel from '@/views/admin/panels/AdminOverviewPanel.vue';
import AdminResourcesPanel from '@/views/admin/panels/AdminResourcesPanel.vue';
import type {
  AiCallRecordSummary,
  AiConfigSummary,
  AiConfigWriteRequest,
  AiUsageStats,
  AuditLogSummary,
  DashboardOverview,
  DashboardTrendPoint,
  DepartmentOption,
  DepartmentWriteRequest,
  DoctorOption,
  DoctorWriteRequest,
  DrugOption,
  DrugWriteRequest,
  NotificationRecordSummary,
  PrescriptionReviewRate,
  PrescriptionRuleSummary,
  PrescriptionRuleWriteRequest,
  PromptTemplateSummary,
  PromptTemplateWriteRequest,
  RiskDistribution,
  ScheduleOption,
  ScheduleWriteRequest,
  TriageAccuracyStats,
} from '@/api/workflow';
import { resolveUiErrorMessage } from '@/utils/zh';

type ResourceKind = 'department' | 'doctor' | 'schedule' | 'drug' | 'rule' | 'ai' | 'prompt';

const authStore = useAuthStore();

const loading = ref(false);
const saving = ref(false);
const error = ref('');
const dashboard = ref<DashboardOverview | null>(null);
const dashboardTrends = ref<DashboardTrendPoint[]>([]);
const aiUsage = ref<AiUsageStats | null>(null);
const prescriptionReviewRate = ref<PrescriptionReviewRate | null>(null);
const riskDistribution = ref<RiskDistribution | null>(null);
const triageAccuracy = ref<TriageAccuracyStats | null>(null);
const departments = ref<DepartmentOption[]>([]);
const doctors = ref<DoctorOption[]>([]);
const schedules = ref<ScheduleOption[]>([]);
const drugs = ref<DrugOption[]>([]);
const rules = ref<PrescriptionRuleSummary[]>([]);
const aiConfigs = ref<AiConfigSummary[]>([]);
const promptTemplates = ref<PromptTemplateSummary[]>([]);
const aiRecords = ref<AiCallRecordSummary[]>([]);
const auditLogs = ref<AuditLogSummary[]>([]);
const notifications = ref<NotificationRecordSummary[]>([]);
const departmentFilter = ref<number | null>(null);
const doctorFilter = ref<number | null>(null);
const drugKeyword = ref('');
const currentKind = ref<ResourceKind>('department');
const currentId = ref<number | null>(null);
const notificationLoading = ref(false);
const ackingNotificationId = ref<number | null>(null);
const notificationSocketState = ref<'idle' | 'connecting' | 'connected' | 'closed'>('idle');
let notificationSocket: WebSocket | null = null;

const departmentForm = reactive<DepartmentWriteRequest>({
  code: '',
  name: '',
  type: '',
  description: '',
  status: 'ACTIVE',
});

const doctorForm = reactive<DoctorWriteRequest>({
  username: '',
  password: '',
  name: '',
  departmentId: 0,
  title: '',
  specialty: '',
  introduction: '',
  status: 'ACTIVE',
});

const scheduleForm = reactive<ScheduleWriteRequest>({
  doctorId: 0,
  departmentId: 0,
  workDate: new Date().toISOString().slice(0, 10),
  period: '上午',
  totalSlots: 20,
  remainingSlots: 20,
  visitLevel: '普通门诊',
  status: 'ACTIVE',
});

const drugForm = reactive<DrugWriteRequest>({
  code: '',
  name: '',
  pinyinCode: '',
  specification: '',
  dosageForm: '',
  packageUnit: '',
  manufacturer: '',
  unitPrice: null,
  defaultUsage: '',
  contraindications: '',
  precautions: '',
  indications: '',
  interactionSummary: '',
  status: 'ACTIVE',
});

const ruleForm = reactive<PrescriptionRuleWriteRequest>({
  ruleCode: '',
  ruleType: 'CONTRAINDICATION',
  applicableDrugs: '',
  applicableDiseases: '',
  applicablePopulations: '',
  conditionExpression: '',
  riskLevel: 'MEDIUM',
  alertMessage: '',
  suggestion: '',
  basis: '',
  seeded: false,
  validationStatus: 'VALID',
  status: 'ACTIVE',
});

const aiForm = reactive<AiConfigWriteRequest>({
  provider: 'LOCAL_RULE',
  modelName: 'local-simulator',
  apiUrl: '',
  apiKey: '',
  keyVersion: 'local',
  taskScope: 'ALL',
  timeoutSeconds: 15,
  defaultConfig: true,
  healthStatus: 'OK',
  status: 'ACTIVE',
  enabled: true,
  priority: 1,
  configVersion: 'v1',
});

const promptForm = reactive<PromptTemplateWriteRequest>({
  templateCode: '',
  taskType: 'TRIAGE',
  deptCode: '',
  templateBody: '',
  variableWhitelist: '',
  version: 1,
  defaultTemplate: true,
  status: 'ACTIVE',
});

const activeTone = computed(() => (error.value ? 'danger' : loading.value ? 'loading' : 'healthy'));
const unreadNotificationCount = computed(() => notifications.value.filter((item) => item.read !== true).length);
const selectedDepartment = computed(() => departments.value.find((item) => item.id === departmentFilter.value) ?? null);
const selectedDoctor = computed(() => doctors.value.find((item) => item.id === doctorFilter.value) ?? null);
const visibleSchedules = computed(() =>
  schedules.value.filter((item) => {
    return (departmentFilter.value === null || item.departmentId === departmentFilter.value) &&
      (doctorFilter.value === null || item.doctorId === doctorFilter.value);
  }),
);
const adminPanels = [
  { id: 'overview', label: '总览' },
  { id: 'master', label: '基础资料' },
  { id: 'resources', label: '排班 / 药品' },
  { id: 'config', label: '规则配置' },
  { id: 'audit', label: '审计记录' },
] as const;

const activeAdminPanel = ref<(typeof adminPanels)[number]['id']>('overview');
const adminPanelComponent = computed(() => {
  switch (activeAdminPanel.value) {
    case 'master':
      return AdminMasterPanel;
    case 'resources':
      return AdminResourcesPanel;
    case 'config':
      return AdminConfigPanel;
    case 'audit':
      return AdminAuditPanel;
    default:
      return AdminOverviewPanel;
  }
});

const adminWorkspace = reactive({
  authStore,
  loading,
  saving,
  error,
  dashboard,
  dashboardTrends,
  aiUsage,
  prescriptionReviewRate,
  riskDistribution,
  triageAccuracy,
  departments,
  doctors,
  schedules,
  drugs,
  rules,
  aiConfigs,
  promptTemplates,
  aiRecords,
  auditLogs,
  notifications,
  departmentFilter,
  doctorFilter,
  drugKeyword,
  currentKind,
  currentId,
  notificationLoading,
  ackingNotificationId,
  notificationSocketState,
  departmentForm,
  doctorForm,
  scheduleForm,
  drugForm,
  ruleForm,
  aiForm,
  promptForm,
  activeTone,
  unreadNotificationCount,
  selectedDepartment,
  selectedDoctor,
  visibleSchedules,
  adminPanels,
  activeAdminPanel,
  formatDateTime,
  formatStatus,
  truncate,
  riskTone,
  loadDashboardBundle,
  loadAll,
  loadNotifications,
  upsertNotification,
  connectNotificationSocket,
  closeNotificationSocket,
  ackNotification,
  syncCurrentSelection,
  saveCurrent,
  toggleCurrent,
  createNew,
  clearFilters,
  selectDepartment,
  selectDoctor,
  selectSchedule,
  selectDrug,
  selectRule,
  selectAi,
  selectPrompt,
  setActiveAdminPanel: (panel: (typeof adminPanels)[number]['id']) => {
    activeAdminPanel.value = panel;
  },
});

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '未记录';
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
}

function formatStatus(value: string | null | undefined) {
  return value || 'UNKNOWN';
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

function resetDepartmentForm(source?: DepartmentOption | null) {
  Object.assign(departmentForm, {
    code: source?.code ?? '',
    name: source?.name ?? '',
    type: source?.type ?? '',
    description: source?.description ?? '',
    status: source?.status ?? 'ACTIVE',
  });
}

function resetDoctorForm(source?: DoctorOption | null) {
  Object.assign(doctorForm, {
    username: source?.username ?? '',
    password: '',
    name: source?.name ?? '',
    departmentId: source?.departmentId ?? departments.value[0]?.id ?? 0,
    title: source?.title ?? '',
    specialty: source?.specialty ?? '',
    introduction: source?.introduction ?? '',
    status: source?.status ?? 'ACTIVE',
  });
}

function resetScheduleForm(source?: ScheduleOption | null) {
  Object.assign(scheduleForm, {
    doctorId: source?.doctorId ?? doctors.value[0]?.id ?? 0,
    departmentId: source?.departmentId ?? departments.value[0]?.id ?? 0,
    workDate: source?.workDate ?? new Date().toISOString().slice(0, 10),
    period: source?.period ?? '上午',
    totalSlots: source?.totalSlots ?? 20,
    remainingSlots: source?.remainingSlots ?? source?.totalSlots ?? 20,
    visitLevel: source?.visitLevel ?? '普通门诊',
    status: source?.status ?? 'ACTIVE',
  });
}

function resetDrugForm(source?: DrugOption | null) {
  Object.assign(drugForm, {
    code: source?.code ?? '',
    name: source?.name ?? '',
    pinyinCode: source?.pinyinCode ?? '',
    specification: source?.specification ?? '',
    dosageForm: source?.dosageForm ?? '',
    packageUnit: source?.packageUnit ?? '',
    manufacturer: source?.manufacturer ?? '',
    unitPrice: source?.unitPrice ?? null,
    defaultUsage: source?.defaultUsage ?? '',
    contraindications: source?.contraindications ?? '',
    precautions: source?.precautions ?? '',
    indications: source?.indications ?? '',
    interactionSummary: source?.interactionSummary ?? '',
    status: source?.status ?? 'ACTIVE',
  });
}

function resetRuleForm(source?: PrescriptionRuleSummary | null) {
  Object.assign(ruleForm, {
    ruleCode: source?.ruleCode ?? '',
    ruleType: source?.ruleType ?? 'CONTRAINDICATION',
    applicableDrugs: source?.applicableDrugs ?? '',
    applicableDiseases: source?.applicableDiseases ?? '',
    applicablePopulations: source?.applicablePopulations ?? '',
    conditionExpression: source?.conditionExpression ?? '',
    riskLevel: source?.riskLevel ?? 'MEDIUM',
    alertMessage: source?.alertMessage ?? '',
    suggestion: source?.suggestion ?? '',
    basis: source?.basis ?? '',
    seeded: source?.seeded ?? false,
    validationStatus: source?.validationStatus ?? 'VALID',
    status: source?.status ?? 'ACTIVE',
  });
}

function resetAiForm(source?: AiConfigSummary | null) {
  Object.assign(aiForm, {
    provider: source?.provider ?? 'LOCAL_RULE',
    modelName: source?.modelName ?? 'local-simulator',
    apiUrl: source?.apiUrl ?? '',
    apiKey: '',
    keyVersion: source?.keyVersion ?? 'local',
    taskScope: source?.taskScope ?? 'ALL',
    timeoutSeconds: source?.timeoutSeconds ?? 15,
    defaultConfig: source?.defaultConfig ?? true,
    healthStatus: source?.healthStatus ?? 'OK',
    status: source?.status ?? 'ACTIVE',
    enabled: source?.enabled ?? true,
    priority: source?.priority ?? 1,
    configVersion: source?.configVersion ?? 'v1',
  });
}

function resetPromptForm(source?: PromptTemplateSummary | null) {
  Object.assign(promptForm, {
    templateCode: source?.templateCode ?? '',
    taskType: source?.taskType ?? 'TRIAGE',
    deptCode: source?.deptCode ?? '',
    templateBody: source?.templateBody ?? '',
    variableWhitelist: source?.variableWhitelist ?? '',
    version: source?.version ?? 1,
    defaultTemplate: source?.defaultTemplate ?? true,
    status: source?.status ?? 'ACTIVE',
  });
}

function selectDepartment(item: DepartmentOption) {
  currentKind.value = 'department';
  currentId.value = item.id;
  resetDepartmentForm(item);
}

function selectDoctor(item: DoctorOption) {
  currentKind.value = 'doctor';
  currentId.value = item.id;
  resetDoctorForm(item);
}

function selectSchedule(item: ScheduleOption) {
  currentKind.value = 'schedule';
  currentId.value = item.id;
  resetScheduleForm(item);
}

function selectDrug(item: DrugOption) {
  currentKind.value = 'drug';
  currentId.value = item.id;
  resetDrugForm(item);
}

function selectRule(item: PrescriptionRuleSummary) {
  currentKind.value = 'rule';
  currentId.value = item.id;
  resetRuleForm(item);
}

function selectAi(item: AiConfigSummary) {
  currentKind.value = 'ai';
  currentId.value = item.id;
  resetAiForm(item);
}

function selectPrompt(item: PromptTemplateSummary) {
  currentKind.value = 'prompt';
  currentId.value = item.id;
  resetPromptForm(item);
}

function ensureDefaults() {
  if (!doctorForm.departmentId) {
    doctorForm.departmentId = departments.value[0]?.id ?? 0;
  }
  if (!scheduleForm.departmentId) {
    scheduleForm.departmentId = departments.value[0]?.id ?? 0;
  }
  if (!scheduleForm.doctorId) {
    scheduleForm.doctorId = doctors.value[0]?.id ?? 0;
  }
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

async function loadAll() {
  loading.value = true;
  error.value = '';
  try {
    const [
      ,
      departmentData,
      doctorData,
      scheduleData,
      drugData,
      ruleData,
      aiConfigData,
      promptData,
      aiRecordData,
      auditData,
      notificationData,
    ] = await Promise.all([
      loadDashboardBundle(),
      adminListDepartments(),
      adminListDoctors(departmentFilter.value),
      listAllSchedules(departmentFilter.value, doctorFilter.value),
      adminListDrugs(drugKeyword.value.trim() || undefined, null),
      listPrescriptionRules(),
      listAiConfig(),
      listPromptTemplates(),
      listAiCallRecords(),
      listAuditLogs(),
      listUnreadNotifications(),
    ]);

    departments.value = departmentData;
    doctors.value = doctorData;
    schedules.value = scheduleData;
    drugs.value = drugData;
    rules.value = ruleData;
    aiConfigs.value = aiConfigData;
    promptTemplates.value = promptData;
    aiRecords.value = aiRecordData;
    auditLogs.value = auditData;
    notifications.value = notificationData;
    ensureDefaults();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '管理工作台加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadNotifications() {
  notificationLoading.value = true;
  try {
    notifications.value = await listUnreadNotifications();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '告警补拉失败');
  } finally {
    notificationLoading.value = false;
  }
}

function upsertNotification(notification: NotificationRecordSummary) {
  notifications.value = [
    notification,
    ...notifications.value.filter((item) => item.id !== notification.id),
  ].slice(0, 30);
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

function closeNotificationSocket() {
  notificationSocket?.close();
  notificationSocket = null;
}

async function ackNotification(id: number) {
  ackingNotificationId.value = id;
  error.value = '';
  try {
    await markNotificationRead(id);
    notifications.value = notifications.value.filter((item) => item.id !== id);
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '标记告警已读失败');
  } finally {
    ackingNotificationId.value = null;
  }
}

function syncCurrentSelection() {
  if (!currentId.value) {
    return;
  }
  if (currentKind.value === 'department') {
    const item = departments.value.find((entry) => entry.id === currentId.value);
    if (item) resetDepartmentForm(item);
  } else if (currentKind.value === 'doctor') {
    const item = doctors.value.find((entry) => entry.id === currentId.value);
    if (item) resetDoctorForm(item);
  } else if (currentKind.value === 'schedule') {
    const item = schedules.value.find((entry) => entry.id === currentId.value);
    if (item) resetScheduleForm(item);
  } else if (currentKind.value === 'drug') {
    const item = drugs.value.find((entry) => entry.id === currentId.value);
    if (item) resetDrugForm(item);
  } else if (currentKind.value === 'rule') {
    const item = rules.value.find((entry) => entry.id === currentId.value);
    if (item) resetRuleForm(item);
  } else if (currentKind.value === 'ai') {
    const item = aiConfigs.value.find((entry) => entry.id === currentId.value);
    if (item) resetAiForm(item);
  } else {
    const item = promptTemplates.value.find((entry) => entry.id === currentId.value);
    if (item) resetPromptForm(item);
  }
}

async function saveCurrent() {
  saving.value = true;
  error.value = '';
  try {
    let savedId = currentId.value;
    if (currentKind.value === 'department') {
      const payload = { ...departmentForm, code: departmentForm.code.trim(), name: departmentForm.name.trim(), status: departmentForm.status.trim() } satisfies DepartmentWriteRequest;
      const saved = currentId.value
        ? await adminUpdateDepartment(currentId.value, payload)
        : await adminCreateDepartment(payload);
      savedId = saved.id;
    } else if (currentKind.value === 'doctor') {
      const payload = { ...doctorForm, username: doctorForm.username.trim(), name: doctorForm.name.trim(), status: doctorForm.status.trim(), password: doctorForm.password?.trim() || null } satisfies DoctorWriteRequest;
      const saved = currentId.value
        ? await adminUpdateDoctor(currentId.value, payload)
        : await adminCreateDoctor(payload);
      savedId = saved.id;
    } else if (currentKind.value === 'schedule') {
      const payload = {
        ...scheduleForm,
        workDate: scheduleForm.workDate,
        status: scheduleForm.status.trim(),
        remainingSlots: scheduleForm.remainingSlots ?? null,
      } satisfies ScheduleWriteRequest;
      const saved = currentId.value
        ? await adminUpdateSchedule(currentId.value, payload)
        : await adminCreateSchedule(payload);
      savedId = saved.id;
    } else if (currentKind.value === 'drug') {
      const payload = {
        ...drugForm,
        code: drugForm.code.trim(),
        name: drugForm.name.trim(),
        status: drugForm.status.trim(),
        unitPrice: drugForm.unitPrice ?? null,
      } satisfies DrugWriteRequest;
      const saved = currentId.value
        ? await adminUpdateDrug(currentId.value, payload)
        : await adminCreateDrug(payload);
      savedId = saved.id;
    } else if (currentKind.value === 'rule') {
      const payload = { ...ruleForm, ruleCode: ruleForm.ruleCode.trim(), ruleType: ruleForm.ruleType.trim(), status: ruleForm.status.trim() } satisfies PrescriptionRuleWriteRequest;
      const saved = currentId.value
        ? await adminUpdatePrescriptionRule(currentId.value, payload)
        : await adminCreatePrescriptionRule(payload);
      savedId = saved.id;
    } else if (currentKind.value === 'ai') {
      const payload = {
        ...aiForm,
        provider: aiForm.provider.trim(),
        modelName: aiForm.modelName.trim(),
        taskScope: aiForm.taskScope.trim(),
        status: aiForm.status.trim(),
        apiKey: aiForm.apiKey?.trim() || null,
      } satisfies AiConfigWriteRequest;
      const saved = currentId.value
        ? await adminUpdateAiConfig(currentId.value, payload)
        : await adminCreateAiConfig(payload);
      savedId = saved.id;
    } else {
      const payload = { ...promptForm, templateCode: promptForm.templateCode.trim(), taskType: promptForm.taskType.trim(), status: promptForm.status.trim() } satisfies PromptTemplateWriteRequest;
      const saved = currentId.value
        ? await adminUpdatePromptTemplate(currentId.value, payload)
        : await adminCreatePromptTemplate(payload);
      savedId = saved.id;
    }
    currentId.value = savedId;
    await loadAll();
    syncCurrentSelection();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '保存失败');
  } finally {
    saving.value = false;
  }
}

async function toggleCurrent() {
  if (!currentId.value) {
    return;
  }
  saving.value = true;
  error.value = '';
  try {
    if (currentKind.value === 'department') {
      await adminToggleDepartment(currentId.value);
    } else if (currentKind.value === 'doctor') {
      await adminToggleDoctor(currentId.value);
    } else if (currentKind.value === 'schedule') {
      await adminToggleSchedule(currentId.value);
    } else if (currentKind.value === 'drug') {
      await adminToggleDrug(currentId.value);
    } else if (currentKind.value === 'rule') {
      await adminTogglePrescriptionRule(currentId.value);
    } else if (currentKind.value === 'ai') {
      await adminToggleAiConfig(currentId.value);
    } else {
      await adminTogglePromptTemplate(currentId.value);
    }
    await loadAll();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '切换状态失败');
  } finally {
    saving.value = false;
  }
}

function createNew(kind: ResourceKind) {
  currentKind.value = kind;
  currentId.value = null;
  if (kind === 'department') resetDepartmentForm();
  if (kind === 'doctor') resetDoctorForm();
  if (kind === 'schedule') resetScheduleForm();
  if (kind === 'drug') resetDrugForm();
  if (kind === 'rule') resetRuleForm();
  if (kind === 'ai') resetAiForm();
  if (kind === 'prompt') resetPromptForm();
}

function clearFilters() {
  departmentFilter.value = null;
  doctorFilter.value = null;
  drugKeyword.value = '';
  void loadAll();
}

watch([departments, doctors], () => {
  ensureDefaults();
});

onMounted(() => {
  void loadAll();
  connectNotificationSocket();
});

onBeforeUnmount(() => {
  closeNotificationSocket();
});
</script>

<template>
  <section class="page">
    <div class="band workspace-shell">
      <div class="band-header">
        <div>
          <h2 class="band-title">管理工作台</h2>
          <p class="band-copy">基础数据、排班、配置和审计分区展示，维护时更像后台，不再像大杂烩单页。</p>
        </div>
        <span class="status-chip" :data-tone="activeTone">
          <span class="chip-dot" />
          <span>{{ authStore.sessionLabel }}</span>
        </span>
      </div>

      <div class="toolbar workspace-topline">
        <span class="pill" :data-tone="notificationSocketState === 'connected' ? 'healthy' : 'loading'">
          <BellRing :size="14" />
          <span>WS {{ notificationSocketState }}</span>
        </span>
        <button class="button-ghost" type="button" @click="loadAll" :disabled="loading">
          <RefreshCw :size="16" :class="{ spinning: loading }" />
          <span>刷新</span>
        </button>
        <button class="button-secondary" type="button" @click="createNew('department')">
          <CirclePlus :size="16" />
          <span>新建科室</span>
        </button>
        <button class="button-secondary" type="button" @click="createNew('doctor')">
          <CirclePlus :size="16" />
          <span>新建医生</span>
        </button>
        <button class="button-secondary" type="button" @click="createNew('schedule')">
          <CirclePlus :size="16" />
          <span>新建排班</span>
        </button>
        <button class="button-secondary" type="button" @click="createNew('drug')">
          <CirclePlus :size="16" />
          <span>新建药品</span>
        </button>
      </div>

      <div class="field-grid" style="margin-top: 0.75rem;">
        <label class="field">
          <span>科室筛选</span>
          <select v-model="departmentFilter">
            <option :value="null">全部科室</option>
            <option v-for="department in departments" :key="department.id" :value="department.id">
              {{ department.name }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>医生筛选</span>
          <select v-model="doctorFilter">
            <option :value="null">全部医生</option>
            <option v-for="doctor in doctors" :key="doctor.id" :value="doctor.id">
              {{ doctor.name }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>药品关键词</span>
          <input v-model="drugKeyword" placeholder="按药品名或拼音筛选" />
        </label>
        <div class="action-row" style="align-self: end;">
          <button class="button-secondary" type="button" @click="loadAll" :disabled="loading">
            <RefreshCw :size="16" :class="{ spinning: loading }" />
            <span>应用筛选</span>
          </button>
          <button class="button-ghost" type="button" @click="clearFilters">
            <span>清空</span>
          </button>
        </div>
      </div>

      <p class="auth-error" v-if="error">{{ error }}</p>

      <div class="segmented workspace-tabs">
        <button
          v-for="panel in adminPanels"
          :key="panel.id"
          type="button"
          class="segment"
          :class="{ active: activeAdminPanel === panel.id }"
          @click="activeAdminPanel = panel.id"
        >
          <span>{{ panel.label }}</span>
        </button>
      </div>

      <component :is="adminPanelComponent" :workspace="adminWorkspace" />
      <AdminEditorPanel :workspace="adminWorkspace" v-show="activeAdminPanel !== 'overview'" />
    </div>
  </section>
</template>
