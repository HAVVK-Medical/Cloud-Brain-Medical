import { http } from './http';
import type { LoginRequest, LoginResponse, LogoutRequest, RefreshRequest, RegisterRequest, Result } from '@/types/api';
import { getUiMessage } from '@/utils/zh';

function requireData<T>(response: Result<T>) {
  if (!response.data) {
    throw new Error(getUiMessage(response.message, '响应为空'));
  }

  return response.data;
}

export async function patientLogin(payload: LoginRequest) {
  const response = await http.post<Result<LoginResponse>>('/patient/login', payload);
  return requireData(response.data);
}

export async function patientRegister(payload: RegisterRequest) {
  const response = await http.post<Result<null>>('/patient/register', payload);
  return response.data;
}

export async function doctorLogin(payload: LoginRequest) {
  const response = await http.post<Result<LoginResponse>>('/doctor/login', payload);
  return requireData(response.data);
}

export async function adminLogin(payload: LoginRequest) {
  const response = await http.post<Result<LoginResponse>>('/admin/login', payload);
  return requireData(response.data);
}

export async function refreshSession(payload: RefreshRequest) {
  const response = await http.post<Result<LoginResponse>>('/auth/refresh', payload);
  return requireData(response.data);
}

export async function logoutSession(payload: LogoutRequest) {
  const response = await http.post<Result<null>>('/auth/logout', payload);
  return response.data;
}
