package com.cloudbrain.application.chat;

import com.cloudbrain.application.ai.AIConfigResolver;
import com.cloudbrain.application.ai.AIModels;
import com.cloudbrain.application.ai.AIProvider;
import com.cloudbrain.application.ai.AIProviderResolver;
import com.cloudbrain.application.ai.PromptTemplateService;
import com.cloudbrain.common.exception.ApiException;
import com.cloudbrain.entity.chat.ChatMessageEntity;
import com.cloudbrain.entity.chat.ChatSessionEntity;
import com.cloudbrain.repository.ChatMessageRepository;
import com.cloudbrain.repository.ChatSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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
    private final AIConfigResolver configResolver;
    private final AIProviderResolver providerResolver;
    private final PromptTemplateService promptTemplateService;
    private final TransactionTemplate transactionTemplate;

    public ChatService(ChatSessionRepository sessionRepo,
                       ChatMessageRepository messageRepo,
                       AIConfigResolver configResolver,
                       AIProviderResolver providerResolver,
                       PromptTemplateService promptTemplateService,
                       TransactionTemplate transactionTemplate) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.configResolver = configResolver;
        this.providerResolver = providerResolver;
        this.promptTemplateService = promptTemplateService;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional(readOnly = true)
    public List<ChatSessionEntity> listSessions(Long userId, String userRole) {
        return sessionRepo.findByUserIdAndUserRoleOrderByUpdatedAtDesc(userId, userRole);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageEntity> getMessages(Long sessionId, Long userId) {
        ChatSessionEntity session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Session not found"));
        if (!session.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Access denied");
        }
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

    @Transactional
    public SseEmitter streamChat(Long sessionId, Long userId, String message, String userRole) {
        return streamChat("CHAT", sessionId, userId, message, userRole);
    }

    @Transactional
    public SseEmitter streamChat(String taskScope, Long sessionId, Long userId, String message, String userRole) {
        ChatSessionEntity session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "Session not found"));
        if (!session.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "Access denied");
        }

        messageRepo.save(new ChatMessageEntity(session, "USER", message.trim()));

        boolean needsTitle = session.getTitle() == null;

        List<ChatMessageEntity> history = messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<AIModels.AIMessage> contextMessages = buildContextMessages(history);

        SseEmitter emitter = new SseEmitter(2 * 60 * 1000L);

        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder fullResponse = new StringBuilder();
                StringBuilder fullThinking = new StringBuilder();

                AIModels.ResolvedAIConfig config = configResolver.resolve(taskScope);
                AIModels.ResolvedPromptTemplate template = promptTemplateService.resolve(taskScope, null,
                        Map.of("userRole", userRole, "inputText", message.trim()));

                if (config == null || config.provider() == null || config.provider().isBlank()
                        || config.apiKey() == null || config.apiKey().isBlank()) {
                    String fallback = "抱歉，AI 服务暂时不可用，请稍后重试。";
                    fullResponse.append(fallback);
                    sendChunk(emitter, "chunk", fallback);
                } else {
                    List<AIModels.AIMessage> messages = new ArrayList<>();
                    messages.add(AIModels.AIMessage.system(template.body()));

                    String conversationContext = formatHistoryAsContext(contextMessages);
                    String fullUserText;
                    if (!conversationContext.isEmpty()) {
                        fullUserText = "对话历史：\n" + conversationContext + "\n当前消息：\n" + message.trim();
                    } else {
                        fullUserText = message.trim();
                    }
                    messages.add(AIModels.AIMessage.user(fullUserText, Collections.emptyList()));

                    AIProvider provider = providerResolver.resolve(config.provider());
                    AIModels.AIChatRequest request = new AIModels.AIChatRequest(
                            taskScope,
                            config.provider(),
                            config.modelName(),
                            config.apiUrl(),
                            config.apiKey(),
                            config.configVersion(),
                            template.promptVersion(),
                            java.util.UUID.randomUUID().toString(),
                            java.util.UUID.randomUUID().toString(),
                            config.timeoutSeconds() == null ? 30 : config.timeoutSeconds(),
                            true,
                            true,
                            0.2D,
                            1000,
                            messages
                    );
                    provider.chatStream(request,
                        chunk -> {
                            fullResponse.append(chunk);
                            sendChunk(emitter, "chunk", chunk);
                        },
                        thinking -> {
                            fullThinking.append(thinking);
                            sendChunk(emitter, "thinking", thinking);
                        }
                    );
                }

                String responseText = fullResponse.toString();
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("provider", "AI");
                meta.put("traceId", java.util.UUID.randomUUID().toString());

                transactionTemplate.executeWithoutResult(status -> {
                    String metaJson;
                    try {
                        metaJson = objectMapper.writeValueAsString(meta);
                    } catch (Exception e) {
                        metaJson = "{}";
                    }
                    ChatMessageEntity assistantMsg = new ChatMessageEntity(session, "ASSISTANT", responseText);
                    assistantMsg.setThinkingContent(fullThinking.toString());
                    assistantMsg.setAiMeta(metaJson);
                    assistantMsg = messageRepo.save(assistantMsg);

                    if (needsTitle) {
                        String title = responseText.replaceAll("\\s+", " ").trim();
                        if (title.isEmpty()) {
                            title = message.trim();
                        }
                        if (title.length() > 50) title = title.substring(0, 50);
                        session.setTitle(title);
                        sessionRepo.save(session);
                    }

                    try {
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data(Map.of(
                                    "messageId", assistantMsg.getId(),
                                    "thinkingContent", fullThinking.toString(),
                                    "meta", meta
                                )));
                    } catch (IOException e) {
                        log.warn("Failed to send done event", e);
                    }
                });

                emitter.complete();
            } catch (Exception e) {
                log.error("Chat stream failed for session {}", sessionId, e);
                try {
                    String errorContent = "抱歉，AI 服务暂时不可用。";
                    transactionTemplate.executeWithoutResult(status -> {
                        ChatMessageEntity errorMsg = new ChatMessageEntity(session, "ASSISTANT", errorContent);
                        messageRepo.save(errorMsg);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data(Map.of("message", errorContent)));
                        } catch (IOException ignored) {}
                    });
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void sendChunk(SseEmitter emitter, String eventName, String chunk) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(Map.of("content", chunk)));
        } catch (IOException e) {
            throw new RuntimeException("SSE send failed", e);
        }
    }

    private String formatHistoryAsContext(List<AIModels.AIMessage> contextMessages) {
        if (contextMessages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (AIModels.AIMessage msg : contextMessages) {
            String roleLabel = "user".equals(msg.role()) ? "用户" : "助手";
            String text = msg.content().stream()
                    .filter(p -> "text".equals(p.type()) && p.text() != null)
                    .map(AIModels.AIContentPart::text)
                    .findFirst()
                    .orElse("");
            if (!text.isBlank()) {
                sb.append(roleLabel).append("：").append(text).append("\n");
            }
        }
        return sb.toString().trim();
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
