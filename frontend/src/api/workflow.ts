import { http } from './http';
import type { PatientInfoResponse, PatientUpdateRequest, Result } from '@/types/api';

export type DepartmentOption = {
  id: number;
  code: string;
  name: string;
  type: string | null;
  description: string | null;
  status?: string;
  doctorCount?: number;
  activeScheduleCount?: number;
  updatedAt?: string | null;
};

export type DrugOption = {
  id: number;
  code: string;
  name: string;
  pinyinCode: string | null;
  specification: string | null;
  dosageForm: string | null;
  packageUnit: string | null;
  manufacturer: string | null;
  unitPrice: number | null;
  defaultUsage: string | null;
  contraindications: string | null;
  precautions?: string | null;
  indications: string | null;
  interactionSummary?: string | null;
  status?: string;
  updatedAt?: string | null;
};

export type DoctorOption = {
  id: number;
  username: string;
  name: string;
  departmentId: number | null;
  departmentName: string | null;
  title: string | null;
  specialty: string | null;
  introduction: string | null;
  status?: string;
  scheduleCount?: number;
  updatedAt?: string | null;
};

export type ScheduleOption = {
  id: number;
  doctorId: number;
  doctorName: string | null;
  departmentId: number;
  departmentName: string | null;
  workDate: string;
  period: string;
  totalSlots: number | null;
  remainingSlots: number | null;
  visitLevel: string | null;
  status: string;
  updatedAt?: string | null;
};

export type TriageRequest = {
  chiefComplaint: string;
};

export type TriageResponse = {
  triageRecordId: number;
  chiefComplaint: string;
  recommendedDepartmentId: number | null;
  recommendedDept: string;
  recommendedDoctors: DoctorOption[];
  availableSchedules: ScheduleOption[];
  reason: string;
  callStatus: string;
  recommendationSource: string;
  aiRecommendedDepartmentId: number | null;
  aiRecommendedDept: string | null;
  degraded: boolean;
};

export type ConversationTriageConfirmRequest = {
  chiefComplaint: string;
  department?: string | null;
  departmentCode?: string | null;
  reason?: string | null;
  urgencyLevel?: string | null;
};

export type RegistrationCreateRequest = {
  scheduleId: number;
  triageRecordId?: number | null;
};

export type RegistrationSummary = {
  id: number;
  patientId: number;
  patientName: string | null;
  doctorId: number;
  doctorName: string | null;
  departmentId: number;
  departmentName: string | null;
  scheduleId: number;
  workDate: string | null;
  period: string | null;
  visitLevel: string | null;
  status: string;
  triageRecordId: number | null;
  chiefComplaint: string | null;
  registrationTime: string | null;
  consultationStartTime: string | null;
  recordConfirmedTime: string | null;
  prescriptionSubmittedTime: string | null;
  completedTime: string | null;
  medicalRecordId: number | null;
  prescriptionId: number | null;
  riskLevel: string | null;
};

export type MedicalRecordGenerateRequest = {
  registrationId: number;
  conversationText: string;
  diagnosisDirection?: string | null;
};

export type MedicalRecordSaveRequest = {
  registrationId: number;
  conversationText?: string | null;
  chiefComplaint?: string | null;
  presentIllness?: string | null;
  pastHistory?: string | null;
  physicalExam?: string | null;
  preliminaryDiagnosis?: string | null;
  treatmentPlan?: string | null;
  docNote?: string | null;
  aiGenerated?: boolean | null;
};

export type MedicalRecordSummary = {
  id: number;
  registrationId: number;
  patientId: number;
  patientName: string | null;
  doctorId: number;
  doctorName: string | null;
  departmentName: string | null;
  chiefComplaint: string | null;
  presentIllness: string | null;
  pastHistory: string | null;
  physicalExam: string | null;
  preliminaryDiagnosis: string | null;
  treatmentPlan: string | null;
  conversationText: string | null;
  docNote: string | null;
  aiGenerated: boolean | null;
  version: number | null;
  createdAt: string | null;
  degraded: boolean;
};

export type DiagnosisSuggestionRequest = {
  registrationId: number;
  conversationText: string;
  diagnosisDirection?: string | null;
};

