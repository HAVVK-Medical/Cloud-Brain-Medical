-- V12__disable_doubao_chat_without_key.sql
-- Doubao CHAT config has been removed from V10 seed data (DeepSeek handles ALL scopes via V11).
-- This migration cleans up any existing doubao rows from earlier deployments.
DELETE FROM ai_config
WHERE provider = 'doubao'
  AND task_scope = 'CHAT';
