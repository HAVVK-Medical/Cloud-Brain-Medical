package com.cloudbrain.controller;

import com.cloudbrain.application.chat.ChatService;
import com.cloudbrain.common.Result;
import com.cloudbrain.entity.chat.ChatMessageEntity;
import com.cloudbrain.entity.chat.ChatSessionEntity;
import com.cloudbrain.security.ActorContext;
import com.cloudbrain.security.ActorContextResolver;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/sessions")
    public Result<List<ChatSessionEntity>> listSessions() {
        ActorContext ctx = ActorContextResolver.requireCurrent();
        List<ChatSessionEntity> sessions = chatService.listSessions(ctx.userId(), ctx.role().name());
        return Result.success(sessions);
    }

    @PostMapping("/sessions")
    public Result<Map<String, Object>> createSession(@RequestBody Map<String, String> body) {
        ActorContext ctx = ActorContextResolver.requireCurrent();
        String firstMessage = body.getOrDefault("firstMessage", "");
        ChatSessionEntity session = chatService.createSession(ctx.userId(), ctx.role().name(), firstMessage);
        return Result.success(Map.of("id", session.getId(), "title", session.getTitle() != null ? session.getTitle() : ""));
    }

    @GetMapping("/sessions/{id}/messages")
    public Result<List<ChatMessageEntity>> getMessages(@PathVariable Long id) {
        ActorContext ctx = ActorContextResolver.requireCurrent();
        List<ChatMessageEntity> messages = chatService.getMessages(id, ctx.userId());
        return Result.success(messages);
    }

    @GetMapping("/stream")
    public SseEmitter streamChat(@RequestParam Long sessionId, @RequestParam String message) {
        ActorContext ctx = ActorContextResolver.requireCurrent();
        return chatService.streamChat(sessionId, ctx.userId(), message, ctx.role().name());
    }

    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        ActorContext ctx = ActorContextResolver.requireCurrent();
        chatService.deleteSession(id, ctx.userId());
        return Result.success(null);
    }
}
