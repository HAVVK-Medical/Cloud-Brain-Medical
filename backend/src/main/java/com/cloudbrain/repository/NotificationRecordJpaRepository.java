package com.cloudbrain.repository;

import com.cloudbrain.entity.core.NotificationRecordEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRecordJpaRepository extends JpaRepository<NotificationRecordEntity, Long> {

    List<NotificationRecordEntity> findByReadFalseOrderByCreatedAtDesc();

    List<NotificationRecordEntity> findByRecipientIdAndRecipientRoleAndReadFalseOrderByCreatedAtDesc(Long recipientId, String recipientRole);

    Optional<NotificationRecordEntity> findFirstByRecipientIdAndRecipientRoleAndAlertTypeAndBusinessRecordIdAndReadFalseOrderByCreatedAtDesc(
            Long recipientId,
            String recipientRole,
            String alertType,
            Long businessRecordId
    );
}
