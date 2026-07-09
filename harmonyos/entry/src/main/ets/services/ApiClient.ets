import http from '@ohos.net.http';
import type common from '@ohos.app.ability.common';
import { API_BASE_URL } from '../common/Config';
import type {
  DepartmentOption,
  DoctorOption,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegistrationCancelRequest,
  RegistrationCreateRequest,
  RegistrationSummary,
  Result,
  ScheduleOption
} from '../models/ApiModels';
import { SessionService } from './SessionService';

interface DepartmentQuery {
  departmentId?: number | null;
}

export class ApiClient {
  private sessionService: SessionService;

  constructor(context: common.Context) {
    this.sessionService = new SessionService(context);
  }

  async patientLogin(payload: LoginRequest): Promise<LoginResponse> {
    const session = await this.post<LoginResponse>('/patient/login', payload, false);
    await this.sessionService.save(session);
    return session;
  }

  async patientRegister(payload: RegisterRequest): Promise<void> {
    await this.post<null>('/patient/register', payload, false);
  }

  async listDepartments(): Promise<DepartmentOption[]> {
    return this.get<DepartmentOption[]>('/departments');
  }

  async listDoctors(departmentId?: number | null): Promise<DoctorOption[]> {
    const query: DepartmentQuery = { departmentId };
    return this.get<DoctorOption[]>('/doctors', query);
  }

  async listSchedules(departmentId?: number | null): Promise<ScheduleOption[]> {
    const query: DepartmentQuery = { departmentId };
    return this.get<ScheduleOption[]>('/schedules/available', query);
  }

  async listRegistrations(): Promise<RegistrationSummary[]> {
    return this.get<RegistrationSummary[]>('/registration/list');
  }

  async createRegistration(payload: RegistrationCreateRequest): Promise<RegistrationSummary> {
    return this.post<RegistrationSummary>('/registration/create', payload);
  }

  async cancelRegistration(registrationId: number, reason: string): Promise<RegistrationSummary> {
    const payload: RegistrationCancelRequest = { reason };
    return this.post<RegistrationSummary>(`/registration/cancel/${registrationId}`, payload);
  }

  async logoutLocal(): Promise<void> {
    await this.sessionService.clear();
  }

  async getSession(): Promise<LoginResponse | null> {
    return this.sessionService.load();
  }

  private async get<T>(path: string, query?: DepartmentQuery): Promise<T> {
    return this.request<T>('GET', path, undefined, true, query);
  }

  private async post<T>(path: string, body?: object, auth = true): Promise<T> {
    return this.request<T>('POST', path, body, auth);
  }

  private async request<T>(
    method: string,
    path: string,
    body?: object,
    auth = true,
    query?: DepartmentQuery
  ): Promise<T> {
    const request = http.createHttp();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json'
    };

    if (auth) {
      const session = await this.sessionService.load();
      if (session?.token) {
        headers.Authorization = `${session.tokenType || 'Bearer'} ${session.token}`;
      }
    }

    const response = await request.request(this.buildUrl(path, query), {
      method: this.resolveMethod(method),
      header: headers,
      connectTimeout: 15000,
      readTimeout: 15000,
      extraData: body ? JSON.stringify(body) : undefined
    });

    request.destroy();

    if (response.responseCode < 200 || response.responseCode >= 300) {
      throw new Error(`请求失败：${response.responseCode}`);
    }

    const raw = String(response.result ?? '');
    const parsed = JSON.parse(raw) as Result<T>;
    if (parsed.code !== 200) {
      throw new Error(parsed.message || '服务响应异常');
    }

    return parsed.data as T;
  }

  private buildUrl(path: string, query?: DepartmentQuery): string {
    const params: string[] = [];
    if (query?.departmentId !== null && query?.departmentId !== undefined) {
      params.push(`departmentId=${encodeURIComponent(String(query.departmentId))}`);
    }

    return `${API_BASE_URL}${path}${params.length ? `?${params.join('&')}` : ''}`;
  }

  private resolveMethod(method: string): http.RequestMethod {
    if (method === 'POST') {
      return http.RequestMethod.POST;
    }
    return http.RequestMethod.GET;
  }
}
