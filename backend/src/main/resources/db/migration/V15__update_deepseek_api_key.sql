-- V15__update_deepseek_api_key.sql
-- Update DeepSeek API key while preserving all other config fields

UPDATE ai_config
SET api_key_encrypted = '8OizW1PqoBzR7940cD+mnXfExQboeoQDP5k24MrenhbN6Q9asLG0QggX6tOiqW3AGFx0ZqIxn1ILAuXaD7AQ',
    health_status = 'UNKNOWN',
    updated_at = CURRENT_TIMESTAMP
WHERE provider = 'DEEPSEEK'
  AND model_name = 'deepseek-v4-flash';
