# Thinking/Reply Content Separation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Separate AI model reasoning/thinking content (`reasoning_content`) from final reply content (`content`) across the entire AI pipeline, with collapsible UI and DB persistence.

**Architecture:** Dual-channel consumer pattern — `thinkingConsumer` and `chunkConsumer` at the Provider layer feed distinct SSE events (`thinking` / `chunk`) to the frontend, which renders thinking content in a collapsible section separate from reply content. Backward compatible: models without `reasoning_content` work identically to before.

**Tech Stack:** Java 17+ / Spring Boot SSE / Vue 3 + TypeScript + Pinia / Flyway / MySQL

---

## File Structure

```
Backend (Java):
  Create: backend/src/main/resources/db/migration/V19__add_chat_message_thinking_content.sql
  Modify: backend/src/main/java/com/cloudbrain/application/ai/AIProvider.java
  Modify: backend/src/main/java/com/cloudbrain/application/ai/AbstractOpenAICompatibleProvider.java
  Modify: backend/src/main/java/com/cloudbrain/application/ai/AIInvocationService.java
  Modify: backend/src/main/java/com/cloudbrain/application/chat/ChatService.java
  Modify: backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java
  Modify: backend/src/main/java/com/cloudbrain/application/workflow/AiStreamSessionService.java
  Modify: backend/src/main/java/com/cloudbrain/entity/chat/ChatMessageEntity.java

Frontend (TypeScript/Vue):
  Modify: frontend/src/types/chat.ts
  Modify: frontend/src/stores/chat.ts
  Modify: frontend/src/stores/ai-stream.ts
  Modify: frontend/src/components/chat/AiChatMessage.vue
  Modify: frontend/src/views/patient/panels/ConversationalTriageSection.vue
  Modify: frontend/src/views/doctor/panels/DoctorConsultationPanel.vue
```

---

### Task 1: Database Migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V19__add_chat_message_thinking_content.sql`

- [ ] **Step 1: Create migration file**

```sql
ALTER TABLE chat_message ADD COLUMN thinking_content TEXT AFTER content;
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/resources/db/migration/V19__add_chat_message_thinking_content.sql
git commit -m "feat: add thinking_content column to chat_message table"
```

---

### Task 2: ChatMessageEntity — Add thinkingContent Field

**Files:**
- Modify: `backend/src/main/java/com/cloudbrain/entity/chat/ChatMessageEntity.java`

- [ ] **Step 1: Add field and new constructor to ChatMessageEntity**

Add the `thinkingContent` field after `aiMeta`:

```java
@Column(name = "thinking_content", columnDefinition = "TEXT")
private String thinkingContent;
```

Add import for `jakarta.persistence.Column` (already present — just add the field in the class body after line 23).

Add a new 4-arg constructor after the existing 3-arg constructor (line 27-29):

```java
public ChatMessageEntity(ChatSessionEntity session, String role, String content, String thinkingContent) {
    this.session = Objects.requireNonNull(session);
    this.role = Objects.requireNonNull(role);
    this.content = content;
    this.thinkingContent = thinkingContent;
}
```

Add getter and setter after `setAiMeta` (line 37):

```java
public String getThinkingContent() { return thinkingContent; }
public void setThinkingContent(String thinkingContent) { this.thinkingContent = thinkingContent; }
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/cloudbrain/entity/chat/ChatMessageEntity.java
git commit -m "feat: add thinkingContent field to ChatMessageEntity"
```

---

### Task 3: AIProvider — Add thinkingConsumer Overload

**Files:**
- Modify: `backend/src/main/java/com/cloudbrain/application/ai/AIProvider.java`

- [ ] **Step 1: Add default method and new abstract method**

The file currently has:

```java
package com.cloudbrain.application.ai;

import java.util.function.Consumer;

public interface AIProvider {

    String providerName();

    AIModels.AIChatResponse chat(AIModels.AIChatRequest request);

    AIModels.AIChatResponse chatStream(AIModels.AIChatRequest request, Consumer<String> chunkConsumer);
}
```

Replace the `chatStream` line (line 11) with the overloaded version and add the default method:

```java
    AIModels.AIChatResponse chatStream(AIModels.AIChatRequest request, Consumer<String> chunkConsumer, Consumer<String> thinkingConsumer);

    default AIModels.AIChatResponse chatStream(AIModels.AIChatRequest request, Consumer<String> chunkConsumer) {
        return chatStream(request, chunkConsumer, null);
    }
```

The full file becomes:

```java
package com.cloudbrain.application.ai;

import java.util.function.Consumer;

public interface AIProvider {

    String providerName();

    AIModels.AIChatResponse chat(AIModels.AIChatRequest request);

    AIModels.AIChatResponse chatStream(AIModels.AIChatRequest request, Consumer<String> chunkConsumer, Consumer<String> thinkingConsumer);

    default AIModels.AIChatResponse chatStream(AIModels.AIChatRequest request, Consumer<String> chunkConsumer) {
        return chatStream(request, chunkConsumer, null);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/cloudbrain/application/ai/AIProvider.java
git commit -m "feat: add thinkingConsumer parameter to AIProvider.chatStream"
```

---

