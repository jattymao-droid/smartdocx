-- P0 integration patch: exam-paper menus/permissions + portal banner admin
-- Run: psql -U postgres -d ry_cloud -f patch_p0_integrations.sql

\ir schema/education_exam_paper_postgresql.sql
\ir edu_portal_banner_menu_postgresql.sql
\ir schema/education_paper_share_postgresql.sql

-- Grant portal banner admin to teacher roles (not only role_id=1)
insert into sys_role_menu (role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
cross join (values (20124), (201241), (201242)) as m(menu_id)
where r.role_key in ('admin', 'edu_admin', 'edu_teacher')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = m.menu_id
  );
