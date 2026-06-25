package com.cloudbrain.application.ai;

import com.cloudbrain.entity.core.PromptTemplateEntity;
import com.cloudbrain.repository.PromptTemplateJpaRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class PromptTemplateService {

    private static final String ACTIVE = "ACTIVE";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*\\}\\}");

    private final PromptTemplateJpaRepository promptTemplateRepository;

    public PromptTemplateService(PromptTemplateJpaRepository promptTemplateRepository) {
        this.promptTemplateRepository = promptTemplateRepository;
    }

    public AIModels.ResolvedPromptTemplate resolve(String taskType, String deptCode, Map<String, String> variables) {
        String normalizedTaskType = normalize(taskType);
        String normalizedDeptCode = blankToNull(deptCode);
        PromptTemplateEntity template = null;
        if (normalizedDeptCode != null) {
            template = promptTemplateRepository
                    .findFirstByTaskTypeAndDeptCodeAndDefaultTemplateTrueAndStatusOrderByVersionDesc(
                            normalizedTaskType,
                            normalizedDeptCode,
                            ACTIVE
                    )
                    .orElse(null);
        }
        if (template == null) {
            template = promptTemplateRepository
                    .findFirstByTaskTypeAndDeptCodeIsNullAndDefaultTemplateTrueAndStatusOrderByVersionDesc(
                            normalizedTaskType,
                            ACTIVE
                    )
                    .orElse(null);
        }
        if (template == null) {
            template = promptTemplateRepository.findByTaskTypeAndStatusOrderByVersionDesc(normalizedTaskType, ACTIVE).stream()
                    .findFirst()
                    .orElse(null);
        }
        if (template == null) {
            return builtin(normalizedTaskType, normalizedDeptCode, variables);
        }
        String body = render(template.getTemplateBody(), variables, template.getVariableWhitelist());
        if (body.isBlank()) {
            body = builtinBody(normalizedTaskType, normalizedDeptCode, variables);
        }
        return new AIModels.ResolvedPromptTemplate(
                template.getTemplateCode(),
                template.getTaskType(),
                template.getDeptCode(),
                body,
                template.getVersion(),
                template.getTemplateCode() + "-v" + template.getVersion()
        );
    }

    public String renderBuiltin(String taskType, String deptCode, Map<String, String> variables) {
        return builtinBody(normalize(taskType), blankToNull(deptCode), variables);
    }

    private AIModels.ResolvedPromptTemplate builtin(String taskType, String deptCode, Map<String, String> variables) {
        return new AIModels.ResolvedPromptTemplate(
                "builtin-" + taskType.toLowerCase(Locale.ROOT),
                taskType,
                deptCode,
                builtinBody(taskType, deptCode, variables),
                0,
                "builtin-" + taskType.toLowerCase(Locale.ROOT) + "-v0"
        );
    }

    private String builtinBody(String taskType, String deptCode, Map<String, String> variables) {
        Map<String, String> safe = variables == null ? Map.of() : new LinkedHashMap<>(variables);
        return switch (taskType) {
            case "TRIAGE" -> """
                    你是医院智能分诊助手。请基于主诉和候选科室给出简洁、审慎的解释。
                    要求：
                    1. 只输出一段中文理由，不要重新改写推荐结果。
                    2. 不要编造未提供的检查结果。
                    3. 如存在急症线索，请提醒尽快就医。
                    主诉：{{chiefComplaint}}
                    候选科室：{{departmentName}}
                    候选医生：{{doctorNames}}
                    """;
            case "MEDICAL_RECORD" -> """
                    你是门诊病历整理助手。请根据问诊内容生成结构化病历草稿，严格使用下列键：
                    chiefComplaint:
                    presentIllness:
                    pastHistory:
                    physicalExam:
                    preliminaryDiagnosis:
                    treatmentPlan:
                    docNote:
                    要求：
                    1. 内容必须简洁、医学表达规范。
                    2. 如信息不足，要明确写出待补充内容。
                    3. 不要输出多余的解释文字。
                    问诊文本：{{conversationText}}
                    诊断方向：{{diagnosisDirection}}
                    科室：{{departmentName}}
                    """;
            case "DIAGNOSIS" -> """
                    你是诊疗建议助手。请基于病史摘要给出结构化建议，严格使用下列键：
                    suggestedDiagnoses:
                    suggestedExamItems:
                    summary:
                    finalDiagnosisDirection:
                    要求：
                    1. 保持审慎，优先给出常见鉴别方向。
                    2. 检查项目要符合当前症状，不要泛化堆砌。
                    3. 不要输出额外段落。
                    问诊文本：{{conversationText}}
                    初步方向：{{diagnosisDirection}}
                    科室：{{departmentName}}
                    """;
            case "PRESCRIPTION_REVIEW" -> """
                    你是处方审核解释助手。请基于本地规则命中的结果给出解释和补充建议，严格使用下列键：
                    llmSuggestion:
                    llmSummary:
                    要求：
                    1. 不要改变本地规则给出的风险等级。
                    2. 只解释命中原因、补充注意事项和复核建议。
                    3. 如上下文不完整，说明需要人工确认。
                    风险等级：{{riskLevel}}
                    本地命中：{{localRuleHits}}
                    缺失上下文：{{missingItems}}
                    处方摘要：{{prescriptionSummary}}
                    """;
            default -> """
                    你是医院智能助手。请根据输入给出简洁、审慎的建议。
                    输入：{{inputText}}
                    """;
        };
    }

    private String render(String template, Map<String, String> variables, String whitelist) {
        if (template == null || template.isBlank()) {
            return "";
        }
        Map<String, String> safeVariables = variables == null ? Map.of() : variables;
        List<String> allowed = parseWhitelist(whitelist);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = safeVariables.getOrDefault(key, "");
            if (!allowed.isEmpty() && !allowed.contains(key)) {
                replacement = "";
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString().trim();
    }

    private List<String> parseWhitelist(String whitelist) {
        if (whitelist == null || whitelist.isBlank()) {
            return List.of();
        }
        String normalized = whitelist
                .replace('[', ' ')
                .replace(']', ' ')
                .replace('{', ' ')
                .replace('}', ' ')
                .replace('"', ' ')
                .replace('\'', ' ')
                .replace('\n', ',')
                .replace('\r', ',');
        return java.util.Arrays.stream(normalized.split("[,;|\\s]+"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .distinct()
                .toList();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