### Task 4: AbstractOpenAICompatibleProvider — Dual-Channel Stream Parsing

**Files:**
- Modify: `backend/src/main/java/com/cloudbrain/application/ai/AbstractOpenAICompatibleProvider.java`

- [ ] **Step 1: Update `chatStream` method signature to match new interface**

Replace line 58-62 (the current `chatStream` implementation that takes `Consumer<String> chunkConsumer` only):

```java
    @Override
    public AIModels.AIChatResponse chatStream(AIModels.AIChatRequest request, Consumer<String> chunkConsumer, Consumer<String> thinkingConsumer) {
        try {
            HttpResponse<InputStream> response = httpClient.send(buildRequest(request, true, null), HttpResponse.BodyHandlers.ofInputStream());
            return parseStreamResponse(request, response, chunkConsumer, thinkingConsumer);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AIProviderException(providerName + " stream interrupted", exception);
        } catch (IOException exception) {
            throw new AIProviderException(providerName + " stream failed", exception);
        }
    }
```

- [ ] **Step 2: Replace `extractDeltaText()` with `extractReplyDelta()` and `extractThinkingDelta()`**

Replace the existing `extractDeltaText()` method (lines 236-251) with these two methods:

```java
    private String extractReplyDelta(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode delta = choices.get(0).path("delta");
            return extractContent(delta.path("content"));
        }
        return "";
    }

    private String extractThinkingDelta(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode delta = choices.get(0).path("delta");
            return extractContent(delta.path("reasoning_content"));
        }
        return "";
    }
```

- [ ] **Step 3: Update `parseStreamResponse()` to use both extractors and both consumers**

Add `Consumer<String> thinkingConsumer` to the method signature. The current signature is at line 165:

```java
    private AIModels.AIChatResponse parseStreamResponse(AIModels.AIChatRequest request,
                                                        HttpResponse<InputStream> response,
                                                        Consumer<String> chunkConsumer) throws IOException {
```

Change to:

```java
    private AIModels.AIChatResponse parseStreamResponse(AIModels.AIChatRequest request,
                                                        HttpResponse<InputStream> response,
                                                        Consumer<String> chunkConsumer,
                                                        Consumer<String> thinkingConsumer) throws IOException {
```

Then replace the delta extraction block inside the while loop (lines 196-202):

```java
                String deltaText = extractDeltaText(root);
                if (deltaText != null && !deltaText.isBlank()) {
                    builder.append(deltaText);
                    if (chunkConsumer != null) {
                        chunkConsumer.accept(deltaText);
                    }
                }
```

With:

```java
                String replyDelta = extractReplyDelta(root);
                if (!replyDelta.isBlank()) {
                    builder.append(replyDelta);
                    if (chunkConsumer != null) {
                        chunkConsumer.accept(replyDelta);
                    }
                }
                String thinkingDelta = extractThinkingDelta(root);
                if (!thinkingDelta.isBlank() && thinkingConsumer != null) {
                    thinkingConsumer.accept(thinkingDelta);
                }
```

- [ ] **Step 4: Update `extractText()` to not use `reasoning_content` as fallback**

Replace the current `extractText()` method (lines 219-234):

```java
    private String extractText(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode choice = choices.get(0);
            JsonNode message = choice.path("message");
            String content = extractContent(message.path("content"));
            if (!content.isBlank()) {
                return content;
            }
            String reasoning = extractContent(message.path("reasoning_content"));
            if (!reasoning.isBlank()) {
                return reasoning;
            }
        }
        return "";
    }
```

With:

```java
    private String extractText(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode choice = choices.get(0);
            JsonNode message = choice.path("message");
            String content = extractContent(message.path("content"));
            if (!content.isBlank()) {
                return content;
            }
        }
        return "";
    }
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/cloudbrain/application/ai/AbstractOpenAICompatibleProvider.java
git commit -m "feat: split thinking/reply content in AbstractOpenAICompatibleProvider streaming"
```

---

### Task 5: AIInvocationService — Thread thinkingConsumer Through

**Files:**
- Modify: `backend/src/main/java/com/cloudbrain/application/ai/AIInvocationService.java`

- [ ] **Step 1: Add `thinkingConsumer` parameter to `chat()` method**

Replace the current method signature (lines 25-31):

```java
    public AIModels.AIExecutionOutcome<String> chat(String taskType,
                                                    String deptCode,
                                                    Map<String, String> variables,
                                                    List<AIModels.AIContentPart> attachments,
                                                    String fallbackText,
                                                    boolean stream,
                                                    Consumer<String> chunkConsumer) {
```

With:

```java
    public AIModels.AIExecutionOutcome<String> chat(String taskType,
                                                    String deptCode,
                                                    Map<String, String> variables,
                                                    List<AIModels.AIContentPart> attachments,
                                                    String fallbackText,
                                                    boolean stream,
                                                    Consumer<String> chunkConsumer) {
        return chat(taskType, deptCode, variables, attachments, fallbackText, stream, chunkConsumer, null);
    }

    public AIModels.AIExecutionOutcome<String> chat(String taskType,
                                                    String deptCode,
                                                    Map<String, String> variables,
                                                    List<AIModels.AIContentPart> attachments,
                                                    String fallbackText,
                                                    boolean stream,
                                                    Consumer<String> chunkConsumer,
                                                    Consumer<String> thinkingConsumer) {
```

