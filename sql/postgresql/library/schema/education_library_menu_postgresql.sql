insert into sys_menu values(20130, E'\u6587\u5e93\u7ba1\u7406', 20300, 9, 'library', 'education/library/index', '', '', 1, 0, 'C', '0', '0', 'education:library:list', 'documentation', 'admin', CURRENT_TIMESTAMP, '', null, E'\u6559\u5b66\u6587\u6863\u6587\u5e93')
on conflict (menu_id) do update set
    parent_id = 20300,
    order_num = 9,
    path = 'library',
    component = 'education/library/index',
    perms = 'education:library:list',
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP;

insert into sys_menu values(201301, E'\u6587\u6863\u67e5\u8be2', 20130, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:library:query', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201302, E'\u6587\u6863\u65b0\u589e', 20130, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:library:add', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201303, E'\u6587\u6863\u4fee\u6539', 20130, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:library:edit', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201304, E'\u6587\u6863\u5220\u9664', 20130, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:library:remove', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201305, E'\u6587\u6863\u5ba1\u6838', 20130, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:library:audit', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_menu values(20131, E'\u6587\u5e93\u5206\u7c7b', 20300, 10, 'libraryCategory', 'education/library/category', '', '', 1, 0, 'C', '0', '0', 'education:library:category', 'tree', 'admin', CURRENT_TIMESTAMP, '', null, E'\u6587\u6863\u5206\u7c7b\u7ef4\u62a4')
on conflict (menu_id) do update set
    parent_id = 20300,
    order_num = 10,
    path = 'libraryCategory',
    component = 'education/library/category',
    perms = 'education:library:category',
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, x.menu_id
from sys_role r
cross join (values (20130), (201301), (201302), (201303), (201304), (201305), (20131), (20300)) as x(menu_id)
where r.role_key in ('admin', 'edu_admin', 'edu_teacher')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = x.menu_id
  );
