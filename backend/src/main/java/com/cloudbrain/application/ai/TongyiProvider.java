package com.cloudbrain.application.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class TongyiProvider extends AbstractOpenAICompatibleProvider {

    private static final String DEFAULT_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    public TongyiProvider(ObjectMapper objectMapper) {
        super("TONGYI", DEFAULT_API_URL, objectMapper);
    }
}
