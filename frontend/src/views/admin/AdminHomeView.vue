<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import {
  LayoutDashboard,
  Building2,
  Layers,
  Settings,
  ShieldCheck,
  RefreshCw,
} from 'lucide-vue-next';
import SideNav from '@/components/layout/SideNav.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import { usePagination } from '@/composables/usePagination';
import AdminEditorPanel from './panels/AdminEditorPanel.vue';
import { useRoute } from 'vue-router';

import {
  adminBatchCreateSchedules,
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
import { useToast } from '@/composables/useToast';
import ConfirmDialog from '@/components/shared/ConfirmDialog.vue';
import type {
  AiCallRecordSummary,
  AiConfigSummary,
  AiConfigWriteRequest,
  AiUsageStats,
  AuditLogSummary,
  BatchScheduleRequest,
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
type CurrentKind = ResourceKind | '';
type AiRecordTaskType = 'ALL' | 'TRIAGE' | 'MEDICAL_RECORD' | 'DIAGNOSIS' | 'PRESCRIPTION_REVIEW';

const authStore = useAuthStore();
const route = useRoute();
const toast = useToast();

const loading = ref(false);
const saving = ref(false);
const error = ref('');
const confirmOpen = ref(false);
const confirmTitle = ref('');
const confirmMessage = ref('');
const confirmAction = ref<(() => void) | null>(null);
const confirmLoading = ref(false);
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
const scheduleFromDate = ref('');
const scheduleToDate = ref('');
const drugKeyword = ref('');
const editorPanelRef = ref<HTMLElement | null>(null);
const currentKind = ref<CurrentKind>('');
const currentId = ref<number | null>(null);
const notificationLoading = ref(false);
const ackingNotificationId = ref<number | null>(null);
const batchCreatingSchedules = ref(false);
const batchScheduleMessage = ref('');
const aiRecordTaskType = ref<AiRecordTaskType>('ALL');
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
  workDate: toDateInputValue(new Date()),
  period: '上午',
  totalSlots: 20,
  remainingSlots: 20,
  visitLevel: '普通门诊',
  status: 'ACTIVE',
});