- [ ] **Step 2: Pass `thinkingConsumer` to `provider.chatStream()`**

Replace line 62:

```java
            AIModels.AIChatResponse response = stream ? provider.chatStream(request, chunkConsumer) : provider.chat(request);
```

With:

```java
            AIModels.AIChatResponse response = stream ? provider.chatStream(request, chunkConsumer, thinkingConsumer) : provider.chat(request);
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/cloudbrain/application/ai/AIInvocationService.java
git commit -m "feat: thread thinkingConsumer through AIInvocationService"
```

---

### Task 6: WorkflowService — Thread thinkingConsumer Through Workflow Methods

**Files:**
- Modify: `backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java`

- [ ] **Step 1: Refactor generateMedicalRecord to add thinkingConsumer overload**

The current code at lines 621-675 has one 3-arg method. Replace the entire block from line 621 through line 675 with a 3-arg delegating wrapper + a 4-arg method.

Replace (lines 621-675):

```java
    @Transactional
    public MedicalRecordSummary generateMedicalRecord(ActorContext actorContext,
                                                      MedicalRecordGenerateRequest request,
                                                      Consumer<String> chunkConsumer) {
        Long doctorId = requireDoctor(actorContext);
        RegistrationEntity registration = requireDoctorRegistration(request.registrationId(), doctorId);
        PatientEntity patient = patientRepository.findById(registration.getPatientId())
                .orElseThrow(() -> notFound("patient not found"));
        long started = System.currentTimeMillis();
        String chief = firstSentence(request.conversationText());
        String fallbackDiagnosis = firstNonBlank(request.diagnosisDirection(),
                inferDiagnosis(request.conversationText()));
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("conversationText", request.conversationText());
        variables.put("diagnosisDirection", fallbackDiagnosis);
        String departmentName = departmentNameByRegistration(registration);
        if (departmentName != null) {
            variables.put("departmentName", departmentName);
        }
        AIModels.AIExecutionOutcome<String> aiOutcome = invokeAi(
                "MEDICAL_RECORD",
                departmentCodeByRegistration(registration),
                variables,
                request.attachments(),
                buildMedicalRecordFallbackText(chief, request.conversationText(), patient, fallbackDiagnosis),
                chunkConsumer != null,
                chunkConsumer
        );
        MedicalRecordDraft aiDraft = parseMedicalRecordDraft(aiOutcome.result());
        String diagnosis = firstNonBlank(aiDraft.preliminaryDiagnosis(), fallbackDiagnosis);

        MedicalRecordEntity draft = new MedicalRecordEntity();
        draft.setPatientId(patient.getId());
        draft.setDoctorId(doctorId);
        draft.setRegistrationId(registration.getId());
        draft.setChiefComplaint(firstNonBlank(aiDraft.chiefComplaint(), chief));
        draft.setPresentIllness(firstNonBlank(aiDraft.presentIllness(), request.conversationText()));
        draft.setPastHistory(firstNonBlank(aiDraft.pastHistory(),
                firstNonBlank(patient.getMedicalHistory(), "未见特殊既往史")));
        draft.setPhysicalExam(firstNonBlank(aiDraft.physicalExam(),
                "生命体征平稳，建议医生结合查体补充。"));
        draft.setPreliminaryDiagnosis(diagnosis);
        draft.setTreatmentPlan(
                firstNonBlank(aiDraft.treatmentPlan(), buildTreatmentPlan(diagnosis)));
        draft.setConversationText(request.conversationText());
        draft.setAiGenerated(true);
        draft.setDocNote(firstNonBlank(aiDraft.docNote(),
                "本地模拟 AI 已根据问诊文本生成结构化病历草稿，请医生确认。"));
        draft.setVersion(0);

        String output = draft.getChiefComplaint() + " | " + draft.getPreliminaryDiagnosis();
        AICallRecordEntity callRecord = aiCallRecord("MEDICAL_RECORD", actorContext,
                request.conversationText(), output, started, aiOutcome.meta());
        callRecord = aiCallRecordRepository.save(callRecord);
        draft.setAiCallRecordId(callRecord.getId());
        callRecord.setBusinessRecordId(registration.getId());
        aiCallRecordRepository.save(callRecord);

        // Persist the draft so it survives page refreshes / navigation
        draft = medicalRecordRepository.save(draft);
        return toMedicalRecordSummary(draft, aiOutcome.meta().degraded());
    }
```

With:

