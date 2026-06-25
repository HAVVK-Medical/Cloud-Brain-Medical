package com.cloudbrain.repository;

import com.cloudbrain.entity.core.ScheduleEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleJpaRepository extends JpaRepository<ScheduleEntity, Long> {

    List<ScheduleEntity> findByDepartmentIdAndWorkDateAndStatusAndRemainingSlotsGreaterThanOrderByPeriodAsc(
            Long departmentId,
            LocalDate workDate,
            String status,
            Integer remainingSlots
    );

    List<ScheduleEntity> findByDoctorIdAndWorkDateAndStatusOrderByPeriodAsc(
            Long doctorId,
            LocalDate workDate,
            String status
    );

    List<ScheduleEntity> findByStatusAndRemainingSlotsGreaterThanOrderByWorkDateAscPeriodAsc(
            String status,
            Integer remainingSlots
    );

    List<ScheduleEntity> findByDepartmentIdAndStatusAndRemainingSlotsGreaterThanOrderByWorkDateAscPeriodAsc(
            Long departmentId,
            String status,
            Integer remainingSlots
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ScheduleEntity schedule
               set schedule.remainingSlots = schedule.remainingSlots - 1,
                   schedule.version = schedule.version + 1
             where schedule.id = :scheduleId
               and schedule.version = :version
               and schedule.remainingSlots > 0
            """)
    int decrementSlotWithVersion(@Param("scheduleId") Long scheduleId, @Param("version") Integer version);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ScheduleEntity schedule
               set schedule.remainingSlots = schedule.remainingSlots + 1,
                   schedule.version = schedule.version + 1
             where schedule.id = :scheduleId
               and schedule.remainingSlots < schedule.totalSlots
            """)
    int releaseSlotOnce(@Param("scheduleId") Long scheduleId);
}
