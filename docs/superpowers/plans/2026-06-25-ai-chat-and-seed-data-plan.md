# AI Chat Assistant & Seed Data Enhancement — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a persistent AI chat assistant for doctors and patients with SSE streaming, optimize AI UX across clinical workflows, rewrite prompt templates, and seed realistic hospital data.

**Architecture:** New `ChatSessionEntity`/`ChatMessageEntity` JPA entities with `ChatService` orchestrating `AIInvocationService.chatStream()`. Frontend `AiChatPanel` floating component with Pinia `useChatStore` driving EventSource-based streaming. Workflow panels enhanced with structured cards, confidence tags, and dual-engine visualization. Two new Flyway migrations (V8 prompts, V9 seed data).

**Tech Stack:** Spring Boot 3.5 + JPA + Flyway, Vue 3 + TypeScript + Pinia + EventSource, MySQL 8.4/H2

---

## File Structure

```
Backend (new):
  entity/chat/ChatSessionEntity.java
  entity/chat/ChatMessageEntity.java
  repository/ChatSessionRepository.java
  repository/ChatMessageRepository.java
  application/chat/ChatService.java
  controller/ChatController.java

Backend (modify):
  security/SecurityConfig.java                  — add /api/chat/** permit rules

Frontend (new):
  types/chat.ts                                 — ChatSession, ChatMessage, SSE event types
  api/chat.ts                                   — chat API functions
  stores/chat.ts                                — useChatStore Pinia store
  components/chat/AiChatMessage.vue             — single message with markdown + cursor
  components/chat/AiChatInput.vue               — textarea + send/stop buttons
  components/chat/AiChatSessionList.vue         — session history sidebar
  components/chat/AiChatPanel.vue               — main draggable panel
  components/chat/AiChatLauncher.vue            — floating FAB button
  styles/ai-global.css                          — global AI interaction styles

Frontend (modify):
  views/doctor/DoctorHomeView.vue               — add AiChatLauncher
  views/patient/PatientHomeView.vue             — add AiChatLauncher
  views/doctor/panels/DoctorConsultationPanel.vue — streaming UX + AI badges
  views/patient/panels/PatientTriagePanel.vue   — card-based triage recommendations
  styles/base.css                               — import ai-global.css

Migrations:
  db/migration/V8__optimize_prompt_templates.sql
  db/migration/V9__seed_hospital_data.sql
```

---

### Task A1: Chat Domain Entities

**Files:**
- Create: `backend/src/main/java/com/cloudbrain/entity/chat/ChatSessionEntity.java`
- Create: `backend/src/main/java/com/cloudbrain/entity/chat/ChatMessageEntity.java`

- [ ] **Step 1: Write ChatSessionEntity**

```java
package com.cloudbrain.entity.chat;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "chat_session")
public class ChatSessionEntity extends BaseAuditableEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_role", nullable = false, length = 32)
    private String userRole; // PATIENT or DOCTOR

    @Column(name = "title", length = 128)
    private String title;

    protected ChatSessionEntity() {}

    public ChatSessionEntity(Long userId, String userRole, String title) {
        this.userId = Objects.requireNonNull(userId);
        this.userRole = Objects.requireNonNull(userRole);
        this.title = title;
    }

    public Long getUserId() { return userId; }
    public String getUserRole() { return userRole; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
```

- [ ] **Step 2: Write ChatMessageEntity**

```java
package com.cloudbrain.entity.chat;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "chat_message")
public class ChatMessageEntity extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSessionEntity session;

    @Column(name = "role", nullable = false, length = 16)
    private String role; // USER or ASSISTANT

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "ai_meta", columnDefinition = "TEXT")
    private String aiMeta; // JSON: provider, model, durationMs, traceId

    protected ChatMessageEntity() {}

    public ChatMessageEntity(ChatSessionEntity session, String role, String content) {
        this.session = Objects.requireNonNull(session);
        this.role = Objects.requireNonNull(role);
        this.content = content;
    }

    public ChatSessionEntity getSession() { return session; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAiMeta() { return aiMeta; }
    public void setAiMeta(String aiMeta) { this.aiMeta = aiMeta; }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/cloudbrain/entity/chat/
git commit -m "feat: add ChatSession and ChatMessage domain entities"
```

---

### Task A2: Chat Repositories

**Files:**
- Create: `backend/src/main/java/com/cloudbrain/repository/ChatSessionRepository.java`
- Create: `backend/src/main/java/com/cloudbrain/repository/ChatMessageRepository.java`

- [ ] **Step 1: Write ChatSessionRepository**

```java
package com.cloudbrain.repository;

import com.cloudbrain.entity.chat.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, Long> {

    List<ChatSessionEntity> findByUserIdAndUserRoleOrderByUpdatedAtDesc(Long userId, String userRole);

    void deleteByIdAndUserId(Long id, Long userId);
}
```

- [ ] **Step 2: Write ChatMessageRepository**

```java
package com.cloudbrain.repository;

import com.cloudbrain.entity.chat.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    void deleteBySessionId(Long sessionId);
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/cloudbrain/repository/
git commit -m "feat: add ChatSession and ChatMessage repositories"
```

---

### Task A3: ChatService

**Files:**
- Create: `backend/src/main/java/com/cloudbrain/application/chat/ChatService.java`

- [ ] **Step 1: Write ChatService**

```java
package com.cloudbrain.application.chat;

import com.cloudbrain.application.ai.AIInvocationService;
import com.cloudbrain.application.ai.AIModels;
import com.cloudbrain.entity.chat.ChatMessageEntity;
import com.cloudbrain.entity.chat.ChatSessionEntity;
import com.cloudbrain.repository.ChatMessageRepository;
import com.cloudbrain.repository.ChatSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_CONTEXT_MESSAGES = 20; // 10 rounds
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final AIInvocationService aiInvocationService;

    public ChatService(ChatSessionRepository sessionRepo,
                       ChatMessageRepository messageRepo,
                       AIInvocationService aiInvocationService) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.aiInvocationService = aiInvocationService;
    }

    @Transactional(readOnly = true)
    public List<ChatSessionEntity> listSessions(Long userId, String userRole) {
        return sessionRepo.findByUserIdAndUserRoleOrderByUpdatedAtDesc(userId, userRole);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageEntity> getMessages(Long sessionId) {
        return messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public ChatSessionEntity createSession(Long userId, String userRole, String firstMessage) {
        String title = firstMessage;
        if (firstMessage != null && firstMessage.length() > 50) {
            title = firstMessage.substring(0, 50);
        }
        ChatSessionEntity session = new ChatSessionEntity(userId, userRole, title);
        session = sessionRepo.save(session);

        if (firstMessage != null && !firstMessage.isBlank()) {
            messageRepo.save(new ChatMessageEntity(session, "USER", firstMessage.trim()));
        }
        return session;
    }

    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        messageRepo.deleteBySessionId(sessionId);
        sessionRepo.deleteByIdAndUserId(sessionId, userId);
    }

    public SseEmitter streamChat(Long sessionId, Long userId, String message, String userRole) {
        ChatSessionEntity session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        if (!session.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to session");
        }

        // Save user message
        messageRepo.save(new ChatMessageEntity(session, "USER", message.trim()));

        // Auto-generate title on first assistant response
        boolean needsTitle = session.getTitle() == null;

        // Load context
        List<ChatMessageEntity> history = messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<AIModels.AIMessage> contextMessages = buildContextMessages(history, userRole);

        // Build SseEmitter
        SseEmitter emitter = new SseEmitter(2 * 60 * 1000L); // 2 min timeout

        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder fullResponse = new StringBuilder();
                AIModels.AIExecutionOutcome<String> outcome = aiInvocationService.chat(
                        "CHAT",
                        null,
                        Map.of("userRole", userRole),
                        Collections.emptyList(),
                        "抱歉，AI 服务暂时不可用，请稍后重试。",
                        true,
                        chunk -> {
                            fullResponse.append(chunk);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("chunk")
                                        .data(Map.of("content", chunk)));
                            } catch (IOException e) {
                                throw new RuntimeException("SSE send failed", e);
                            }
                        }
                );

                // Build AI meta JSON
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("provider", outcome.meta().provider());
                meta.put("model", outcome.meta().modelName());
                meta.put("durationMs", outcome.meta().durationMs());
                meta.put("traceId", outcome.meta().traceId());
                meta.put("degraded", outcome.meta().degraded());
                String metaJson = objectMapper.writeValueAsString(meta);

                // Save assistant message
                ChatMessageEntity assistantMsg = new ChatMessageEntity(session, "ASSISTANT", fullResponse.toString());
                assistantMsg.setAiMeta(metaJson);
                assistantMsg = messageRepo.save(assistantMsg);

                // Update title if needed
                if (needsTitle) {
                    String title = fullResponse.toString().replaceAll("\\s+", " ").trim();
                    if (title.length() > 50) title = title.substring(0, 50);
                    session.setTitle(title);
                    sessionRepo.save(session);
                }

                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(Map.of("messageId", assistantMsg.getId(), "meta", meta)));

                emitter.complete();
            } catch (Exception e) {
                log.error("Chat stream failed for session {}", sessionId, e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of("message", "AI 服务暂时不可用")));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private List<AIModels.AIMessage> buildContextMessages(List<ChatMessageEntity> history, String userRole) {
        // Last N messages as context (exclude the just-saved user message for system prompt building)
        List<ChatMessageEntity> contextSlice = history.size() > MAX_CONTEXT_MESSAGES
                ? history.subList(history.size() - MAX_CONTEXT_MESSAGES, history.size())
                : history;

        List<AIModels.AIMessage> messages = new ArrayList<>();
        for (ChatMessageEntity msg : contextSlice) {
            if ("USER".equals(msg.getRole())) {
                messages.add(AIModels.AIMessage.user(msg.getContent(), Collections.emptyList()));
            } else {
                messages.add(AIModels.AIMessage.assistant(msg.getContent()));
            }
        }
        return messages;
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/cloudbrain/application/chat/
git commit -m "feat: add ChatService with SSE streaming chat"
```

---

### Task A4: ChatController

