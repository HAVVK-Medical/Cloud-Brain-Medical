package com.cloudbrain.application.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class ChatServiceTest {

    private ChatSessionRepository sessionRepository;
    private ChatMessageRepository messageRepository;
    private AIConfigResolver configResolver;
    private AIProviderResolver providerResolver;
    private PromptTemplateService promptTemplateService;
    private ChatService service;

    @BeforeEach
    void setUp() {
        sessionRepository = org.mockito.Mockito.mock(ChatSessionRepository.class);
        messageRepository = org.mockito.Mockito.mock(ChatMessageRepository.class);
        configResolver = org.mockito.Mockito.mock(AIConfigResolver.class);
        providerResolver = org.mockito.Mockito.mock(AIProviderResolver.class);
        promptTemplateService = org.mockito.Mockito.mock(PromptTemplateService.class);
        service = new ChatService(
                sessionRepository,
                messageRepository,
                configResolver,
                providerResolver,
                promptTemplateService,
                new TransactionTemplate(noopTransactionManager())
        );
    }

    @Test
    void listAndGetMessagesRequireSessionOwnership() {
        ChatSessionEntity owned = session(1L, 10L, "PATIENT", "问诊");
        List<ChatMessageEntity> messages = List.of(new ChatMessageEntity(owned, "USER", "你好"));
        when(sessionRepository.findByUserIdAndUserRoleOrderByUpdatedAtDesc(10L, "PATIENT")).thenReturn(List.of(owned));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(owned));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(1L)).thenReturn(messages);

        assertThat(service.listSessions(10L, "PATIENT")).containsExactly(owned);
        assertThat(service.getMessages(1L, 10L)).containsExactlyElementsOf(messages);

        assertThatThrownBy(() -> service.getMessages(1L, 99L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Access denied");
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMessages(99L, 10L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Session not found");
    }

    @Test
    void createSessionTruncatesTitleAndStoresFirstMessage() {
        String longMessage = "x".repeat(80);
        when(sessionRepository.save(any(ChatSessionEntity.class))).thenAnswer(invocation -> {
            ChatSessionEntity entity = invocation.getArgument(0);
            entity.setId(101L);
            return entity;
        });

        ChatSessionEntity created = service.createSession(10L, "PATIENT", "  " + longMessage + "  ");

        assertThat(created.getTitle()).hasSize(50);
        assertThat(created.getUserId()).isEqualTo(10L);
        ArgumentCaptor<ChatMessageEntity> messageCaptor = ArgumentCaptor.forClass(ChatMessageEntity.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getContent()).isEqualTo(longMessage);
    }

    @Test
    void createSessionAllowsNullOrBlankFirstMessageWithoutPersistingMessage() {
        when(sessionRepository.save(any(ChatSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatSessionEntity withoutMessage = service.createSession(10L, "PATIENT", null);
        ChatSessionEntity blankMessage = service.createSession(10L, "PATIENT", "   ");

        assertThat(withoutMessage.getTitle()).isNull();
        assertThat(blankMessage.getTitle()).isBlank();
        verify(messageRepository, never()).save(any(ChatMessageEntity.class));
    }

    @Test
    void deleteSessionRemovesMessagesAndOwnedSession() {
        service.deleteSession(12L, 10L);

        verify(messageRepository).deleteBySessionId(12L);
        verify(sessionRepository).deleteByIdAndUserId(12L, 10L);
    }

    @Test
    void streamChatSavesUserMessageBeforeReturningEmitter() {
        ChatSessionEntity session = session(3L, 10L, "PATIENT", "旧标题");
        when(sessionRepository.findById(3L)).thenReturn(Optional.of(session));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(3L)).thenReturn(List.of());
        when(messageRepository.save(any(ChatMessageEntity.class))).thenAnswer(invocation -> {
            ChatMessageEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId("ASSISTANT".equals(entity.getRole()) ? 302L : 301L);
            }
            return entity;
        });
        when(configResolver.resolve("CHAT")).thenReturn(null);
        when(promptTemplateService.resolve(eq("CHAT"), eq(null), any()))
                .thenReturn(new AIModels.ResolvedPromptTemplate("tpl", "CHAT", null, "system", 1, "tpl-v1"));

        var emitter = service.streamChat(3L, 10L, "  继续问诊  ", "PATIENT");

        assertThat(emitter).isNotNull();
        ArgumentCaptor<ChatMessageEntity> captor = ArgumentCaptor.forClass(ChatMessageEntity.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("USER");
        assertThat(captor.getValue().getContent()).isEqualTo("继续问诊");
    }

    @Test
    void streamChatRejectsUnknownOrForeignSession() {
        when(sessionRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.streamChat(404L, 10L, "hi", "PATIENT"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Session not found");

        when(sessionRepository.findById(5L)).thenReturn(Optional.of(session(5L, 22L, "PATIENT", "foreign")));
        assertThatThrownBy(() -> service.streamChat(5L, 10L, "hi", "PATIENT"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Access denied");
    }

    @Test
    void streamChatFallbackPersistsAssistantMessageWhenConfigCannotAuthenticate() throws Exception {
        ChatSessionEntity session = session(13L, 10L, "PATIENT", null);
        CountDownLatch assistantSaved = new CountDownLatch(1);
        List<ChatMessageEntity> savedMessages = new ArrayList<>();
        when(sessionRepository.findById(13L)).thenReturn(Optional.of(session));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(13L)).thenReturn(List.of());
        when(messageRepository.save(any(ChatMessageEntity.class))).thenAnswer(invocation -> {
            ChatMessageEntity entity = invocation.getArgument(0);
            savedMessages.add(entity);
            if ("ASSISTANT".equals(entity.getRole())) {
                entity.setId(1302L);
                assistantSaved.countDown();
            }
            return entity;
        });
        when(sessionRepository.save(any(ChatSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(configResolver.resolve("CHAT")).thenReturn(new AIModels.ResolvedAIConfig(
                1L, " ", "model", "https://api", " ", "kv1", "CHAT", 15, "cfg-v1"));
        when(promptTemplateService.resolve(eq("CHAT"), eq(null), any()))
                .thenReturn(new AIModels.ResolvedPromptTemplate("tpl", "CHAT", null, "system", 1, "tpl-v1"));

        service.streamChat(13L, 10L, "hello", "PATIENT");

        assertThat(assistantSaved.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(savedMessages).anySatisfy(message -> {
            assertThat(message.getRole()).isEqualTo("ASSISTANT");
            assertThat(message.getContent()).contains("AI");
            assertThat(message.getAiMeta()).contains("\"provider\":\"AI\"");
        });
        assertThat(session.getTitle()).isNotBlank();
    }

    @Test
    void streamChatPersistsErrorMessageWhenProviderFails() throws Exception {
        ChatSessionEntity session = session(14L, 10L, "PATIENT", "old");
        CountDownLatch errorSaved = new CountDownLatch(1);
        List<ChatMessageEntity> savedMessages = new ArrayList<>();
        when(sessionRepository.findById(14L)).thenReturn(Optional.of(session));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(14L)).thenReturn(List.of());
        when(messageRepository.save(any(ChatMessageEntity.class))).thenAnswer(invocation -> {
            ChatMessageEntity entity = invocation.getArgument(0);
            savedMessages.add(entity);
            if ("ASSISTANT".equals(entity.getRole())) {
                errorSaved.countDown();
            }
            return entity;
        });
        when(configResolver.resolve("CHAT")).thenReturn(new AIModels.ResolvedAIConfig(
                1L, "mock", "model", "https://api", "secret", "kv1", "CHAT", 15, "cfg-v1"));
        when(promptTemplateService.resolve(eq("CHAT"), eq(null), any()))
                .thenReturn(new AIModels.ResolvedPromptTemplate("tpl", "CHAT", null, "system", 1, "tpl-v1"));
        AIProvider provider = org.mockito.Mockito.mock(AIProvider.class);
        when(providerResolver.resolve("mock")).thenReturn(provider);
        when(provider.chatStream(any(AIModels.AIChatRequest.class), any(), any()))
                .thenThrow(new IllegalStateException("provider down"));

        service.streamChat(14L, 10L, "hello", "PATIENT");

        assertThat(errorSaved.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(savedMessages).anySatisfy(message -> {
            assertThat(message.getRole()).isEqualTo("ASSISTANT");
            assertThat(message.getContent()).contains("AI");
        });
    }

    @Test
    void streamChatUsesRemoteProviderHistoryThinkingAndUpdatesUntitledSession() throws Exception {
        ChatSessionEntity session = session(7L, 10L, "PATIENT", null);
        ChatMessageEntity oldUser = new ChatMessageEntity(session, "USER", "old question");
        ChatMessageEntity oldAssistant = new ChatMessageEntity(session, "ASSISTANT", "old answer");
        CountDownLatch assistantSaved = new CountDownLatch(1);
        List<ChatMessageEntity> savedMessages = new ArrayList<>();
        when(sessionRepository.findById(7L)).thenReturn(Optional.of(session));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(7L)).thenReturn(List.of(oldUser, oldAssistant));
        when(messageRepository.save(any(ChatMessageEntity.class))).thenAnswer(invocation -> {
            ChatMessageEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId("ASSISTANT".equals(entity.getRole()) ? 702L : 701L);
            }
            savedMessages.add(entity);
            if ("ASSISTANT".equals(entity.getRole())) {
                assistantSaved.countDown();
            }
            return entity;
        });
        when(sessionRepository.save(any(ChatSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(configResolver.resolve("CHAT")).thenReturn(new AIModels.ResolvedAIConfig(
                1L,
                "mock",
                "chat-model",
                "https://api",
                "secret",
                "kv1",
                "CHAT",
                null,
                "cfg-v1"
        ));
        when(promptTemplateService.resolve(eq("CHAT"), eq(null), any()))
                .thenReturn(new AIModels.ResolvedPromptTemplate("tpl", "CHAT", null, "system prompt", 1, "tpl-v1"));
        AIProvider provider = org.mockito.Mockito.mock(AIProvider.class);
        when(providerResolver.resolve("mock")).thenReturn(provider);
        when(provider.chatStream(any(AIModels.AIChatRequest.class), any(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> chunkConsumer = invocation.getArgument(1);
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> thinkingConsumer = invocation.getArgument(2);
            thinkingConsumer.accept("thinking");
            chunkConsumer.accept("remote answer");
            return new AIModels.AIChatResponse("mock", "chat-model", "req", "resp", "stop", "remote answer", "{}");
        });

        var emitter = service.streamChat(7L, 10L, " next question ", "PATIENT");

        assertThat(emitter).isNotNull();
        assertThat(assistantSaved.await(2, TimeUnit.SECONDS)).isTrue();
        ArgumentCaptor<AIModels.AIChatRequest> requestCaptor = ArgumentCaptor.forClass(AIModels.AIChatRequest.class);
        verify(provider).chatStream(requestCaptor.capture(), any(), any());
        AIModels.AIChatRequest request = requestCaptor.getValue();
        assertThat(request.timeoutSeconds()).isEqualTo(30);
        assertThat(request.messages()).hasSize(2);
        assertThat(request.messages().get(0).role()).isEqualTo("system");
        assertThat(request.messages().get(1).content().get(0).text()).contains("old question").contains("next question");
        assertThat(savedMessages).anySatisfy(message -> {
            assertThat(message.getRole()).isEqualTo("ASSISTANT");
            assertThat(message.getContent()).isEqualTo("remote answer");
            assertThat(message.getThinkingContent()).isEqualTo("thinking");
            assertThat(message.getAiMeta()).contains("\"provider\":\"AI\"");
        });
        assertThat(session.getTitle()).isEqualTo("remote answer");
    }

    private ChatSessionEntity session(Long id, Long userId, String role, String title) {
        ChatSessionEntity session = new ChatSessionEntity(userId, role, title);
        session.setId(id);
        return session;
    }

    private PlatformTransactionManager noopTransactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
    }
}
