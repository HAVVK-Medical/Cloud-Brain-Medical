CREATE TABLE department (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_department_code UNIQUE (code)
);

CREATE TABLE patient (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(32),
    name VARCHAR(64) NOT NULL,
    gender VARCHAR(32),
    birth_date DATE,
    age INT,
    allergy_history TEXT,
    medical_history TEXT,
    id_card_number VARCHAR(64),
    remark TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_patient_username UNIQUE (username),
    CONSTRAINT uk_patient_phone UNIQUE (phone)
);

CREATE TABLE doctor (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(64) NOT NULL,
    department_id BIGINT,
    title VARCHAR(64),
    specialty VARCHAR(128),
    introduction TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_doctor_username UNIQUE (username),
    CONSTRAINT fk_doctor_department FOREIGN KEY (department_id) REFERENCES department (id)
);

CREATE TABLE admin (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_admin_username UNIQUE (username)
);

CREATE TABLE schedule (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    period VARCHAR(32) NOT NULL,
    total_slots INT NOT NULL,
    remaining_slots INT NOT NULL,
    visit_level VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_schedule_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT fk_schedule_department FOREIGN KEY (department_id) REFERENCES department (id)
);

CREATE INDEX idx_schedule_work_date ON schedule (work_date);

CREATE TABLE triage_record (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    chief_complaint TEXT NOT NULL,
    recommended_dept VARCHAR(128),
    recommended_doctors TEXT,
    call_status VARCHAR(32) NOT NULL,
    recommendation_source VARCHAR(64),
    ai_call_record_id BIGINT,
    registration_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_triage_patient FOREIGN KEY (patient_id) REFERENCES patient (id)
);

CREATE TABLE registration (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    triage_record_id BIGINT,
    status VARCHAR(32) NOT NULL,
    department_snapshot VARCHAR(128),
    doctor_snapshot VARCHAR(255),
    visit_level_snapshot VARCHAR(64),
    consultation_start_time TIMESTAMP,
    record_confirmed_time TIMESTAMP,
    prescription_submitted_time TIMESTAMP,
    completed_time TIMESTAMP,
    cancelled_time TIMESTAMP,
    cancel_reason VARCHAR(255),
    slot_released BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_registration_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_registration_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT fk_registration_department FOREIGN KEY (department_id) REFERENCES department (id),
    CONSTRAINT fk_registration_schedule FOREIGN KEY (schedule_id) REFERENCES schedule (id),
    CONSTRAINT uk_registration_patient_schedule UNIQUE (patient_id, schedule_id)
);

CREATE TABLE consultation_note (
    id BIGSERIAL PRIMARY KEY,
    registration_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    conversation_text TEXT,
    chief_complaint_summary TEXT,
    diagnosis_direction TEXT,
    patient_context TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_consultation_note_registration FOREIGN KEY (registration_id) REFERENCES registration (id)
);

CREATE TABLE medical_record (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    registration_id BIGINT NOT NULL,
    chief_complaint TEXT,
    present_illness TEXT,
    past_history TEXT,
    physical_exam TEXT,
    preliminary_diagnosis TEXT,
    treatment_plan TEXT,
    conversation_text TEXT,
    ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    ai_call_record_id BIGINT,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_medical_record_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_medical_record_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT fk_medical_record_registration FOREIGN KEY (registration_id) REFERENCES registration (id)
);

CREATE TABLE drug (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    pinyin_code VARCHAR(128),
    specification VARCHAR(255),
    dosage_form VARCHAR(64),
    contraindications TEXT,
    interaction_summary TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_drug_code UNIQUE (code)
);

CREATE INDEX idx_drug_name ON drug (name);
CREATE INDEX idx_drug_pinyin_code ON drug (pinyin_code);

CREATE TABLE prescription (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    registration_id BIGINT NOT NULL,
    review_id BIGINT,
    risk_level VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prescription_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_prescription_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT fk_prescription_registration FOREIGN KEY (registration_id) REFERENCES registration (id)
);

CREATE TABLE prescription_item (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    drug_id BIGINT,
    drug_name VARCHAR(128) NOT NULL,
    specification VARCHAR(255),
    dosage_form VARCHAR(64),
    package_unit VARCHAR(32),
    manufacturer VARCHAR(255),
    unit_price DECIMAL(12, 2),
    default_usage VARCHAR(128),
    dosage VARCHAR(64),
    frequency VARCHAR(64),
    duration VARCHAR(64),
    quantity VARCHAR(64),
    usage_instruction TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prescription_item_prescription FOREIGN KEY (prescription_id) REFERENCES prescription (id),
    CONSTRAINT fk_prescription_item_drug FOREIGN KEY (drug_id) REFERENCES drug (id)
);

