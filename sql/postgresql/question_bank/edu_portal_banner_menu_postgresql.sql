-- Portal home banner admin menu under ������� (20300)

insert into sys_menu values(
  20124, E'\u95e8\u6237Banner', 20300, 5, 'portal-banner', 'education/portal/banner/index', '', '', 1, 0, 'C', '0', '0', 'education:portal:banner:list', 'example', 'admin', CURRENT_TIMESTAMP, '', null, E'\u95e8\u6237\u9996\u9875 Banner \u80cc\u666f\u56fe/\u89c6\u9891\u914d\u7f6e'
)
on conflict (menu_id) do update set
  menu_name = excluded.menu_name,
  parent_id = excluded.parent_id,
  order_num = excluded.order_num,
  path = excluded.path,
  component = excluded.component,
  perms = excluded.perms,
  icon = excluded.icon,
  update_by = 'admin',
  update_time = CURRENT_TIMESTAMP;

insert into sys_menu values(201241, E'Banner\u67e5\u8be2', 20124, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:portal:banner:query', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201242, E'Banner\u4fee\u6539', 20124, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:portal:banner:edit', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select 1, x.menu_id
from (values (20124), (201241), (201242)) as x(menu_id)
where not exists (
  select 1 from sys_role_menu rm where rm.role_id = 1 and rm.menu_id = x.menu_id
);

insert into sys_role_menu (role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
cross join (values (20124), (201241), (201242)) as m(menu_id)
where r.role_key in ('edu_admin', 'edu_teacher')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = m.menu_id
  );
