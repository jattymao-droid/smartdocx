-- Question bank menus for RuoYi-Cloud (standalone top-level group)

insert into sys_menu values(
  20300, E'\u9898\u5e93\u4e2d\u5fc3', 0, 6, 'question-bank-center', 'Layout', '', '', 1, 0, 'M', '0', '0', '', 'education', 'admin', CURRENT_TIMESTAMP, '', null, E'\u9898\u5e93\u4e0e\u7ec4\u5377\u529f\u80fd'
)
on conflict (menu_id) do update set
  menu_name = excluded.menu_name,
  parent_id = excluded.parent_id,
  order_num = excluded.order_num,
  path = excluded.path,
  component = excluded.component,
  menu_type = excluded.menu_type,
  icon = excluded.icon,
  update_by = 'admin',
  update_time = CURRENT_TIMESTAMP;

-- Minimal edu_subject for question bank (Cloud)
create table if not exists edu_subject (
    subject_id      bigserial primary key,
    subject_name    varchar(30) not null,
    full_score      numeric(6, 2),
    order_num       integer default 0,
    status          char(1) default '0',
    create_by       varchar(64) default 'admin',
    create_time     timestamp default CURRENT_TIMESTAMP,
    update_by       varchar(64),
    update_time     timestamp,
    remark          varchar(500),
    constraint uk_edu_subject_name unique (subject_name)
);

create index if not exists idx_edu_subject_status_order on edu_subject (status, order_num, subject_id);

insert into edu_subject (subject_name, full_score, order_num, status, remark)
values (E'\u8bed\u6587', 120, 1, '0', E'\u9ed8\u8ba4\u5b66\u79d1')
on conflict (subject_name) do nothing;
insert into edu_subject (subject_name, full_score, order_num, status, remark)
values (E'\u6570\u5b66', 120, 2, '0', E'\u9ed8\u8ba4\u5b66\u79d1')
on conflict (subject_name) do nothing;
insert into edu_subject (subject_name, full_score, order_num, status, remark)
values (E'\u82f1\u8bed', 120, 3, '0', E'\u9ed8\u8ba4\u5b66\u79d1')
on conflict (subject_name) do nothing;
insert into edu_subject (subject_name, full_score, order_num, status, remark)
values (E'\u7269\u7406', 100, 4, '0', E'\u9ed8\u8ba4\u5b66\u79d1')
on conflict (subject_name) do nothing;
insert into edu_subject (subject_name, full_score, order_num, status, remark)
values (E'\u5316\u5b66', 100, 5, '0', E'\u9ed8\u8ba4\u5b66\u79d1')
on conflict (subject_name) do nothing;
insert into edu_subject (subject_name, full_score, order_num, status, remark)
values (E'\u751f\u7269', 100, 6, '0', E'\u9ed8\u8ba4\u5b66\u79d1')
on conflict (subject_name) do nothing;

insert into sys_menu values(20107, E'\u5b66\u79d1\u7ba1\u7406', 20300, 4, 'subject', 'education/subject/index', '', '', 1, 0, 'C', '0', '0', 'education:subject:list', 'dict', 'admin', CURRENT_TIMESTAMP, '', null, E'\u5b66\u79d1\u7ba1\u7406\u83dc\u5355')
on conflict (menu_id) do nothing;
insert into sys_menu values(201070, E'\u5b66\u79d1\u67e5\u8be2', 20107, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:subject:query',  '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201071, E'\u5b66\u79d1\u65b0\u589e', 20107, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:subject:add',    '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201072, E'\u5b66\u79d1\u4fee\u6539', 20107, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:subject:edit',   '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201073, E'\u5b66\u79d1\u5220\u9664', 20107, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:subject:remove', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select 1, x.menu_id
from (values (20300),(20107),(201070),(201071),(201072),(201073)) as x(menu_id)
where not exists (select 1 from sys_role_menu rm where rm.role_id = 1 and rm.menu_id = x.menu_id);
