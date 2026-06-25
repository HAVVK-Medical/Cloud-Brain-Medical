package com.cloudbrain.repository;

import com.cloudbrain.entity.core.RegistrationEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistrationJpaRepository extends JpaRepository<RegistrationEntity, Long> {

    List<RegistrationEntity> findByPatientIdOrderByRegistrationTimeDesc(Long patientId);

    List<RegistrationEntity> findByDoctorIdAndStatusOrderByRegistrationTimeAsc(Long doctorId, String status);

    List<RegistrationEntity> findByDoctorIdAndStatusInOrderByRegistrationTimeAsc(Long doctorId, Collection<String> statuses);

    List<RegistrationEntity> findByDoctorIdOrderByRegistrationTimeDesc(Long doctorId);

    Optional<RegistrationEntity> findFirstByPatientIdAndScheduleIdAndStatusNotOrderByRegistrationTimeDesc(
            Long patientId,
            Long scheduleId,
            String excludedStatus
    );

    boolean existsByPatientIdAndScheduleIdAndStatusNot(Long patientId, Long scheduleId, String excludedStatus);

    Optional<RegistrationEntity> findByIdAndPatientId(Long id, Long patientId);

    Optional<RegistrationEntity> findByIdAndDoctorId(Long id, Long doctorId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RegistrationEntity registration
               set registration.status = :targetStatus,
                   registration.version = registration.version + 1
             where registration.id = :registrationId
               and registration.version = :version
               and registration.status = :expectedStatus
            """)
    int updateStatusWithVersion(@Param("registrationId") Long registrationId,
                                @Param("expectedStatus") String expectedStatus,
                                @Param("targetStatus") String targetStatus,
                                @Param("version") Integer version);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RegistrationEntity registration
               set registration.status = :cancelledStatus,
                   registration.cancelReason = :cancelReason,
                   registration.cancelledTime = :cancelledTime,
                   registration.slotReleased = true,
                   registration.version = registration.version + 1
             where registration.id = :registrationId
               and registration.patientId = :patientId
               and registration.status = :waitingStatus
               and registration.slotReleased = false
            """)
    int cancelWaitingRegistrationOnce(@Param("registrationId") Long registrationId,
                                      @Param("patientId") Long patientId,
                                      @Param("waitingStatus") String waitingStatus,
                                      @Param("cancelledStatus") String cancelledStatus,
                                      @Param("cancelReason") String cancelReason,
                                      @Param("cancelledTime") LocalDateTime cancelledTime);

    long countByDoctorIdAndStatusAndRegistrationTimeBetween(Long doctorId,
                                                            String status,
                                                            LocalDateTime startTime,
                                                            LocalDateTime endTime);
}
