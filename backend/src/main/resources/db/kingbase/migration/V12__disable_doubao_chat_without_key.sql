-- V12__disable_doubao_chat_without_key.sql
-- Disable the Doubao CHAT config that has no API key, so DeepSeek ALL config
-- can be selected as fallback. The AIConfigResolver code also filters out
-- configs without API keys at the application level — this migration provides
-- belt-and-suspenders safety at the data level.

UPDATE ai_config
SET enabled = FALSE
WHERE provider = 'doubao'
  AND task_scope = 'CHAT'
  AND api_key_encrypted IS NULL
  AND enabled = TRUE;
