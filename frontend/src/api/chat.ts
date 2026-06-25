import { http } from './http';
import { AUTH_STORAGE_KEY } from '@/constants/auth';
import type { Result } from '@/types/api';
import type { ChatSession, ChatMessage } from '@/types/chat';

function unwrap<T>(response: Result<T>): T {
  if (!response.data) {
    throw new Error(response.message || 'API error');
  }
  return response.data;
}

export async function listSessions(): Promise<ChatSession[]> {
  const res = await http.get<Result<ChatSession[]>>('/chat/sessions');
  return unwrap(res.data);
}

export async function createSession(firstMessage: string): Promise<{ id: number; title: string }> {
  const res = await http.post<Result<{ id: number; title: string }>>('/chat/sessions', { firstMessage });
  return unwrap(res.data);
}

export async function getMessages(sessionId: number): Promise<ChatMessage[]> {
  const res = await http.get<Result<ChatMessage[]>>(`/chat/sessions/${sessionId}/messages`);
  return unwrap(res.data);
}

export async function deleteSession(sessionId: number): Promise<void> {
  await http.delete(`/chat/sessions/${sessionId}`);
}

export function buildStreamUrl(sessionId: number, message: string): string {
  const token = localStorage.getItem(AUTH_STORAGE_KEY);
  let parsed: { token?: string } = {};
  try {
    if (token) parsed = JSON.parse(token);
  } catch { /* ignore */ }
  const jwt = parsed.token || '';
  return `/api/chat/stream?sessionId=${sessionId}&message=${encodeURIComponent(message)}&token=${encodeURIComponent(jwt)}`;
}
