ALTER TABLE ai_config
    ALTER COLUMN config_version TYPE VARCHAR(64);

ALTER TABLE ai_call_record
    ALTER COLUMN config_version TYPE VARCHAR(64);

ALTER TABLE ai_call_record
    ALTER COLUMN prompt_version TYPE VARCHAR(64);

ALTER TABLE prescription_item
    ALTER COLUMN dosage TYPE DECIMAL(12, 2);

ALTER TABLE prescription_item
    ALTER COLUMN quantity TYPE INT;

ALTER TABLE prescription_item
    ALTER COLUMN default_usage TYPE VARCHAR(128);

