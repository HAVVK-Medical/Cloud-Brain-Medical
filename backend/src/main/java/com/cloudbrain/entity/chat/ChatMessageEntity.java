package com.cloudbrain.entity.chat;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "chat_message")
public class ChatMessageEntity extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSessionEntity session;

    @Column(name = "role", nullable = false, length = 16)
    private String role;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "ai_meta", columnDefinition = "TEXT")
    private String aiMeta;

    protected ChatMessageEntity() {}

    public ChatMessageEntity(ChatSessionEntity session, String role, String content) {
        this.session = Objects.requireNonNull(session);
        this.role = Objects.requireNonNull(role);
        this.content = content;
    }

    public ChatSessionEntity getSession() { return session; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAiMeta() { return aiMeta; }
    public void setAiMeta(String aiMeta) { this.aiMeta = aiMeta; }
}
