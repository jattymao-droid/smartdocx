-- Portal top header background image config (no fixed config_id to avoid PK conflicts)
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT E'\u95e8\u6237\u9876\u680f-\u80cc\u666f\u6a21\u5f0f', 'portal.header.banner.mode', 'none', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'none=\u9ed8\u8ba4, image=\u80cc\u666f\u56fe'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.header.banner.mode');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT E'\u95e8\u6237\u9876\u680f-\u80cc\u666f\u56fe\u7247', 'portal.header.banner.imageUrl', '', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'\u9876\u680f\u80cc\u666f\u56fe URL'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.header.banner.imageUrl');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT E'\u95e8\u6237\u9876\u680f-\u906e\u7f69', 'portal.header.banner.overlay', '0.4', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'0-1\u6697\u8272\u906e\u7f69'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.header.banner.overlay');