**Files:**
- Create: `backend/src/main/java/com/cloudbrain/controller/ChatController.java`

- [ ] **Step 1: Write ChatController**

```java
package com.cloudbrain.controller;

import com.cloudbrain.application.chat.ChatService;
import com.cloudbrain.common.Result;
import com.cloudbrain.entity.chat.ChatMessageEntity;
import com.cloudbrain.entity.chat.ChatSessionEntity;
import com.cloudbrain.security.ActorContext;
import com.cloudbrain.security.ActorContextResolver;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/sessions")
    public Result<List<ChatSessionEntity>> listSessions() {
        ActorContext ctx = ActorContextResolver.requireCurrent();
        List<ChatSessionEntity> sessions = chatService.listSessions(ctx.userId(), ctx.role().name());
        return Result.success(sessions);
    }

    @PostMapping("/sessions")
    public Result<Map<String, Object>> createSession(@RequestBody Map<String, String> body) {
        ActorContext ctx = ActorContextResolver.requireCurrent();
        String firstMessage = body.getOrDefault("firstMessage", "");
        ChatSessionEntity session = chatService.createSession(ctx.userId(), ctx.role().name(), firstMessage);
        return Result.success(Map.of("id", session.getId(), "title", session.getTitle() != null ? session.getTitle() : ""));
    }

    @GetMapping("/sessions/{id}/messages")
    public Result<List<ChatMessageEntity>> getMessages(@PathVariable Long id) {
        ActorContextResolver.requireCurrent();
        List<ChatMessageEntity> messages = chatService.getMessages(id);
        return Result.success(messages);
    }

    @GetMapping("/stream")
    public SseEmitter streamChat(@RequestParam Long sessionId, @RequestParam String message) {
        ActorContext ctx = ActorContextResolver.requireCurrent();
        return chatService.streamChat(sessionId, ctx.userId(), message, ctx.role().name());
    }

    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        ActorContext ctx = ActorContextResolver.requireCurrent();
        chatService.deleteSession(id, ctx.userId());
        return Result.success(null);
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/cloudbrain/controller/ChatController.java
git commit -m "feat: add ChatController with REST + SSE endpoints"
```

---

### Task A5: Security Config & Chat Table Migration

**Files:**
- Modify: `backend/src/main/java/com/cloudbrain/security/SecurityConfig.java`
- Create: `backend/src/main/resources/db/migration/V8__add_chat_tables.sql`

- [ ] **Step 1: Add chat SSE endpoint to SecurityConfig permitAll**

Find the line in SecurityConfig.java that permits SSE access:
```java
.requestMatchers(HttpMethod.GET, "/api/ai-stream-sessions/*/events").permitAll()
```

Add below it:
```java
.requestMatchers(HttpMethod.GET, "/api/chat/stream").permitAll()
```

- [ ] **Step 2: Add chat role-based access rules**

Find the line:
```java
.requestMatchers("/api/doctor/**").hasRole("DOCTOR")
```

Add chat endpoints before the doctor/patient rules:
```java
.requestMatchers("/api/chat/**").hasAnyRole("PATIENT", "DOCTOR")
```

Adding it right after the `permitAll` lines and before the role-specific rules.

- [ ] **Step 3: Write V8 migration for chat tables**

```sql
-- V8__add_chat_tables.sql
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_role VARCHAR(32) NOT NULL,
    title VARCHAR(128),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT,
    ai_meta TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES chat_session(id)
);

CREATE INDEX idx_chat_session_user ON chat_session(user_id, user_role);
CREATE INDEX idx_chat_message_session ON chat_message(session_id);
```

- [ ] **Step 4: Verify migration compiles**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/cloudbrain/security/SecurityConfig.java backend/src/main/resources/db/migration/V8__add_chat_tables.sql
git commit -m "feat: add chat security rules and V8 migration for chat tables"
```

---

### Task B1: Chat Frontend Types & API

**Files:**
- Create: `frontend/src/types/chat.ts`
- Create: `frontend/src/api/chat.ts`

- [ ] **Step 1: Write chat types**

```typescript
// frontend/src/types/chat.ts

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
```

- [ ] **Step 2: Write chat API module**

```typescript
// frontend/src/api/chat.ts

import { http } from './http';
import type { Result } from '@/types/api';
import type { ChatSession, ChatMessage } from '@/types/chat';

function unwrap<T>(response: Result<T>): T {
  if (response.code !== 0 && response.code !== 200) {
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
  const token = localStorage.getItem('cloud-brain-session');
  let parsed: { token?: string } = {};
  try {
    if (token) parsed = JSON.parse(token);
  } catch { /* ignore */ }
  const jwt = parsed.token || '';
  return `/api/chat/stream?sessionId=${sessionId}&message=${encodeURIComponent(message)}&token=${encodeURIComponent(jwt)}`;
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/types/chat.ts frontend/src/api/chat.ts
git commit -m "feat: add chat types and API module"
```

---

### Task B2: Chat Pinia Store

**Files:**
- Create: `frontend/src/stores/chat.ts`

- [ ] **Step 1: Write useChatStore**

```typescript
// frontend/src/stores/chat.ts

import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { listSessions, createSession, getMessages, deleteSession, buildStreamUrl } from '@/api/chat';
import type { ChatSession, ChatMessage, ChatMeta } from '@/types/chat';

export const useChatStore = defineStore('chat', () => {
  const sessions = ref<ChatSession[]>([]);
  const currentSessionId = ref<number | null>(null);
  const messages = ref<ChatMessage[]>([]);
  const isStreaming = ref(false);
  const streamAbortController = ref<AbortController | null>(null);
  const streamBuffer = ref('');

  const currentSession = computed(() =>
    sessions.value.find(s => s.id === currentSessionId.value) ?? null
  );

  async function fetchSessions() {
    sessions.value = await listSessions();
  }

  async function selectSession(id: number) {
    currentSessionId.value = id;
    messages.value = await getMessages(id);
  }

  async function createNewSession(firstMessage: string): Promise<ChatSession> {
    const result = await createSession(firstMessage);
    await fetchSessions();
    const session = sessions.value.find(s => s.id === result.id);
    if (session) {
      currentSessionId.value = session.id;
      messages.value = [];
    }
    return session!;
  }

  async function removeSession(id: number) {
    await deleteSession(id);
    if (currentSessionId.value === id) {
      currentSessionId.value = null;
      messages.value = [];
    }
    sessions.value = sessions.value.filter(s => s.id !== id);
  }

  function sendMessage(text: string): AbortController {
    const controller = new AbortController();
    streamAbortController.value = controller;

    // Add user message optimistically
    const userMsg: ChatMessage = {
      id: Date.now(),
      role: 'USER',
      content: text,
      aiMeta: null,
      createdAt: new Date().toISOString(),
    };
    messages.value.push(userMsg);

    // Add assistant placeholder
    const assistantMsg: ChatMessage = {
      id: Date.now() + 1,
      role: 'ASSISTANT',
      content: '',
      aiMeta: null,
      createdAt: new Date().toISOString(),
    };
    messages.value.push(assistantMsg);
    streamBuffer.value = '';

    isStreaming.value = true;

    const url = buildStreamUrl(currentSessionId.value!, text);
    const eventSource = new EventSource(url);

    eventSource.addEventListener('chunk', (event) => {
      const data = JSON.parse(event.data);
      streamBuffer.value += data.content;
      assistantMsg.content = streamBuffer.value;
    });

    eventSource.addEventListener('done', (event) => {
      const data = JSON.parse(event.data);
      assistantMsg.id = data.messageId;
      assistantMsg.aiMeta = JSON.stringify(data.meta);
      isStreaming.value = false;
      eventSource.close();
      // Refresh sessions to get updated title
      fetchSessions();
    });

    eventSource.addEventListener('error', () => {
      if (isStreaming.value) {
        assistantMsg.content = streamBuffer.value || 'AI 服务暂时不可用，请稍后重试。';
        isStreaming.value = false;
      }
      eventSource.close();
    });

    controller.signal.addEventListener('abort', () => {
      eventSource.close();
      isStreaming.value = false;
      if (!streamBuffer.value) {
        assistantMsg.content = '已停止生成。';
      }
    });

    return controller;
  }

  function stopStreaming() {
    streamAbortController.value?.abort();
    streamAbortController.value = null;
    isStreaming.value = false;
  }

  return {
    sessions,
    currentSessionId,
    messages,
    isStreaming,
    streamBuffer,
    currentSession,
    fetchSessions,
    selectSession,
    createNewSession,
    removeSession,
    sendMessage,
    stopStreaming,
  };
});
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/stores/chat.ts
git commit -m "feat: add useChatStore Pinia store with SSE streaming"
```

---

### Task B3: AiChatMessage Component

**Files:**
- Create: `frontend/src/components/chat/AiChatMessage.vue`

- [ ] **Step 1: Write AiChatMessage.vue (markdown render + typewriter cursor)**

```vue
<script setup lang="ts">
import { computed } from 'vue';
import type { ChatMessage, ChatMeta } from '@/types/chat';

const props = defineProps<{
  message: ChatMessage;
  isStreaming: boolean;
}>();

const meta = computed<ChatMeta | null>(() => {
  if (!props.message.aiMeta) return null;
  try {
    return JSON.parse(props.message.aiMeta);
  } catch {
    return null;
  }
});

const showCursor = computed(() => {
  return props.isStreaming && props.message.role === 'ASSISTANT' && !props.message.content;
});

const renderedContent = computed(() => {
  return simpleMarkdown(props.message.content);
});

function simpleMarkdown(text: string): string {
  if (!text) return '';
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\n\n/g, '</p><p>')
    .replace(/\n/g, '<br>')
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    .replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>');
}
</script>

<template>
  <div class="chat-msg" :class="message.role === 'USER' ? 'chat-msg--user' : 'chat-msg--ai'">
    <div class="chat-msg__avatar">
      <span v-if="message.role === 'USER'">👤</span>
      <span v-else>🧠</span>
    </div>
    <div class="chat-msg__body">
      <div class="chat-msg__content" v-html="renderedContent">
      </div>
      <span v-if="showCursor" class="chat-msg__cursor">▌</span>
      <div v-if="meta" class="chat-msg__meta">
        <span class="meta-tag">{{ meta.model }}</span>
        <span class="meta-tag" v-if="meta.degraded" style="background:var(--danger);color:#fff">降级</span>
        <span class="meta-time">{{ meta.durationMs }}ms</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-msg {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.chat-msg--user {
  flex-direction: row-reverse;
}
.chat-msg__avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--surface-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}
.chat-msg__body {
  max-width: 75%;
}
.chat-msg--user .chat-msg__body {
  background: var(--primary);
  color: #fff;
  border-radius: 12px 12px 4px 12px;
  padding: 10px 14px;
}
.chat-msg--ai .chat-msg__body {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 12px 12px 12px 4px;
  padding: 10px 14px;
  border-left: 3px solid var(--primary);
}
.chat-msg__content :deep(p) { margin: 0 0 8px; }
.chat-msg__content :deep(p:last-child) { margin-bottom: 0; }
.chat-msg__content :deep(code) {
  background: rgba(0,0,0,.06);
  padding: 1px 4px;
  border-radius: 3px;
  font-size: .9em;
}
.chat-msg__content :deep(ul) { margin: 4px 0; padding-left: 18px; }
.chat-msg__cursor {
  animation: blink 1s step-end infinite;
  color: var(--primary);
}
@keyframes blink {
  50% { opacity: 0; }
}
.chat-msg__meta {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: wrap;
}
.meta-tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--surface-soft);
  color: var(--muted);
}
.meta-time {
  font-size: 11px;
  color: var(--muted);
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/chat/AiChatMessage.vue
git commit -m "feat: add AiChatMessage component with markdown and typewriter cursor"
```

---

### Task B4: AiChatInput Component

**Files:**
- Create: `frontend/src/components/chat/AiChatInput.vue`

- [ ] **Step 1: Write AiChatInput.vue**

```vue
<script setup lang="ts">
import { ref, nextTick } from 'vue';
import { Send, Square } from 'lucide-vue-next';

const props = defineProps<{
  isStreaming: boolean;
  disabled: boolean;
}>();

const emit = defineEmits<{
  send: [text: string];
  stop: [];
}>();

const input = ref('');
const textareaRef = ref<HTMLTextAreaElement | null>(null);

function handleSend() {
  const text = input.value.trim();
  if (!text || props.isStreaming) return;
  emit('send', text);
  input.value = '';
  nextTick(() => textareaRef.value?.focus());
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    handleSend();
  }
}
</script>

