-- textbook catalog admin menu
insert into sys_menu values(20121, E'\u6559\u6750\u76ee\u5f55\u7ba1\u7406', 20201, 7, 'textbook-catalog', 'education/textbook-catalog/index', '', '', 1, 0, 'C', '0', '0', 'education:textbook:list', 'tree-table', 'admin', CURRENT_TIMESTAMP, '', null, E'\u7ef4\u62a4\u5b66\u79d1\u6559\u6750\u7248\u672c\u4e0e\u7ae0\u8282')
on conflict (menu_id) do nothing;
insert into sys_menu values(201211, E'\u76ee\u5f55\u67e5\u8be2', 20121, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:textbook:query', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201212, E'\u76ee\u5f55\u65b0\u589e', 20121, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:textbook:add', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201213, E'\u76ee\u5f55\u4fee\u6539', 20121, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:textbook:edit', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201214, E'\u76ee\u5f55\u5220\u9664', 20121, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:textbook:remove', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_role_menu (role_id, menu_id)
select r.role_id, x.menu_id
from sys_role r
cross join (values (20121), (201211), (201212), (201213), (201214)) as x(menu_id)
where r.role_key in ('admin', 'edu_admin', 'edu_teacher')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = x.menu_id
  );
