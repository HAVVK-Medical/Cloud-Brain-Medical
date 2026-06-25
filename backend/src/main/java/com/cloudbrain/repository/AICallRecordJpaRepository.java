package com.cloudbrain.repository;

import com.cloudbrain.entity.core.AICallRecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AICallRecordJpaRepository extends JpaRepository<AICallRecordEntity, Long> {

    List<AICallRecordEntity> findByTaskTypeOrderByCreatedAtDesc(String taskType);

    List<AICallRecordEntity> findAllByOrderByCreatedAtDesc();
}