export type DiagnosisSuggestionResponse = {
  id: number;
  registrationId: number;
  suggestedDiagnoses: string;
  suggestedExamItems: string;
  adoptionStatus: string;
  summary: string;
  degraded: boolean;
};

export type DiagnosisSuggestionAdoptRequest = {
  finalDiagnosis: string;
};

export type DiagnosisSuggestionIgnoreRequest = {
  reason?: string | null;
};

export type PrescriptionItemRequest = {
  drugId: number;
  dosage: number;
  frequency: string;
  duration: string;
  quantity: number;
  usageInstruction?: string | null;
};

export type PrescriptionItemSummary = {
  id: number | null;
  drugId: number | null;
  drugName: string | null;
  specification: string | null;
  dosageForm: string | null;
  packageUnit: string | null;
  unitPrice: number | null;
  dosage: number | null;
  frequency: string | null;
  duration: string | null;
  quantity: number | null;
  usageInstruction: string | null;
};

export type PrescriptionReviewRequest = {
  registrationId: number;
  items: PrescriptionItemRequest[];
};

export type PrescriptionSubmitRequest = {
  registrationId: number;
  reviewId: number;
  items: PrescriptionItemRequest[];
  manualConfirmation?: string | null;
};

export type PrescriptionRuleHit = {
  ruleId: number | null;
  ruleCode: string | null;
  ruleType: string | null;
  riskLevel: string | null;
  alertMessage: string | null;
  suggestion: string | null;
  basisSnapshot: string | null;
};

export type PrescriptionReviewResponse = {
  reviewId: number | null;
  registrationId: number | null;
  prescriptionId: number | null;
  reviewStatus: string;
  riskLevel: string | null;
  localRuleHits: string | null;
  ruleHits: PrescriptionRuleHit[];
  ruleEngineStatus: string | null;
  contextMissingItems: string | null;
  contextMissingItemList: string[];
  llmSuggestion: string | null;
  llmSummary: string | null;
  llmCallStatus: string | null;
  prescriptionSnapshotHash: string | null;
  reviewContextHash: string | null;
  degraded: boolean | null;
  bindStatus: string | null;
  items: PrescriptionItemSummary[];
};

export type PrescriptionSummary = {
  id: number;
  registrationId: number;
  patientId: number;
  patientName: string | null;
  doctorId: number;
  doctorName: string | null;
  departmentName: string | null;
  status: string;
  riskLevel: string | null;
  reviewId: number | null;
  items: PrescriptionItemSummary[];
  review: PrescriptionReviewResponse | null;
  createdAt: string | null;
};

export type ConsultationWorkspace = {
  registration: RegistrationSummary;
  latestMedicalRecord: MedicalRecordSummary | null;
  latestPrescription: PrescriptionSummary | null;
  recentReviews: PrescriptionReviewResponse[];
  nextActions: string[];
};

export type FeedbackCreateRequest = {
  registrationId: number;
  rating: number;
  triageAccurate?: boolean | null;
  comment?: string | null;
};

export type FeedbackResponse = {
  id: number;
  registrationId: number;
  rating: number;
  triageAccurate: boolean | null;
  comment: string | null;
  createdAt: string | null;
};

export type DashboardOverview = {
  todayRegistrations: number;
  todayVisits: number;
  waitingRegistrations: number;
  completedRegistrations: number;
  todayPrescriptions: number;
  todayAiCallRecords: number;
  medicalRecords: number;
  prescriptions: number;
  highRiskReviews: number;
  feedbackCount: number;
  aiCallRecords: number;
  updatedAt: string | null;
};

export type DashboardTrendPoint = {
  date: string;
  registrations: number;
  visits: number;
  prescriptions: number;
  aiCalls: number;
};

export type AiUsageBucket = {
  taskType: string;
  calls: number;
  successCalls: number;
  failedCalls: number;
  degradedCalls: number;
  usageRate: number;
  averageDurationMs: number;
};

export type AiUsageStats = {
  totalCalls: number;
  successCalls: number;
  failedCalls: number;
  degradedCalls: number;
  successRate: number;
  averageDurationMs: number;
  buckets: AiUsageBucket[];
  updatedAt: string | null;
};

