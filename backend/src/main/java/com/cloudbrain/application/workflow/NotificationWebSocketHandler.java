package com.cloudbrain.application.workflow;

import com.cloudbrain.dto.workflow.WorkflowDtos.NotificationRecordSummary;
import com.cloudbrain.repository.SessionTokenJpaRepository;
import com.cloudbrain.security.ActorContext;
import com.cloudbrain.security.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
    private static final String SESSION_KEY_ATTRIBUTE = "notificationSessionKey";

    private final JwtTokenUtil jwtTokenUtil;
    private final SessionTokenJpaRepository sessionTokenRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, Set<WebSocketSession>> sessionsByRecipient = new ConcurrentHashMap<>();

    public NotificationWebSocketHandler(JwtTokenUtil jwtTokenUtil,
                                        SessionTokenJpaRepository sessionTokenRepository,
                                        ObjectMapper objectMapper) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.sessionTokenRepository = sessionTokenRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Optional<ActorContext> actor = extractBearerToken(session)
                .filter(this::isActiveToken)
                .flatMap(jwtTokenUtil::parseActorContext)
                .filter(context -> context.isDoctor() || context.isAdmin());
        if (actor.isEmpty()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("invalid notification token"));
            return;
        }

        String key = sessionKey(actor.get());
        session.getAttributes().put(SESSION_KEY_ATTRIBUTE, key);
        sessionsByRecipient.computeIfAbsent(key, ignored -> new CopyOnWriteArraySet<>()).add(session);
        send(session, "connected", Map.of("role", actor.get().role().name(), "sentAt", Instant.now().toString()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.debug("notification websocket transport error: {}", exception.getMessage());
        removeSession(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    public void publish(NotificationRecordSummary notification) {
        if (notification == null || notification.recipientId() == null || notification.recipientRole() == null) {
            return;
        }

        publishToKey(notification.recipientRole().toUpperCase() + ":" + notification.recipientId(), notification);
        if ("DOCTOR".equalsIgnoreCase(notification.recipientRole())) {
            sessionsByRecipient.keySet().stream()
                    .filter(key -> key.startsWith("ADMIN:"))
                    .forEach(key -> publishToKey(key, notification));
        }
    }

    private void publishToKey(String key, NotificationRecordSummary notification) {
        Set<WebSocketSession> sessions = sessionsByRecipient.getOrDefault(key, Set.of());
        for (WebSocketSession session : sessions) {
            try {
                send(session, "notification", notification);
            } catch (IOException exception) {
                log.debug("failed to push notification {} to {}: {}", notification.id(), key, exception.getMessage());
                removeSession(session);
            }
        }
    }

    private void send(WebSocketSession session, String type, Object payload) throws IOException {
        if (!session.isOpen()) {
            return;
        }
        Map<String, Object> message = Map.of(
                "type", type,
                "payload", payload,
                "sentAt", Instant.now().toString()
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
    }

    private void removeSession(WebSocketSession session) {
        Object key = session.getAttributes().get(SESSION_KEY_ATTRIBUTE);
        if (!(key instanceof String sessionKey)) {
            return;
        }
        Set<WebSocketSession> sessions = sessionsByRecipient.get(sessionKey);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByRecipient.remove(sessionKey);
        }
    }

    private Optional<String> extractBearerToken(WebSocketSession session) {
        String authorization = session.getHandshakeHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return Optional.of(authorization.substring(7).trim()).filter(value -> !value.isBlank());
        }

        return Optional.ofNullable(session.getUri())
                .map(this::tokenFromQuery)
                .filter(value -> !value.isBlank());
    }

    private String tokenFromQuery(URI uri) {
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return "";
        }
        for (String part : query.split("&")) {
            int index = part.indexOf('=');
            String name = index < 0 ? part : part.substring(0, index);
            if ("token".equals(URLDecoder.decode(name, StandardCharsets.UTF_8))) {
                String value = index < 0 ? "" : part.substring(index + 1);
                return URLDecoder.decode(value, StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private boolean isActiveToken(String token) {
        String tokenHash = jwtTokenUtil.hashToken(token);
        return sessionTokenRepository.findByTokenHashAndStatus(tokenHash, "ACTIVE")
                .filter(session -> session.getExpiresAt() == null || session.getExpiresAt().isAfter(Instant.now()))
                .isPresent();
    }

    private String sessionKey(ActorContext actorContext) {
        if (actorContext.isAdmin()) {
            return "ADMIN:" + actorContext.userId();
        }
        return "DOCTOR:" + actorContext.getRequiredDoctorId();
    }
}
