package com.cloudbrain.application.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AIModelsTest {

    @Test
    void stringHelpersHandleNullWhitespaceAndLengthBoundaries() {
        assertThat(AIModels.safe(null)).isEmpty();
        assertThat(AIModels.firstNonBlank(null, " ", "  value  ")).isEqualTo("value");
        assertThat(AIModels.firstNonBlank((String[]) null)).isEmpty();
        assertThat(AIModels.shorten(" a   b   c ", 20)).isEqualTo("a b c");
        assertThat(AIModels.shorten("abcdef", 3)).isEqualTo("abc...");
        assertThat(AIModels.shorten(null, 3)).isEmpty();
        assertThat(AIModels.normalizeKey("  DeepSeek  ")).isEqualTo("deepseek");
    }

    @Test
    void nameHelpersSplitJoinAndIgnoreBlankItems() {
        assertThat(AIModels.splitNames("张三, 李四\n王五")).containsExactly("张三", "李四", "王五");
        assertThat(AIModels.splitNames("  ")).isEmpty();
        assertThat(AIModels.joinNames(List.of(" 张三 ", "", "李四"))).isEqualTo("张三、李四");
        assertThat(AIModels.joinNames(null)).isEmpty();
        assertThat(AIModels.joinLines("a", "", null, "b")).isEqualTo("a\nb");
    }

    @Test
    void messageFactoriesBuildExpectedRolesAndContentParts() {
        var extra = List.of(AIModels.AIContentPart.imageUrl("https://example.test/a.png", "high"));
        var user = AIModels.AIMessage.user(" hello ", extra);

        assertThat(AIModels.AIMessage.system("sys").role()).isEqualTo("system");
        assertThat(AIModels.AIMessage.assistant("ok").content()).extracting(AIModels.AIContentPart::text).containsExactly("ok");
        assertThat(user.role()).isEqualTo("user");
        assertThat(user.content()).hasSize(2);
        assertThat(AIModels.AIMessage.user(" ", extra).content()).containsExactlyElementsOf(extra);
    }

    @Test
    void streamTextSerializesStructuredAiResults() {
        var meta = AIModels.AIInvocationMeta.local("TRIAGE", "v1", "ok");
        assertThat(meta.provider()).isEqualTo("LOCAL_RULE");
        assertThat(meta.degraded()).isTrue();
        assertThat(new AIModels.AIExecutionOutcome<>("result", meta).hasResult()).isTrue();
        assertThat(new AIModels.AIExecutionOutcome<>(null, meta).hasResult()).isFalse();

        assertThat(new AIModels.TriageAIResult("RESP", "呼吸内科", List.of("张医生", "李医生"), "咳嗽", meta).toStreamText())
                .contains("recommendedDepartmentCode: RESP", "recommendedDoctorNames: 张医生、李医生");
        assertThat(new AIModels.MedicalRecordAIResult("咳嗽", "三天", "无", "体温正常", "感冒", "休息", "复诊", meta).toStreamText())
                .contains("chiefComplaint: 咳嗽", "docNote: 复诊");
        assertThat(new AIModels.DiagnosisAIResult("感冒", "血常规", "轻症", "呼吸道感染", meta).toStreamText())
                .contains("suggestedExamItems: 血常规");
        assertThat(new AIModels.PrescriptionReviewAIResult("通过", "低风险", meta).toStreamText())
                .contains("llmSuggestion: 通过", "llmSummary: 低风险");
    }
}
