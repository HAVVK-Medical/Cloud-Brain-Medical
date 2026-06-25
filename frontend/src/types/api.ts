import type { WorkspaceRole } from './enums';

export interface Result<T> {
  code: number;
  message: string;
  data: T | null;
  timestamp: number;
}

export interface HealthResponse {
  service: string;
  status: string;
  javaVersion: string;
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
  role: WorkspaceRole;
  patientId: number | null;
  doctorId: number | null;
  username: string;
  displayName: string | null;
  expiresAt: number;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface LogoutRequest extends RefreshRequest {
  reason?: string;
}

export interface PatientInfoResponse {
  patientId: number;
  username: string;
  realName: string | null;
  gender: string | null;
  age: number | null;
  phone: string | null;
  idCardNumber: string | null;
  medicalHistory: string | null;
  remark: string | null;
}

export interface PatientUpdateRequest {
  realName?: string | null;
  gender?: string | null;
  age?: number | null;
  phone?: string | null;
  idCardNumber?: string | null;
  medicalHistory?: string | null;
  remark?: string | null;
}