export type PrescriptionReviewRate = {
  totalReviews: number;
  lowRiskReviews: number;
  mediumRiskReviews: number;
  highRiskReviews: number;
  manualRequiredReviews: number;
  unknownReviews: number;
  passRate: number;
  updatedAt: string | null;
};

export type RiskDistributionBucket = {
  riskLevel: string;
  count: number;
  ratio: number;
};

export type RiskDistribution = {
  totalReviews: number;
  buckets: RiskDistributionBucket[];
  updatedAt: string | null;
};

export type TriageAccuracyStats = {
  feedbackCount: number;
  accurateCount: number;
  inaccurateCount: number;
  noFeedbackCount: number;
  accuracyRate: number;
  sampleCount: number;
  updatedAt: string | null;
};

export type DashboardBundle = {
  overview: DashboardOverview | null;
  trends: DashboardTrendPoint[];
  aiUsage: AiUsageStats | null;
  prescriptionReviewRate: PrescriptionReviewRate | null;
  riskDistribution: RiskDistribution | null;
  triageAccuracy: TriageAccuracyStats | null;
};

export type AiStreamTaskType = 'MEDICAL_RECORD' | 'DIAGNOSIS';

export type AiStreamSessionCreateRequest = {
  taskType: AiStreamTaskType;
  registrationId: number;
  conversationText: string;
  diagnosisDirection?: string | null;
};

export type AiStreamSessionCreateResponse = {
  sessionId: string;
  streamToken: string;
  taskType: AiStreamTaskType;
  expiresAt: string;
};

export type AiConfigSummary = {
  id: number;
  provider: string;
  modelName: string;
  apiUrl?: string | null;
  taskScope: string;
  timeoutSeconds: number;
  defaultConfig: boolean;
  healthStatus: string | null;
  configVersion: string;
  enabled: boolean;
  priority: number;
  status: string;
  hasApiKey?: boolean | null;
  keyVersion?: string | null;
  updatedAt: string | null;
};

export type PrescriptionRuleSummary = {
  id: number;
  ruleCode: string;
  ruleType: string;
  applicableDrugs: string | null;
  applicableDiseases: string | null;
  applicablePopulations: string | null;
  conditionExpression: string | null;
  riskLevel: string | null;
  alertMessage: string | null;
  suggestion: string | null;
  basis: string | null;
  seeded: boolean | null;
  version: number | null;
  validationStatus: string | null;
  status: string;
  updatedAt: string | null;
};

export type AiCallRecordSummary = {
  id: number;
  taskType: string;
  businessRecordId: number | null;
  operatorId: number | null;
  operatorRole: string | null;
  provider: string | null;
  modelName: string | null;
  configVersion: string | null;
  promptVersion: string | null;
  inputSummary: string | null;
  outputSummary: string | null;
  callStatus: string;
  errorSummary: string | null;
  durationMs: number | null;
  traceId: string | null;
  degraded: boolean | null;
  retryCount: number | null;
  createdAt: string | null;
  businessSummary: string | null;
};

export type PatientProfile = PatientInfoResponse;

export type AuditLogSummary = {
  id: number;
  actorId: number | null;
  actorRole: string | null;
  action: string;
  resourceType: string | null;
  resourceId: number | null;
  traceId: string | null;
  success: boolean;
  message: string | null;
  occurredAt: string | null;
};

export type NotificationRecordSummary = {
  id: number;
  recipientId: number;
  recipientRole: string;
  alertType: string;
  statisticsBucket: string | null;
  displayLevel: string | null;
  businessRecordId: number | null;
  patientSummary: string | null;
  riskSummary: string | null;
  read: boolean | null;
  createdAt: string | null;
};

export type PromptTemplateSummary = {
  id: number;
  templateCode: string;
  taskType: string;
  deptCode: string | null;
  templateBody: string | null;
  variableWhitelist: string | null;
  version: number | null;
  defaultTemplate: boolean | null;
  status: string;
  updatedAt: string | null;
};

export type PromptTemplateWriteRequest = {
  templateCode: string;
  taskType: string;
  deptCode?: string | null;
  templateBody?: string | null;
  variableWhitelist?: string | null;
  version?: number | null;
  defaultTemplate?: boolean | null;
  status: string;
};

