ALTER TABLE department
    ADD COLUMN description TEXT;

ALTER TABLE triage_record
    ADD COLUMN ai_response_raw TEXT;

ALTER TABLE triage_record
    ADD CONSTRAINT fk_triage_record_registration FOREIGN KEY (registration_id) REFERENCES registration (id);

ALTER TABLE registration
    ADD COLUMN registration_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE registration
    ADD CONSTRAINT fk_registration_triage_record FOREIGN KEY (triage_record_id) REFERENCES triage_record (id);

ALTER TABLE consultation_note
    ADD CONSTRAINT fk_consultation_note_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id);

ALTER TABLE medical_record
    ADD CONSTRAINT fk_medical_record_ai_call FOREIGN KEY (ai_call_record_id) REFERENCES ai_call_record (id);

ALTER TABLE drug
    ADD COLUMN package_unit VARCHAR(32);

ALTER TABLE drug
    ADD COLUMN manufacturer VARCHAR(255);

ALTER TABLE drug
    ADD COLUMN unit_price DECIMAL(12, 2);

ALTER TABLE drug
    ADD COLUMN default_usage TEXT;

ALTER TABLE drug
    ADD COLUMN precautions TEXT;

ALTER TABLE drug
    ADD COLUMN indications TEXT;

ALTER TABLE prescription
    ADD CONSTRAINT fk_prescription_review FOREIGN KEY (review_id) REFERENCES prescription_review (id);

ALTER TABLE prescription_review
    ALTER COLUMN prescription_id DROP NOT NULL;

ALTER TABLE prescription_rule_definition
    ADD COLUMN applicable_diseases VARCHAR(255);

ALTER TABLE prescription_rule_definition
    ADD COLUMN applicable_populations VARCHAR(64);

ALTER TABLE diagnosis_suggestion_record
    ADD CONSTRAINT fk_diagnosis_suggestion_record_patient FOREIGN KEY (patient_id) REFERENCES patient (id);

ALTER TABLE diagnosis_suggestion_record
    ADD CONSTRAINT fk_diagnosis_suggestion_record_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id);

ALTER TABLE diagnosis_suggestion_record
    ADD CONSTRAINT fk_diagnosis_suggestion_record_consultation_note FOREIGN KEY (consultation_note_id) REFERENCES consultation_note (id);

ALTER TABLE diagnosis_suggestion_record
    ADD CONSTRAINT fk_diagnosis_suggestion_record_adoption_doctor FOREIGN KEY (adoption_doctor_id) REFERENCES doctor (id);

CREATE INDEX idx_triage_record_patient ON triage_record (patient_id);
CREATE INDEX idx_triage_record_registration ON triage_record (registration_id);
CREATE INDEX idx_registration_patient ON registration (patient_id);
CREATE INDEX idx_registration_doctor ON registration (doctor_id);
CREATE INDEX idx_registration_schedule ON registration (schedule_id);
CREATE INDEX idx_medical_record_patient ON medical_record (patient_id);
CREATE INDEX idx_medical_record_registration ON medical_record (registration_id);
CREATE INDEX idx_prescription_review_patient ON prescription_review (patient_id);
CREATE INDEX idx_prescription_review_doctor ON prescription_review (doctor_id);
CREATE INDEX idx_prescription_review_registration ON prescription_review (registration_id);
CREATE INDEX idx_prescription_review_ref ON prescription (review_id);
CREATE INDEX idx_diagnosis_suggestion_registration ON diagnosis_suggestion_record (registration_id);
CREATE INDEX idx_diagnosis_suggestion_ai_call ON diagnosis_suggestion_record (ai_call_record_id);
CREATE INDEX idx_ai_call_trace_id ON ai_call_record (trace_id);
CREATE INDEX idx_ai_call_request_id ON ai_call_record (request_id);

