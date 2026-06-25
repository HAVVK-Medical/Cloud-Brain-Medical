package com.cloudbrain.application.ai;

import com.cloudbrain.application.admin.ConfigCipher;
import com.cloudbrain.entity.core.AIConfigEntity;
import com.cloudbrain.repository.AIConfigJpaRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AIConfigResolver {

    private static final String ACTIVE = "ACTIVE";
    private static final List<String> FALLBACK_SCOPES = List.of("ALL", "GLOBAL", "DEFAULT", "AI");

    private final AIConfigJpaRepository aiConfigRepository;
    private final ConfigCipher configCipher;

    public AIConfigResolver(AIConfigJpaRepository aiConfigRepository, ConfigCipher configCipher) {
        this.aiConfigRepository = aiConfigRepository;
        this.configCipher = configCipher;
    }

    public AIModels.ResolvedAIConfig resolve(String taskScope) {
        String normalizedTaskScope = normalize(taskScope);
        AIConfigEntity entity = aiConfigRepository.findAll().stream()
                .filter(this::isEnabledAndActive)
                .filter(config -> matchesScope(config.getTaskScope(), normalizedTaskScope))
                .sorted(configComparator(normalizedTaskScope))
                .findFirst()
                .orElse(null);
        if (entity == null) {
            return null;
        }
        return new AIModels.ResolvedAIConfig(
                entity.getId(),
                normalizeProvider(entity.getProvider()),
                entity.getModelName(),
                entity.getApiUrl(),
                configCipher.decrypt(entity.getApiKeyEncrypted()),
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeProvider(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