export type DepartmentWriteRequest = {
  code: string;
  name: string;
  type?: string | null;
  description?: string | null;
  status: string;
};

export type DoctorWriteRequest = {
  username: string;
  password?: string | null;
  name: string;
  departmentId: number;
  title?: string | null;
  specialty?: string | null;
  introduction?: string | null;
  status: string;
};

export type ScheduleWriteRequest = {
  doctorId: number;
  departmentId: number;
  workDate: string;
  period: string;
  totalSlots: number;
  remainingSlots?: number | null;
  visitLevel: string;
  status: string;
};

export type BatchScheduleRequest = {
  doctorId: number;
  departmentId: number;
  workDates: string[];
  periods: string[];
  totalSlots: number;
  remainingSlots?: number | null;
  visitLevel: string;
  status: string;
};

export type DrugWriteRequest = {
  code: string;
  name: string;
  pinyinCode?: string | null;
  specification?: string | null;
  dosageForm?: string | null;
  packageUnit?: string | null;
  manufacturer?: string | null;
  unitPrice?: number | null;
  defaultUsage?: string | null;
  contraindications?: string | null;
  precautions?: string | null;
  indications?: string | null;
  interactionSummary?: string | null;
  status: string;
};

export type PrescriptionRuleWriteRequest = {
  ruleCode: string;
  ruleType: string;
  applicableDrugs?: string | null;
  applicableDiseases?: string | null;
  applicablePopulations?: string | null;
  conditionExpression?: string | null;
  riskLevel?: string | null;
  alertMessage?: string | null;
  suggestion?: string | null;
  basis?: string | null;
  seeded?: boolean | null;
  validationStatus?: string | null;
  status: string;
};

export type AiConfigWriteRequest = {
  provider: string;
  modelName: string;
  apiUrl?: string | null;
  apiKey?: string | null;
  keyVersion?: string | null;
  taskScope: string;
  timeoutSeconds: number;
  defaultConfig?: boolean | null;
  healthStatus?: string | null;
  status: string;
  enabled?: boolean | null;
  priority: number;
  configVersion?: string | null;
};

function unwrap<T>(response: Result<T>) {
  if (response.code !== 200) {
    throw new Error(response.message || 'API error');
  }
  if (response.data === null || response.data === undefined) {
    throw new Error(response.message || 'empty response');
  }
  return response.data;
}

export async function listDepartments() {
  return unwrap((await http.get<Result<DepartmentOption[]>>('/departments')).data);
}

export async function adminListDepartments() {
  return unwrap((await http.get<Result<DepartmentOption[]>>('/admin/departments')).data);
}

export async function adminCreateDepartment(payload: DepartmentWriteRequest) {
  return unwrap((await http.post<Result<DepartmentOption>>('/admin/departments', payload)).data);
}

export async function adminUpdateDepartment(id: number, payload: DepartmentWriteRequest) {
  return unwrap((await http.put<Result<DepartmentOption>>(`/admin/departments/${id}`, payload)).data);
}

export async function adminToggleDepartment(id: number) {
  return unwrap((await http.patch<Result<DepartmentOption>>(`/admin/departments/${id}/toggle`)).data);
}

export async function listDoctors(departmentId?: number | null) {
  return unwrap((await http.get<Result<DoctorOption[]>>('/doctors', { params: { departmentId } })).data);
}

export async function adminListDoctors(departmentId?: number | null) {
  return unwrap((await http.get<Result<DoctorOption[]>>('/admin/doctors', { params: { departmentId } })).data);
}

export async function adminCreateDoctor(payload: DoctorWriteRequest) {
  return unwrap((await http.post<Result<DoctorOption>>('/admin/doctors', payload)).data);
}

export async function adminUpdateDoctor(id: number, payload: DoctorWriteRequest) {
  return unwrap((await http.put<Result<DoctorOption>>(`/admin/doctors/${id}`, payload)).data);
}

export async function adminToggleDoctor(id: number) {
  return unwrap((await http.patch<Result<DoctorOption>>(`/admin/doctors/${id}/toggle`)).data);
}