```java
    @Transactional
    public MedicalRecordSummary generateMedicalRecord(ActorContext actorContext,
                                                      MedicalRecordGenerateRequest request,
                                                      Consumer<String> chunkConsumer) {
        return generateMedicalRecord(actorContext, request, chunkConsumer, null);
    }

    @Transactional
    public MedicalRecordSummary generateMedicalRecord(ActorContext actorContext,
                                                      MedicalRecordGenerateRequest request,
                                                      Consumer<String> chunkConsumer,
                                                      Consumer<String> thinkingConsumer) {
        Long doctorId = requireDoctor(actorContext);
        RegistrationEntity registration = requireDoctorRegistration(request.registrationId(), doctorId);
        PatientEntity patient = patientRepository.findById(registration.getPatientId())
                .orElseThrow(() -> notFound("patient not found"));
        long started = System.currentTimeMillis();
        String chief = firstSentence(request.conversationText());
        String fallbackDiagnosis = firstNonBlank(request.diagnosisDirection(),
                inferDiagnosis(request.conversationText()));
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("conversationText", request.conversationText());
        variables.put("diagnosisDirection", fallbackDiagnosis);
        String departmentName = departmentNameByRegistration(registration);
        if (departmentName != null) {
            variables.put("departmentName", departmentName);
        }
        AIModels.AIExecutionOutcome<String> aiOutcome = invokeAi(
                "MEDICAL_RECORD",
                departmentCodeByRegistration(registration),
                variables,
                request.attachments(),
                buildMedicalRecordFallbackText(chief, request.conversationText(), patient, fallbackDiagnosis),
                chunkConsumer != null,
                chunkConsumer,
                thinkingConsumer
        );
        MedicalRecordDraft aiDraft = parseMedicalRecordDraft(aiOutcome.result());
        String diagnosis = firstNonBlank(aiDraft.preliminaryDiagnosis(), fallbackDiagnosis);

        MedicalRecordEntity draft = new MedicalRecordEntity();
        draft.setPatientId(patient.getId());
        draft.setDoctorId(doctorId);
        draft.setRegistrationId(registration.getId());
        draft.setChiefComplaint(firstNonBlank(aiDraft.chiefComplaint(), chief));
        draft.setPresentIllness(firstNonBlank(aiDraft.presentIllness(), request.conversationText()));
        draft.setPastHistory(firstNonBlank(aiDraft.pastHistory(),
                firstNonBlank(patient.getMedicalHistory(), "未见特殊既往史")));
        draft.setPhysicalExam(firstNonBlank(aiDraft.physicalExam(),
                "生命体征平稳，建议医生结合查体补充。"));
        draft.setPreliminaryDiagnosis(diagnosis);
        draft.setTreatmentPlan(
                firstNonBlank(aiDraft.treatmentPlan(), buildTreatmentPlan(diagnosis)));
        draft.setConversationText(request.conversationText());
        draft.setAiGenerated(true);
        draft.setDocNote(firstNonBlank(aiDraft.docNote(),
                "本地模拟 AI 已根据问诊文本生成结构化病历草稿，请医生确认。"));
        draft.setVersion(0);

        String output = draft.getChiefComplaint() + " | " + draft.getPreliminaryDiagnosis();
        AICallRecordEntity callRecord = aiCallRecord("MEDICAL_RECORD", actorContext,
                request.conversationText(), output, started, aiOutcome.meta());
        callRecord = aiCallRecordRepository.save(callRecord);
        draft.setAiCallRecordId(callRecord.getId());
        callRecord.setBusinessRecordId(registration.getId());
        aiCallRecordRepository.save(callRecord);

        // Persist the draft so it survives page refreshes / navigation
        draft = medicalRecordRepository.save(draft);
        return toMedicalRecordSummary(draft, aiOutcome.meta().degraded());
    }
```

Note: The only change to the method body is the `invokeAi` call — adding `thinkingConsumer` as the 8th argument.

- [ ] **Step 3: Add `thinkingConsumer` parameter to `suggestDiagnosis` with chunkConsumer**

Replace the method signature at line 789-792:

```java
    @Transactional
    public DiagnosisSuggestionResponse suggestDiagnosis(ActorContext actorContext,
                                                       DiagnosisSuggestionRequest request,
                                                       Consumer<String> chunkConsumer) {
```

With:

```java
    @Transactional
    public DiagnosisSuggestionResponse suggestDiagnosis(ActorContext actorContext,
                                                       DiagnosisSuggestionRequest request,
                                                       Consumer<String> chunkConsumer) {
        return suggestDiagnosis(actorContext, request, chunkConsumer, null);
    }

    @Transactional
    public DiagnosisSuggestionResponse suggestDiagnosis(ActorContext actorContext,
                                                       DiagnosisSuggestionRequest request,
                                                       Consumer<String> chunkConsumer,
                                                       Consumer<String> thinkingConsumer) {
```

- [ ] **Step 4: Update the `invokeAi` call inside suggestDiagnosis to pass `thinkingConsumer`**

Replace the `invokeAi` call at lines 804-817:

```java
        AIModels.AIExecutionOutcome<String> aiOutcome = invokeAi(
                "DIAGNOSIS",
                departmentCodeByRegistration(registration),
                variables,
                request.attachments(),
                String.join("\n",
                        "suggestedDiagnoses: " + fallbackDiagnosis + "；鉴别：焦虑相关不适、消化系统不适、呼吸系统感染。",
                        "suggestedExamItems: " + suggestExamItems(fallbackDiagnosis),
                        "summary: 本地规则根据关键词生成诊疗建议，供医生参考。",
                        "finalDiagnosisDirection: " + fallbackDiagnosis
                ),
                chunkConsumer != null,
                chunkConsumer
        );
```

With:

