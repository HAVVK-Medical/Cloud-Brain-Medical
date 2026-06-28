package com.cloudbrain.repository;

import com.cloudbrain.entity.chat.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, Long> {
    List<ChatSessionEntity> findByUserIdAndUserRoleOrderByUpdatedAtDesc(Long userId, String userRole);
    void deleteByIdAndUserId(Long id, Long userId);
}