export async function getDepartment(id: number) {
  return unwrap((await http.get<Result<DepartmentOption>>(`/departments/${id}`)).data);
}

export async function getDoctor(id: number) {
  return unwrap((await http.get<Result<DoctorOption>>(`/doctors/${id}`)).data);
}

export async function listSchedules(departmentId?: number | null) {
  return unwrap((await http.get<Result<ScheduleOption[]>>('/schedules/available', { params: { departmentId } })).data);
}

export async function listAllSchedules(
  departmentId?: number | null,
  doctorId?: number | null,
  from?: string | null,
  to?: string | null,
) {
  return unwrap((await http.get<Result<ScheduleOption[]>>('/admin/schedules', {
    params: { departmentId, doctorId, from, to },
  })).data);
}

export async function adminCreateSchedule(payload: ScheduleWriteRequest) {
  return unwrap((await http.post<Result<ScheduleOption>>('/admin/schedules', payload)).data);
}

export async function adminBatchCreateSchedules(payload: BatchScheduleRequest) {
  return unwrap((await http.post<Result<ScheduleOption[]>>('/admin/schedules/batch', payload)).data);
}

export async function adminUpdateSchedule(id: number, payload: ScheduleWriteRequest) {
  return unwrap((await http.put<Result<ScheduleOption>>(`/admin/schedules/${id}`, payload)).data);
}

export async function adminToggleSchedule(id: number) {
  return unwrap((await http.patch<Result<ScheduleOption>>(`/admin/schedules/${id}/toggle`)).data);
}

export async function getPatientInfo() {
  return unwrap((await http.get<Result<PatientInfoResponse>>('/patient/info')).data);
}

export async function updatePatientInfo(payload: PatientUpdateRequest) {
  return unwrap((await http.put<Result<PatientInfoResponse>>('/patient/info', payload)).data);
}

export async function triageConsult(payload: TriageRequest) {
  return unwrap((await http.post<Result<TriageResponse>>('/triage/consult', payload)).data);
}

export async function confirmConversationTriage(payload: ConversationTriageConfirmRequest) {
  return unwrap((await http.post<Result<TriageResponse>>('/triage/conversation/confirm', payload)).data);
}

export async function listTriageHistory() {
  return unwrap((await http.get<Result<TriageResponse[]>>('/triage/history')).data);
}

export async function createRegistration(payload: RegistrationCreateRequest) {
  return unwrap((await http.post<Result<RegistrationSummary>>('/registration/create', payload)).data);
}

export async function cancelRegistration(registrationId: number, reason?: string) {
  return unwrap((await http.post<Result<RegistrationSummary>>(`/registration/cancel/${registrationId}`, { reason })).data);
}

export async function listDoctorQueue() {
  return unwrap((await http.get<Result<RegistrationSummary[]>>('/doctor/queue')).data);
}

export async function listDoctorSchedules(doctorId: number) {
  return unwrap((await http.get<Result<ScheduleOption[]>>(`/schedules/doctor/${doctorId}/available`)).data);
}

export async function listRegistrations() {
  return unwrap((await http.get<Result<RegistrationSummary[]>>('/registration/list')).data);
}

export async function beginConsultation(registrationId: number) {
  return unwrap((await http.post<Result<RegistrationSummary>>(`/consultation/${registrationId}/begin`)).data);
}

export async function completeConsultation(registrationId: number) {
  return unwrap((await http.post<Result<RegistrationSummary>>(`/consultation/${registrationId}/complete`)).data);
}

export async function getWorkspace(registrationId: number) {
  return unwrap((await http.get<Result<ConsultationWorkspace>>(`/consultation/${registrationId}/workspace`)).data);
}

export async function generateMedicalRecord(payload: MedicalRecordGenerateRequest) {
  return unwrap((await http.post<Result<MedicalRecordSummary>>('/medical-record/generate', payload)).data);
}

export async function saveMedicalRecord(payload: MedicalRecordSaveRequest) {
  return unwrap((await http.post<Result<MedicalRecordSummary>>('/medical-record/save', payload)).data);
}