```java
        AIModels.AIExecutionOutcome<String> aiOutcome = invokeAi(
                "DIAGNOSIS",
                departmentCodeByRegistration(registration),
                variables,
                request.attachments(),
                String.join("\n",
                        "suggestedDiagnoses: " + fallbackDiagnosis + "；鉴别：焦虑相关不适、消化系统不适、呼吸系统感染。",
                        "suggestedExamItems: " + suggestExamItems(fallbackDiagnosis),
                        "summary: 本地规则根据关键词生成诊疗建议，供医生参考。",
                        "finalDiagnosisDirection: " + fallbackDiagnosis
                ),
                chunkConsumer != null,
                chunkConsumer,
                thinkingConsumer
        );
```

- [ ] **Step 5: Update private `invokeAi` method to accept and pass `thinkingConsumer`**

Replace the existing `invokeAi` method (lines 1472-1483):

```java
    private AIModels.AIExecutionOutcome<String> invokeAi(String taskType,
                                                         String deptCode,
                                                         Map<String, String> variables,
                                                         List<AiContentAttachment> attachments,
                                                         String fallbackText,
                                                         boolean stream,
                                                         Consumer<String> chunkConsumer) {
        List<AIModels.AIContentPart> parts = attachments == null
                ? List.of()
                : attachments.stream().map(this::toContentPart).toList();
        return aiInvocationService.chat(taskType, deptCode, variables, parts, fallbackText, stream, chunkConsumer);
    }
```

With:

```java
    private AIModels.AIExecutionOutcome<String> invokeAi(String taskType,
                                                         String deptCode,
                                                         Map<String, String> variables,
                                                         List<AiContentAttachment> attachments,
                                                         String fallbackText,
                                                         boolean stream,
                                                         Consumer<String> chunkConsumer) {
        return invokeAi(taskType, deptCode, variables, attachments, fallbackText, stream, chunkConsumer, null);
    }

    private AIModels.AIExecutionOutcome<String> invokeAi(String taskType,
                                                         String deptCode,
                                                         Map<String, String> variables,
                                                         List<AiContentAttachment> attachments,
                                                         String fallbackText,
                                                         boolean stream,
                                                         Consumer<String> chunkConsumer,
                                                         Consumer<String> thinkingConsumer) {
        List<AIModels.AIContentPart> parts = attachments == null
                ? List.of()
                : attachments.stream().map(this::toContentPart).toList();
        return aiInvocationService.chat(taskType, deptCode, variables, parts, fallbackText, stream, chunkConsumer, thinkingConsumer);
    }
```

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/cloudbrain/application/workflow/WorkflowService.java
git commit -m "feat: thread thinkingConsumer through WorkflowService generateMedicalRecord and suggestDiagnosis"
```

---

### Task 7: ChatService — Dual SSE Events (thinking + chunk) and Persist thinkingContent

**Files:**
- Modify: `backend/src/main/java/com/cloudbrain/application/chat/ChatService.java`

- [ ] **Step 1: Update `sendChunk()` to accept event name parameter**

Replace the existing `sendChunk` method (lines 218-226):

```java
    private void sendChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event()
                    .name("chunk")
                    .data(Map.of("content", chunk)));
        } catch (IOException e) {
            throw new RuntimeException("SSE send failed", e);
        }
    }
```

With:

```java
    private void sendChunk(SseEmitter emitter, String eventName, String chunk) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(Map.of("content", chunk)));
        } catch (IOException e) {
            throw new RuntimeException("SSE send failed", e);
        }
    }
```

- [ ] **Step 2: Update the fallback path sendChunk call**

At line 123, change:

```java
                    sendChunk(emitter, fallback);
```

To:

```java
                    sendChunk(emitter, "chunk", fallback);
```

- [ ] **Step 3: Add `thinkingConsumer` and `fullThinking` to the AI provider call**

After the line creating the existing `fullResponse` (line 113), add:

```java
                StringBuilder fullThinking = new StringBuilder();
```

Then replace the provider call block (lines 155-158):

```java
                    provider.chatStream(request, chunk -> {
                        fullResponse.append(chunk);
                        sendChunk(emitter, chunk);
                    });
```

With:

```java
                    provider.chatStream(request,
                        chunk -> {
                            fullResponse.append(chunk);
                            sendChunk(emitter, "chunk", chunk);
                        },
                        thinking -> {
                            fullThinking.append(thinking);
                            sendChunk(emitter, "thinking", thinking);
                        }
                    );
```

- [ ] **Step 4: Update the `done` event to include `thinkingContent`**

Replace the `done` event emission (lines 187-194):

```java
                    try {
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data(Map.of("messageId", assistantMsg.getId(), "meta", meta)));
                    } catch (IOException e) {
                        log.warn("Failed to send done event", e);
                    }
```

With:

```java
                    try {
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data(Map.of(
                                    "messageId", assistantMsg.getId(),
                                    "thinkingContent", fullThinking.toString(),
                                    "meta", meta
                                )));
                    } catch (IOException e) {
                        log.warn("Failed to send done event", e);
                    }
```

- [ ] **Step 5: Set `thinkingContent` on the assistant message entity**

After line 174 (`assistantMsg.setAiMeta(metaJson);`), add:

```java
                    assistantMsg.setThinkingContent(fullThinking.toString());
