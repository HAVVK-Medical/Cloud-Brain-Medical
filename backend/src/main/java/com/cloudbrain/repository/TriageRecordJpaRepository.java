package com.cloudbrain.repository;

import com.cloudbrain.entity.core.TriageRecordEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TriageRecordJpaRepository extends JpaRepository<TriageRecordEntity, Long> {

    List<TriageRecordEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<TriageRecordEntity> findAllByOrderByCreatedAtDesc();

    Optional<TriageRecordEntity> findByRegistrationId(Long registrationId);

    List<TriageRecordEntity> findByPatientIdAndRegistrationIdIsNullOrderByCreatedAtDesc(Long patientId);

    Optional<TriageRecordEntity> findByAiCallRecordId(Long aiCallRecordId);

    List<TriageRecordEntity> findByCallStatusOrderByCreatedAtDesc(String callStatus);
}