export async function listPatientMedicalRecords(patientId?: number) {
  const url = patientId ? `/medical-record/list/patient/${patientId}` : '/medical-record/list/patient';
  return unwrap((await http.get<Result<MedicalRecordSummary[]>>(url)).data);
}

export async function listDoctorMedicalRecords() {
  return unwrap((await http.get<Result<MedicalRecordSummary[]>>('/medical-record/list/doctor')).data);
}

export async function searchDoctorMedicalRecords(keyword?: string) {
  return unwrap(
    (await http.get<Result<MedicalRecordSummary[]>>('/medical-record/list/doctor', { params: { keyword } })).data,
  );
}

export async function getMedicalRecord(id: number) {
  return unwrap((await http.get<Result<MedicalRecordSummary>>(`/medical-record/${id}`)).data);
}

export async function diagnose(payload: DiagnosisSuggestionRequest) {
  return unwrap((await http.post<Result<DiagnosisSuggestionResponse>>('/diagnosis/suggest', payload)).data);
}

export async function adoptDiagnosisSuggestion(id: number, payload: DiagnosisSuggestionAdoptRequest) {
  return unwrap((await http.patch<Result<DiagnosisSuggestionResponse>>(`/diagnosis/${id}/adopt`, payload)).data);
}

export async function ignoreDiagnosisSuggestion(id: number, payload?: DiagnosisSuggestionIgnoreRequest) {
  return unwrap((await http.patch<Result<DiagnosisSuggestionResponse>>(`/diagnosis/${id}/ignore`, payload ?? {})).data);
}

export async function reviewPrescription(payload: PrescriptionReviewRequest) {
  return unwrap((await http.post<Result<PrescriptionReviewResponse>>('/prescription/check', payload)).data);
}

export async function submitPrescription(payload: PrescriptionSubmitRequest) {
  return unwrap((await http.post<Result<PrescriptionSummary>>('/prescription/submit', payload)).data);
}

export async function listPatientPrescriptions(patientId?: number) {
  const url = patientId ? `/prescription/list/patient/${patientId}` : '/prescription/list/patient';
  return unwrap((await http.get<Result<PrescriptionSummary[]>>(url)).data);
}

export async function listDoctorPrescriptions() {
  return unwrap((await http.get<Result<PrescriptionSummary[]>>('/prescription/list/doctor')).data);
}

export async function getPrescription(id: number) {
  return unwrap((await http.get<Result<PrescriptionSummary>>(`/prescription/${id}`)).data);
}

export async function getPrescriptionReview(id: number) {
  return unwrap((await http.get<Result<PrescriptionReviewResponse>>(`/prescription/${id}/review`)).data);
}

export async function searchDrugs(keyword?: string) {
  return unwrap((await http.get<Result<DrugOption[]>>('/drugs/search', { params: { keyword } })).data);
}

export async function adminListDrugs(keyword?: string, status?: string | null) {
  return unwrap((await http.get<Result<DrugOption[]>>('/admin/drugs', { params: { keyword, status } })).data);
}

export async function adminCreateDrug(payload: DrugWriteRequest) {
  return unwrap((await http.post<Result<DrugOption>>('/admin/drugs', payload)).data);
}

export async function adminUpdateDrug(id: number, payload: DrugWriteRequest) {
  return unwrap((await http.put<Result<DrugOption>>(`/admin/drugs/${id}`, payload)).data);
}

export async function adminToggleDrug(id: number) {
  return unwrap((await http.patch<Result<DrugOption>>(`/admin/drugs/${id}/toggle`)).data);
}

export async function createFeedback(payload: FeedbackCreateRequest) {
  return unwrap((await http.post<Result<FeedbackResponse>>('/feedback/create', payload)).data);
}

export async function listPatientFeedback() {
  return unwrap((await http.get<Result<FeedbackResponse[]>>('/feedback/list')).data);
}

export async function listFeedback() {
  return unwrap((await http.get<Result<FeedbackResponse[]>>('/feedback/list')).data);
}

export async function getDashboardOverview() {
  return unwrap((await http.get<Result<DashboardOverview>>('/dashboard/overview')).data);
}

export async function getDashboardTrends(startDate?: string, endDate?: string) {
  return unwrap((await http.get<Result<DashboardTrendPoint[]>>('/dashboard/trends', { params: { startDate, endDate } })).data);
}

