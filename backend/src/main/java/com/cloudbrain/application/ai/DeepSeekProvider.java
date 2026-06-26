package com.cloudbrain.application.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class DeepSeekProvider extends AbstractOpenAICompatibleProvider {

    private static final String DEFAULT_API_URL = "https://api.deepseek.com/v1";

    public DeepSeekProvider(ObjectMapper objectMapper) {
        super("DEEPSEEK", DEFAULT_API_URL, objectMapper);
    }
}
