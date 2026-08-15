-- Portal home banner carousel slides + hero copy (optional config keys)

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT E'\u95e8\u6237\u9996\u9875-Banner\u5e7b\u706f\u7247', 'portal.home.banner.slides', '[]', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'JSON \u6570\u7ec4\uff0c\u6bcf\u9879\u542b title/desc/bg/imageUrl'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.slides');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT E'\u95e8\u6237\u9996\u9875-Banner\u4e3b\u6807\u9898', 'portal.home.banner.heroTitle', '', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'\u56fe\u7247/\u89c6\u9891\u6a21\u5f0f\u4e0b Hero \u4e3b\u6807\u9898'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.heroTitle');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT E'\u95e8\u6237\u9996\u9875-Banner\u526f\u6807\u9898', 'portal.home.banner.heroDesc', '', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'\u56fe\u7247/\u89c6\u9891\u6a21\u5f0f\u4e0b Hero \u526f\u6807\u9898'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.heroDesc');