CREATE TABLE ai_call_record (
    id BIGSERIAL PRIMARY KEY,
    task_type VARCHAR(64) NOT NULL,
    business_record_id BIGINT,
    operator_id BIGINT,
    operator_role VARCHAR(32),
    provider VARCHAR(64),
    model_name VARCHAR(128),
    config_version INT,
    prompt_version INT,
    request_id VARCHAR(64),
    input_summary TEXT,
    output_summary TEXT,
    call_status VARCHAR(32) NOT NULL,
    error_summary TEXT,
    duration_ms BIGINT,
    trace_id VARCHAR(64),
    degraded BOOLEAN NOT NULL DEFAULT FALSE,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_call_task_status ON ai_call_record (task_type, call_status);
CREATE INDEX idx_ai_call_business_record ON ai_call_record (business_record_id);

CREATE TABLE prescription_review (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    registration_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    risk_level VARCHAR(32),
    local_rule_hits TEXT,
    rule_engine_status VARCHAR(32),
    context_missing_items TEXT,
    llm_suggestion TEXT,
    llm_summary TEXT,
    llm_call_status VARCHAR(32),
    ai_call_record_id BIGINT,
    prescription_snapshot_hash VARCHAR(128),
    review_context_hash VARCHAR(128),
    manual_confirmation TEXT,
    bind_status VARCHAR(32),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prescription_review_prescription FOREIGN KEY (prescription_id) REFERENCES prescription (id),
    CONSTRAINT fk_prescription_review_ai_call FOREIGN KEY (ai_call_record_id) REFERENCES ai_call_record (id)
);

CREATE TABLE prescription_rule_definition (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(128) NOT NULL,
    rule_type VARCHAR(64) NOT NULL,
    applicable_drugs TEXT,
    condition_expression TEXT,
    risk_level VARCHAR(32),
    alert_message TEXT,
    suggestion TEXT,
    basis TEXT,
    seeded BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    validation_status VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_prescription_rule_definition_rule_code UNIQUE (rule_code)
);

CREATE TABLE ai_config (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(64) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    api_url VARCHAR(255),
    api_key_encrypted TEXT,
    key_version VARCHAR(64),
    task_scope VARCHAR(64) NOT NULL,
    timeout_seconds INT NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    health_status VARCHAR(32),
    config_version INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_config_task_scope ON ai_config (task_scope);

CREATE TABLE feedback (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    registration_id BIGINT NOT NULL,
    rating INT NOT NULL,
    triage_accurate BOOLEAN,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_feedback_registration UNIQUE (registration_id),
    CONSTRAINT fk_feedback_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_feedback_registration FOREIGN KEY (registration_id) REFERENCES registration (id)
);

CREATE TABLE triage_accuracy_feedback (
    id BIGSERIAL PRIMARY KEY,
    feedback_id BIGINT NOT NULL,
    recommended_dept_snapshot VARCHAR(128),
    actual_dept_snapshot VARCHAR(128),
    accuracy_label VARCHAR(32),
    reason_tags TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_triage_accuracy_feedback_feedback FOREIGN KEY (feedback_id) REFERENCES feedback (id)
);

CREATE TABLE diagnosis_suggestion_record (
    id BIGSERIAL PRIMARY KEY,
    registration_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    consultation_note_id BIGINT,
    suggested_diagnoses TEXT,
    suggested_exam_items TEXT,
    adoption_status VARCHAR(32),
    final_diagnosis_direction VARCHAR(255),
    adoption_doctor_id BIGINT,
    adoption_time TIMESTAMP,
    ai_call_record_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_diagnosis_suggestion_record_registration FOREIGN KEY (registration_id) REFERENCES registration (id),
    CONSTRAINT fk_diagnosis_suggestion_record_ai_call FOREIGN KEY (ai_call_record_id) REFERENCES ai_call_record (id)
);

CREATE TABLE notification_record (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    recipient_role VARCHAR(32) NOT NULL,
    alert_type VARCHAR(64) NOT NULL,
    statistics_bucket VARCHAR(64),
    display_level VARCHAR(32),
    business_record_id BIGINT,
    patient_summary TEXT,
    risk_summary TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_recipient_read ON notification_record (recipient_id, is_read);

CREATE TABLE prompt_template (
    id BIGSERIAL PRIMARY KEY,
    template_code VARCHAR(128) NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    dept_code VARCHAR(64),
    template_body TEXT,
    variable_whitelist TEXT,
    version INT NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_prompt_template_template_code UNIQUE (template_code)
);

CREATE TABLE his_sync_record (
    id BIGSERIAL PRIMARY KEY,
    sync_type VARCHAR(64) NOT NULL,
    idempotent_key VARCHAR(128) NOT NULL,
    external_id VARCHAR(128),
    sync_status VARCHAR(32) NOT NULL,
    request_summary TEXT,
    response_summary TEXT,
    error_summary TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_his_sync_record_idempotent_key UNIQUE (idempotent_key)
);