<template>
  <div class="chat-input">
    <textarea
      ref="textareaRef"
      v-model="input"
      class="chat-input__field"
      :disabled="isStreaming || disabled"
      placeholder="输入您的问题..."
      rows="2"
      @keydown="handleKeydown"
    />
    <button
      v-if="!isStreaming"
      class="chat-input__btn chat-input__btn--send"
      :disabled="disabled || !input.trim()"
      @click="handleSend"
      title="发送"
    >
      <Send :size="16" />
    </button>
    <button
      v-else
      class="chat-input__btn chat-input__btn--stop"
      @click="emit('stop')"
      title="停止生成"
    >
      <Square :size="14" />
    </button>
  </div>
</template>

<style scoped>
.chat-input {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding: 12px;
  border-top: 1px solid var(--border);
  background: var(--surface);
}
.chat-input__field {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  font-family: inherit;
  resize: none;
  outline: none;
  background: var(--bg);
  color: var(--text);
}
.chat-input__field:focus {
  border-color: var(--primary);
}
.chat-input__btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.chat-input__btn--send {
  background: var(--primary);
  color: #fff;
}
.chat-input__btn--send:disabled {
  opacity: .4;
  cursor: not-allowed;
}
.chat-input__btn--stop {
  background: var(--danger);
  color: #fff;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/chat/AiChatInput.vue
git commit -m "feat: add AiChatInput component with send/stop"
```

---

### Task B5: AiChatSessionList Component

**Files:**
- Create: `frontend/src/components/chat/AiChatSessionList.vue`

- [ ] **Step 1: Write AiChatSessionList.vue**

```vue
<script setup lang="ts">
import { Plus, Trash2, MessageSquare } from 'lucide-vue-next';
import type { ChatSession } from '@/types/chat';

defineProps<{
  sessions: ChatSession[];
  currentId: number | null;
}>();

const emit = defineEmits<{
  select: [id: number];
  delete: [id: number];
  new: [];
}>();
</script>

<template>
  <div class="session-list">
    <div class="session-list__header">
      <span class="session-list__title">对话历史</span>
      <button class="session-list__new-btn" @click="emit('new')" title="新建对话">
        <Plus :size="16" />
      </button>
    </div>
    <div class="session-list__items">
      <button
        v-for="s in sessions"
        :key="s.id"
        class="session-item"
        :class="{ active: s.id === currentId }"
        @click="emit('select', s.id)"
      >
        <MessageSquare :size="14" class="session-item__icon" />
        <span class="session-item__title">{{ s.title || '新对话' }}</span>
        <button
          class="session-item__delete"
          @click.stop="emit('delete', s.id)"
          title="删除"
        >
          <Trash2 :size="12" />
        </button>
      </button>
      <p v-if="!sessions.length" class="session-list__empty">暂无对话</p>
    </div>
  </div>
</template>

<style scoped>
.session-list {
  width: 220px;
  border-right: 1px solid var(--border);
  background: var(--surface-soft);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.session-list__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-bottom: 1px solid var(--border);
}
.session-list__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
}
.session-list__new-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: var(--primary);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.session-list__items {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 10px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  color: var(--text);
  text-align: left;
}
.session-item:hover { background: var(--surface); }
.session-item.active { background: var(--primary-soft); }
.session-item__icon { flex-shrink: 0; color: var(--muted); }
.session-item__title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.session-item__delete {
  opacity: 0;
  border: none;
  background: none;
  cursor: pointer;
  color: var(--muted);
  padding: 2px;
}
.session-item:hover .session-item__delete { opacity: 1; }
.session-item__delete:hover { color: var(--danger); }
.session-list__empty {
  text-align: center;
  color: var(--muted);
  font-size: 13px;
  padding: 20px 0;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/chat/AiChatSessionList.vue
git commit -m "feat: add AiChatSessionList sidebar component"
```

---

### Task B6: AiChatPanel — Main Draggable Panel

**Files:**
- Create: `frontend/src/components/chat/AiChatPanel.vue`

- [ ] **Step 1: Write AiChatPanel.vue**

```vue
<script setup lang="ts">
import { onMounted, watch, ref } from 'vue';
import { X, Minimize2, Maximize2 } from 'lucide-vue-next';
import { useChatStore } from '@/stores/chat';
import AiChatMessage from './AiChatMessage.vue';
import AiChatInput from './AiChatInput.vue';
import AiChatSessionList from './AiChatSessionList.vue';

const props = defineProps<{
  visible: boolean;
}>();

const emit = defineEmits<{
  close: [];
}>();

const store = useChatStore();
const minimized = ref(false);
const sessionIdInput = ref('');

onMounted(() => {
  store.fetchSessions();
});

watch(() => props.visible, (v) => {
  if (v) store.fetchSessions();
});

async function handleNewSession() {
  const text = sessionIdInput.value.trim() || '你好';
  await store.createNewSession(text);
}

function handleSend(text: string) {
  store.sendMessage(text);
}

async function handleSelectSession(id: number) {
  await store.selectSession(id);
}

async function handleDeleteSession(id: number) {
  await store.removeSession(id);
}
</script>

<template>
  <Transition name="panel">
    <div v-if="visible" class="chat-panel" :class="{ minimized }">
      <div class="chat-panel__header">
        <span class="chat-panel__title">🧠 AI 助手</span>
        <div class="chat-panel__actions">
          <button class="chat-panel__action-btn" @click="minimized = !minimized" :title="minimized ? '展开' : '最小化'">
            <Maximize2 v-if="minimized" :size="14" />
            <Minimize2 v-else :size="14" />
          </button>
          <button class="chat-panel__action-btn" @click="emit('close')" title="关闭">
            <X :size="14" />
          </button>
        </div>
      </div>

      <div v-if="!minimized" class="chat-panel__body">
        <AiChatSessionList
          :sessions="store.sessions"
          :current-id="store.currentSessionId"
          @select="handleSelectSession"
          @delete="handleDeleteSession"
          @new="handleNewSession"
        />

        <div class="chat-panel__main">
          <div v-if="!store.currentSessionId" class="chat-panel__empty">
            <p>👋 你好！我是云脑医疗助手。</p>
            <p>选择左侧对话或发送消息开始新对话。</p>
            <div class="chat-panel__quick-start">
              <input
                v-model="sessionIdInput"
                class="chat-panel__quick-input"
                placeholder="输入第一条消息..."
                @keydown.enter="handleNewSession"
              />
              <button class="chat-panel__quick-btn" @click="handleNewSession">开始</button>
            </div>
          </div>

          <template v-else>
            <div class="chat-panel__messages">
              <AiChatMessage
                v-for="msg in store.messages"
                :key="msg.id"
                :message="msg"
                :is-streaming="store.isStreaming"
              />
            </div>
            <AiChatInput
              :is-streaming="store.isStreaming"
              :disabled="!store.currentSessionId"
              @send="handleSend"
              @stop="store.stopStreaming()"
            />
          </template>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.chat-panel {
  position: fixed;
  bottom: 80px;
  right: 20px;
  width: 680px;
  height: 520px;
  background: var(--surface);
  border-radius: 12px;
  box-shadow: 0 8px 40px rgba(0,0,0,.15);
  display: flex;
  flex-direction: column;
  z-index: 1000;
  overflow: hidden;
}
.chat-panel.minimized {
  height: auto;
  width: 260px;
}
.chat-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: var(--primary);
  color: #fff;
  cursor: move;
  user-select: none;
}
.chat-panel__title {
  font-size: 14px;
  font-weight: 600;
}
.chat-panel__actions {
  display: flex;
  gap: 4px;
}
.chat-panel__action-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: rgba(255,255,255,.15);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-panel__action-btn:hover { background: rgba(255,255,255,.3); }
.chat-panel__body {
  flex: 1;
  display: flex;
  overflow: hidden;
}
.chat-panel__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.chat-panel__empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--muted);
  padding: 24px;
}
.chat-panel__empty p { margin: 0; font-size: 14px; }
.chat-panel__quick-start {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  width: 100%;
  max-width: 320px;
}
.chat-panel__quick-input {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 13px;
  outline: none;
}
.chat-panel__quick-input:focus { border-color: var(--primary); }
.chat-panel__quick-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  background: var(--primary);
  color: #fff;
  cursor: pointer;
  font-size: 13px;
}
.chat-panel__messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}
.panel-enter-active, .panel-leave-active {
  transition: all .25s ease;
}
.panel-enter-from, .panel-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(.96);
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/chat/AiChatPanel.vue
git commit -m "feat: add AiChatPanel draggable chat panel"
```

---

### Task B7: AiChatLauncher — Floating Button

**Files:**
- Create: `frontend/src/components/chat/AiChatLauncher.vue`

- [ ] **Step 1: Write AiChatLauncher.vue**

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { MessageCircle, Stethoscope } from 'lucide-vue-next';
import AiChatPanel from './AiChatPanel.vue';

const props = defineProps<{
  role: 'doctor' | 'patient';
}>();

const panelVisible = ref(false);
</script>

<template>
  <div class="chat-launcher">
    <button class="chat-launcher__fab" @click="panelVisible = !panelVisible" :title="role === 'doctor' ? 'AI 助手' : '智能分诊'">
      <MessageCircle v-if="role === 'doctor'" :size="22" />
      <Stethoscope v-else :size="22" />
    </button>
    <AiChatPanel :visible="panelVisible" @close="panelVisible = false" />
  </div>
</template>

<style scoped>
.chat-launcher {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 999;
}
.chat-launcher__fab {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: none;
  background: var(--primary);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0,0,0,.2);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform .2s, box-shadow .2s;
}
.chat-launcher__fab:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 24px rgba(0,0,0,.3);
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/chat/AiChatLauncher.vue
git commit -m "feat: add AiChatLauncher floating button"
```

