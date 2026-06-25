package com.cloudbrain.repository;

import com.cloudbrain.entity.auth.AuditLogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, Long> {

    List<AuditLogEntity> findTop50ByOrderByCreatedAtDesc();
}
