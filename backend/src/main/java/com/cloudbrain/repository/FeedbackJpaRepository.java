package com.cloudbrain.repository;

import com.cloudbrain.entity.core.FeedbackEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackJpaRepository extends JpaRepository<FeedbackEntity, Long> {

    Optional<FeedbackEntity> findByRegistrationId(Long registrationId);

    List<FeedbackEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    long countByTriageAccurate(Boolean triageAccurate);
}
