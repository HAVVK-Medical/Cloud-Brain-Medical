package com.cloudbrain.application.ai;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class AIInvocationService {

    private final AIConfigResolver configResolver;
    private final AIProviderResolver providerResolver;
    private final PromptTemplateService promptTemplateService;

    public AIInvocationService(AIConfigResolver configResolver,
                               AIProviderResolver providerResolver,
                               PromptTemplateService promptTemplateService) {
        this.configResolver = configResolver;
        this.providerResolver = providerResolver;
        this.promptTemplateService = promptTemplateService;
    }

    public AIModels.AIExecutionOutcome<String> chat(String taskType,
                                                    String deptCode,
                                                    Map<String, String> variables,
                                                    List<AIModels.AIContentPart> attachments,
                                                    String fallbackText,
                                                    boolean stream,
                                                    Consumer<String> chunkConsumer) {
        String normalizedTaskType = normalize(taskType);
        AIModels.ResolvedAIConfig config = configResolver.resolve(normalizedTaskType);
        AIModels.ResolvedPromptTemplate template = promptTemplateService.resolve(normalizedTaskType, deptCode, variables);
        List<AIModels.AIMessage> messages = buildMessages(normalizedTaskType, template.body(), variables, attachments);
        if (config == null || config.provider() == null || config.provider().isBlank() || config.apiKey() == null || config.apiKey().isBlank()) {
            return new AIModels.AIExecutionOutcome<>(
                    fallbackText,
                    AIModels.AIInvocationMeta.local(normalizedTaskType, template.promptVersion(), fallbackText)
            );
        }
        try {
            long started = System.currentTimeMillis();
            AIProvider provider = providerResolver.resolve(config.provider());
            AIModels.AIChatRequest request = new AIModels.AIChatRequest(
                    normalizedTaskType,
                    config.provider(),
                    config.modelName(),
                    config.apiUrl(),
                    config.apiKey(),
                    config.configVersion(),
                    template.promptVersion(),
                    java.util.UUID.randomUUID().toString(),
                    java.util.UUID.randomUUID().toString(),
                    config.timeoutSeconds() == null ? 30 : config.timeoutSeconds(),
                    stream,
                    stream,
                    temperatureFor(normalizedTaskType),
                    maxTokensFor(normalizedTaskType),
                    messages
            );
            AIModels.AIChatResponse response = stream ? provider.chatStream(request, chunkConsumer) : provider.chat(request);
            String text = AIModels.firstNonBlank(response.text(), fallbackText);
            AIModels.AIInvocationMeta meta = AIModels.AIInvocationMeta.remote(
                    normalizedTaskType,
                    config.provider(),
                    config.modelName(),
                    config.apiUrl(),
                    config.configVersion(),
                    template.promptVersion(),
                    request.requestId(),
                    request.traceId(),
                    Math.max(1L, System.currentTimeMillis() - started),
                    false,
                    null,
                    text
            );
            return new AIModels.AIExecutionOutcome<>(text, meta);
        } catch (Exception exception) {
            return new AIModels.AIExecutionOutcome<>(
                    fallbackText,
                    AIModels.AIInvocationMeta.local(
                            normalizedTaskType,
                            template.promptVersion(),
                            fallbackText,
                            true,
                            exception.getMessage()
                    )
            );
        }
    }

    private List<AIModels.AIMessage> buildMessages(String taskType,
                                                   String systemPrompt,
                                                   Map<String, String> variables,
                                                   List<AIModels.AIContentPart> attachments) {
        AIModels.AIMessage system = AIModels.AIMessage.system(systemPrompt);
        String userText = userPromptText(taskType, variables);
        AIModels.AIMessage user = AIModels.AIMessage.user(userText, attachments);
        return List.of(system, user);
    }

    private String userPromptText(String taskType, Map<String, String> variables) {
        Map<String, String> safe = variables == null ? Map.of() : variables;
        return switch (normalize(taskType)) {
            case "TRIAGE" -> String.join("\n",
                    "请根据主诉独立判断最合适的科室，不要受任何已有科室提示的影响。",
                    "主诉：" + safe.getOrDefault("chiefComplaint", ""),
                    "",
                    "请按以下格式输出（每行一个键值对，用英文冒号分隔）：",
                    "recommendedDepartment: 推荐科室全称（如：心内科、神经内科、骨科、皮肤科、内科）",
                    "reason: 推荐理由（1-2句话）");
            case "MEDICAL_RECORD" -> String.join("\n",
                    "请根据问诊内容生成结构化病历草稿，按以下格式输出（每行一个键值对，英文冒号分隔）：",
                    "chiefComplaint: 主诉（简明扼要）",
                    "presentIllness: 现病史",
                    "pastHistory: 既往史",
                    "physicalExam: 体格检查",
                    "preliminaryDiagnosis: 初步诊断",
                    "treatmentPlan: 治疗计划",
                    "docNote: 备注",
                    "",
                    "问诊文本：" + safe.getOrDefault("conversationText", ""),
                    "诊断方向：" + safe.getOrDefault("diagnosisDirection", ""),
                    "科室：" + safe.getOrDefault("departmentName", ""));
            case "DIAGNOSIS" -> String.join("\n",
                    "请根据问诊内容提供诊疗建议，按以下格式输出（每行一个键值对，英文冒号分隔）：",
                    "suggestedDiagnoses: 可能的诊断列表（每行一个，附带置信度百分比）",
                    "suggestedExamItems: 建议检查项目",
                    "summary: 总结",
                    "finalDiagnosisDirection: 最终诊断方向",
                    "",
                    "问诊文本：" + safe.getOrDefault("conversationText", ""),
                    "初步方向：" + safe.getOrDefault("diagnosisDirection", ""),
                    "科室：" + safe.getOrDefault("departmentName", ""));
            case "PRESCRIPTION_REVIEW" -> String.join("\n",
                    "请输出处方审核解释。",
                    "风险等级：" + safe.getOrDefault("riskLevel", ""),
                    "本地命中：" + safe.getOrDefault("localRuleHits", ""),
                    "缺失上下文：" + safe.getOrDefault("missingItems", ""),
                    "处方摘要：" + safe.getOrDefault("prescriptionSummary", ""));
            default -> safe.getOrDefault("inputText", "");
        };
    }

    private Double temperatureFor(String taskType) {
        return switch (normalize(taskType)) {
            case "MEDICAL_RECORD", "PRESCRIPTION_REVIEW" -> 0.2D;
            case "DIAGNOSIS" -> 0.3D;
            case "TRIAGE" -> 0.25D;
            default -> 0.2D;
        };
    }

    private Integer maxTokensFor(String taskType) {
        return switch (normalize(taskType)) {
            case "MEDICAL_RECORD" -> 1400;
            case "DIAGNOSIS" -> 1200;
            case "PRESCRIPTION_REVIEW" -> 900;
            case "TRIAGE" -> 500;
            default -> 1000;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