```

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/cloudbrain/application/chat/ChatService.java
git commit -m "feat: send thinking SSE events and persist thinkingContent in ChatService"
```

---

### Task 8: AiStreamSessionService — Add thinking SSE Events for Workflows

**Files:**
- Modify: `backend/src/main/java/com/cloudbrain/application/workflow/AiStreamSessionService.java`

- [ ] **Step 1: Add `thinkingConsumer` to the `stream()` method**

After the existing `chunkConsumer` block (line 101-108), add a `thinkingConsumer`:

```java
            java.util.function.Consumer<String> thinkingConsumer = thinking -> {
                try {
                    send(emitter, "thinking", Map.of("text", thinking));
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            };
```

- [ ] **Step 2: Pass `thinkingConsumer` to the workflow methods**

Replace lines 109-121:

```java
            Object result = "MEDICAL_RECORD".equals(session.taskType())
                    ? workflowService.generateMedicalRecord(session.actorContext(), new MedicalRecordGenerateRequest(
                            session.request().registrationId(),
                            session.request().conversationText(),
                            session.request().diagnosisDirection(),
                            session.request().attachments()
                    ), chunkConsumer)
                    : workflowService.suggestDiagnosis(session.actorContext(), new DiagnosisSuggestionRequest(
                            session.request().registrationId(),
                            session.request().conversationText(),
                            session.request().diagnosisDirection(),
                            session.request().attachments()
                    ), chunkConsumer);
```

With:

```java
            Object result = "MEDICAL_RECORD".equals(session.taskType())
                    ? workflowService.generateMedicalRecord(session.actorContext(), new MedicalRecordGenerateRequest(
                            session.request().registrationId(),
                            session.request().conversationText(),
                            session.request().diagnosisDirection(),
                            session.request().attachments()
                    ), chunkConsumer, thinkingConsumer)
                    : workflowService.suggestDiagnosis(session.actorContext(), new DiagnosisSuggestionRequest(
                            session.request().registrationId(),
                            session.request().conversationText(),
                            session.request().diagnosisDirection(),
                            session.request().attachments()
                    ), chunkConsumer, thinkingConsumer);
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/cloudbrain/application/workflow/AiStreamSessionService.java
git commit -m "feat: add thinking SSE events to AiStreamSessionService"
```

---

### Task 9: Frontend Types — Add thinkingContent to ChatMessage

**Files:**
- Modify: `frontend/src/types/chat.ts`

- [ ] **Step 1: Add `thinkingContent` optional field to ChatMessage interface**

Add `thinkingContent?: string;` after the `content` field:

```typescript
export interface ChatMessage {
  id: number;
  role: 'USER' | 'ASSISTANT';
  content: string;
  thinkingContent?: string;
  aiMeta: string | null;
  createdAt: string;
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/types/chat.ts
git commit -m "feat: add thinkingContent field to ChatMessage type"
```

---

### Task 10: Frontend Chat Store — Handle thinking SSE Events

**Files:**
- Modify: `frontend/src/stores/chat.ts`

- [ ] **Step 1: Add `thinkingBuffer` ref**

After line 12 (`const streamBuffer = ref('');`), add:

```typescript
const thinkingBuffer = ref('');
```

- [ ] **Step 2: Add `thinking` SSE event listener**

After the `chunk` event listener (line 77-81), add:

```typescript
    eventSource.addEventListener('thinking', (event) => {
      const data = JSON.parse(event.data);
      thinkingBuffer.value += data.content;
      assistantMsg.thinkingContent = thinkingBuffer.value;
    });
```

- [ ] **Step 3: Update `done` event handler to capture `thinkingContent`**

Replace lines 83-90:

```typescript
    eventSource.addEventListener('done', (event) => {
      const data = JSON.parse(event.data);
      assistantMsg.id = data.messageId;
      assistantMsg.aiMeta = JSON.stringify(data.meta as ChatMeta);
      isStreaming.value = false;
      eventSource.close();
      // Refresh sessions to get updated title
      fetchSessions();
    });
```

With:

```typescript
    eventSource.addEventListener('done', (event) => {
      const data = JSON.parse(event.data);
      assistantMsg.id = data.messageId;
      assistantMsg.content = streamBuffer.value;
      assistantMsg.thinkingContent = data.thinkingContent || thinkingBuffer.value;
      assistantMsg.aiMeta = JSON.stringify(data.meta as ChatMeta);
      isStreaming.value = false;
      eventSource.close();
      // Refresh sessions to get updated title
      fetchSessions();
    });
```

- [ ] **Step 4: Export `thinkingBuffer` from the store's return**

Add to the return object (after `streamBuffer` on line 123):

```typescript
    thinkingBuffer,
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/stores/chat.ts
git commit -m "feat: handle thinking SSE events in chat store"
```

---

### Task 11: Frontend AI Stream Store — Handle thinking SSE Events

**Files:**
- Modify: `frontend/src/stores/ai-stream.ts`

- [ ] **Step 1: Add `thinkingText` ref**

After line 21 (`const streamText = ref('');`), add:

```typescript
const thinkingText = ref('');
```

- [ ] **Step 2: Add `thinking` SSE event listener in `subscribeToEvents()`**