export async function getDashboardAiUsage(taskType?: string) {
  return unwrap((await http.get<Result<AiUsageStats>>('/dashboard/ai-usage', { params: { taskType } })).data);
}

export async function getDashboardPrescriptionReviewRate() {
  return unwrap((await http.get<Result<PrescriptionReviewRate>>('/dashboard/prescription-review-rate')).data);
}

export async function getDashboardRiskDistribution() {
  return unwrap((await http.get<Result<RiskDistribution>>('/dashboard/risk-distribution')).data);
}

export async function getDashboardTriageAccuracy() {
  return unwrap((await http.get<Result<TriageAccuracyStats>>('/dashboard/triage-accuracy')).data);
}

export async function createAiStreamSession(payload: AiStreamSessionCreateRequest) {
  return unwrap((await http.post<Result<AiStreamSessionCreateResponse>>('/ai-stream-sessions', payload)).data);
}

export async function cancelAiStreamSession(sessionId: string) {
  await http.delete<Result<null>>(`/ai-stream-sessions/${sessionId}`);
}

export async function listAiConfig() {
  return unwrap((await http.get<Result<AiConfigSummary[]>>('/admin/ai-config')).data);
}

export async function adminCreateAiConfig(payload: AiConfigWriteRequest) {
  return unwrap((await http.post<Result<AiConfigSummary>>('/admin/ai-config', payload)).data);
}

export async function adminUpdateAiConfig(id: number, payload: AiConfigWriteRequest) {
  return unwrap((await http.put<Result<AiConfigSummary>>(`/admin/ai-config/${id}`, payload)).data);
}

export async function adminToggleAiConfig(id: number) {
  return unwrap((await http.patch<Result<AiConfigSummary>>(`/admin/ai-config/${id}/toggle`)).data);
}

export async function listPrescriptionRules() {
  return unwrap((await http.get<Result<PrescriptionRuleSummary[]>>('/admin/prescription-rules')).data);
}

export async function adminCreatePrescriptionRule(payload: PrescriptionRuleWriteRequest) {
  return unwrap((await http.post<Result<PrescriptionRuleSummary>>('/admin/prescription-rules', payload)).data);
}

export async function adminUpdatePrescriptionRule(id: number, payload: PrescriptionRuleWriteRequest) {
  return unwrap((await http.put<Result<PrescriptionRuleSummary>>(`/admin/prescription-rules/${id}`, payload)).data);
}

export async function adminTogglePrescriptionRule(id: number) {
  return unwrap((await http.patch<Result<PrescriptionRuleSummary>>(`/admin/prescription-rules/${id}/toggle`)).data);
}

export async function listAiCallRecords(taskType?: string | null) {
  return unwrap((await http.get<Result<AiCallRecordSummary[]>>('/admin/ai-records', { params: { taskType } })).data);
}

export async function listPromptTemplates() {
  return unwrap((await http.get<Result<PromptTemplateSummary[]>>('/admin/prompt-templates')).data);
}

export async function adminCreatePromptTemplate(payload: PromptTemplateWriteRequest) {
  return unwrap((await http.post<Result<PromptTemplateSummary>>('/admin/prompt-templates', payload)).data);
}

export async function adminUpdatePromptTemplate(id: number, payload: PromptTemplateWriteRequest) {
  return unwrap((await http.put<Result<PromptTemplateSummary>>(`/admin/prompt-templates/${id}`, payload)).data);
}

export async function adminTogglePromptTemplate(id: number) {
  return unwrap((await http.patch<Result<PromptTemplateSummary>>(`/admin/prompt-templates/${id}/toggle`)).data);
}

export async function listAuditLogs() {
  return unwrap((await http.get<Result<AuditLogSummary[]>>('/admin/audit-logs')).data);
}

export async function listUnreadNotifications() {
  return unwrap((await http.get<Result<NotificationRecordSummary[]>>('/notifications/unread')).data);
}

export async function markNotificationRead(id: number) {
  return unwrap((await http.put<Result<NotificationRecordSummary>>(`/notifications/${id}/read`)).data);
}
