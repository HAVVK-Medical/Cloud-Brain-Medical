package com.cloudbrain.application.ai;

import java.util.function.Consumer;

public interface AIProvider {

    String providerName();

    AIModels.AIChatResponse chat(AIModels.AIChatRequest request);

    AIModels.AIChatResponse chatStream(AIModels.AIChatRequest request, Consumer<String> chunkConsumer);
}