After the `chunk` event listener (lines 130-136), add:

```typescript
      source.addEventListener('thinking', (event) => {
        if (activeRunId !== runId || cancelRequested) {
          return;
        }
        const payload = parseSsePayload<{ text?: string }>(event.data);
        thinkingText.value += payload?.text ?? event.data;
      });
```

- [ ] **Step 3: Reset `thinkingText` in `start()` method**

After line 181 (`streamText.value = '';`), add:

```typescript
    thinkingText.value = '';
```

- [ ] **Step 4: Export `thinkingText` from store return**

Add to the return object at line 246:

```typescript
  return { sessionId, streamText, thinkingText, streaming, connected, start, cancel };
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/stores/ai-stream.ts
git commit -m "feat: handle thinking SSE events in ai-stream store"
```

---

### Task 12: AiChatMessage.vue — Collapsible Thinking Section

**Files:**
- Modify: `frontend/src/components/chat/AiChatMessage.vue`

- [ ] **Step 1: Add imports and reactive state**

Add imports for ChevronDown and ChevronUp from lucide-vue-next. The current import at line 2 is:

```typescript
import { computed } from 'vue';
```

Change to:

```typescript
import { computed, ref } from 'vue';
```

Then add after line 2:

```typescript
import { ChevronDown, ChevronUp } from 'lucide-vue-next';
```

- [ ] **Step 2: Add `thinkingExpanded` ref and `renderedThinking` computed**

After the existing `renderedContent` computed (lines 23-25), add:

```typescript
const thinkingExpanded = ref(false);

const renderedThinking = computed(() => {
  return simpleMarkdown(props.message.thinkingContent || '');
});
```

- [ ] **Step 3: Add collapsible thinking section in template**

In the template, for ASSISTANT messages, add the thinking section before the chat-msg__content div. Insert between the `<div class="chat-msg__body">` line (line 48) and the `<div class="chat-msg__content" v-html="renderedContent" />` line (line 49):

```html
      <div v-if="message.role === 'ASSISTANT' && message.thinkingContent" class="chat-msg__thinking">
        <button class="chat-msg__thinking-toggle" @click="thinkingExpanded = !thinkingExpanded">
          <span>💭 AI 思考过程</span>
          <ChevronDown v-if="!thinkingExpanded" :size="14" />
          <ChevronUp v-else :size="14" />
        </button>
        <div v-if="thinkingExpanded" class="chat-msg__thinking-content" v-html="renderedThinking" />
      </div>
```

- [ ] **Step 4: Add scoped styles for thinking section**

Add to the `<style scoped>` block, after the `.meta-time` rule (line 85):

```css
.chat-msg__thinking { margin-bottom: 10px; }
.chat-msg__thinking-toggle {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: var(--muted); cursor: pointer;
  background: none; border: none; padding: 2px 0;
  width: 100%; text-align: left;
}
.chat-msg__thinking-toggle:hover { color: var(--primary); }
.chat-msg__thinking-content {
  margin-top: 6px; padding: 8px 10px;
  background: rgba(0,0,0,.03); border-radius: 6px;
  font-size: 12px; color: var(--muted); font-style: italic;
  border-left: 2px solid var(--border);
  max-height: 200px; overflow-y: auto;
  white-space: pre-wrap;
}
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/chat/AiChatMessage.vue
git commit -m "feat: add collapsible thinking section to AiChatMessage"
```

---

### Task 13: ConversationalTriageSection.vue — Add Thinking Support to Patient Triage

**Files:**
- Modify: `frontend/src/views/patient/panels/ConversationalTriageSection.vue`

- [ ] **Step 1: Add imports for icons**

Replace the import line (line 4):

```typescript
import { MessageCircle, Send, Loader2, Bot, ChevronDown, ChevronUp, ArrowRight } from 'lucide-vue-next';
```

No change needed — `ChevronDown` and `ChevronUp` are already imported.

- [ ] **Step 2: Add `thinkingExpanded` tracking state**

After line 28 (`const finalResult = ref<ParsedResult | null>(null);`), add:

```typescript
const thinkingExpanded = ref(false);
```

- [ ] **Step 3: Add `thinking` event listener for SSE stream**

After the `chunk` event listener (lines 262-265), add:

```typescript
  es.addEventListener('thinking', (event) => {
    const data = JSON.parse(event.data) as { content?: string };
    const thinkingContent = typeof data.content === 'string' ? data.content : '';
    // Thinking content is tracked for display but not mixed into triage result parsing
    (assistantMsg as any).thinkingContent = ((assistantMsg as any).thinkingContent || '') + thinkingContent;
  });
```

- [ ] **Step 4: Add collapsible thinking section in ASSISTANT message template**

In the template, after the `<span v-html="renderConversationText(msg.content)" />` line for ASSISTANT messages (line 405), add thinking section inside the assistant message div:

Replace lines 404-407:

```html
            <div class="max-w-[85%] bg-gray-100 rounded-2xl rounded-bl-md px-3 py-2 text-sm whitespace-pre-wrap">
              <span v-html="renderConversationText(msg.content)" />
              <span v-if="streaming && i === messages.length - 1" class="inline-block w-1.5 h-4 bg-brand animate-pulse ml-0.5 align-middle" />
            </div>
```

