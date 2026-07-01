import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { Result } from '@/types/api';
import type {
  AiConfigWriteRequest,
  BatchScheduleRequest,
  DepartmentWriteRequest,
  DoctorWriteRequest,
  DrugWriteRequest,
  PrescriptionRuleWriteRequest,
  PromptTemplateWriteRequest,
  ScheduleWriteRequest,
} from './workflow';

vi.mock('./http', () => ({
  http: {
    delete: vi.fn(),
    get: vi.fn(),
    patch: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

const { http } = await import('./http');
const workflow = await import('./workflow');

const get = vi.mocked(http.get);
const post = vi.mocked(http.post);
const put = vi.mocked(http.put);
const patch = vi.mocked(http.patch);
const del = vi.mocked(http.delete);

function result<T>(data: T | null, message = 'success', code = 200): Result<T> {
  return { code, message, data, timestamp: 1 };
}

function ok(data: unknown = { id: 1 }) {
  return Promise.resolve({ data: result(data) });
}

describe('workflow api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    get.mockImplementation(() => ok());
    post.mockImplementation(() => ok());
    put.mockImplementation(() => ok());
    patch.mockImplementation(() => ok());
    del.mockImplementation(() => Promise.resolve({ data: result(null) }));
  });

  it('maps workflow read endpoints to backend routes and query params', async () => {
    const cases: Array<{ name: string; call: () => Promise<unknown>; expectCall: () => void }> = [
      { name: 'listDepartments', call: workflow.listDepartments, expectCall: () => expect(get).toHaveBeenCalledWith('/departments') },
      { name: 'adminListDepartments', call: workflow.adminListDepartments, expectCall: () => expect(get).toHaveBeenCalledWith('/admin/departments') },
      { name: 'listDoctors', call: () => workflow.listDoctors(1), expectCall: () => expect(get).toHaveBeenCalledWith('/doctors', { params: { departmentId: 1 } }) },
      { name: 'adminListDoctors', call: () => workflow.adminListDoctors(null), expectCall: () => expect(get).toHaveBeenCalledWith('/admin/doctors', { params: { departmentId: null } }) },
      { name: 'getDepartment', call: () => workflow.getDepartment(2), expectCall: () => expect(get).toHaveBeenCalledWith('/departments/2') },
      { name: 'getDoctor', call: () => workflow.getDoctor(3), expectCall: () => expect(get).toHaveBeenCalledWith('/doctors/3') },
      { name: 'listSchedules', call: () => workflow.listSchedules(4), expectCall: () => expect(get).toHaveBeenCalledWith('/schedules/available', { params: { departmentId: 4 } }) },
      { name: 'listAllSchedules', call: () => workflow.listAllSchedules(1, 2, '2026-01-01', '2026-01-02'), expectCall: () => expect(get).toHaveBeenCalledWith('/admin/schedules', { params: { departmentId: 1, doctorId: 2, from: '2026-01-01', to: '2026-01-02' } }) },
      { name: 'getPatientInfo', call: workflow.getPatientInfo, expectCall: () => expect(get).toHaveBeenCalledWith('/patient/info') },
      { name: 'listTriageHistory', call: workflow.listTriageHistory, expectCall: () => expect(get).toHaveBeenCalledWith('/triage/history') },
      { name: 'listDoctorQueue', call: workflow.listDoctorQueue, expectCall: () => expect(get).toHaveBeenCalledWith('/doctor/queue') },
      { name: 'listDoctorSchedules', call: () => workflow.listDoctorSchedules(10), expectCall: () => expect(get).toHaveBeenCalledWith('/schedules/doctor/10/available') },
      { name: 'listRegistrations', call: workflow.listRegistrations, expectCall: () => expect(get).toHaveBeenCalledWith('/registration/list') },
      { name: 'getWorkspace', call: () => workflow.getWorkspace(40), expectCall: () => expect(get).toHaveBeenCalledWith('/consultation/40/workspace') },
      { name: 'listPatientMedicalRecords default', call: () => workflow.listPatientMedicalRecords(), expectCall: () => expect(get).toHaveBeenCalledWith('/medical-record/list/patient') },
      { name: 'listPatientMedicalRecords patient', call: () => workflow.listPatientMedicalRecords(100), expectCall: () => expect(get).toHaveBeenCalledWith('/medical-record/list/patient/100') },
      { name: 'listDoctorMedicalRecords', call: workflow.listDoctorMedicalRecords, expectCall: () => expect(get).toHaveBeenCalledWith('/medical-record/list/doctor') },
      { name: 'searchDoctorMedicalRecords', call: () => workflow.searchDoctorMedicalRecords('cold'), expectCall: () => expect(get).toHaveBeenCalledWith('/medical-record/list/doctor', { params: { keyword: 'cold' } }) },
      { name: 'getMedicalRecord', call: () => workflow.getMedicalRecord(5), expectCall: () => expect(get).toHaveBeenCalledWith('/medical-record/5') },
      { name: 'listPatientPrescriptions default', call: () => workflow.listPatientPrescriptions(), expectCall: () => expect(get).toHaveBeenCalledWith('/prescription/list/patient') },
      { name: 'listPatientPrescriptions patient', call: () => workflow.listPatientPrescriptions(100), expectCall: () => expect(get).toHaveBeenCalledWith('/prescription/list/patient/100') },
      { name: 'listDoctorPrescriptions', call: workflow.listDoctorPrescriptions, expectCall: () => expect(get).toHaveBeenCalledWith('/prescription/list/doctor') },
      { name: 'getPrescription', call: () => workflow.getPrescription(6), expectCall: () => expect(get).toHaveBeenCalledWith('/prescription/6') },
      { name: 'getPrescriptionReview', call: () => workflow.getPrescriptionReview(7), expectCall: () => expect(get).toHaveBeenCalledWith('/prescription/7/review') },
      { name: 'searchDrugs', call: () => workflow.searchDrugs('aspirin'), expectCall: () => expect(get).toHaveBeenCalledWith('/drugs/search', { params: { keyword: 'aspirin' } }) },
      { name: 'adminListDrugs', call: () => workflow.adminListDrugs('a', 'ACTIVE'), expectCall: () => expect(get).toHaveBeenCalledWith('/admin/drugs', { params: { keyword: 'a', status: 'ACTIVE' } }) },
      { name: 'listPatientFeedback', call: workflow.listPatientFeedback, expectCall: () => expect(get).toHaveBeenCalledWith('/feedback/list') },
      { name: 'listFeedback', call: workflow.listFeedback, expectCall: () => expect(get).toHaveBeenCalledWith('/feedback/list') },
      { name: 'getDashboardOverview', call: workflow.getDashboardOverview, expectCall: () => expect(get).toHaveBeenCalledWith('/dashboard/overview') },
      { name: 'getDashboardTrends', call: () => workflow.getDashboardTrends('2026-01-01', '2026-01-02'), expectCall: () => expect(get).toHaveBeenCalledWith('/dashboard/trends', { params: { startDate: '2026-01-01', endDate: '2026-01-02' } }) },
      { name: 'getDashboardAiUsage', call: () => workflow.getDashboardAiUsage('DIAGNOSIS'), expectCall: () => expect(get).toHaveBeenCalledWith('/dashboard/ai-usage', { params: { taskType: 'DIAGNOSIS' } }) },
      { name: 'getDashboardPrescriptionReviewRate', call: workflow.getDashboardPrescriptionReviewRate, expectCall: () => expect(get).toHaveBeenCalledWith('/dashboard/prescription-review-rate') },
      { name: 'getDashboardRiskDistribution', call: workflow.getDashboardRiskDistribution, expectCall: () => expect(get).toHaveBeenCalledWith('/dashboard/risk-distribution') },
      { name: 'getDashboardTriageAccuracy', call: workflow.getDashboardTriageAccuracy, expectCall: () => expect(get).toHaveBeenCalledWith('/dashboard/triage-accuracy') },
      { name: 'listAiConfig', call: workflow.listAiConfig, expectCall: () => expect(get).toHaveBeenCalledWith('/admin/ai-config') },
      { name: 'listPrescriptionRules', call: workflow.listPrescriptionRules, expectCall: () => expect(get).toHaveBeenCalledWith('/admin/prescription-rules') },
      { name: 'listAiCallRecords', call: () => workflow.listAiCallRecords('CHAT'), expectCall: () => expect(get).toHaveBeenCalledWith('/admin/ai-records', { params: { taskType: 'CHAT' } }) },
      { name: 'listPromptTemplates', call: workflow.listPromptTemplates, expectCall: () => expect(get).toHaveBeenCalledWith('/admin/prompt-templates') },
      { name: 'listAuditLogs', call: workflow.listAuditLogs, expectCall: () => expect(get).toHaveBeenCalledWith('/admin/audit-logs') },
      { name: 'listUnreadNotifications', call: workflow.listUnreadNotifications, expectCall: () => expect(get).toHaveBeenCalledWith('/notifications/unread') },
    ];

    for (const testCase of cases) {
      get.mockClear();
      await expect(testCase.call()).resolves.toEqual({ id: 1 });
      testCase.expectCall();
      expect(testCase.name).not.toBe('');
    }
  });

  it('maps workflow write endpoints to backend routes and payloads', async () => {
    const department: DepartmentWriteRequest = { code: 'card', name: 'Cardiology', status: 'ACTIVE' };
    const doctor: DoctorWriteRequest = { username: 'doc', name: 'Doctor', departmentId: 1, status: 'ACTIVE' };
    const schedule: ScheduleWriteRequest = { doctorId: 10, departmentId: 1, workDate: '2026-01-01', period: 'AM', totalSlots: 10, visitLevel: 'NORMAL', status: 'ACTIVE' };
    const batch: BatchScheduleRequest = { doctorId: 10, departmentId: 1, workDates: ['2026-01-01'], periods: ['AM'], totalSlots: 10, visitLevel: 'NORMAL', status: 'ACTIVE' };
    const drug: DrugWriteRequest = { code: 'D1', name: 'Drug', status: 'ACTIVE' };
    const rule: PrescriptionRuleWriteRequest = { ruleCode: 'R1', ruleType: 'ALLERGY', status: 'ACTIVE' };
    const aiConfig: AiConfigWriteRequest = { provider: 'deepseek', modelName: 'model', taskScope: 'CHAT', timeoutSeconds: 30, status: 'ACTIVE', priority: 1 };
    const promptTemplate: PromptTemplateWriteRequest = { templateCode: 'TPL', taskType: 'CHAT', status: 'ACTIVE' };
    const cases: Array<{ call: () => Promise<unknown>; expectCall: () => void }> = [
      { call: () => workflow.adminCreateDepartment(department), expectCall: () => expect(post).toHaveBeenCalledWith('/admin/departments', department) },
      { call: () => workflow.adminUpdateDepartment(1, department), expectCall: () => expect(put).toHaveBeenCalledWith('/admin/departments/1', department) },
      { call: () => workflow.adminToggleDepartment(1), expectCall: () => expect(patch).toHaveBeenCalledWith('/admin/departments/1/toggle') },
      { call: () => workflow.adminCreateDoctor(doctor), expectCall: () => expect(post).toHaveBeenCalledWith('/admin/doctors', doctor) },
      { call: () => workflow.adminUpdateDoctor(2, doctor), expectCall: () => expect(put).toHaveBeenCalledWith('/admin/doctors/2', doctor) },
      { call: () => workflow.adminToggleDoctor(2), expectCall: () => expect(patch).toHaveBeenCalledWith('/admin/doctors/2/toggle') },
      { call: () => workflow.updatePatientInfo({ realName: 'Alice' }), expectCall: () => expect(put).toHaveBeenCalledWith('/patient/info', { realName: 'Alice' }) },
      { call: () => workflow.adminCreateSchedule(schedule), expectCall: () => expect(post).toHaveBeenCalledWith('/admin/schedules', schedule) },
      { call: () => workflow.adminBatchCreateSchedules(batch), expectCall: () => expect(post).toHaveBeenCalledWith('/admin/schedules/batch', batch) },
      { call: () => workflow.adminUpdateSchedule(3, schedule), expectCall: () => expect(put).toHaveBeenCalledWith('/admin/schedules/3', schedule) },
      { call: () => workflow.adminToggleSchedule(3), expectCall: () => expect(patch).toHaveBeenCalledWith('/admin/schedules/3/toggle') },
      { call: () => workflow.triageConsult({ chiefComplaint: 'cough' }), expectCall: () => expect(post).toHaveBeenCalledWith('/triage/consult', { chiefComplaint: 'cough' }) },
      { call: () => workflow.confirmConversationTriage({ chiefComplaint: 'cough', department: 'Cardiology' }), expectCall: () => expect(post).toHaveBeenCalledWith('/triage/conversation/confirm', { chiefComplaint: 'cough', department: 'Cardiology' }) },
      { call: () => workflow.createRegistration({ scheduleId: 4, triageRecordId: 5 }), expectCall: () => expect(post).toHaveBeenCalledWith('/registration/create', { scheduleId: 4, triageRecordId: 5 }) },
      { call: () => workflow.cancelRegistration(6, 'manual'), expectCall: () => expect(post).toHaveBeenCalledWith('/registration/cancel/6', { reason: 'manual' }) },
      { call: () => workflow.beginConsultation(7), expectCall: () => expect(post).toHaveBeenCalledWith('/consultation/7/begin') },
      { call: () => workflow.completeConsultation(7), expectCall: () => expect(post).toHaveBeenCalledWith('/consultation/7/complete') },
      { call: () => workflow.generateMedicalRecord({ registrationId: 8, conversationText: 'chat' }), expectCall: () => expect(post).toHaveBeenCalledWith('/medical-record/generate', { registrationId: 8, conversationText: 'chat' }) },
      { call: () => workflow.saveMedicalRecord({ registrationId: 8, chiefComplaint: 'cough' }), expectCall: () => expect(post).toHaveBeenCalledWith('/medical-record/save', { registrationId: 8, chiefComplaint: 'cough' }) },
      { call: () => workflow.diagnose({ registrationId: 8, conversationText: 'chat' }), expectCall: () => expect(post).toHaveBeenCalledWith('/diagnosis/suggest', { registrationId: 8, conversationText: 'chat' }) },
      { call: () => workflow.adoptDiagnosisSuggestion(9, { finalDiagnosis: 'cold' }), expectCall: () => expect(patch).toHaveBeenCalledWith('/diagnosis/9/adopt', { finalDiagnosis: 'cold' }) },
      { call: () => workflow.ignoreDiagnosisSuggestion(9), expectCall: () => expect(patch).toHaveBeenCalledWith('/diagnosis/9/ignore', {}) },
      { call: () => workflow.reviewPrescription({ registrationId: 8, items: [] }), expectCall: () => expect(post).toHaveBeenCalledWith('/prescription/check', { registrationId: 8, items: [] }) },
      { call: () => workflow.submitPrescription({ registrationId: 8, reviewId: 9, items: [] }), expectCall: () => expect(post).toHaveBeenCalledWith('/prescription/submit', { registrationId: 8, reviewId: 9, items: [] }) },
      { call: () => workflow.adminCreateDrug(drug), expectCall: () => expect(post).toHaveBeenCalledWith('/admin/drugs', drug) },
      { call: () => workflow.adminUpdateDrug(10, drug), expectCall: () => expect(put).toHaveBeenCalledWith('/admin/drugs/10', drug) },
      { call: () => workflow.adminToggleDrug(10), expectCall: () => expect(patch).toHaveBeenCalledWith('/admin/drugs/10/toggle') },
      { call: () => workflow.createFeedback({ registrationId: 8, rating: 5 }), expectCall: () => expect(post).toHaveBeenCalledWith('/feedback/create', { registrationId: 8, rating: 5 }) },
      { call: () => workflow.createAiStreamSession({ taskType: 'DIAGNOSIS', registrationId: 8, conversationText: 'chat' }), expectCall: () => expect(post).toHaveBeenCalledWith('/ai-stream-sessions', { taskType: 'DIAGNOSIS', registrationId: 8, conversationText: 'chat' }) },
      { call: () => workflow.adminCreateAiConfig(aiConfig), expectCall: () => expect(post).toHaveBeenCalledWith('/admin/ai-config', aiConfig) },
      { call: () => workflow.adminUpdateAiConfig(11, aiConfig), expectCall: () => expect(put).toHaveBeenCalledWith('/admin/ai-config/11', aiConfig) },
      { call: () => workflow.adminToggleAiConfig(11), expectCall: () => expect(patch).toHaveBeenCalledWith('/admin/ai-config/11/toggle') },
      { call: () => workflow.adminCreatePrescriptionRule(rule), expectCall: () => expect(post).toHaveBeenCalledWith('/admin/prescription-rules', rule) },
      { call: () => workflow.adminUpdatePrescriptionRule(12, rule), expectCall: () => expect(put).toHaveBeenCalledWith('/admin/prescription-rules/12', rule) },
      { call: () => workflow.adminTogglePrescriptionRule(12), expectCall: () => expect(patch).toHaveBeenCalledWith('/admin/prescription-rules/12/toggle') },
      { call: () => workflow.adminCreatePromptTemplate(promptTemplate), expectCall: () => expect(post).toHaveBeenCalledWith('/admin/prompt-templates', promptTemplate) },
      { call: () => workflow.adminUpdatePromptTemplate(13, promptTemplate), expectCall: () => expect(put).toHaveBeenCalledWith('/admin/prompt-templates/13', promptTemplate) },
      { call: () => workflow.adminTogglePromptTemplate(13), expectCall: () => expect(patch).toHaveBeenCalledWith('/admin/prompt-templates/13/toggle') },
      { call: () => workflow.markNotificationRead(14), expectCall: () => expect(put).toHaveBeenCalledWith('/notifications/14/read') },
    ];

    for (const testCase of cases) {
      post.mockClear();
      put.mockClear();
      patch.mockClear();
      await expect(testCase.call()).resolves.toEqual({ id: 1 });
      testCase.expectCall();
    }
  });

  it('handles cancellation delete and unwrap failures', async () => {
    await expect(workflow.cancelAiStreamSession('session-1')).resolves.toBeUndefined();
    expect(del).toHaveBeenCalledWith('/ai-stream-sessions/session-1');

    get.mockResolvedValueOnce({ data: result(null, 'empty response') });
    await expect(workflow.listDepartments()).rejects.toThrow('empty response');

    get.mockResolvedValueOnce({ data: result({ id: 1 }, 'bad request', 400) });
    await expect(workflow.listDepartments()).rejects.toThrow('bad request');
  });
});
