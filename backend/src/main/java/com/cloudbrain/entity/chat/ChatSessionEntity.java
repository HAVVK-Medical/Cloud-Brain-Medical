package com.cloudbrain.entity.chat;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "chat_session")
public class ChatSessionEntity extends BaseAuditableEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_role", nullable = false, length = 32)
    private String userRole;

    @Column(name = "title", length = 128)
    private String title;

    protected ChatSessionEntity() {}

    public ChatSessionEntity(Long userId, String userRole, String title) {
        this.userId = Objects.requireNonNull(userId);
        this.userRole = Objects.requireNonNull(userRole);
        this.title = title;
    }

    public Long getUserId() { return userId; }
    public String getUserRole() { return userRole; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