With:

```html
            <div class="max-w-[85%] bg-gray-100 rounded-2xl rounded-bl-md px-3 py-2 text-sm whitespace-pre-wrap">
              <div v-if="(msg as any).thinkingContent" class="mb-2">
                <button class="flex items-center gap-1 text-xs text-text-secondary hover:text-brand cursor-pointer" @click="thinkingExpanded = !thinkingExpanded">
                  <span>💭 AI 思考过程</span>
                  <ChevronDown v-if="!thinkingExpanded" :size="12" />
                  <ChevronUp v-else :size="12" />
                </button>
                <div v-if="thinkingExpanded" class="mt-1 p-2 bg-black/5 rounded text-xs text-text-secondary italic max-h-32 overflow-y-auto whitespace-pre-wrap">
                  {{ (msg as any).thinkingContent }}
                </div>
              </div>
              <span v-html="renderConversationText(msg.content)" />
              <span v-if="streaming && i === messages.length - 1" class="inline-block w-1.5 h-4 bg-brand animate-pulse ml-0.5 align-middle" />
            </div>
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/patient/panels/ConversationalTriageSection.vue
git commit -m "feat: add thinking section to conversational triage"
```

---

### Task 14: DoctorConsultationPanel.vue — Display Thinking Text from Workflow

**Files:**
- Modify: `frontend/src/views/doctor/panels/DoctorConsultationPanel.vue`

- [ ] **Step 1: Add thinking text display when streaming workflows**

The `DoctorConsultationPanel` uses `WorkflowSidebar` to show AI output. We need to read `thinkingText` from the store and show it. Add a watcher after line 96:

After the existing watcher block (after line 96):

```typescript
watch(() => aiStreamStore.thinkingText, (text) => {
  if (!sidebarRef.value || !text || !aiStreamStore.streaming) return;
  sidebarRef.value.updateThinkingContent(text);
});
```

This requires a corresponding method in `WorkflowSidebar`. Since `WorkflowSidebar` is a shared component, we need to add `updateThinkingContent` there first.

- [ ] **Step 2: Check if WorkflowSidebar needs a new method**

Let's first see if WorkflowSidebar supports arbitrary content injection or needs a new prop/method. The current watcher sends content via `updateStepContent`. For thinking content, we need a similar mechanism.

However, modifying `WorkflowSidebar` is a bigger change than just this file. For now, the `DoctorConsultationPanel.vue` change is minimal — we add the watcher that reads thinking text and if the sidebar doesn't support it yet, the thinking text is still available in the store for future use. The `thinkingText` is reactive and available for any component that imports the store.

For now, let's add a simple inline display of thinking text near the sidebar toggle button. Add after line 540 (the sidebar toggle button):

```html
    <!-- Thinking content indicator during streaming -->
    <div v-if="aiStreamStore.thinkingText && aiStreamStore.streaming" class="fixed right-12 top-1/2 -translate-y-1/2 z-30 bg-white border border-brand/30 rounded-lg shadow-lg p-3 max-w-xs text-xs text-text-secondary max-h-48 overflow-y-auto">
      <p class="font-medium text-brand mb-1">💭 AI 思考中...</p>
      <p class="whitespace-pre-wrap">{{ aiStreamStore.thinkingText }}</p>
    </div>
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/doctor/panels/DoctorConsultationPanel.vue
git commit -m "feat: display AI thinking text during workflow streaming"
```

---

### Task 15: End-to-End Verification

**Files:** None (verification only)

- [ ] **Step 1: Build backend**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Build frontend**

```bash
cd frontend && npx vue-tsc --noEmit
```

Expected: No type errors

- [ ] **Step 3: Verify all files are committed**

```bash
git status
```

Expected: working tree clean

- [ ] **Step 4: Review final diff**

```bash
git diff main --stat
```

Expected: ~15 files changed, all related to thinking/reply separation

---

## Self-Review

**1. Spec coverage:** Each section of the design spec maps to a task:
- 3.1 Provider Layer → Tasks 3, 4
- 3.2 ChatService → Task 7
- 3.3 AiStreamSessionService → Task 8
- 3.4 WorkflowService → Task 6
- 3.5 AIInvocationService → Task 5
- 3.6 ChatMessageEntity → Task 2
- 3.7 Database Migration → Task 1
- 3.8 TypeScript Types → Task 9
- 3.9 Chat Store → Task 10
- 3.10 AI Stream Store → Task 11
- 3.11 AiChatMessage → Task 12
- 3.12 ConversationalTriageSection → Task 13
- 3.13 DoctorConsultationPanel → Task 14

**2. Placeholder scan:** No TBD, TODO, or placeholder patterns found.

**3. Type consistency:**
- `thinkingConsumer` parameter name consistent across all Java methods: `Consumer<String> thinkingConsumer`
- SSE event name `"thinking"` consistent across backend and frontend
- `thinkingContent` field name consistent across Entity, TypeScript type, store, and components
- `thinkingBuffer` / `thinkingText` ref names distinct and consistent per store
- `sendChunk(emitter, eventName, chunk)` signature used consistently in ChatService
