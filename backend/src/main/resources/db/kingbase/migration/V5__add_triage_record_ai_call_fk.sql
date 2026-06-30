ALTER TABLE triage_record
    ADD CONSTRAINT fk_triage_record_ai_call FOREIGN KEY (ai_call_record_id) REFERENCES ai_call_record (id);

CREATE INDEX idx_triage_record_ai_call ON triage_record (ai_call_record_id);
