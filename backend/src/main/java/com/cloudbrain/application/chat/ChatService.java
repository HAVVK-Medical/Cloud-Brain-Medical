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
    private static final int MAX_CONTEXT_MESSAGES = 20;
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

        messageRepo.save(new ChatMessageEntity(session, "USER", message.trim()));

        boolean needsTitle = session.getTitle() == null;

        List<ChatMessageEntity> history = messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<AIModels.AIMessage> contextMessages = buildContextMessages(history);

        SseEmitter emitter = new SseEmitter(2 * 60 * 1000L);

        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder fullResponse = new StringBuilder();
                AIModels.AIExecutionOutcome<String> outcome = aiInvocationService.chat(
                        "CHAT", null, Map.of("userRole", userRole),
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

                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("provider", outcome.meta().provider());
                meta.put("model", outcome.meta().modelName());
                meta.put("durationMs", outcome.meta().durationMs());
                meta.put("traceId", outcome.meta().traceId());
                meta.put("degraded", outcome.meta().degraded());
                String metaJson = objectMapper.writeValueAsString(meta);

                ChatMessageEntity assistantMsg = new ChatMessageEntity(session, "ASSISTANT", fullResponse.toString());
                assistantMsg.setAiMeta(metaJson);
                assistantMsg = messageRepo.save(assistantMsg);

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

    private List<AIModels.AIMessage> buildContextMessages(List<ChatMessageEntity> history) {
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
