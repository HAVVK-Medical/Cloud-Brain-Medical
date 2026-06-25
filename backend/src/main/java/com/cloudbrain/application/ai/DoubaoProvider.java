package com.cloudbrain.application.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class DoubaoProvider extends AbstractOpenAICompatibleProvider {

    private static final String DEFAULT_API_URL = "https://ark.cn-beijing.volces.com/api/v3";

    public DoubaoProvider(ObjectMapper objectMapper) {
        super("DOUBAO", DEFAULT_API_URL, objectMapper);
    }
}
