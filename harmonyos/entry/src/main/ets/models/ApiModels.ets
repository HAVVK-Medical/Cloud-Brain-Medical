export interface Result<T> {
  code: number;
  message: string;
  data: T | null;
  timestamp: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  realName: string;
  phone: string;
  gender: string;
  age: number | null;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  role: string;
  patientId: number | null;
  doctorId: number | null;
  username: string;
  displayName: string | null;
  expiresAt: number;
}

export interface DepartmentOption {
  id: number;
  code: string;
  name: string;
  type: string | null;
  description: string | null;
}

export interface DoctorOption {
  id: number;
  username: string;
  name: string;
  departmentId: number | null;
  departmentName: string | null;
  title: string | null;
  specialty: string | null;
  introduction: string | null;
}

export interface ScheduleOption {
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
}

export interface RegistrationCreateRequest {
  scheduleId: number;
  triageRecordId?: number | null;
}

export interface RegistrationCancelRequest {
  reason?: string;
}

export interface RegistrationSummary {
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
}
