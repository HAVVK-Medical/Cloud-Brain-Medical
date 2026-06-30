-- V15__update_deepseek_api_key.sql
-- Keep DeepSeek config present but do not seed credentials

UPDATE ai_config
SET api_key_encrypted = NULL,
    key_version = NULL,
    health_status = 'UNKNOWN',
    updated_at = CURRENT_TIMESTAMP
WHERE provider = 'DEEPSEEK'
  AND model_name = 'deepseek-v4-flash';
