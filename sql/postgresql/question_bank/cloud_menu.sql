-- Re-parent question-bank menus created with parent 20201
update sys_menu set parent_id = 20300, update_by = 'admin', update_time = CURRENT_TIMESTAMP
where menu_id in (20121, 20122) and parent_id = 20201;

-- 题库管理 as top-level menu
update sys_menu set parent_id = 0, order_num = 5, update_by = 'admin', update_time = CURRENT_TIMESTAMP
where menu_id = 20120;

update sys_menu set parent_id = 20120, order_num = 1, update_by = 'admin', update_time = CURRENT_TIMESTAMP
where menu_id = 20122;

-- Grant admin role (role_id=1)
insert into sys_role_menu (role_id, menu_id)
select 1, x.menu_id
from (values
  (20300),
  (20120),(201201),(201202),(201203),(201204),(201205),(201206),
  (20121),(201211),(201212),(201213),(201214),
  (20122),
  (20124),(201241),(201242),
  (20125),(201251),(201252),(201253),(201254),(201255)
) as x(menu_id)
where not exists (
  select 1 from sys_role_menu rm where rm.role_id = 1 and rm.menu_id = x.menu_id
);
