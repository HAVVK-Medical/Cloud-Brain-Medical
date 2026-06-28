package com.cloudbrain.application.ai;

import com.cloudbrain.application.admin.ConfigCipher;
import com.cloudbrain.entity.core.AIConfigEntity;
import com.cloudbrain.repository.AIConfigJpaRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AIConfigResolver {

    private static final Logger log = LoggerFactory.getLogger(AIConfigResolver.class);
    private static final String ACTIVE = "ACTIVE";
    private static final List<String> FALLBACK_SCOPES = List.of("ALL", "GLOBAL", "DEFAULT", "AI");

    private final AIConfigJpaRepository aiConfigRepository;
    private final ConfigCipher configCipher;

    public AIConfigResolver(AIConfigJpaRepository aiConfigRepository, ConfigCipher configCipher) {
        this.aiConfigRepository = aiConfigRepository;
        this.configCipher = configCipher;
    }

    /**
     * Resolve the best-matching AI config for a given task scope.
     *
     * Configs without an API key are excluded — a config that cannot authenticate
     * is not a valid candidate. Decryption failures are also treated as "no valid
     * config" rather than throwing, so callers can fall back gracefully.
     */
    public AIModels.ResolvedAIConfig resolve(String taskScope) {
        String normalizedTaskScope = normalize(taskScope);
        AIConfigEntity entity = aiConfigRepository.findAll().stream()
                .filter(this::isEnabledAndActive)
                .filter(config -> hasApiKey(config))
                .filter(config -> matchesScope(config.getTaskScope(), normalizedTaskScope))
                .sorted(configComparator(normalizedTaskScope))
                .findFirst()
                .orElse(null);
        if (entity == null) {
            return null;
        }
        String apiKey;
        try {
            apiKey = configCipher.decrypt(entity.getApiKeyEncrypted());
        } catch (Exception e) {
            log.warn("Failed to decrypt API key for ai_config id={} provider={} — skipping", entity.getId(), entity.getProvider(), e);
            return null;
        }
        return new AIModels.ResolvedAIConfig(
                entity.getId(),
                normalizeProvider(entity.getProvider()),
                entity.getModelName(),
                entity.getApiUrl(),
                apiKey,
                entity.getKeyVersion(),
                entity.getTaskScope(),
                entity.getTimeoutSeconds(),
                entity.getConfigVersion()
        );
    }

    private Comparator<AIConfigEntity> configComparator(String taskScope) {
        return Comparator
                .comparing((AIConfigEntity config) -> !Objects.equals(normalize(config.getTaskScope()), taskScope))
                .thenComparing(config -> !Boolean.TRUE.equals(config.getDefaultConfig()))
                .thenComparing(AIConfigEntity::getPriority, Comparator.reverseOrder())
                .thenComparing(AIConfigEntity::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private boolean isEnabledAndActive(AIConfigEntity entity) {
        return entity != null
                && Boolean.TRUE.equals(entity.getEnabled())
                && ACTIVE.equalsIgnoreCase(entity.getStatus());
    }

    private boolean matchesScope(String configScope, String taskScope) {
        String normalizedConfigScope = normalize(configScope);
        if (normalizedConfigScope.isBlank() || normalizedTaskScopeFallback(normalizedConfigScope, taskScope)) {
            return true;
        }
        return Objects.equals(normalizedConfigScope, taskScope);
    }

    private boolean normalizedTaskScopeFallback(String normalizedConfigScope, String taskScope) {
        if (taskScope == null || taskScope.isBlank()) {
            return FALLBACK_SCOPES.contains(normalizedConfigScope.toUpperCase(Locale.ROOT));
        }
        return FALLBACK_SCOPES.contains(normalizedConfigScope.toUpperCase(Locale.ROOT));
    }

    private boolean hasApiKey(AIConfigEntity entity) {
        return entity.getApiKeyEncrypted() != null && !entity.getApiKeyEncrypted().isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeProvider(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
