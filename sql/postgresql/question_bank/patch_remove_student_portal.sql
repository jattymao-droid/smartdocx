-- Remove student portal role, menus, and optional student-only tables
-- Run: psql -U postgres -d ry_cloud -f patch_remove_student_portal.sql

-- Portal / student-portal menus (banner, exam browse, student API perms)
UPDATE sys_menu SET status = '1', update_by = 'admin', update_time = CURRENT_TIMESTAMP
WHERE menu_id IN (20124, 201241, 201242, 201255, 20351, 20352, 20353, 20354, 20355, 20356, 20357, 20358)
   OR perms LIKE 'education:student:%'
   OR perms = 'education:exam-paper:portal'
   OR perms LIKE 'education:portal:%';

DELETE FROM sys_role_menu
WHERE menu_id IN (
    SELECT menu_id FROM sys_menu
    WHERE menu_id IN (20124, 201241, 201242, 201255, 20351, 20352, 20353, 20354, 20355, 20356, 20357, 20358)
       OR perms LIKE 'education:student:%'
       OR perms = 'education:exam-paper:portal'
       OR perms LIKE 'education:portal:%'
);

-- Soft-delete student portal login role (keep teacher roster menu 20100)
UPDATE sys_role SET del_flag = '2', update_by = 'admin', update_time = CURRENT_TIMESTAMP
WHERE role_key = 'edu_student' AND del_flag = '0';

DELETE FROM sys_role_menu
WHERE role_id IN (SELECT role_id FROM sys_role WHERE role_key = 'edu_student');

DELETE FROM sys_user_role
WHERE role_id IN (SELECT role_id FROM sys_role WHERE role_key = 'edu_student');

-- Portal banner sys_config (no longer used)
DELETE FROM sys_config WHERE config_key LIKE 'portal.home.banner.%';

-- Student-only feature tables (safe if never created)
DROP TABLE IF EXISTS edu_qb_discussion_reply;
DROP TABLE IF EXISTS edu_qb_discussion_thread;
DROP TABLE IF EXISTS edu_qb_student_check_in;
