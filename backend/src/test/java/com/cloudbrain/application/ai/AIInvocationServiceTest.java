package com.cloudbrain.application.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AIInvocationServiceTest {

    private AIConfigResolver configResolver;
    private AIProviderResolver providerResolver;
    private PromptTemplateService templateService;
    private AIProvider provider;
    private AIInvocationService service;

    @BeforeEach
    void setUp() {
        configResolver = org.mockito.Mockito.mock(AIConfigResolver.class);
        providerResolver = org.mockito.Mockito.mock(AIProviderResolver.class);
        templateService = org.mockito.Mockito.mock(PromptTemplateService.class);
        provider = org.mockito.Mockito.mock(AIProvider.class);
        service = new AIInvocationService(configResolver, providerResolver, templateService);
    }

    @Test
    void chatReturnsLocalFallbackWhenNoUsableConfig() {
        when(configResolver.resolve("TRIAGE")).thenReturn(null);
        when(templateService.resolve(eq("TRIAGE"), eq(null), any()))
                .thenReturn(new AIModels.ResolvedPromptTemplate("builtin", "TRIAGE", null, "system prompt", 0, "builtin-v0"));

        var outcome = service.chat("triage", null, Map.of("chiefComplaint", "头痛"), List.of(), "fallback", false, null);

        assertThat(outcome.result()).isEqualTo("fallback");
        assertThat(outcome.meta().provider()).isEqualTo("LOCAL_RULE");
        assertThat(outcome.meta().degraded()).isTrue();
    }

    @Test
    void chatBuildsRemoteRequestForNonStreamingCall() {
        var config = new AIModels.ResolvedAIConfig(7L, "MOCK", "mock-model", "https://ai.example", "key", "kv1",
                "DIAGNOSIS", 9, "cfg1");
        when(configResolver.resolve("DIAGNOSIS")).thenReturn(config);
        when(templateService.resolve(eq("DIAGNOSIS"), eq("neurology"), any()))
                .thenReturn(new AIModels.ResolvedPromptTemplate("tpl", "DIAGNOSIS", "neurology", "system prompt", 2, "tpl-v2"));
        when(providerResolver.resolve("MOCK")).thenReturn(provider);
        when(provider.chat(any())).thenAnswer(invocation -> {
            AIModels.AIChatRequest request = invocation.getArgument(0);
            assertThat(request.taskType()).isEqualTo("DIAGNOSIS");
            assertThat(request.temperature()).isEqualTo(0.3D);
            assertThat(request.maxTokens()).isEqualTo(1200);
            assertThat(request.timeoutSeconds()).isEqualTo(9);
            assertThat(request.messages()).hasSize(2);
            return new AIModels.AIChatResponse("MOCK", "mock-model", request.requestId(), "resp-1", "stop", "remote answer", "{}");
        });

        var outcome = service.chat(" diagnosis ", "neurology", Map.of("conversationText", "头痛"), List.of(), "fallback", false, null);

        assertThat(outcome.result()).isEqualTo("remote answer");
        assertThat(outcome.meta().provider()).isEqualTo("MOCK");
        assertThat(outcome.meta().degraded()).isFalse();
    }

    @Test
    void chatBuildsTaskSpecificPromptsForTriagePrescriptionReviewAndDefaultScope() {
        when(configResolver.resolve("TRIAGE")).thenReturn(config("TRIAGE"));
        when(configResolver.resolve("PRESCRIPTION_REVIEW")).thenReturn(config("PRESCRIPTION_REVIEW"));
        when(configResolver.resolve("CHAT")).thenReturn(config("CHAT"));
        when(templateService.resolve(eq("TRIAGE"), eq("cardiology"), any()))
                .thenReturn(new AIModels.ResolvedPromptTemplate("tpl", "TRIAGE", "cardiology", "system prompt", 1, "tpl-v1"));
        when(templateService.resolve(eq("PRESCRIPTION_REVIEW"), eq("cardiology"), any()))
                .thenReturn(new AIModels.ResolvedPromptTemplate("tpl", "PRESCRIPTION_REVIEW", "cardiology", "system prompt", 1, "tpl-v1"));
        when(templateService.resolve(eq("CHAT"), eq(null), any()))
                .thenReturn(new AIModels.ResolvedPromptTemplate("tpl", "CHAT", null, "system prompt", 1, "tpl-v1"));
        when(providerResolver.resolve("MOCK")).thenReturn(provider);
        List<AIModels.AIChatRequest> requests = new ArrayList<>();
        when(provider.chat(any())).thenAnswer(invocation -> {
            AIModels.AIChatRequest request = invocation.getArgument(0);
            requests.add(request);
            return new AIModels.AIChatResponse("MOCK", "mock-model", request.requestId(), "resp", "stop", "ok", "{}");
        });

        service.chat("triage", "cardiology", Map.of("chiefComplaint", "chest pain"), List.of(), "fallback", false, null);
        service.chat("prescription_review", "cardiology", Map.of(
                "riskLevel", "HIGH",
                "localRuleHits", "duplicate",
                "missingItems", "allergy",
                "prescriptionSummary", "drug A"
        ), List.of(), "fallback", false, null);
        service.chat("chat", null, Map.of("inputText", "hello"), List.of(), "fallback", false, null);

        assertThat(requests).hasSize(3);
        assertThat(requests.get(0).temperature()).isEqualTo(0.25D);
        assertThat(requests.get(0).maxTokens()).isEqualTo(500);
        assertThat(requests.get(0).messages().get(1).content().get(0).text()).contains("chest pain");
        assertThat(requests.get(1).temperature()).isEqualTo(0.2D);
        assertThat(requests.get(1).maxTokens()).isEqualTo(900);
        assertThat(requests.get(1).messages().get(1).content().get(0).text()).contains("HIGH", "duplicate", "allergy", "drug A");
        assertThat(requests.get(2).temperature()).isEqualTo(0.2D);
        assertThat(requests.get(2).maxTokens()).isEqualTo(1000);
        assertThat(requests.get(2).messages().get(1).content().get(0).text()).isEqualTo("hello");
    }

    @Test
    @SuppressWarnings("unchecked")
    void chatStreamsChunksAndFallsBackOnProviderException() {
        var config = new AIModels.ResolvedAIConfig(8L, "MOCK", "mock-model", null, "key", "kv1",
                "MEDICAL_RECORD", null, "cfg1");
        when(configResolver.resolve("MEDICAL_RECORD")).thenReturn(config);
        when(templateService.resolve(eq("MEDICAL_RECORD"), eq(null), any()))
                .thenReturn(new AIModels.ResolvedPromptTemplate("tpl", "MEDICAL_RECORD", null, "system prompt", 1, "tpl-v1"));
        when(providerResolver.resolve("MOCK")).thenReturn(provider);
        when(provider.chatStream(any(), any(), any())).thenAnswer(invocation -> {
            Consumer<String> chunk = invocation.getArgument(1);
            Consumer<String> thinking = invocation.getArgument(2);
            chunk.accept("远程");
            thinking.accept("思考");
            return new AIModels.AIChatResponse("MOCK", "mock-model", "req", "resp", "stop", "", "{}");
        });
        List<String> chunks = new ArrayList<>();
        List<String> thinking = new ArrayList<>();

        var streamed = service.chat("medical_record", null, Map.of("conversationText", "咳嗽"),
                List.of(AIModels.AIContentPart.text("附件")), "fallback", true, chunks::add, thinking::add);

        assertThat(chunks).containsExactly("远程");
        assertThat(thinking).containsExactly("思考");
        assertThat(streamed.result()).isEqualTo("fallback");
        assertThat(streamed.meta().provider()).isEqualTo("MOCK");

        when(providerResolver.resolve("MOCK")).thenThrow(new AIProviderException("boom"));
        var failed = service.chat("medical_record", null, Map.of(), List.of(), "local", false, null);
        assertThat(failed.result()).isEqualTo("local");
        assertThat(failed.meta().provider()).isEqualTo("LOCAL_RULE");
        assertThat(failed.meta().errorSummary()).contains("boom");
    }

    private AIModels.ResolvedAIConfig config(String taskScope) {
        return new AIModels.ResolvedAIConfig(1L, "MOCK", "mock-model", "https://ai.example", "key", "kv1",
                taskScope, null, "cfg1");
    }
}
