-- Portal banner sys_config (ASCII-safe unicode escapes)

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 9, E'\u95e8\u6237\u9996\u9875-Banner\u6a21\u5f0f', 'portal.home.banner.mode', 'none', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'none=\u9ed8\u8ba4\u6e10\u53d8, image=\u80cc\u666f\u56fe, video=\u80cc\u666f\u89c6\u9891'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.mode');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 10, E'\u95e8\u6237\u9996\u9875-Banner\u56fe\u7247', 'portal.home.banner.imageUrl', '', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'\u56fe\u7247URL\uff0c\u5982 /profile/upload/... \u6216\u5b8c\u6574 http \u5730\u5740'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.imageUrl');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 11, E'\u95e8\u6237\u9996\u9875-Banner\u89c6\u9891', 'portal.home.banner.videoUrl', '', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'mp4/webm \u89c6\u9891\u5730\u5740'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.videoUrl');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 12, E'\u95e8\u6237\u9996\u9875-Banner\u89c6\u9891\u5c01\u9762', 'portal.home.banner.videoPoster', '', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'\u89c6\u9891\u52a0\u8f7d\u524d\u5c01\u9762\u56fe'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.videoPoster');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 13, E'\u95e8\u6237\u9996\u9875-Banner\u906e\u7f69', 'portal.home.banner.overlay', '0.42', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'0-1\uff0c\u8d8a\u5927\u6587\u5b57\u5bf9\u6bd4\u8d8a\u5f3a\uff0c\u5efa\u8bae 0.3-0.55'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.overlay');