---

### Task B8: Integrate Chat Launcher into Doctor & Patient Views

**Files:**
- Modify: `frontend/src/views/doctor/DoctorHomeView.vue`
- Modify: `frontend/src/views/patient/PatientHomeView.vue`

- [ ] **Step 1: Add AiChatLauncher to DoctorHomeView.vue**

At the end of the `<script setup>` imports block, add:
```typescript
import AiChatLauncher from '@/components/chat/AiChatLauncher.vue';
```

At the end of the `<template>`, just before `</section>`, add:
```html
<AiChatLauncher role="doctor" />
```

- [ ] **Step 2: Add AiChatLauncher to PatientHomeView.vue**

At the end of the `<script setup>` imports block, add:
```typescript
import AiChatLauncher from '@/components/chat/AiChatLauncher.vue';
```

At the end of the `<template>`, just before `</section>`, add:
```html
<AiChatLauncher role="patient" />
```

- [ ] **Step 3: Verify frontend compiles**

```bash
cd frontend && npm run build --if-present || npx vite build
```

Expected: Build succeeds

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/doctor/DoctorHomeView.vue frontend/src/views/patient/PatientHomeView.vue
git commit -m "feat: integrate chat launcher into doctor and patient views"
```

---

### Task C1: Workflow AI — Triage Card-Based Optimization

**Files:**
- Modify: `frontend/src/views/patient/panels/PatientTriagePanel.vue`

- [ ] **Step 1: Read existing PatientTriagePanel.vue**

Read the file to understand current triage result rendering. The component receives `workspace` prop containing `triageResult`, `triaging`, `runTriage`, `triageForm`, `error`, etc.

- [ ] **Step 2: Update triage result rendering with structured cards**

Replace the flat triage result display with card-based layout. The key changes:

In the `<template>`, after the triage form section, replace the triage result display area:

```html
<div v-if="workspace.triageResult" class="triage-result">
  <!-- Recommended Department Card -->
  <div class="triage-card triage-card--dept">
    <div class="triage-card__header">
      <span class="triage-card__icon">🏥</span>
      <span class="triage-card__title">推荐科室</span>
      <span v-if="workspace.triageResult.recommendationSource === 'AI'"
            class="ai-badge">AI 推荐</span>
      <span v-else class="ai-badge ai-badge--local">本地规则匹配</span>
    </div>
    <div class="triage-card__body">
      <strong>{{ workspace.triageResult.recommendedDept }}</strong>
    </div>
  </div>

  <!-- Recommended Doctors Card -->
  <div v-if="workspace.triageResult.recommendedDoctors.length" class="triage-card triage-card--doctors">
    <div class="triage-card__header">
      <span class="triage-card__icon">👨‍⚕️</span>
      <span class="triage-card__title">推荐医生</span>
    </div>
    <div class="triage-card__body">
      <div v-for="doc in workspace.triageResult.recommendedDoctors" :key="doc.id"
           class="doctor-card"
           :class="{ selected: workspace.selectedDoctor?.id === doc.id }"
           @click="workspace.chooseDoctor(doc.id)">
        <div class="doctor-card__avatar">{{ doc.name[0] }}</div>
        <div class="doctor-card__info">
          <span class="doctor-card__name">{{ doc.name }}</span>
          <span class="doctor-card__title">{{ doc.title }}</span>
          <span class="doctor-card__specialty">{{ doc.specialty }}</span>
        </div>
      </div>
    </div>
  </div>

  <!-- AI Reasoning (collapsible) -->
  <details v-if="workspace.triageResult.reason" class="triage-reason">
    <summary>查看 AI 分析依据 ▼</summary>
    <p class="triage-reason__text">{{ workspace.triageResult.reason }}</p>
  </details>

  <!-- Degraded warning -->
  <div v-if="workspace.triageResult.recommendationSource !== 'AI'"
       class="degraded-notice">
    ⚠️ AI 服务不可用，当前使用本地规则引擎匹配，建议结合医生人工判断。
  </div>
</div>
```

- [ ] **Step 3: Add triage card styles to the component `<style scoped>`**

```css
.triage-result { margin-top: 20px; }
.triage-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  margin-bottom: 14px;
  overflow: hidden;
}
.triage-card--dept { border-left: 4px solid var(--primary); }
.triage-card--doctors { border-left: 4px solid var(--accent); }
.triage-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--surface-soft);
  font-size: 13px;
  font-weight: 600;
}
.triage-card__icon { font-size: 16px; }
.triage-card__body { padding: 12px 14px; }
.ai-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: var(--primary-soft);
  color: var(--primary);
  font-weight: 500;
  margin-left: auto;
}
.ai-badge--local {
  background: #fff3cd;
  color: var(--accent);
}
.doctor-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
  margin-bottom: 6px;
}
.doctor-card:hover { background: var(--surface-soft); }
.doctor-card.selected {
  border-color: var(--primary);
  background: var(--primary-soft);
}
.doctor-card__avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
}
.doctor-card__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.doctor-card__name { font-weight: 600; font-size: 14px; }
.doctor-card__title { font-size: 12px; color: var(--muted); }
.doctor-card__specialty { font-size: 12px; color: var(--muted); }
.triage-reason {
  margin-bottom: 14px;
  font-size: 13px;
}
.triage-reason summary {
  cursor: pointer;
  color: var(--primary);
  padding: 6px 0;
}
.triage-reason__text {
  padding: 10px 14px;
  background: var(--surface-soft);
  border-radius: 8px;
  font-size: 13px;
  color: var(--muted);
  border-left: 3px solid var(--primary);
  margin: 4px 0 0;
}
.degraded-notice {
  padding: 10px 14px;
  background: #fffbea;
  border: 1px solid #f0d77b;
  border-radius: 8px;
  font-size: 13px;
  color: var(--accent);
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/patient/panels/PatientTriagePanel.vue
git commit -m "feat: optimize triage panel with card-based AI recommendations"
```

---

### Task C2: Workflow AI — Medical Record Streaming UX

**Files:**
- Modify: `frontend/src/views/doctor/panels/DoctorConsultationPanel.vue`

- [ ] **Step 1: Read existing DoctorConsultationPanel.vue**

Read the file to understand how AI-generated medical records and diagnosis suggestions are displayed. Look for `generatingRecord`, `diagnosingRecord`, `streamText`, `recordForm`, `diagnosisSuggestion`, etc.

- [ ] **Step 2: Add field-level streaming indicator**

In the medical record form section, add an `ai-generated` class and streaming indicator to each field that gets AI-populated. Wrap AI-enabled fields with:

```html
<div class="record-field" :class="{ 'ai-field--streaming': workspace.generatingRecord }">
  <label class="record-field__label">
    {{ fieldLabel }}
    <span v-if="workspace.generatingRecord" class="streaming-cursor">▌</span>
    <span v-else-if="workspace.recordForm.aiGenerated" class="ai-badge-inline">AI 生成</span>
  </label>
  <!-- existing input/textarea -->
</div>
```

- [ ] **Step 3: Add AI generation action buttons**

Below the medical record form actions, add dedicated AI action buttons:

```html
<div class="record-ai-actions">
  <button class="btn btn--ai" @click="workspace.generateDraftMedicalRecord"
          :disabled="workspace.generatingRecord">
    <Sparkles :size="14" />
    <span>{{ workspace.generatingRecord ? 'AI 生成中...' : 'AI 生成病历' }}</span>
  </button>
  <button class="btn btn--ghost" @click="/* adopt all */ workspace.saveCurrentMedicalRecord()"
          :disabled="!workspace.recordForm.chiefComplaint">
    采纳全部
  </button>
  <button class="btn btn--ghost" @click="workspace.generateDraftMedicalRecord">
    重新生成
  </button>
</div>
```

Note: `Sparkles` icon needs to be imported from `lucide-vue-next`:
```typescript
import { /* existing imports */, Sparkles } from 'lucide-vue-next';
```

- [ ] **Step 4: Add streaming and AI styles**

```css
.ai-field--streaming {
  border-left: 3px solid var(--primary);
  padding-left: 10px;
  background: var(--primary-soft);
  transition: border-color .3s;
}
.ai-badge-inline {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 8px;
  background: var(--primary-soft);
  color: var(--primary);
  margin-left: 8px;
  font-weight: 500;
}
.streaming-cursor {
  animation: blink 1s step-end infinite;
  color: var(--primary);
  margin-left: 4px;
}
@keyframes blink { 50% { opacity: 0; } }
.record-ai-actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
  flex-wrap: wrap;
}
.btn--ai {
  background: var(--primary-soft);
  color: var(--primary);
  border: 1px solid var(--primary);
}
.btn--ai:disabled { opacity: .5; cursor: not-allowed; }
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/doctor/panels/DoctorConsultationPanel.vue
git commit -m "feat: add streaming indicators and AI badges to consultation panel"
```

---

### Task C3: Workflow AI — Diagnosis Tags + Confidence

**Files:**
- Modify: `frontend/src/views/doctor/panels/DoctorConsultationPanel.vue` (same file as Task C2, additional changes)

- [ ] **Step 1: Add diagnosis suggestion display with confidence tags**

In the consultation panel, find or add the diagnosis suggestion section. Render suggested diagnoses as colored tags:

```html
<div v-if="workspace.diagnosisSuggestion" class="diagnosis-suggestions">
  <h4 class="suggestion-title">🤖 AI 诊断建议</h4>
  <div class="diagnosis-tags">
    <span
      v-for="(diag, idx) in parseDiagnoses(workspace.diagnosisSuggestion.suggestedDiagnoses)"
      :key="idx"
      class="diagnosis-tag"
      :style="{ background: confidenceBg(diag.confidence), color: confidenceFg(diag.confidence) }"
    >
      {{ diag.name }}
      <span v-if="diag.confidence" class="diagnosis-tag__confidence">
        {{ diag.confidence }}%
      </span>
      <button class="diagnosis-tag__adopt" @click="adoptDiagnosis(diag.name)">采纳</button>
    </span>
  </div>
  <!-- Suggested exam items as checklist -->
  <div v-if="workspace.diagnosisSuggestion.suggestedExamItems" class="exam-checklist">
    <h5>建议检查项目</h5>
    <label v-for="(exam, idx) in parseExamItems(workspace.diagnosisSuggestion.suggestedExamItems)"
           :key="idx" class="exam-item">
      <input type="checkbox" />
      <span>{{ exam }}</span>
    </label>
  </div>
