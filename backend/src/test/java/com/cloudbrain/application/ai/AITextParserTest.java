package com.cloudbrain.application.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AITextParserTest {

    @Test
    void parseKeyValueBlockKeepsOrderAndTrimsKeysAndValues() {
        Map<String, String> parsed = AITextParser.parseKeyValueBlock("""
                recommendedDepartmentCode: RESP
                  recommendedDepartmentName :  呼吸内科
                reason: 咳嗽三天: 伴发热
                invalid line without delimiter
                : missing key
                recommendedDoctorNames: 张医生，李医生
                """);

        assertThat(parsed)
                .containsExactly(
                        Map.entry("recommendedDepartmentCode", "RESP"),
                        Map.entry("recommendedDepartmentName", "呼吸内科"),
                        Map.entry("reason", "咳嗽三天: 伴发热"),
                        Map.entry("recommendedDoctorNames", "张医生，李医生")
                );
    }

    @Test
    void parseKeyValueBlockReturnsEmptyMapForNullBlankOrMalformedInput() {
        assertThat(AITextParser.parseKeyValueBlock(null)).isEmpty();
        assertThat(AITextParser.parseKeyValueBlock("   \n\t")).isEmpty();
        assertThat(AITextParser.parseKeyValueBlock("not a key value line")).isEmpty();
    }

    @Test
    void firstNonBlankReturnsTrimmedValueOrFallback() {
        assertThat(AITextParser.firstNonBlank(Map.of("summary", "  需要复诊  "), "summary", "fallback"))
                .isEqualTo("需要复诊");
        assertThat(AITextParser.firstNonBlank(Map.of("summary", "  "), "summary", "fallback"))
                .isEqualTo("fallback");
        assertThat(AITextParser.firstNonBlank(null, "summary", "fallback"))
                .isEqualTo("fallback");
    }
}
