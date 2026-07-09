ALTER TABLE registration
    DROP CONSTRAINT uk_registration_patient_schedule;

CREATE UNIQUE INDEX uk_registration_active_patient_schedule
    ON registration (patient_id, schedule_id)
    WHERE status <> 'CANCELLED';

CREATE UNIQUE INDEX uk_ai_config_default_enabled_scope
    ON ai_config (task_scope)
    WHERE is_default = TRUE AND enabled = TRUE;