</div>
```

- [ ] **Step 2: Add helper functions in `<script setup>`**

```typescript
interface DiagnosisEntry { name: string; confidence: number }

function parseDiagnoses(text: string): DiagnosisEntry[] {
  if (!text) return [];
  return text.split('\n').filter(Boolean).map(line => {
    const match = line.match(/^(.+?)\s*(\d{1,3})%?\s*$/);
    if (match) return { name: match[1].trim(), confidence: parseInt(match[2]) };
    return { name: line.replace(/^[-*\d.]+\s*/, '').trim(), confidence: 0 };
  });
}

function parseExamItems(text: string): string[] {
  if (!text) return [];
  return text.split('\n').filter(Boolean)
    .map(line => line.replace(/^[-*\d.]+\s*/, '').trim());
}

function confidenceBg(conf: number): string {
  if (conf >= 75) return '#d4edda';
  if (conf >= 50) return '#fff3cd';
  return '#f8f0ff';
}

function confidenceFg(conf: number): string {
  if (conf >= 75) return '#155724';
  if (conf >= 50) return '#856404';
  return '#6f42c1';
}

function adoptDiagnosis(name: string) {
  workspace.recordForm.preliminaryDiagnosis = name;
}
```

- [ ] **Step 3: Add diagnosis suggestion styles**

```css
.diagnosis-suggestions {
  margin-top: 20px;
  padding: 16px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  border-left: 4px solid var(--primary);
}
.suggestion-title { margin: 0 0 12px; font-size: 14px; }
.diagnosis-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.diagnosis-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}
.diagnosis-tag__confidence {
  font-size: 11px;
  opacity: .7;
}
.diagnosis-tag__adopt {
  border: none;
  background: rgba(0,0,0,.08);
  border-radius: 4px;
  padding: 1px 6px;
  font-size: 11px;
  cursor: pointer;
}
.exam-checklist {
  margin-top: 14px;
}
.exam-checklist h5 { margin: 0 0 8px; font-size: 13px; color: var(--muted); }
.exam-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/doctor/panels/DoctorConsultationPanel.vue
git commit -m "feat: add diagnosis confidence tags and exam checklist"
```

---

### Task C4: Workflow AI — Prescription Review Dual-Engine Visualization

**Files:**
- Modify: `frontend/src/views/doctor/panels/DoctorConsultationPanel.vue` (same file, additional changes)

- [ ] **Step 1: Add dual-engine review visualization**

In the prescription review section, replace or enhance the existing review display:

```html
<div v-if="workspace.reviewResult" class="review-result">
  <div class="review-risk-bar" :class="riskBarClass(workspace.reviewResult.riskLevel)">
    <span class="review-risk-bar__label">风险等级: {{ workspace.reviewResult.riskLevel || '未知' }}</span>
  </div>

  <!-- Local Rule Engine Hits -->
  <div v-if="workspace.reviewResult.localRuleHits" class="review-section">
    <h4 class="review-section__title">📋 本地规则引擎</h4>
    <div v-for="(hit, idx) in parseRuleHits(workspace.reviewResult.localRuleHits)"
         :key="idx" class="rule-hit" :class="'rule-hit--' + (hit.riskLevel || 'WARNING').toLowerCase()">
      <span class="rule-hit__risk" :data-level="hit.riskLevel">
        {{ riskLabel(hit.riskLevel) }}
      </span>
      <div class="rule-hit__body">
        <strong>{{ hit.ruleName || hit.alertMessage }}</strong>
        <p v-if="hit.alertMessage">{{ hit.alertMessage }}</p>
        <p v-if="hit.suggestion" class="rule-hit__suggestion">💡 {{ hit.suggestion }}</p>
      </div>
    </div>
  </div>

  <!-- LLM Analysis -->
  <div v-if="workspace.reviewResult.llmSummary" class="review-section review-section--llm">
    <h4 class="review-section__title">
      🤖 AI 分析补充
      <span class="ai-badge">AI 生成</span>
    </h4>
    <p class="llm-summary">{{ workspace.reviewResult.llmSummary }}</p>
  </div>
</div>
```

- [ ] **Step 2: Add helper functions**

```typescript
interface RuleHitEntry {
  ruleName?: string;
  alertMessage?: string;
  suggestion?: string;
  riskLevel?: string;
}

function parseRuleHits(raw: string | Record<string, unknown>[]): RuleHitEntry[] {
  if (Array.isArray(raw)) return raw as RuleHitEntry[];
  if (typeof raw === 'string') {
    try { return JSON.parse(raw) as RuleHitEntry[]; }
    catch { return []; }
  }
  return [];
}

function riskLabel(level: string | null | undefined): string {
  switch ((level || '').toUpperCase()) {
    case 'HIGH': case 'DANGER': case 'CRITICAL': return '🔴 高风险';
    case 'MEDIUM': case 'WARNING': return '🟡 中风险';
    default: return '🟢 低风险';
  }
}

