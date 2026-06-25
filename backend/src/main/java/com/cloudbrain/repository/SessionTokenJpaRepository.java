package com.cloudbrain.repository;

import com.cloudbrain.entity.auth.SessionTokenEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionTokenJpaRepository extends JpaRepository<SessionTokenEntity, Long> {

    Optional<SessionTokenEntity> findByTokenIdAndStatus(String tokenId, String status);

    Optional<SessionTokenEntity> findByTokenHashAndStatus(String tokenHash, String status);

    Optional<SessionTokenEntity> findByRefreshHashAndStatus(String refreshHash, String status);

    List<SessionTokenEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndStatusAndExpiresAtAfter(Long userId, String status, Instant expiresAt);
}
