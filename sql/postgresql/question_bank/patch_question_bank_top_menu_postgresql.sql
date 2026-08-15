-- Promote 题库管理 (20120) to top-level menu; nest 新增试题 (20122) under it.

update sys_menu
set parent_id = 0,
    order_num = 5,
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP
where menu_id = 20120;

update sys_menu
set parent_id = 20120,
    order_num = 1,
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP
where menu_id = 20122;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, 20120
from sys_role r
where r.role_key in ('admin', 'edu_admin', 'edu_teacher')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = 20120
  );
