-- V11__add_deepseek_config.sql
-- Add DeepSeek v4 Flash as AI provider with API key

INSERT INTO ai_config (provider, model_name, api_url, api_key_encrypted, key_version, task_scope, timeout_seconds, is_default, health_status, config_version, enabled, priority, status, created_at, updated_at)
SELECT 'DEEPSEEK', 'deepseek-v4-flash', NULL, 'lg1HP1iqRS3hnpFmG6yNJUJDcCNwKoGiHlQQDRkp1jUpJ5b/sYpa/gP/zci/buPigFuQ4sDPJbXWqLG/XA4uWA==', 'v1', 'ALL', 60, TRUE, 'UNKNOWN', '1.0.0', TRUE, 5, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_config WHERE provider = 'DEEPSEEK' AND model_name = 'deepseek-v4-flash');