const batchScheduleForm = reactive({
  doctorId: 0,
  departmentId: 0,
  startDate: toDateInputValue(new Date()),
  endDate: toDateInputValue(addDays(new Date(), 6)),
  weekdays: [1, 2, 3, 4, 5] as number[],
  periods: ['上午', '下午'] as string[],
  totalSlots: 20,
  remainingSlots: 20 as number | null,
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
      (doctorFilter.value === null || item.doctorId === doctorFilter.value) &&
      (!scheduleFromDate.value || item.workDate >= scheduleFromDate.value) &&
      (!scheduleToDate.value || item.workDate <= scheduleToDate.value);
  }),
);
const paginatedDepartments = usePagination(computed(() => departments.value), 8);
const paginatedDoctors = usePagination(computed(() => doctors.value), 8);
const paginatedDrugs = usePagination(computed(() => drugs.value), 8);
const paginatedRules = usePagination(computed(() => rules.value), 8);
const paginatedAiConfigs = usePagination(computed(() => aiConfigs.value), 8);
const paginatedPromptTemplates = usePagination(computed(() => promptTemplates.value), 8);
const paginatedSchedules = usePagination(visibleSchedules, 8);
const paginatedAiRecords = usePagination(computed(() => aiRecords.value), 8);
const paginatedAuditLogs = usePagination(computed(() => auditLogs.value), 8);
const batchSchedulePreviewDates = computed(() => {
  if (!batchScheduleForm.startDate || !batchScheduleForm.endDate || batchScheduleForm.weekdays.length === 0) {
    return [];
  }
  const start = new Date(`${batchScheduleForm.startDate}T00:00:00`);
  const end = new Date(`${batchScheduleForm.endDate}T00:00:00`);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || start > end) {
    return [];
  }
  const selectedWeekdays = new Set(batchScheduleForm.weekdays);
  const dates: string[] = [];
  for (const cursor = new Date(start); cursor <= end; cursor.setDate(cursor.getDate() + 1)) {
    if (selectedWeekdays.has(cursor.getDay())) {
      dates.push(toDateInputValue(cursor));
    }
  }
  return dates;
});
const batchSchedulePreviewCount = computed(() => batchSchedulePreviewDates.value.length * batchScheduleForm.periods.length);
const editorVisible = computed(() => {
  if (!currentKind.value) {
    return false;
  }
  if (route.name === 'admin-master-data') {
    return ['department', 'doctor', 'drug'].includes(currentKind.value);
  }
  if (route.name === 'admin-resources') {
    return currentKind.value === 'schedule';
  }
  if (route.name === 'admin-config') {
    return ['rule', 'ai', 'prompt'].includes(currentKind.value);
  }
  return false;
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
  scheduleFromDate,
  scheduleToDate,
  drugKeyword,
  currentKind,
  currentId,
  notificationLoading,
  ackingNotificationId,
  batchCreatingSchedules,
  batchScheduleMessage,
  aiRecordTaskType,
  notificationSocketState,
  departmentForm,
  doctorForm,
  scheduleForm,
  batchScheduleForm,
  drugForm,
  ruleForm,
  aiForm,
  promptForm,
  activeTone,
  unreadNotificationCount,
  selectedDepartment,
  selectedDoctor,
  visibleSchedules,
  paginatedDepartments,
  paginatedDoctors,
  paginatedDrugs,
  paginatedRules,
  paginatedAiConfigs,
  paginatedPromptTemplates,
  paginatedSchedules,
  paginatedAiRecords,
  paginatedAuditLogs,
  batchSchedulePreviewDates,
  batchSchedulePreviewCount,
  editorVisible,
  formatDateTime,
  formatStatus,
  formatPromptDeptCode,
  truncate,
  riskTone,
  loadDashboardBundle,
  loadAll,
  loadNotifications,
  upsertNotification,
  connectNotificationSocket,
  closeNotificationSocket,
  ackNotification,
  loadAiRecords,
  changeAiRecordTaskType,
  createBatchSchedules,
  syncCurrentSelection,
  saveCurrent,
  toggleCurrent,
  requestToggleCurrent,
  createNew,
  clearFilters,
  selectDepartment,
  selectDoctor,
  selectSchedule,
  selectDrug,
  selectRule,
  selectAi,
  selectPrompt,
  closeEditor,
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

function formatPromptDeptCode(value: string | null | undefined) {
  const code = value?.trim();
  return code || '全局模板';
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

function toDateInputValue(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function addDays(date: Date, days: number) {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
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
  const fallbackDoctor = doctors.value.find((item) => item.departmentId === departments.value[0]?.id) ?? doctors.value[0] ?? null;
  const fallbackDepartmentId = fallbackDoctor?.departmentId ?? departments.value[0]?.id ?? 0;
  Object.assign(scheduleForm, {
    doctorId: source?.doctorId ?? fallbackDoctor?.id ?? 0,
    departmentId: source?.departmentId ?? fallbackDepartmentId,
    workDate: source?.workDate ?? toDateInputValue(new Date()),
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
    deptCode: source?.deptCode?.trim() ?? '',
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
  revealEditor();
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

function closeEditor() {
  currentId.value = null;
  currentKind.value = '';
}

function revealEditor() {
  void nextTick(() => {
    editorPanelRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    editorPanelRef.value?.focus({ preventScroll: true });
  });
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
  if (!batchScheduleForm.departmentId) {
    batchScheduleForm.departmentId = departments.value[0]?.id ?? 0;
  }
  if (!batchScheduleForm.doctorId) {
    const defaultDoctor = doctors.value.find((item) => item.departmentId === batchScheduleForm.departmentId) ?? doctors.value[0];
    batchScheduleForm.doctorId = defaultDoctor?.id ?? 0;
  }
}

async function loadAiRecords() {
  const taskType = aiRecordTaskType.value === 'ALL' ? null : aiRecordTaskType.value;
  aiRecords.value = await listAiCallRecords(taskType);
  paginatedAiRecords.resetPage();
}

async function changeAiRecordTaskType(taskType: AiRecordTaskType) {
  aiRecordTaskType.value = taskType;
  error.value = '';
  try {
    await loadAiRecords();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, 'AI调用记录加载失败');
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
      listAllSchedules(
        departmentFilter.value,
        doctorFilter.value,
        scheduleFromDate.value || null,
        scheduleToDate.value || null,
      ),
      adminListDrugs(drugKeyword.value.trim() || undefined, null),
      listPrescriptionRules(),
      listAiConfig(),
      listPromptTemplates(),
      listAiCallRecords(aiRecordTaskType.value === 'ALL' ? null : aiRecordTaskType.value),
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

async function createBatchSchedules() {
  error.value = '';
  batchScheduleMessage.value = '';
  if (!batchScheduleForm.departmentId || !batchScheduleForm.doctorId) {
    error.value = '请选择科室和医生后再生成排班';
    return;
  }
  if (!batchSchedulePreviewDates.value.length || !batchScheduleForm.periods.length) {
    error.value = '请选择有效的日期范围、星期和时段';
    return;
  }
  const totalSlots = Number(batchScheduleForm.totalSlots);
  const remainingSlots = batchScheduleForm.remainingSlots === null || batchScheduleForm.remainingSlots === undefined || Number.isNaN(Number(batchScheduleForm.remainingSlots))
    ? null
    : Number(batchScheduleForm.remainingSlots);
  if (!Number.isInteger(totalSlots) || totalSlots < 1) {
    error.value = '总号源必须是大于 0 的整数';
    return;
  }
  if (remainingSlots !== null && (!Number.isInteger(remainingSlots) || remainingSlots < 0)) {
    error.value = '剩余号源必须是非负整数';
    return;
  }
  if (remainingSlots !== null && remainingSlots > totalSlots) {
    error.value = '剩余号源不能大于总号源';
    return;
  }
  const payload: BatchScheduleRequest = {
    doctorId: batchScheduleForm.doctorId,
    departmentId: batchScheduleForm.departmentId,
    workDates: batchSchedulePreviewDates.value,
    periods: batchScheduleForm.periods,
    totalSlots,
    remainingSlots,
    visitLevel: batchScheduleForm.visitLevel.trim(),
    status: batchScheduleForm.status.trim(),
  };
  batchCreatingSchedules.value = true;
  try {
    const created = await adminBatchCreateSchedules(payload);
    batchScheduleMessage.value = `已生成 ${created.length} 条排班`;
    await loadAll();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '批量生成排班失败');
  } finally {
    batchCreatingSchedules.value = false;
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
    } else if (currentKind.value === 'prompt') {
      const payload = {
        ...promptForm,
        templateCode: promptForm.templateCode.trim(),
        taskType: promptForm.taskType.trim(),
        deptCode: promptForm.deptCode?.trim() || null,
        status: promptForm.status.trim(),
      } satisfies PromptTemplateWriteRequest;
      const saved = currentId.value
        ? await adminUpdatePromptTemplate(currentId.value, payload)
        : await adminCreatePromptTemplate(payload);
      savedId = saved.id;
    } else {
      return;
    }
    currentId.value = savedId;
    toast.success(currentId.value ? '保存成功' : '创建成功');
    await loadAll();
    syncCurrentSelection();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '保存失败');
  } finally {
    saving.value = false;
  }
}

function requestToggleCurrent() {
  if (!currentId.value) return;
  confirmTitle.value = '确认切换状态';
  confirmMessage.value = '确认要切换该资源的状态吗？';
  confirmAction.value = () => toggleCurrent();
  confirmOpen.value = true;
}

async function toggleCurrent() {
  if (!currentId.value) return;
  confirmOpen.value = false;
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
    toast.success('状态已更新');
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
  revealEditor();
}

function clearFilters() {
  departmentFilter.value = null;
  doctorFilter.value = null;
  scheduleFromDate.value = '';
  scheduleToDate.value = '';
  drugKeyword.value = '';
  paginatedDepartments.resetPage();
  paginatedDoctors.resetPage();
  paginatedDrugs.resetPage();
  paginatedRules.resetPage();
  paginatedAiConfigs.resetPage();
  paginatedPromptTemplates.resetPage();
  paginatedSchedules.resetPage();
  paginatedAiRecords.resetPage();
  paginatedAuditLogs.resetPage();
  void loadAll();
}

watch([departments, doctors], () => {
  ensureDefaults();
});

watch(() => batchScheduleForm.doctorId, (doctorId) => {
  const doctor = doctors.value.find((item) => item.id === doctorId);
  if (doctor?.departmentId && batchScheduleForm.departmentId !== doctor.departmentId) {
    batchScheduleForm.departmentId = doctor.departmentId;
  }
});

watch(() => batchScheduleForm.departmentId, (departmentId) => {
  const currentDoctor = doctors.value.find((item) => item.id === batchScheduleForm.doctorId);
  if (currentDoctor?.departmentId === departmentId) {
    return;
  }
  batchScheduleForm.doctorId = doctors.value.find((item) => item.departmentId === departmentId)?.id ?? doctors.value[0]?.id ?? 0;
});

watch([departmentFilter, doctorFilter, scheduleFromDate, scheduleToDate, drugKeyword], () => {
  paginatedDepartments.resetPage();
  paginatedDoctors.resetPage();
  paginatedDrugs.resetPage();
  paginatedRules.resetPage();
  paginatedAiConfigs.resetPage();
  paginatedPromptTemplates.resetPage();
  paginatedSchedules.resetPage();
  paginatedAiRecords.resetPage();
  paginatedAuditLogs.resetPage();
});

watch(() => route.name, () => {
  if (!editorVisible.value) {
    closeEditor();
  }
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
  <div class="flex flex-1 min-h-0">
    <SideNav
      title="管理工作台"
      :subtitle="authStore.sessionLabel"
      :items="[
        { id: 'overview', label: '总览', path: '/admin/overview', icon: LayoutDashboard },
        { id: 'master', label: '基础数据', path: '/admin/master-data', icon: Building2 },
        { id: 'resources', label: '排班与资源', path: '/admin/resources', icon: Layers },
        { id: 'config', label: '配置', path: '/admin/config', icon: Settings },
        { id: 'audit', label: '审计', path: '/admin/audit', icon: ShieldCheck },
      ]"
    />

    <div class="flex-1 overflow-y-auto p-6">
      <p v-if="error" class="mb-4 p-3 rounded-md bg-red-50 text-danger text-sm">{{ error }}
          <button class="ml-2 underline cursor-pointer hover:text-red-800" @click="error = ''">关闭</button>
        </p>

      <div class="flex items-center gap-3 mb-6 flex-wrap">
        <span class="flex-1" />
        <button class="btn-ghost" type="button" @click="loadAll" :disabled="loading">
          <RefreshCw :size="16" :class="{ 'animate-spin': loading }" />
          <span>{{ loading ? '加载中' : '刷新' }}</span>
        </button>
      </div>

      <RouterView v-slot="{ Component: PanelComp }">
        <Suspense>
          <component :is="PanelComp" :workspace="adminWorkspace" v-if="PanelComp" />
        </Suspense>
      </RouterView>

      <div v-if="adminWorkspace.editorVisible" ref="editorPanelRef" class="mt-6 outline-none" tabindex="-1">
        <AdminEditorPanel :workspace="adminWorkspace" />
      </div>
    </div>
  </div>
  <ConfirmDialog
    :open="confirmOpen"
    :title="confirmTitle"
    :message="confirmMessage"
    level="warning"
    confirm-label="确认"
    :loading="saving"
    @confirm="confirmAction?.()"
    @cancel="confirmOpen = false"
  />
</template>