function riskBarClass(level: string | null | undefined): string {
  switch ((level || '').toUpperCase()) {
    case 'HIGH': case 'DANGER': case 'CRITICAL': return 'risk--high';
    case 'MEDIUM': case 'WARNING': return 'risk--medium';
    default: return 'risk--low';
  }
}
```

- [ ] **Step 3: Add review styles**

```css
.review-result { margin-top: 20px; }
.review-risk-bar {
  padding: 10px 16px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 14px;
}
.risk--high { background: #f8d7da; color: #721c24; border-left: 4px solid var(--danger); }
.risk--medium { background: #fff3cd; color: #856404; border-left: 4px solid var(--accent); }
.risk--low { background: #d4edda; color: #155724; border-left: 4px solid var(--success); }
.review-section {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 12px;
}
.review-section--llm {
  border-left: 4px solid var(--primary);
  background: var(--primary-soft);
}
.review-section__title { margin: 0 0 10px; font-size: 14px; }
.rule-hit {
  display: flex;
  gap: 12px;
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 8px;
  background: var(--bg);
}
.rule-hit__risk {
  flex-shrink: 0;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
}
.rule-hit__risk[data-level="HIGH"],
.rule-hit__risk[data-level="DANGER"],
.rule-hit__risk[data-level="CRITICAL"] { background: #f8d7da; color: #721c24; }
.rule-hit__risk[data-level="MEDIUM"],
.rule-hit__risk[data-level="WARNING"] { background: #fff3cd; color: #856404; }
.rule-hit__risk[data-level="LOW"],
.rule-hit__risk[data-level="INFO"] { background: #d4edda; color: #155724; }
.rule-hit__body p { margin: 4px 0 0; font-size: 13px; color: var(--muted); }
.rule-hit__suggestion { color: var(--primary) !important; }
.llm-summary { font-size: 14px; line-height: 1.6; margin: 0; }
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/doctor/panels/DoctorConsultationPanel.vue
git commit -m "feat: add dual-engine prescription review visualization"
```

---

### Task C5: Global AI Interaction Styles

**Files:**
- Create: `frontend/src/styles/ai-global.css`
- Modify: `frontend/src/styles/base.css`

- [ ] **Step 1: Write ai-global.css**

```css
/* AI Global Interaction Styles */

/* All AI-generated content gets a light blue accent */
.ai-content {
  background: #f0f7ff;
  border-left: 3px solid var(--primary);
  padding: 12px 16px;
  border-radius: 6px;
  margin: 8px 0;
  position: relative;
}

/* AI badge - small label on AI content */
.ai-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: var(--primary-soft);
  color: var(--primary);
  font-weight: 500;
  white-space: nowrap;
}

/* Degraded (non-AI) badge */
.degraded-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #fff3cd;
  color: var(--accent);
  font-weight: 500;
}

/* Typewriter cursor animation */
@keyframes ai-cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.ai-cursor {
  display: inline-block;
  width: 2px;
  height: 1.1em;
  background: var(--primary);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: ai-cursor-blink 1s step-end infinite;
}

/* Skeleton loading pulse for non-streaming AI */
@keyframes ai-pulse {
  0%, 100% { opacity: .4; }
  50% { opacity: .8; }
}

.ai-skeleton {
  background: var(--surface-soft);
  border-radius: 6px;
  animation: ai-pulse 1.5s ease-in-out infinite;
  min-height: 20px;
  margin: 4px 0;
}

/* Degraded notice bar */
.ai-degraded-notice {
  padding: 8px 14px;
  background: #fffbea;
  border: 1px solid #f0d77b;
  border-radius: 8px;
  font-size: 13px;
  color: var(--accent);
  display: flex;
  align-items: center;
  gap: 6px;
}

/* Feedback buttons */
.ai-feedback {
  display: flex;
  gap: 4px;
  margin-top: 8px;
}

.ai-feedback__btn {
  border: none;
  background: none;
  cursor: pointer;
  font-size: 14px;
  padding: 2px 6px;
  border-radius: 4px;
  opacity: .5;
  transition: opacity .2s;
}

.ai-feedback__btn:hover { opacity: 1; }

/* Copy button for AI content */
.ai-copy-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  border: none;
  background: var(--surface);
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 12px;
  cursor: pointer;
  opacity: 0;
  transition: opacity .2s;
  color: var(--muted);
}

.ai-content:hover .ai-copy-btn { opacity: 1; }
```

- [ ] **Step 2: Import ai-global.css in base.css**

Add at the end of `frontend/src/styles/base.css`:
```css
@import './ai-global.css';
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/styles/ai-global.css frontend/src/styles/base.css
git commit -m "feat: add global AI interaction styles"
```

---

### Task D1: Prompt Engineering V8 Migration (was V8, now V8-like)

> **Note:** The migration `V8__add_chat_tables.sql` was created in Task A5. This prompt optimization migration should be numbered `V10__optimize_prompt_templates.sql` to run after both chat tables (V8) and seed data (V9).

**Files:**
- Create: `backend/src/main/resources/db/migration/V10__optimize_prompt_templates.sql`

- [ ] **Step 1: Write V10__optimize_prompt_templates.sql**

```sql
-- V10__optimize_prompt_templates.sql
-- Optimize existing prompt templates and add CHAT task type

-- ============================================================
-- TRIAGE: 急诊分诊护士长
-- ============================================================
UPDATE prompt_template
SET template_body = '你是医院智能分诊助手，拥有20年急诊分诊经验。根据患者的症状描述，推荐最合适的科室和医生。

## 核心规则
1. 仅基于症状推荐，不做出确定性诊断
2. 如果症状涉及多个科室，按优先级排列，推荐不超过2个科室
3. 对于危急症状（胸痛、呼吸困难、大出血、意识丧失等），urgency_level设为urgent
4. 使用专业但易懂的语言
5. 如果信息不足以判断，明确指出需要补充的信息

## 科室匹配参考
- 胸痛、胸闷、心悸 → 心内科(cardiology)
- 头痛、眩晕、抽搐 → 神经内科(neurology)
- 骨折、关节痛、腰腿痛 → 骨科(orthopedics)
- 皮疹、过敏、皮肤问题 → 皮肤科(dermatology)
- 发热、咳嗽、腹痛等内科症状 → 内科(internal-medicine)
- 儿童患者 → 优先儿科相关科室

## 输出格式
输出JSON，包含以下字段：
- recommended_dept: 推荐科室名称
- recommended_doctors: 推荐医生姓名列表（从可用医生中选择）
- urgency_level: normal|urgent|emergency
- reasoning: 分析理由（1-3句话）',
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE template_code = 'builtin-TRIAGE' AND is_default = TRUE;

-- ============================================================
-- MEDICAL_RECORD: 资深主治医师
-- ============================================================
UPDATE prompt_template
SET template_body = '你是资深主治医师，擅长撰写规范的结构化病历。根据问诊对话内容，生成专业病历草稿。

## 病历结构
1. **主诉**: 患者的主要症状和持续时间（简明扼要，不超过20字）
2. **现病史**: 发病时间、诱因、症状演变、伴随症状、已做检查
3. **既往史**: 相关既往病史、用药史、过敏史
4. **体格检查**: 生命体征及阳性体征
5. **初步诊断**: 基于现有信息的初步诊断（可写"待完善"）
6. **治疗计划**: 进一步检查建议、用药建议、随访建议

## 写作规范
- 使用专业医学术语
- 客观描述，避免主观臆断
- 信息缺失的字段标注"[待补充]"
- 不编造患者未提及的症状或体征
- 如果对话信息不足以支撑完整病历，在可用信息基础上生成，缺失字段标注待补充

## 输出格式
以清晰的分段文本输出，每个段落对应上述结构。',
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE template_code = 'builtin-MEDICAL_RECORD' AND is_default = TRUE;

-- ============================================================
-- DIAGNOSIS: 多学科会诊顾问
-- ============================================================
UPDATE prompt_template
SET template_body = '你是多学科会诊(MDT)顾问，根据病历和问诊信息提供鉴别诊断思路。

## 分析要求
1. 列出2-5个可能的诊断，按可能性从高到低排列
2. 每个诊断附带置信度（百分数，基于现有信息）
3. 区分"可能性较高的诊断"和"需要排除的诊断"
4. 建议的检查项目按优先级排列（必查/建议查/可选查）

## 安全边界
- 明确声明：此为AI辅助诊断建议，最终诊断由执业医师决定
- 不遗漏危急重症的可能
- 如果症状指向危急情况，首先建议紧急处理

## 输出格式
第一段：可能性较高的诊断（每个一行，附带置信度百分比）
第二段：需要排除的诊断
第三段：建议检查项目（按优先级分组）
第四段：特别提示（如有危急信号或注意事项）',
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE template_code = 'builtin-DIAGNOSIS' AND is_default = TRUE;

-- ============================================================
-- PRESCRIPTION_REVIEW: 临床药师
-- ============================================================
UPDATE prompt_template
SET template_body = '你是临床药师，负责审核处方的合理性和安全性。

## 审核维度（对每个维度给出pass/warning/danger判定）

### 1. 适应症匹配
- 处方药品是否与诊断匹配
- 是否存在无适应症用药

### 2. 剂量合理性
- 剂量是否在推荐范围内
- 特殊人群（老年人、儿童、肝肾功能不全）是否需要调整剂量

### 3. 药物相互作用
- 处方药物之间是否有已知相互作用
- 与患者可能正在使用的其他药物的潜在相互作用

### 4. 禁忌症
- 是否存在基于患者既往史、过敏史的禁忌用药

### 5. 特殊人群考量
- 孕妇、哺乳期、老年人、儿童的特殊注意

## 最终评估
- 综合所有维度给出风险等级：低风险(LOW)/中风险(MEDIUM)/高风险(HIGH)
- 如有任何danger级判定，风险等级不低于MEDIUM
- 给出自然语言总结和建议

## 输出文本
先给出各维度判定（pass/warning/danger + 简短理由），然后给出综合风险等级，最后给出自然语言总结。',
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE template_code = 'builtin-PRESCRIPTION_REVIEW' AND is_default = TRUE;

-- ============================================================
-- CHAT: 云脑医疗助手 (NEW)
-- ============================================================
INSERT INTO prompt_template (template_code, task_type, dept_code, template_body, variable_whitelist, version, is_default, status, created_at, updated_at)
SELECT 'builtin-CHAT', 'CHAT', NULL,
'你是"云脑医疗助手"，一款专业的医疗AI助手，为医生和患者提供支持。

## 用户身份
当前沟通对象是：{{userRole}}（DOCTOR为医生，PATIENT为患者）

## 医生模式（userRole=DOCTOR）
你作为同行顾问，使用专业术语交流，辅助临床决策。
你可以帮助：
- 提供鉴别诊断思路
- 解读检查检验结果
- 查询药物信息（适应症、禁忌、相互作用）
- 讨论诊疗方案
- 病历书写建议
- 处方合理性分析

## 患者模式（userRole=PATIENT）
你作为健康顾问，用通俗易懂的语言交流，强调就医必要性。
你可以帮助：
- 症状初步分析（基于循证医学知识）
- 就医科室建议
- 检查项目及准备事项解释
- 健康生活方式建议
- 用药注意事项提醒（不推荐具体药品剂量）

## 核心安全规则（优先执行）
1. 绝不提供确定性诊断结论，明确说明"最终诊断需由执业医师结合体格检查和辅助检查确定"
2. 绝不推荐具体药品剂量，仅提供一般性药物知识
3. 遇到紧急症状描述（胸痛、呼吸困难、大出血、意识改变、严重外伤等），第一反应是建议立即就医或拨打急救电话
4. 对超出知识范围的问题，诚实说明并建议咨询专科医生
5. 不提供替代执业医师的医疗建议
6. 不评价或贬低具体医生或医疗机构的诊疗方案

## 输出风格
- 简洁清晰，分点列举，避免冗长段落
- 使用Markdown格式化，但避免复杂表格
- 引用权威医学指南时标注来源（如"根据《中国XX指南（2024版）》"）
- 对不确定的信息标注"需要进一步确认"',
'userRole', 1, TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_template WHERE template_code = 'builtin-CHAT'
);

-- ============================================================
-- AI Config for CHAT task type
-- ============================================================
INSERT INTO ai_config (provider, model_name, api_url, api_key_encrypted, key_version, task_scope, timeout_seconds, is_default, health_status, config_version, enabled, priority, status, created_at, updated_at)
SELECT 'doubao', 'doubao-seed-1-6-251015', NULL, NULL, NULL, 'CHAT', 60, TRUE, 'UNKNOWN', '1.0.0', TRUE, 10, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM ai_config WHERE task_scope = 'CHAT' AND is_default = TRUE AND status = 'ACTIVE'
);
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/resources/db/migration/V10__optimize_prompt_templates.sql
git commit -m "feat: add V10 optimized prompt templates and chat AI config"
```

---

### Task E1: Seed Hospital Data Migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V9__seed_hospital_data.sql`

- [ ] **Step 1: Write V9__seed_hospital_data.sql**

```sql
-- V9__seed_hospital_data.sql
-- Seed realistic hospital data for a small clinic
-- All inserts use WHERE NOT EXISTS for idempotency

-- ============================================================
-- DEPARTMENTS (add 4 new, keep existing internal-medicine)
-- ============================================================
INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'cardiology', '心内科', '二级科室', '冠心病、高血压、心律失常、心力衰竭等心血管系统疾病', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'cardiology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'neurology', '神经内科', '二级科室', '头痛、眩晕、癫痫、帕金森综合征、脑血管病等神经系统疾病', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'neurology');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'orthopedics', '骨科', '一级科室', '骨折创伤、关节疾病、脊柱疾病、运动损伤', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'orthopedics');

INSERT INTO department (code, name, type, description, status, created_at, updated_at)
SELECT 'dermatology', '皮肤科', '一级科室', '过敏性皮肤病、银屑病、痤疮、湿疹、真菌感染等皮肤疾病', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'dermatology');

-- ============================================================
-- DOCTORS (keep existing doctor01, add 7 more)
-- ============================================================
-- Password: doctor123 (BCrypt, same as existing)
INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '李心怡', (SELECT id FROM department WHERE code = 'cardiology'),
       '副主任医师', '冠心病、高血压、心律失常',
       '从事心血管内科临床工作15年，擅长冠心病介入治疗和高血压管理。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor02');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor03', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '王建华', (SELECT id FROM department WHERE code = 'neurology'),
       '主任医师', '头痛、癫痫、帕金森综合征',
       '神经内科主任医师，博士生导师，从事神经病学临床和科研工作30年，在癫痫和帕金森病诊疗方面有丰富经验。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor03');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor04', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '陈思远', (SELECT id FROM department WHERE code = 'neurology'),
       '主治医师', '脑血管病、眩晕、失眠',
       '神经内科主治医师，专注于脑血管病的急性期治疗和二级预防，对眩晕和睡眠障碍诊疗有深入研究。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor04');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor05', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '赵刚', (SELECT id FROM department WHERE code = 'orthopedics'),
       '副主任医师', '骨折创伤、关节置换',
       '骨科副主任医师，擅长四肢骨折微创治疗和髋膝关节置换术，年手术量超过500台。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor05');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor06', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '刘磊', (SELECT id FROM department WHERE code = 'orthopedics'),
       '主治医师', '运动损伤、脊柱微创',
       '骨科主治医师，运动医学方向，擅长关节镜手术和脊柱微创治疗。曾担任省级运动队医疗保障医师。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor06');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor07', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '孙丽华', (SELECT id FROM department WHERE code = 'dermatology'),
       '主任医师', '过敏性皮肤病、银屑病、痤疮',
       '皮肤科主任医师，从事皮肤科临床工作25年，在银屑病生物制剂治疗和疑难皮肤病诊断方面经验丰富。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor07');

INSERT INTO doctor (username, password_hash, name, department_id, title, specialty, introduction, status, created_at, updated_at)
SELECT 'doctor08', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '周晓峰', (SELECT id FROM department WHERE code = 'dermatology'),
       '主治医师', '湿疹、荨麻疹、真菌感染',
       '皮肤科主治医师，专注于过敏性皮肤病的综合治疗和皮肤真菌病的规范化诊疗。',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE username = 'doctor08');

-- ============================================================
-- PATIENTS (add 24 new, keep existing patient01)
-- Password: patient123 (BCrypt, same as existing)
-- ============================================================
INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '王小明', '13800010002', 'MALE', '2018-03-15', 8, '无', '无', '110101201803150002', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient02');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient03', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '刘思琪', '13800010003', 'FEMALE', '2001-07-22', 25, '青霉素', '无', '320501200107220003', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient03');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient04', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '陈建国', '13800010004', 'MALE', '1968-11-03', 58, '无', '高血压10年，规律服用降压药', '440103196811030004', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient04');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient05', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '张秀兰', '13800010005', 'FEMALE', '1959-05-18', 67, '磺胺类', '糖尿病15年，冠心病，口服二甲双胍和阿托伐他汀', '330102195905180005', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient05');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient06', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '李明', '13800010006', 'MALE', '1991-02-14', 35, '无', '无', '510107199102140006', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient06');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient07', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '赵雪', '13800010007', 'FEMALE', '1998-09-30', 28, '头孢类', '过敏性鼻炎（季节性）', '410103199809300007', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient07');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient08', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '孙文博', '13800010008', 'MALE', '1984-06-20', 42, '无', '高血脂，口服阿托伐他汀', '320102198406200008', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient08');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient09', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '周婷婷', '13800010009', 'FEMALE', '2007-12-01', 19, '无', '无', '500112200712010009', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient09');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient10', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '吴国栋', '13800010010', 'MALE', '1971-04-08', 55, '阿司匹林', '高血压8年，痛风病史3年', '440304197104080010', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient10');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient11', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '郑小雨', '13800010011', 'FEMALE', '2014-08-25', 12, '无', '哮喘（儿童期发作，间歇使用吸入剂）', '330108201408250011', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient11');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient12', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '冯丽', '13800010012', 'FEMALE', '1981-01-12', 45, '无', '甲状腺功能减退症，口服左甲状腺素钠片', '610103198101120012', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient12');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient13', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '褚志强', '13800010013', 'MALE', '1964-10-17', 62, '无', '2型糖尿病10年、高血压15年，口服二甲双胍和氨氯地平', '120104196410170013', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient13');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient14', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '蒋芳', '13800010014', 'FEMALE', '1995-03-28', 31, '无', '无', '430103199503280014', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient14');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient15', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '沈浩', '13800010015', 'MALE', '1978-07-05', 48, '碘造影剂', '冠心病，PCI支架术后2年，口服阿司匹林和氯吡格雷', '310105197807050015', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient15');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient16', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '韩雪梅', '13800010016', 'FEMALE', '1954-12-22', 72, '青霉素、头孢类', '骨质疏松（椎体压缩骨折史）、高血压20年', '210102195412220016', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient16');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient17', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '杨波', '13800010017', 'MALE', '2000-05-10', 26, '无', '无', '530102200005100017', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient17');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient18', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '朱琳', '13800010018', 'FEMALE', '1988-11-08', 38, '无', '慢性胃炎（胃镜确诊），偶用奥美拉唑', '370102198811080018', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient18');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient19', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '秦汉', '13800010019', 'MALE', '1956-02-28', 70, '无', '前列腺增生、高血压，口服坦索罗辛和氨氯地平', '610103195602280019', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient19');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient20', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '许诺', '13800010020', 'FEMALE', '2010-06-15', 16, '无', '无', '450103201006150020', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient20');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient21', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '何勇', '13800010021', 'MALE', '1974-09-12', 52, '无', '酒精性肝病，肝功能轻度异常，定期复查', '340103197409120021', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient21');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient22', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '吕萍', '13800010022', 'FEMALE', '1966-04-03', 60, '磺胺类', '类风湿关节炎15年，口服甲氨蝶呤，偶用塞来昔布止痛', '420103196604030022', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient22');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient23', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '施伟', '13800010023', 'MALE', '2004-01-20', 22, '无', '无', '350203200401200023', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient23');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient24', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '张蕾', '13800010024', 'FEMALE', '1993-08-08', 33, '无', '妊娠24周，定期产检，无妊娠并发症', '510105199308080024', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient24');

INSERT INTO patient (username, password_hash, name, phone, gender, birth_date, age, allergy_history, medical_history, id_card_number, status, created_at, updated_at)
SELECT 'patient25', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '许文强', '13800010025', 'MALE', '1948-10-01', 78, '无', '高血压30年、2型糖尿病20年、慢性肾脏病3期，口服多种药物', '310101194810010025', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE username = 'patient25');

-- ============================================================
-- DRUGS (15 common drugs)
-- ============================================================
INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-001', '阿托伐他汀钙片', 'ATFTTGP', '20mg×7片', '片剂', '盒', '辉瑞制药', 42.50,
       '口服，每次20mg，每日1次，晚餐时服用',
       '活动性肝病、不明原因转氨酶持续升高、妊娠及哺乳期妇女',
       '治疗前及治疗期间监测肝功能，出现肌痛需查CK',
       '高胆固醇血症、混合型高脂血症、冠心病',
       '与环孢素、克拉霉素、伊曲康唑合用增加肌病风险',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-001');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-002', '苯磺酸氨氯地平片', 'BHSALDPP', '5mg×7片', '片剂', '盒', '辉瑞制药', 32.80,
       '口服，每次5mg，每日1次，可增至10mg',
       '严重低血压、主动脉瓣狭窄',
       '肝功能不全者慎用，老年人从小剂量开始',
       '原发性高血压、稳定性心绞痛',
       '与CYP3A4抑制剂合用需监测',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-002');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-003', '阿司匹林肠溶片', 'ASPLCRP', '100mg×30片', '片剂', '盒', '拜耳医药', 15.60,
       '口服，每次100mg，每日1次，餐前整片吞服',
       '活动性消化性溃疡、出血体质、对阿司匹林过敏',
       '长期使用注意消化道出血风险，手术前需停药',
       '心脑血管疾病预防、稳定型心绞痛、缺血性卒中',
       '与抗凝药合用增加出血风险',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-003');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-004', '盐酸氟桂利嗪胶囊', 'YSFGLQJN', '5mg×20粒', '胶囊', '盒', '西安杨森', 28.30,
       '口服，每次5-10mg，每晚1次',
       '抑郁症、帕金森病、锥体外系疾病',
       '长期使用可能出现体重增加和嗜睡',
       '偏头痛预防、眩晕（中枢性或周围性）',
       '与酒精或镇静药合用加重嗜睡',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-004');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-005', '卡马西平片', 'KMXPP', '100mg×100片', '片剂', '瓶', '诺华制药', 45.00,
       '口服，初始每次100mg，每日2次，逐渐增量',
       '房室传导阻滞、骨髓抑制、对卡马西平过敏',
       '需监测血常规和肝功能，避免突然停药',
       '癫痫（部分性发作、全身强直-阵挛发作）、三叉神经痛',
       '与多种药物有相互作用，需查阅完整说明书',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-005');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-006', '布洛芬缓释胶囊', 'BLFHSJN', '300mg×20粒', '胶囊', '盒', '中美史克', 18.90,
       '口服，每次300mg，每日2次',
       '活动性消化性溃疡、严重心衰、对NSAID过敏',
       '肾功能不全者慎用，不推荐长期大量使用',
       '轻中度疼痛（头痛、关节痛、牙痛、痛经）、发热',
       '与抗凝药、甲氨蝶呤合用需谨慎',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-006');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-007', '塞来昔布胶囊', 'SLXBJN', '200mg×6粒', '胶囊', '盒', '辉瑞制药', 38.50,
       '口服，每次200mg，每日1-2次',
       '磺胺类药物过敏、活动性消化道溃疡、严重心衰',
       '心血管疾病患者慎用',
       '骨关节炎、类风湿关节炎、强直性脊柱炎',
       '与华法林合用增加出血风险',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-007');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-008', '氯雷他定片', 'LLTDP', '10mg×6片', '片剂', '盒', '先灵葆雅', 22.30,
       '口服，每次10mg，每日1次',
       '对氯雷他定或其辅料过敏',
       '肝功能不全者起始剂量减半',
       '过敏性鼻炎、慢性特发性荨麻疹',
       '与酮康唑、红霉素合用增加血药浓度',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-008');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-009', '糠酸莫米松乳膏', 'KSMMSRG', '5g/支', '乳膏', '支', '默沙东', 35.20,
       '外用，每日1次，涂于患处',
       '皮肤感染（细菌、真菌、病毒）、酒渣鼻、口周皮炎',
       '不宜长期大面积使用，面部和皮肤皱褶处慎用',
       '湿疹、特应性皮炎、接触性皮炎、银屑病',
       '无明显全身药物相互作用',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-009');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-010', '奥美拉唑肠溶胶囊', 'AMLZCRJN', '20mg×14粒', '胶囊', '盒', '阿斯利康', 52.40,
       '口服，每次20mg，每日1-2次，晨起空腹服用',
       '对奥美拉唑或苯并咪唑类过敏',
       '长期使用需注意维生素B12缺乏和骨折风险',
       '胃食管反流病、消化性溃疡、根除幽门螺杆菌（联合方案）',
       '与氯吡格雷合用降低后者抗血小板效果',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-010');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-011', '盐酸二甲双胍片', 'YSEJSGP', '500mg×20片', '片剂', '盒', '中美史克', 12.80,
       '口服，起始每次500mg，每日2次，餐中服用，可增至2000mg/日',
       '严重肾功能不全(eGFR<30)、急性代谢性酸中毒',
       '使用含碘造影剂前需暂停，肾功能监测',
       '2型糖尿病（一线用药）',
       '与造影剂、酒精合用需注意',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-011');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-012', '头孢呋辛酯片', 'TBFXZP', '250mg×12片', '片剂', '盒', '葛兰素史克', 46.70,
       '口服，每次250mg，每日2次，餐后服用',
       '头孢菌素类过敏、青霉素严重过敏者',
       '肾功能不全者需调整剂量',
       '呼吸道感染、泌尿道感染、皮肤软组织感染',
       '与丙磺舒合用延长半衰期',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-012');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-013', '阿莫西林胶囊', 'AMXLJN', '500mg×24粒', '胶囊', '盒', '联邦制药', 18.20,
       '口服，每次500mg，每8小时1次',
       '青霉素过敏、传染性单核细胞增多症',
       '肾功能不全者延长给药间隔',
       '敏感菌引起的呼吸道、泌尿道、胆道感染',
       '与别嘌醇合用增加皮疹风险',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-013');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-014', '氯化钠注射液', 'LHNYSY', '250ml:2.25g/袋', '注射液', '袋', '科伦药业', 4.50,
       '静脉滴注，用量遵医嘱',
       '高钠血症、水钠潴留',
       '心衰、高血压、肾功能不全患者慎用',
       '脱水、低钠血症、药物稀释溶剂',
       '与多种药物配伍使用',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-014');

INSERT INTO drug (code, name, pinyin_code, specification, dosage_form, package_unit, manufacturer, unit_price, default_usage, contraindications, precautions, indications, interaction_summary, status, created_at, updated_at)
SELECT 'DRUG-015', '葡萄糖注射液', 'PTTZSY', '500ml:25g/袋', '注射液', '袋', '科伦药业', 5.20,
       '静脉滴注，用量遵医嘱',
       '未纠正的糖尿病酮症酸中毒、高血糖高渗状态',
       '糖尿病患者需监测血糖',
       '补充能量和体液、低血糖、药物稀释溶剂',
       '与多种药物配伍使用',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM drug WHERE code = 'DRUG-015');

-- ============================================================
-- SCHEDULES (7-day rolling from today for all 8 doctors)
-- ============================================================
-- Use a function-like approach: generate schedules for each doctor
-- Each doctor gets AM (30 slots) and PM (20 slots) for next 7 days

-- Helper: insert schedule if not exists for a given doctor+date+period
-- Using a simple approach with explicit dates relative to CURRENT_DATE

-- Doctor 3001 (张明远, 内科)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT 3001, (SELECT id FROM department WHERE code = 'internal-medicine'), DATE_ADD(CURRENT_DATE, INTERVAL n DAY), 'AM', 30, 15+FLOOR(RAND()*16), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = 3001 AND work_date = DATE_ADD(CURRENT_DATE, INTERVAL n DAY) AND period = 'AM');

INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT 3001, (SELECT id FROM department WHERE code = 'internal-medicine'), DATE_ADD(CURRENT_DATE, INTERVAL n DAY), 'PM', 20, 5+FLOOR(RAND()*16), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = 3001 AND work_date = DATE_ADD(CURRENT_DATE, INTERVAL n DAY) AND period = 'PM');

-- Remaining 7 doctors: doctor02-doctor08
-- doctor02 (李心怡, 心内科)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'cardiology'), DATE_ADD(CURRENT_DATE, INTERVAL days.n DAY), period.p, period.slots, GREATEST(0, period.slots - FLOOR(RAND()*20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor02') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = DATE_ADD(CURRENT_DATE, INTERVAL days.n DAY) AND period = period.p);

-- doctor03 (王建华, 神经内科)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'neurology'), DATE_ADD(CURRENT_DATE, INTERVAL days.n DAY), period.p, period.slots, GREATEST(0, period.slots - FLOOR(RAND()*20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor03') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = DATE_ADD(CURRENT_DATE, INTERVAL days.n DAY) AND period = period.p);

-- doctor04 (陈思远, 神经内科)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'neurology'), DATE_ADD(CURRENT_DATE, INTERVAL days.n DAY), period.p, period.slots, GREATEST(0, period.slots - FLOOR(RAND()*20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor04') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = DATE_ADD(CURRENT_DATE, INTERVAL days.n DAY) AND period = period.p);

-- doctor05 (赵刚, 骨科)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'orthopedics'), DATE_ADD(CURRENT_DATE, INTERVAL days.n DAY), period.p, period.slots, GREATEST(0, period.slots - FLOOR(RAND()*20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor05') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = DATE_ADD(CURRENT_DATE, INTERVAL days.n DAY) AND period = period.p);

-- doctor06 (刘磊, 骨科)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'orthopedics'), DATE_ADD(CURRENT_DATE, INTERVAL days.n DAY), period.p, period.slots, GREATEST(0, period.slots - FLOOR(RAND()*20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor06') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = DATE_ADD(CURRENT_DATE, INTERVAL days.n DAY) AND period = period.p);

-- doctor07 (孙丽华, 皮肤科)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'dermatology'), DATE_ADD(CURRENT_DATE, INTERVAL days.n DAY), period.p, period.slots, GREATEST(0, period.slots - FLOOR(RAND()*20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor07') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = DATE_ADD(CURRENT_DATE, INTERVAL days.n DAY) AND period = period.p);

-- doctor08 (周晓峰, 皮肤科)
INSERT INTO schedule (doctor_id, department_id, work_date, period, total_slots, remaining_slots, visit_level, status, version, created_at, updated_at)
SELECT d.id, (SELECT id FROM department WHERE code = 'dermatology'), DATE_ADD(CURRENT_DATE, INTERVAL days.n DAY), period.p, period.slots, GREATEST(0, period.slots - FLOOR(RAND()*20)), 'NORMAL', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT id FROM doctor WHERE username = 'doctor08') d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days
CROSS JOIN (SELECT 'AM' AS p, 30 AS slots UNION SELECT 'PM', 20) period
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE doctor_id = d.id AND work_date = DATE_ADD(CURRENT_DATE, INTERVAL days.n DAY) AND period = period.p);
```

- [ ] **Step 2: Verify migration syntax**

Run the SQL against a local MySQL or H2 database to verify syntax. The `WHERE NOT EXISTS` pattern is compatible with both.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/V9__seed_hospital_data.sql
git commit -m "feat: add V9 hospital seed data migration"
```

---

## Plan Self-Review Checklist

1. **Spec coverage**: Each spec section maps to tasks:
   - A (Chat Backend) → Tasks A1–A5 ✓
   - B (Chat Frontend) → Tasks B1–B8 ✓
   - C (Prompt Engineering) → Task D1 (V10) ✓
   - D (Seed Data) → Task E1 (V9) ✓

2. **No placeholders**: All code is provided inline, no TBD/TODO. ✓

3. **Type consistency**: 
   - ChatSession/ChatMessage entities match repository signatures ✓
   - TypeScript types match API responses ✓
   - Store actions match component usage ✓

4. **Migration order**: V8 (chat tables) → V9 (seed data) → V10 (optimized prompts). All sequential, no conflicts. ✓
