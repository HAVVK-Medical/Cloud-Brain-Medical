export interface ChatSession {
  id: number;
  userId: number;
  userRole: string;
  title: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ChatMessage {
  id: number;
  role: 'USER' | 'ASSISTANT';
  content: string;
  thinkingContent?: string;
  aiMeta: string | null;
  createdAt: string;
}

export interface ChatMeta {
  provider: string;
  model: string;
  durationMs: number;
  traceId: string;
  degraded: boolean;
}
