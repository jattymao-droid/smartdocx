-- Library preview page limit (sys_config)
-- Run: psql -U postgres -d ry_cloud -f patch_edu_library_preview_limit.sql

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 910, E'\u6587\u5e93-\u9884\u89c8\u9875\u6570\u4e0a\u9650', 'edu.library.preview.max-pages', '5', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'\u95e8\u6237 PDF \u9884\u89c8\u6700\u591a\u663e\u793a\u9875\u6570\uff0c\u8d85\u51fa\u90e8\u5206\u9700\u4e0b\u8f7d\u67e5\u770b'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'edu.library.preview.max-pages');
