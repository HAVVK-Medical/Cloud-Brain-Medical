package com.cloudbrain.repository;

import com.cloudbrain.entity.core.TriageAccuracyFeedbackEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TriageAccuracyFeedbackJpaRepository extends JpaRepository<TriageAccuracyFeedbackEntity, Long> {

    Optional<TriageAccuracyFeedbackEntity> findByFeedbackId(Long feedbackId);
}
