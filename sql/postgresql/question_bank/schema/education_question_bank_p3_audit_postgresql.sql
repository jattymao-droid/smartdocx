-- -----------------------------------------------------------------------------
-- education_question_bank_p3_audit_postgresql.sql
-- P3 Sprint 2: question audit permission
-- -----------------------------------------------------------------------------

insert into sys_menu values(201208, E'\u9898\u5e93\u5ba1\u6838', 20120, 8, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:question:audit', '#', 'admin', CURRENT_TIMESTAMP, '', null, E'\u5ba1\u6838\u5f85\u5ba1\u6838\u8bd5\u9898')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, 201208
from sys_role r
where r.role_key in ('admin', 'edu_admin')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = 201208
  );
