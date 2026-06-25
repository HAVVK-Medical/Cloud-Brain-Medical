package com.cloudbrain.application.ai;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AIProviderResolver {

    private final List<AIProvider> providers;

    public AIProviderResolver(List<AIProvider> providers) {
        this.providers = providers;
    }

    public AIProvider resolve(String providerName) {
        String normalized = normalize(providerName);
        return providers.stream()
                .filter(provider -> normalize(provider.providerName()).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new AIProviderException("unsupported ai provider: " + providerName));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
