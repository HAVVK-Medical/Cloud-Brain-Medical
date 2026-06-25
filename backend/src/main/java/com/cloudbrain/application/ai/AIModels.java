package com.cloudbrain.application.ai;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class AIModels {

    private AIModels() {
    }

    public static String safe(String value) {
        return value == null ? "" : value;
    }

    public static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    public static String shorten(String value, int length) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= length) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, length)) + "...";
    }

    public static List<String> splitNames(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("[,，、;；/|\\n\\r\\t]+"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }

    public static String joinNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return "";
        }
        return names.stream()
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining("、"));
    }

    public static String joinLines(String... lines) {
        return Arrays.stream(lines)
                .filter(line -> line != null && !line.isBlank())
                .collect(Collectors.joining("\n"));
    }

    public static String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record AIContentPart(
            String type,
            String text,
            String url,
            String data,
            String mimeType,
            String detail,
            String name
    ) {
        public static AIContentPart text(String text) {
            return new AIContentPart("text", text, null, null, null, null, null);
        }

        public static AIContentPart imageUrl(String url, String detail) {
            return new AIContentPart("image_url", null, url, null, null, detail, null);
        }

        public static AIContentPart videoUrl(String url) {
            return new AIContentPart("video_url", null, url, null, null, null, null);
        }

        public static AIContentPart inputAudio(String data, String format) {
            return new AIContentPart("input_audio", null, null, data, format, null, null);
        }

        public static AIContentPart fileUrl(String url, String mimeType, String name) {
            return new AIContentPart("file_url", null, url, null, mimeType, null, name);
        }
    }

    public record AIMessage(
            String role,
            List<AIContentPart> content
    ) {
        public static AIMessage system(String text) {
            return new AIMessage("system", List.of(AIContentPart.text(text)));
        }

        public static AIMessage user(String text, List<AIContentPart> extraParts) {
            List<AIContentPart> parts = new ArrayList<>();
            if (text != null && !text.isBlank()) {
                parts.add(AIContentPart.text(text));
            }
            if (extraParts != null && !extraParts.isEmpty()) {
                parts.addAll(extraParts);
            }
            return new AIMessage("user", List.copyOf(parts));
        }

        public static AIMessage assistant(String text) {
            return new AIMessage("assistant", List.of(AIContentPart.text(text)));
        }
    }

    public record AIChatRequest(
            String taskType,
            String provider,
            String modelName,
            String apiUrl,
            String apiKey,
            String configVersion,
            String promptVersion,
            String requestId,
            String traceId,
            int timeoutSeconds,
            boolean stream,
            boolean includeUsage,
            Double temperature,
            Integer maxTokens,
            List<AIMessage> messages
    ) {
    }

    public record AIChatResponse(
            String provider,
            String modelName,
            String requestId,
            String responseId,
            String finishReason,
            String text,
            String rawResponse
    ) {
    }

    public record ResolvedAIConfig(
            Long id,
            String provider,
            String modelName,
            String apiUrl,
            String apiKey,
            String keyVersion,
            String taskScope,
            Integer timeoutSeconds,
            String configVersion
    ) {
    }

    public record ResolvedPromptTemplate(
            String templateCode,
            String taskType,
            String deptCode,
            String body,
            Integer version,
            String promptVersion
    ) {
    }

    public record AIInvocationMeta(
            String taskType,
            String provider,
            String modelName,
            String apiUrl,
            String configVersion,
            String promptVersion,
            String requestId,
            String traceId,
            String callStatus,
            String errorSummary,
            long durationMs,
            boolean degraded,
            String responseText
    ) {
        public static AIInvocationMeta local(String taskType, String promptVersion, String responseText) {
            return local(taskType, promptVersion, responseText, true, null);
        }

        public static AIInvocationMeta local(String taskType,
                                             String promptVersion,
                                             String responseText,
                                             boolean degraded,
                                             String errorSummary) {
            return new AIInvocationMeta(
                    taskType,
                    "LOCAL_RULE",
                    "local-simulator",
                    null,
                    "local-v1",
                    promptVersion,
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(),
                    "COMPLETED",
                    errorSummary,
                    1L,
                    degraded,
                    responseText
            );
        }

        public static AIInvocationMeta remote(String taskType,
                                              String provider,
                                              String modelName,
                                              String apiUrl,
                                              String configVersion,
                                              String promptVersion,
                                              String requestId,
                                              String traceId,
                                              long durationMs,
                                              boolean degraded,
                                              String errorSummary,
                                              String responseText) {
            return new AIInvocationMeta(
                    taskType,
                    provider,
                    modelName,
                    apiUrl,
                    configVersion,
                    promptVersion,
                    requestId,
                    traceId,
                    "COMPLETED",
                    errorSummary,
                    durationMs,
                    degraded,
                    responseText
            );
        }
    }

    public record AIExecutionOutcome<T>(
            T result,
            AIInvocationMeta meta
    ) {
        public boolean hasResult() {
            return result != null;
        }
    }

    public record TriageAIResult(
            String recommendedDepartmentCode,
            String recommendedDepartmentName,
            List<String> recommendedDoctorNames,
            String reason,
            AIInvocationMeta meta
    ) {
        public TriageAIResult withMeta(AIInvocationMeta meta) {
            return new TriageAIResult(recommendedDepartmentCode, recommendedDepartmentName, recommendedDoctorNames, reason, meta);
        }

        public String toStreamText() {
            return joinLines(
                    "recommendedDepartmentCode: " + safe(recommendedDepartmentCode),
                    "recommendedDepartmentName: " + safe(recommendedDepartmentName),
                    "recommendedDoctorNames: " + joinNames(recommendedDoctorNames),
                    "reason: " + safe(reason)
            );
        }
    }

    public record MedicalRecordAIResult(
            String chiefComplaint,
            String presentIllness,
            String pastHistory,
            String physicalExam,
            String preliminaryDiagnosis,
            String treatmentPlan,
            String docNote,
            AIInvocationMeta meta
    ) {
        public MedicalRecordAIResult withMeta(AIInvocationMeta meta) {
            return new MedicalRecordAIResult(chiefComplaint, presentIllness, pastHistory, physicalExam, preliminaryDiagnosis, treatmentPlan, docNote, meta);
        }

        public String toStreamText() {
            return joinLines(
                    "chiefComplaint: " + safe(chiefComplaint),
                    "presentIllness: " + safe(presentIllness),
                    "pastHistory: " + safe(pastHistory),
                    "physicalExam: " + safe(physicalExam),
                    "preliminaryDiagnosis: " + safe(preliminaryDiagnosis),
                    "treatmentPlan: " + safe(treatmentPlan),
                    "docNote: " + safe(docNote)
            );
        }
    }

    public record DiagnosisAIResult(
            String suggestedDiagnoses,
            String suggestedExamItems,
            String summary,
            String finalDiagnosisDirection,
            AIInvocationMeta meta
    ) {
        public DiagnosisAIResult withMeta(AIInvocationMeta meta) {
            return new DiagnosisAIResult(suggestedDiagnoses, suggestedExamItems, summary, finalDiagnosisDirection, meta);
        }

        public String toStreamText() {
            return joinLines(
                    "suggestedDiagnoses: " + safe(suggestedDiagnoses),
                    "suggestedExamItems: " + safe(suggestedExamItems),
                    "summary: " + safe(summary),
                    "finalDiagnosisDirection: " + safe(finalDiagnosisDirection)
            );
        }
    }

    public record PrescriptionReviewAIResult(
            String llmSuggestion,
            String llmSummary,
            AIInvocationMeta meta
    ) {
        public PrescriptionReviewAIResult withMeta(AIInvocationMeta meta) {
            return new PrescriptionReviewAIResult(llmSuggestion, llmSummary, meta);
        }

        public String toStreamText() {
            return joinLines(
                    "llmSuggestion: " + safe(llmSuggestion),
                    "llmSummary: " + safe(llmSummary)
            );
        }
    }
}
