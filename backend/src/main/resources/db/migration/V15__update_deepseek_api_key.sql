-- V15__update_deepseek_api_key.sql
-- Update DeepSeek API key while preserving all other config fields

UPDATE ai_config
SET api_key_encrypted = '4/k6kOtIEb2Cgq43XgFqCbKJydpxpwqkh8LcHSFhKsmcAi+bM2NxuHhfktcN4VfR/JKfzBfOjXGIuE+hJSx1',
    health_status = 'UNKNOWN',
    updated_at = CURRENT_TIMESTAMP
WHERE provider = 'DEEPSEEK'
  AND model_name = 'deepseek-v4-flash';
