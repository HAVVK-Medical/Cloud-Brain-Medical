package com.cloudbrain.application.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cloudbrain.application.admin.ConfigCipher;
import com.cloudbrain.entity.core.AIConfigEntity;
import com.cloudbrain.repository.AIConfigJpaRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AIConfigResolverTest {

    private AIConfigJpaRepository repository;
    private ConfigCipher cipher;
    private AIConfigResolver resolver;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(AIConfigJpaRepository.class);
        cipher = new ConfigCipher("unit-test-secret-that-is-long-enough");
        resolver = new AIConfigResolver(repository, cipher);
    }

    @Test
    void resolvePrefersExactScopeDefaultAndHighestPriority() {
        AIConfigEntity fallback = config(1L, "all", false, 20, "fallback-key");
        AIConfigEntity lowPriority = config(2L, "triage", false, 1, "low-key");
        AIConfigEntity exactDefault = config(3L, " TRIAGE ", true, 10, "exact-key");
        when(repository.findAll()).thenReturn(List.of(fallback, lowPriority, exactDefault));

        var resolved = resolver.resolve("triage");

        assertThat(resolved).isNotNull();
        assertThat(resolved.id()).isEqualTo(3L);
        assertThat(resolved.provider()).isEqualTo("DEEPSEEK");
        assertThat(resolved.apiKey()).isEqualTo("exact-key");
        assertThat(resolved.timeoutSeconds()).isEqualTo(45);
    }

    @Test
    void resolveFallsBackToGlobalScopeWhenTaskSpecificConfigIsUnavailable() {
        AIConfigEntity disabled = config(1L, "triage", true, 100, "disabled-key");
        disabled.setEnabled(false);
        AIConfigEntity inactive = config(2L, "triage", true, 90, "inactive-key");
        inactive.setStatus("INACTIVE");
        AIConfigEntity blankKey = config(3L, "triage", true, 80, " ");
        AIConfigEntity global = config(4L, "GLOBAL", false, 1, "global-key");
        when(repository.findAll()).thenReturn(List.of(disabled, inactive, blankKey, global));

        var resolved = resolver.resolve("triage");

        assertThat(resolved).isNotNull();
        assertThat(resolved.id()).isEqualTo(4L);
        assertThat(resolved.apiKey()).isEqualTo("global-key");
    }

    @Test
    void resolveReturnsNullWhenDecryptFailsOrNoConfigMatches() {
        AIConfigEntity badCipher = config(1L, "diagnosis", true, 1, "ok");
        badCipher.setApiKeyEncrypted("not-a-ciphertext");
        when(repository.findAll()).thenReturn(List.of(badCipher));

        assertThat(resolver.resolve("diagnosis")).isNull();

        when(repository.findAll()).thenReturn(List.of(config(2L, "triage", true, 1, "triage-key")));
        assertThat(resolver.resolve("medical_record")).isNull();
    }

    @Test
    void resolveHandlesBlankTaskScopeAndNullProviderValues() {
        AIConfigEntity fallback = config(1L, " DEFAULT ", false, 3, "fallback-key");
        fallback.setProvider(null);
        when(repository.findAll()).thenReturn(List.of(fallback));

        var resolved = resolver.resolve(" ");

        assertThat(resolved).isNotNull();
        assertThat(resolved.id()).isEqualTo(1L);
        assertThat(resolved.provider()).isEmpty();
        assertThat(resolved.apiKey()).isEqualTo("fallback-key");
    }

    @Test
    void resolveIgnoresNullConfigEntriesAndBlankApiKeys() {
        AIConfigEntity blankKey = config(1L, "CHAT", true, 10, "valid");
        blankKey.setApiKeyEncrypted("\t");
        List<AIConfigEntity> configs = new ArrayList<>();
        configs.add(null);
        configs.add(blankKey);
        when(repository.findAll()).thenReturn(configs);

        assertThat(resolver.resolve("CHAT")).isNull();
    }

    private AIConfigEntity config(Long id, String taskScope, boolean defaultConfig, int priority, String apiKey) {
        AIConfigEntity entity = new AIConfigEntity();
        entity.setId(id);
        entity.setProvider(" deepseek ");
        entity.setModelName("deepseek-chat");
        entity.setApiUrl("https://api.example.com");
        entity.setApiKeyEncrypted(apiKey == null || apiKey.isBlank() ? apiKey : cipher.encrypt(apiKey));
        entity.setKeyVersion("kv1");
        entity.setTaskScope(taskScope);
        entity.setTimeoutSeconds(45);
        entity.setDefaultConfig(defaultConfig);
        entity.setHealthStatus("OK");
        entity.setConfigVersion("v2");
        entity.setEnabled(true);
        entity.setPriority(priority);
        entity.setStatus("ACTIVE");
        entity.setUpdatedAt(Instant.now().plusSeconds(priority));
        return entity;
    }
}
