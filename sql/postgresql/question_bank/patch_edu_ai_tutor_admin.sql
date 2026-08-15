-- AI tutor: sys_config (DeepSeek defaults) + admin menu
-- Run: psql -U postgres -d ry_cloud -f patch_edu_ai_tutor_admin.sql

-- sys_config keys (DeepSeek)
INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 901, E'AI\u8bb2\u9898-\u542f\u7528', 'edu.qb.ai.enabled', 'true', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'true=\u5f00\u542f\u95e8\u6237AI\u8bb2\u9898'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'edu.qb.ai.enabled');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 902, E'AI\u8bb2\u9898-\u63a5\u53e3\u5730\u5740', 'edu.qb.ai.base-url', 'https://api.deepseek.com', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'OpenAI\u517c\u5bb9API\u5730\u5740\uff0cDeepSeek: https://api.deepseek.com'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'edu.qb.ai.base-url');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 903, E'AI\u8bb2\u9898-API\u5bc6\u94a5', 'edu.qb.ai.api-key', '', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'DeepSeek/OpenAI API Key\uff0c\u8bf7\u5728\u7ba1\u7406\u540e\u53f0\u914d\u7f6e'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'edu.qb.ai.api-key');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 904, E'AI\u8bb2\u9898-\u6a21\u578b', 'edu.qb.ai.model', 'deepseek-chat', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'DeepSeek\u5bf9\u8bdd\u6a21\u578b deepseek-chat / deepseek-reasoner'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'edu.qb.ai.model');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 905, E'AI\u8bb2\u9898-\u6e29\u5ea6', 'edu.qb.ai.temperature', '0.7', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'0-1\uff0c\u8d8a\u9ad8\u8d8a\u6709\u521b\u9020\u6027'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'edu.qb.ai.temperature');

-- Admin menu under ??????? (20300)
INSERT INTO sys_menu VALUES(
  20126, E'AI\u8bb2\u9898\u914d\u7f6e', 20300, 6, 'ai-tutor', 'education/portal/ai-tutor/index', '', '', 1, 0, 'C', '0', '0', 'education:ai-tutor:list', 'education', 'admin', CURRENT_TIMESTAMP, '', NULL, E'\u95e8\u6237AI\u8bb2\u9898\u6a21\u578b\u4e0e\u63a5\u53e3\u914d\u7f6e'
)
ON CONFLICT (menu_id) DO UPDATE SET
  menu_name = excluded.menu_name,
  parent_id = excluded.parent_id,
  order_num = excluded.order_num,
  path = excluded.path,
  component = excluded.component,
  perms = excluded.perms,
  icon = excluded.icon,
  update_by = 'admin',
  update_time = CURRENT_TIMESTAMP;

INSERT INTO sys_menu VALUES(201261, E'AI\u8bb2\u9898\u67e5\u8be2', 20126, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:ai-tutor:query', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_menu VALUES(201262, E'AI\u8bb2\u9898\u4fee\u6539', 20126, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:ai-tutor:edit', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '')
ON CONFLICT (menu_id) DO NOTHING;

-- Admin role menus
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, x.menu_id
FROM (VALUES (20126), (201261), (201262)) AS x(menu_id)
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = x.menu_id
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN (VALUES (20126), (201261), (201262)) AS m(menu_id)
WHERE r.role_key IN ('edu_admin', 'edu_teacher')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
  );
