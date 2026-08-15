-- Minimal edu_subject dependency for question bank module
-- Run BEFORE edu_qb_* schema scripts if edu_subject does not exist

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
values ('语文', 120, 1, '0', '默认学科')
on conflict (subject_name) do nothing;
insert into edu_subject (subject_name, full_score, order_num, status, remark)
values ('数学', 120, 2, '0', '默认学科')
on conflict (subject_name) do nothing;
insert into edu_subject (subject_name, full_score, order_num, status, remark)
values ('英语', 120, 3, '0', '默认学科')
on conflict (subject_name) do nothing;
insert into edu_subject (subject_name, full_score, order_num, status, remark)
values ('物理', 100, 4, '0', '默认学科')
on conflict (subject_name) do nothing;
insert into edu_subject (subject_name, full_score, order_num, status, remark)
values ('化学', 100, 5, '0', '默认学科')
on conflict (subject_name) do nothing;
insert into edu_subject (subject_name, full_score, order_num, status, remark)
values ('生物', 100, 6, '0', '默认学科')
on conflict (subject_name) do nothing;
insert into edu_subject (subject_name, full_score, order_num, status, remark)
values ('总分', 660, 99, '0', '默认学科')
on conflict (subject_name) do nothing;

insert into sys_menu values(20107, '学科管理', 20203, 1, 'subject', 'education/subject/index', '', '', 1, 0, 'C', '0', '0', 'education:subject:list', 'dict', 'admin', CURRENT_TIMESTAMP, '', null, '学科管理菜单')
on conflict (menu_id) do nothing;
insert into sys_menu values(201070, '学科查询', 20107, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:subject:query',  '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201071, '学科新增', 20107, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:subject:add',    '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201072, '学科修改', 20107, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:subject:edit',   '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201073, '学科删除', 20107, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:subject:remove', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, x.menu_id
from sys_role r
cross join (values
  (20107),
  (201070),(201071),(201072),(201073)
) as x(menu_id)
where r.role_key in ('admin', 'edu_admin')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = x.menu_id
  );


-- -----------------------------------------------------------------------------
-- [17/51] education_class_teacher_postgresql.sql
-- -----------------------------------------------------------------------------

-- Class homeroom and subject teacher assignments

create table if not exists edu_class_teacher (
    id            bigserial primary key,
    class_id      bigint not null,
    teacher_id    bigint not null,
    role_type     char(1) not null,
    subject_name  varchar(30),
    create_by     varchar(64) default 'admin',
    create_time   timestamp default CURRENT_TIMESTAMP,
    update_by     varchar(64),
    update_time   timestamp,
    constraint fk_edu_class_teacher_class foreign key (class_id) references edu_class(class_id) on delete cascade,
    constraint fk_edu_class_teacher_teacher foreign key (teacher_id) references edu_teacher(teacher_id) on delete cascade,
    constraint ck_edu_class_teacher_role check (role_type in ('0', '1'))
);

create unique index if not exists uk_edu_class_teacher_head on edu_class_teacher (class_id) where role_type = '0';
create unique index if not exists uk_edu_class_teacher_subject on edu_class_teacher (class_id, subject_name) where role_type = '1';
create index if not exists idx_edu_class_teacher_teacher on edu_class_teacher (teacher_id);

comment on table edu_class_teacher is 'Class teacher assignment (homeroom and subject)';
comment on column edu_class_teacher.role_type is '0=head teacher, 1=subject teacher';

-- Backfill head teacher from legacy text column when possible
insert into edu_class_teacher (class_id, teacher_id, role_type, subject_name, create_by)
select c.class_id, t.teacher_id, '0', null, 'admin'
from edu_class c
join edu_teacher t on t.teacher_name = c.head_teacher and t.status = '0'
where c.head_teacher is not null and c.head_teacher <> ''
  and not exists (
    select 1 from edu_class_teacher ct where ct.class_id = c.class_id and ct.role_type = '0'
  );


-- -----------------------------------------------------------------------------
-- [18/51] education_class_options_postgresql.sql
-- -----------------------------------------------------------------------------

-- Class combo type and class level dictionary options (idempotent)

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select E'\u73ed\u7ea7\u7ec4\u5408\u7c7b\u578b', 'edu_combo_type', '0', 'admin', CURRENT_TIMESTAMP, E'\u9ad8\u4e2d\u9009\u79d1\u7ec4\u5408\u7c7b\u578b'
where not exists (select 1 from sys_dict_type where dict_type = 'edu_combo_type');

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select E'\u73ed\u7ea7\u5c42\u6b21', 'edu_class_level', '0', 'admin', CURRENT_TIMESTAMP, E'\u73ed\u7ea7\u6559\u5b66\u5c42\u6b21'
where not exists (select 1 from sys_dict_type where dict_type = 'edu_class_level');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time)
select v.sort, v.label, v.label, 'edu_combo_type', 'N', '0', 'admin', CURRENT_TIMESTAMP
from (values
    (1, E'\u7269\u5316\u751f'),
    (2, E'\u7269\u5316\u5730'),
    (3, E'\u7269\u5316\u653f'),
    (4, E'\u7269\u751f\u5730'),
    (5, E'\u7269\u751f\u653f'),
    (6, E'\u53f2\u5730\u653f'),
    (7, E'\u53f2\u5730\u751f'),
    (8, E'\u5168\u6587'),
    (9, E'\u827a\u672f'),
    (10, E'\u4f53\u80b2')
) as v(sort, label)
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'edu_combo_type' and d.dict_label = v.label
);

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, is_default, status, create_by, create_time)
select v.sort, v.label, v.label, 'edu_class_level', 'N', '0', 'admin', CURRENT_TIMESTAMP
from (values
    (1, E'\u5c16\u5b50\u73ed'),
    (2, E'\u91cd\u70b9\u73ed'),
    (3, E'\u5b9e\u9a8c\u73ed'),
    (4, E'\u5e73\u884c\u73ed'),
    (5, E'\u666e\u901a\u73ed')
) as v(sort, label)
where not exists (
    select 1 from sys_dict_data d where d.dict_type = 'edu_class_level' and d.dict_label = v.label
);


-- -----------------------------------------------------------------------------
-- [19/51] education_system_menu_restore_postgresql.sql
-- -----------------------------------------------------------------------------

-- Restore system management menus (100-104) and recreate education menus with new IDs (20100+)

-- 1) Restore system menus (keep their existing button children 1000-1024 etc)
update sys_menu set
  menu_name = '用户管理',
  parent_id = 1,
  order_num = 20,
  path = 'user',
  component = 'system/user/index',
  perms = 'system:user:list',
  visible = '0',
  menu_type = 'C',
  icon = 'user',
  update_by = 'admin',
  update_time = CURRENT_TIMESTAMP
where menu_id = 100;

update sys_menu set
  menu_name = '角色管理',
  parent_id = 1,
  order_num = 21,
  path = 'role',
  component = 'system/role/index',
  perms = 'system:role:list',
  visible = '0',
  menu_type = 'C',
  icon = 'peoples',
  update_by = 'admin',
  update_time = CURRENT_TIMESTAMP
where menu_id = 101;

update sys_menu set
  menu_name = '菜单管理',
  parent_id = 1,
  order_num = 22,
  path = 'menu',
  component = 'system/menu/index',
  perms = 'system:menu:list',
  visible = '0',
  menu_type = 'C',
  icon = 'tree-table',
  update_by = 'admin',
  update_time = CURRENT_TIMESTAMP
where menu_id = 102;

update sys_menu set
  menu_name = '部门管理',
  parent_id = 1,
  order_num = 23,
  path = 'dept',
  component = 'system/dept/index',
  perms = 'system:dept:list',
  visible = '0',
  menu_type = 'C',
  icon = 'tree',
  update_by = 'admin',
  update_time = CURRENT_TIMESTAMP
where menu_id = 103;

update sys_menu set
  menu_name = '岗位管理',
  parent_id = 1,
  order_num = 24,
  path = 'post',
  component = 'system/post/index',
  perms = 'system:post:list',
  visible = '0',
  menu_type = 'C',
  icon = 'post',
  update_by = 'admin',
  update_time = CURRENT_TIMESTAMP
where menu_id = 104;

-- 2) Create education menus with new IDs
insert into sys_menu values(20100, '学生档案', 1, 1, 'student',    'education/student/index',    '', '', 1, 0, 'C', '0', '0', 'education:student:list',    'user',      'admin', CURRENT_TIMESTAMP, '', null, '学生档案菜单')
on conflict (menu_id) do nothing;
insert into sys_menu values(20101, '成绩分析', 1, 2, 'analysis',   'education/analysis/index',   '', '', 1, 0, 'C', '0', '0', 'education:score:list',   'chart',     'admin', CURRENT_TIMESTAMP, '', null, '成绩分析菜单')
on conflict (menu_id) do nothing;
insert into sys_menu values(20102, '考勤管理', 1, 3, 'attendance', 'education/attendance/index', '', '', 1, 0, 'C', '0', '0', 'education:attendance:list', 'date',      'admin', CURRENT_TIMESTAMP, '', null, '考勤管理菜单')
on conflict (menu_id) do nothing;
insert into sys_menu values(20103, '预警中心', 1, 4, 'alert',      'education/alert/index',      '', '', 1, 0, 'C', '0', '0', 'education:alert:list',      'guide',     'admin', CURRENT_TIMESTAMP, '', null, '预警中心菜单')
on conflict (menu_id) do nothing;
insert into sys_menu values(20104, '成长记录', 1, 5, 'growth',     'education/growth/index',     '', '', 1, 0, 'C', '0', '0', 'education:growth:list',     'education', 'admin', CURRENT_TIMESTAMP, '', null, '成长记录菜单')
on conflict (menu_id) do nothing;
insert into sys_menu values(20105, '教师管理', 1, 7, 'teacher',    'education/teacher/index',    '', '', 1, 0, 'C', '0', '0', 'education:teacher:list',    'people',    'admin', CURRENT_TIMESTAMP, '', null, '教师管理菜单')
on conflict (menu_id) do nothing;

-- 3) Button-level permissions for education menus
insert into sys_menu values(201000, '学生查询', 20100, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:student:query',  '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201001, '学生新增', 20100, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:student:add',    '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201002, '学生修改', 20100, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:student:edit',   '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201003, '学生删除', 20100, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:student:remove', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_menu values(201010, '成绩查询', 20101, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:score:query',  '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201011, '成绩新增', 20101, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:score:add',    '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201012, '成绩修改', 20101, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:score:edit',   '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201013, '成绩删除', 20101, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:score:remove', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_menu values(201020, '考勤查询', 20102, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:attendance:query',  '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201021, '考勤新增', 20102, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:attendance:add',    '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201022, '考勤修改', 20102, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:attendance:edit',   '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201023, '考勤删除', 20102, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:attendance:remove', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_menu values(201030, '预警查询', 20103, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:alert:query',  '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201031, '预警新增', 20103, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:alert:add',    '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201032, '预警修改', 20103, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:alert:edit',   '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201033, '预警删除', 20103, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:alert:remove', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_menu values(201040, '成长查询', 20104, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:growth:query',  '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201041, '成长新增', 20104, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:growth:add',    '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201042, '成长修改', 20104, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:growth:edit',   '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201043, '成长删除', 20104, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:growth:remove', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_menu values(201050, '教师查询', 20105, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:teacher:query',  '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201051, '教师新增', 20105, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:teacher:add',    '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201052, '教师修改', 20105, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:teacher:edit',   '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201053, '教师删除', 20105, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:teacher:remove', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

-- 4) Grant admin role access to new education menus (role_id=1)
insert into sys_role_menu (role_id, menu_id)
select 1, x.menu_id
from (values
  (20100),(20101),(20102),(20103),(20104),(20105),
  (201000),(201001),(201002),(201003),
  (201010),(201011),(201012),(201013),
  (201020),(201021),(201022),(201023),
  (201030),(201031),(201032),(201033),
  (201040),(201041),(201042),(201043),
  (201050),(201051),(201052),(201053)
) as x(menu_id)
where not exists (select 1 from sys_role_menu rm where rm.role_id = 1 and rm.menu_id = x.menu_id);


-- -----------------------------------------------------------------------------
-- [20/51] education_grade_class_merge_menu_postgresql.sql
-- -----------------------------------------------------------------------------

update sys_menu
set menu_name = '年级班级',
    path = 'gradeClass',
    component = 'education/gradeClass/index',
    perms = 'education:grade:list',
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP
where menu_id = 2000;

update sys_menu
set visible = '1',
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP
where menu_id = 2001;

-- 合并页使用班级按钮权限，为教育角色补齐授权
insert into sys_role_menu (role_id, menu_id)
select r.role_id, x.menu_id
from sys_role r
cross join (values (20010), (20011), (20012), (20013)) as x(menu_id)
where r.role_key in ('admin', 'edu_admin', 'edu_head_teacher', 'edu_grade_leader')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = x.menu_id
  );


-- -----------------------------------------------------------------------------
-- [21/51] education_menu_group_postgresql.sql
-- -----------------------------------------------------------------------------

-- Group menus under 学情中心 by functional area

-- 1) Create directory groups
insert into sys_menu values(20200, '学籍管理', 1, 1, 'student-affairs', 'ParentView', '', '', 1, 0, 'M', '0', '0', '', 'user', 'admin', CURRENT_TIMESTAMP, '', null, '学籍与师生基础信息分组')
on conflict (menu_id) do update set
  menu_name = excluded.menu_name,
  parent_id = excluded.parent_id,
  order_num = excluded.order_num,
  path = excluded.path,
  component = excluded.component,
  menu_type = excluded.menu_type,
  visible = excluded.visible,
  is_frame = excluded.is_frame,
  is_cache = excluded.is_cache,
  icon = excluded.icon,
  update_by = 'admin',
  update_time = CURRENT_TIMESTAMP,
  remark = excluded.remark;

insert into sys_menu values(20201, '教学管理', 1, 2, 'teaching', 'ParentView', '', '', 1, 0, 'M', '0', '0', '', 'chart', 'admin', CURRENT_TIMESTAMP, '', null, '考试成绩与考勤业务分组')
on conflict (menu_id) do update set
  menu_name = excluded.menu_name,
  parent_id = excluded.parent_id,
  order_num = excluded.order_num,
  path = excluded.path,
  component = excluded.component,
  menu_type = excluded.menu_type,
  visible = excluded.visible,
  is_frame = excluded.is_frame,
  is_cache = excluded.is_cache,
  icon = excluded.icon,
  update_by = 'admin',
  update_time = CURRENT_TIMESTAMP,
  remark = excluded.remark;

insert into sys_menu values(20202, '预警跟踪', 1, 3, 'warning-track', 'ParentView', '', '', 1, 0, 'M', '0', '0', '', 'guide', 'admin', CURRENT_TIMESTAMP, '', null, '预警与成长跟踪分组')
on conflict (menu_id) do update set
  menu_name = excluded.menu_name,
  parent_id = excluded.parent_id,
  order_num = excluded.order_num,
  path = excluded.path,
  component = excluded.component,
  menu_type = excluded.menu_type,
  visible = excluded.visible,
  is_frame = excluded.is_frame,
  is_cache = excluded.is_cache,
  icon = excluded.icon,
  update_by = 'admin',
  update_time = CURRENT_TIMESTAMP,
  remark = excluded.remark;

insert into sys_menu values(20203, '基础配置', 1, 4, 'basic-settings', 'ParentView', '', '', 1, 0, 'M', '0', '0', '', 'dict', 'admin', CURRENT_TIMESTAMP, '', null, '基础参数与公告配置分组')
on conflict (menu_id) do update set
  menu_name = excluded.menu_name,
  parent_id = excluded.parent_id,
  order_num = excluded.order_num,
  path = excluded.path,
  component = excluded.component,
  menu_type = excluded.menu_type,
  visible = excluded.visible,
  is_frame = excluded.is_frame,
  is_cache = excluded.is_cache,
  icon = excluded.icon,
  update_by = 'admin',
  update_time = CURRENT_TIMESTAMP,
  remark = excluded.remark;

insert into sys_menu values(20204, '平台管理', 1, 5, 'platform-admin', 'ParentView', '', '', 1, 0, 'M', '0', '0', '', 'system', 'admin', CURRENT_TIMESTAMP, '', null, '平台权限与组织管理分组')
on conflict (menu_id) do update set
  menu_name = excluded.menu_name,
  parent_id = excluded.parent_id,
  order_num = excluded.order_num,
  path = excluded.path,
  component = excluded.component,
  menu_type = excluded.menu_type,
  visible = excluded.visible,
  is_frame = excluded.is_frame,
  is_cache = excluded.is_cache,
  icon = excluded.icon,
  update_by = 'admin',
  update_time = CURRENT_TIMESTAMP,
  remark = excluded.remark;

-- 2) Re-group education business menus
update sys_menu set parent_id = 20200, order_num = 1, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 20100;
update sys_menu set parent_id = 20200, order_num = 2, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 2000;
update sys_menu set parent_id = 20200, order_num = 3, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 20105;
update sys_menu set parent_id = 20200, order_num = 4, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 2001;

update sys_menu set parent_id = 20201, order_num = 1, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 20106;
update sys_menu set parent_id = 20201, order_num = 2, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 20101;
update sys_menu set parent_id = 20201, order_num = 3, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 20102;

update sys_menu set parent_id = 20202, order_num = 1, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 20103;
update sys_menu set parent_id = 20202, order_num = 2, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 20104;

-- 3) Re-group platform/config menus currently under 学情中心
update sys_menu set parent_id = 20203, order_num = 1, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 105;
update sys_menu set parent_id = 20203, order_num = 2, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 106;
update sys_menu set parent_id = 20203, order_num = 3, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 107;

update sys_menu set parent_id = 20204, order_num = 1, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 100;
update sys_menu set parent_id = 20204, order_num = 2, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 101;
update sys_menu set parent_id = 20204, order_num = 3, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 102;
update sys_menu set parent_id = 20204, order_num = 4, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 103;
update sys_menu set parent_id = 20204, order_num = 5, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 104;
update sys_menu set parent_id = 20204, order_num = 6, update_by = 'admin', update_time = CURRENT_TIMESTAMP where menu_id = 108;

-- 4) Grant group directories to admin and education roles
insert into sys_role_menu (role_id, menu_id)
select r.role_id, x.menu_id
from sys_role r
cross join (values
  (20200),(20201),(20202),(20203),(20204)
) as x(menu_id)
where r.role_key = 'admin'
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = x.menu_id
  );

insert into sys_role_menu (role_id, menu_id)
select r.role_id, x.menu_id
from sys_role r
cross join (values
  (20200),(20201),(20202)
) as x(menu_id)
where r.role_key in ('edu_admin', 'edu_teacher', 'edu_head_teacher', 'edu_grade_leader')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = x.menu_id
  );


-- -----------------------------------------------------------------------------
-- [22/51] education_roles_postgresql.sql
-- -----------------------------------------------------------------------------

-- 教育角色初始化与授权（增量写入，不删除已有授权）

do $$
declare
    role_id_admin bigint;
    role_id_teacher bigint;
    role_id_head_teacher bigint;
    role_id_grade_leader bigint;
    role_id_wallpaper_admin bigint;
begin
    select role_id into role_id_admin from sys_role where role_key = 'edu_admin' and del_flag = '0';
    if role_id_admin is null then
        insert into sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
        values ('教务管理员', 'edu_admin', 3, '1', 1, 1, '0', '0', 'admin', CURRENT_TIMESTAMP, '学情系统教务管理员')
        returning role_id into role_id_admin;
    end if;

    select role_id into role_id_teacher from sys_role where role_key = 'edu_teacher' and del_flag = '0';
    if role_id_teacher is null then
        insert into sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
        values ('任课教师', 'edu_teacher', 4, '1', 1, 1, '0', '0', 'admin', CURRENT_TIMESTAMP, '学情系统任课教师')
        returning role_id into role_id_teacher;
    end if;

    select role_id into role_id_head_teacher from sys_role where role_key = 'edu_head_teacher' and del_flag = '0';
    if role_id_head_teacher is null then
        insert into sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
        values ('班主任', 'edu_head_teacher', 5, '1', 1, 1, '0', '0', 'admin', CURRENT_TIMESTAMP, '学情系统班主任')
        returning role_id into role_id_head_teacher;
    end if;

    select role_id into role_id_grade_leader from sys_role where role_key = 'edu_grade_leader' and del_flag = '0';
    if role_id_grade_leader is null then
        insert into sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
        values ('年级主任', 'edu_grade_leader', 6, '1', 1, 1, '0', '0', 'admin', CURRENT_TIMESTAMP, '学情系统年级主任')
        returning role_id into role_id_grade_leader;
    end if;

    select role_id into role_id_wallpaper_admin from sys_role where role_key = 'edu_wallpaper_admin' and del_flag = '0';
    if role_id_wallpaper_admin is null then
        insert into sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
        values (E'\u58c1\u7eb8\u7ba1\u7406\u5458', 'edu_wallpaper_admin', 7, '1', 1, 1, '0', '0', 'admin', CURRENT_TIMESTAMP, E'\u6559\u5ba4\u58c1\u7eb8\u4e0e\u8bbe\u5907\u7edf\u7ba1')
        returning role_id into role_id_wallpaper_admin;
    end if;

    insert into sys_role_menu(role_id, menu_id)
    select role_id_admin, x.menu_id
    from (values
        (20200),(20201),(20202),(20203),
        (2000),(2001),
        (20000),(20001),(20002),(20003),
        (20010),(20011),(20012),(20013),
        (20100),(20101),(20102),(20103),(20104),(20105),
        (20106),(20107),
        (201000),(201001),(201002),(201003),
        (201010),(201011),(201012),(201013),
        (201020),(201021),(201022),(201023),
        (201030),(201031),(201032),(201033),
        (201040),(201041),(201042),(201043),
        (201050),(201051),(201052),(201053),(201054),
        (201060),(201061),(201062),(201063),(201064),
        (201070),(201071),(201072),(201073),
        (201090)
    ) as x(menu_id)
    where not exists (
        select 1 from sys_role_menu rm where rm.role_id = role_id_admin and rm.menu_id = x.menu_id
    );

    insert into sys_role_menu(role_id, menu_id)
    select role_id_teacher, x.menu_id
    from (values
        (20200),(20201),(20202),
        (2000),(2001),
        (20100),(20101),(20102),(20103),(20104),
        (20106),
        (201000),(201001),
        (201010),(201011),
        (201020),(201021),
        (201030),(201031),
        (201040),(201041),
        (201060),
        (201090)
    ) as x(menu_id)
    where not exists (
        select 1 from sys_role_menu rm where rm.role_id = role_id_teacher and rm.menu_id = x.menu_id
    );

    insert into sys_role_menu(role_id, menu_id)
    select role_id_head_teacher, x.menu_id
    from (values
        (20200),(20201),(20202),
        (2000),(2001),
        (20100),(20101),(20102),(20103),(20104),
        (20106),
        (201000),(201001),
        (201010),(201011),
        (201020),(201021),
        (201030),(201031),(201032),
        (201040),(201041),(201042),
        (201060),
        (201090)
    ) as x(menu_id)
    where not exists (
        select 1 from sys_role_menu rm where rm.role_id = role_id_head_teacher and rm.menu_id = x.menu_id
    );

    insert into sys_role_menu(role_id, menu_id)
    select role_id_grade_leader, x.menu_id
    from (values
        (20200),(20201),(20202),
        (2000),(2001),
        (20100),(20101),(20102),(20103),(20104),
        (20106),
        (201000),(201001),
        (201010),(201011),
        (201020),(201021),
        (201030),(201031),(201032),
        (201040),(201041),(201042),
        (201060),
        (201090)
    ) as x(menu_id)
    where not exists (
        select 1 from sys_role_menu rm where rm.role_id = role_id_grade_leader and rm.menu_id = x.menu_id
    );
end $$;


-- -----------------------------------------------------------------------------
-- [23/51] education_role_menu_postgresql.sql
-- -----------------------------------------------------------------------------

-- Education role permission menu and grants (idempotent)

insert into sys_menu values(20108, E'\u89d2\u8272\u6743\u9650', 20203, 4, 'eduRole', 'education/role/index', '', '', 1, 0, 'C', '0', '0', 'education:role:list', 'peoples', 'admin', CURRENT_TIMESTAMP, '', null, E'\u6559\u80b2\u89d2\u8272\u6743\u9650\u914d\u7f6e')
on conflict (menu_id) do update set
  menu_name = excluded.menu_name,
  parent_id = excluded.parent_id,
  order_num = excluded.order_num,
  path = excluded.path,
  component = excluded.component,
  perms = excluded.perms,
  icon = excluded.icon,
  update_by = 'admin',
  update_time = CURRENT_TIMESTAMP,
  remark = excluded.remark;

insert into sys_menu values(201081, E'\u89d2\u8272\u67e5\u8be2', 20108, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'education:role:query', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201082, E'\u89d2\u8272\u4fee\u6539', 20108, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'education:role:edit', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, x.menu_id
from sys_role r
cross join (values (20108), (201081), (201082)) as x(menu_id)
where r.role_key in ('admin', 'edu_admin')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = x.menu_id
  );

insert into sys_role_menu (role_id, menu_id)
select r.role_id, 20203
from sys_role r
where r.role_key = 'edu_admin'
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = 20203
  );


-- -----------------------------------------------------------------------------
-- [24/51] education_teacher_account_menu_postgresql.sql
-- -----------------------------------------------------------------------------

insert into sys_menu values(201054, '开通账号', 20105, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:teacher:account', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, 201054
from sys_role r
where r.role_key in ('admin', 'edu_admin')
  and not exists (select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = 201054);


-- -----------------------------------------------------------------------------
-- [25/51] education_dashboard_perm_postgresql.sql
-- -----------------------------------------------------------------------------

insert into sys_menu values(201090, '学情总览', 1, 0, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:dashboard:view', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, 201090
from sys_role r
where r.role_key in ('admin', 'edu_admin', 'edu_teacher', 'edu_head_teacher', 'edu_grade_leader')
  and not exists (select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = 201090);


-- -----------------------------------------------------------------------------
-- [26/51] wx_miniapp_postgresql.sql
-- -----------------------------------------------------------------------------

-- ============================================================
-- HS \u5b66\u60c5\u7ba1\u7406 - \u5fae\u4fe1\u5c0f\u7a0b\u5e8f\u76f8\u5173\u8868\uff08\u4e00\u671f\u8d77\uff09
-- PostgreSQL
-- ============================================================

-- \u5fae\u4fe1\u7528\u6237\uff08\u9759\u9ed8\u767b\u5f55 / \u540e\u7eed\u4e8c\u671f\u6269\u5c55\uff09
CREATE TABLE IF NOT EXISTS wx_user (
    wx_user_id      BIGSERIAL PRIMARY KEY,
    openid          VARCHAR(64) NOT NULL,
    unionid         VARCHAR(64),
    nick_name       VARCHAR(64),
    avatar_url      VARCHAR(512),
    phone           VARCHAR(20),
    session_key     VARCHAR(256),
    last_login_time TIMESTAMP,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP,
    CONSTRAINT uk_wx_user_openid UNIQUE (openid)
);

COMMENT ON TABLE wx_user IS E'\u5fae\u4fe1\u5c0f\u7a0b\u5e8f\u7528\u6237';
COMMENT ON COLUMN wx_user.openid IS E'\u5fae\u4fe1 openid';
COMMENT ON COLUMN wx_user.session_key IS E'\u4f1a\u8bdd\u5bc6\u94a5\uff08\u4ec5\u540e\u7aef\u4fdd\u5b58\uff09';

CREATE INDEX IF NOT EXISTS idx_wx_user_unionid ON wx_user (unionid);

-- \u516c\u5f00\u5206\u73ed\u67e5\u8be2\u5ba1\u8ba1\u65e5\u5fd7\uff08\u4e0d\u8bb0\u5f55\u654f\u611f\u67e5\u8be2\u503c\uff09
CREATE TABLE IF NOT EXISTS wx_class_query_log (
    log_id          BIGSERIAL PRIMARY KEY,
    query_id        BIGINT NOT NULL,
    wx_user_id      BIGINT,
    openid          VARCHAR(64),
    client_ip       VARCHAR(64),
    success         CHAR(1) DEFAULT '0',
    fail_msg        VARCHAR(255),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE wx_class_query_log IS E'\u5c0f\u7a0b\u5e8f\u5206\u73ed\u67e5\u8be2\u65e5\u5fd7';
COMMENT ON COLUMN wx_class_query_log.success IS E'0\u5931\u8d25 1\u6210\u529f';

CREATE INDEX IF NOT EXISTS idx_wx_class_query_log_query ON wx_class_query_log (query_id);
CREATE INDEX IF NOT EXISTS idx_wx_class_query_log_time ON wx_class_query_log (create_time);

-- ============================================================
-- \u4e8c\u671f\uff1a\u5bb6\u957f-\u5b66\u751f\u7ed1\u5b9a
-- ============================================================
CREATE TABLE IF NOT EXISTS wx_student_bind (
    bind_id         BIGSERIAL PRIMARY KEY,
    wx_user_id      BIGINT NOT NULL REFERENCES wx_user(wx_user_id),
    student_id      BIGINT NOT NULL,
    bind_status     CHAR(1) DEFAULT '0',
    verify_type     VARCHAR(20),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP,
    CONSTRAINT uk_wx_student_bind_user_student UNIQUE (wx_user_id, student_id)
);

COMMENT ON TABLE wx_student_bind IS E'\u5bb6\u957f\u5fae\u4fe1\u7ed1\u5b9a\u5b66\u751f';
COMMENT ON COLUMN wx_student_bind.bind_status IS E'0\u6b63\u5e38 1\u5df2\u89e3\u7ed1';

CREATE INDEX IF NOT EXISTS idx_wx_student_bind_student ON wx_student_bind (student_id);
CREATE INDEX IF NOT EXISTS idx_wx_student_bind_user ON wx_student_bind (wx_user_id);

-- ============================================================
-- \u4e09\u671f\uff1a\u6559\u5e08\u5fae\u4fe1\u7ed1\u5b9a sys_user
-- ============================================================
CREATE TABLE IF NOT EXISTS wx_user_bind (
    bind_id         BIGSERIAL PRIMARY KEY,
    wx_user_id      BIGINT NOT NULL REFERENCES wx_user(wx_user_id),
    user_id         BIGINT NOT NULL,
    bind_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_wx_user_bind_wx_user UNIQUE (wx_user_id),
    CONSTRAINT uk_wx_user_bind_user UNIQUE (user_id)
);

COMMENT ON TABLE wx_user_bind IS E'\u6559\u5e08\u5fae\u4fe1\u4e0e\u7cfb\u7edf\u8d26\u53f7\u7ed1\u5b9a';

CREATE INDEX IF NOT EXISTS idx_wx_user_bind_user ON wx_user_bind (user_id);


-- -----------------------------------------------------------------------------
-- [27/51] wx_student_account_postgresql.sql
-- -----------------------------------------------------------------------------

-- Obsolete standalone student-account menu (20112) removed in [28].


-- -----------------------------------------------------------------------------
-- [28/51] education_student_login_password_postgresql.sql
-- -----------------------------------------------------------------------------

-- Student login password + remove standalone wx account menu

alter table edu_student add column if not exists login_password varchar(100);

comment on column edu_student.login_password is E'\u5c0f\u7a0b\u5e8f\u5b66\u751f\u767b\u5f55\u5bc6\u7801\uff08BCrypt\uff09';

delete from sys_role_menu where menu_id in (20112, 201120, 201121);
delete from sys_menu where menu_id in (20112, 201120, 201121);


-- -----------------------------------------------------------------------------
-- [29/51] education_student_account_menu_postgresql.sql
-- -----------------------------------------------------------------------------

insert into sys_menu values(201004, E'\u5f00\u901a\u8d26\u53f7', 20100, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:student:account', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, 201004
from sys_role r
where r.role_key in ('admin', 'edu_admin')
  and not exists (select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = 201004);


-- -----------------------------------------------------------------------------
-- [30/51] education_top_menu_layout_fix_postgresql.sql
-- -----------------------------------------------------------------------------

-- Promote education menu groups to top-level routes (align with frontend /student-affairs/* paths).
-- Hide legacy "学情中心" wrapper (menu_id=1). Root dirs must use Layout, not ParentView.

update sys_menu
set parent_id = 0,
    component = 'Layout',
    order_num = case menu_id
      when 20200 then 1
      when 20201 then 2
      when 20202 then 3
      when 20203 then 4
      when 20204 then 5
      else order_num
    end,
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP
where menu_id in (20200, 20201, 20202, 20203, 20204);

update sys_menu
set visible = '1',
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP
where menu_id = 1;

update sys_menu
set component = 'Layout',
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP
where menu_id = 20205
  and parent_id = 0;


-- -----------------------------------------------------------------------------
-- [31/51] education_class_query_postgresql.sql
-- -----------------------------------------------------------------------------

create table if not exists edu_class_assignment (
    assignment_id      bigserial primary key,
    student_name       varchar(50) not null,
    id_card_no         varchar(32) not null,
    id_card_suffix     varchar(4) not null,
    grade_name         varchar(30),
    class_name         varchar(30),
    classroom          varchar(50),
    head_teacher       varchar(50),
    head_teacher_phone varchar(20),
    status char(1) default '1',
    create_by          varchar(64) default 'admin',
    create_time        timestamp default CURRENT_TIMESTAMP,
    update_by          varchar(64),
    update_time        timestamp,
    remark             varchar(500),
    constraint uk_edu_class_assignment_id_card unique (id_card_no)
);

create index if not exists idx_edu_class_assignment_search
    on edu_class_assignment (student_name, id_card_suffix);

insert into sys_menu values(
    20110, E'\u5206\u73ed\u67e5\u8be2', 20200, 5, 'class-query',
    'education/classQuery/index', '', 'ClassQuery', 1, 0, 'C', '0', '0',
    'education:classQuery:list', 'search', 'admin', CURRENT_TIMESTAMP, '', null,
    E'\u5b66\u751f\u5206\u73ed\u4fe1\u606f\u5bfc\u5165\u4e0e\u67e5\u8be2'
) on conflict (menu_id) do update set
    menu_name = excluded.menu_name,
    parent_id = excluded.parent_id,
    order_num = excluded.order_num,
    path = excluded.path,
    component = excluded.component,
    perms = excluded.perms,
    icon = excluded.icon,
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP,
    remark = excluded.remark;

insert into sys_menu values(201100, E'\u5206\u73ed\u67e5\u8be2\u6743\u9650', 20110, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:classQuery:query', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201101, E'\u5206\u73ed\u5bfc\u5165', 20110, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:classQuery:import', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201102, E'\u5206\u73ed\u5220\u9664', 20110, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:classQuery:remove', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, x.menu_id
from sys_role r
cross join (values
  (20110),
  (201100),(201101),(201102)
) as x(menu_id)
where r.role_key in ('admin', 'edu_admin', 'edu_teacher', 'edu_head_teacher', 'edu_grade_leader')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = x.menu_id
  );


-- -----------------------------------------------------------------------------
-- [32/51] education_class_query_fix_postgresql.sql
-- -----------------------------------------------------------------------------

update sys_menu
set route_name = 'ClassQuery',
    path = 'class-query',
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP
where menu_id = 20110;

update sys_menu
set menu_name = E'\u5206\u73ed\u67e5\u8be2\u6743\u9650',
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP
where menu_id = 201100;


-- -----------------------------------------------------------------------------
-- [33/51] education_class_query_status_postgresql.sql
-- -----------------------------------------------------------------------------

alter table edu_class_assignment add column if not exists status char(1) default '1';

update edu_class_assignment set status = '0' where status is null;

insert into sys_menu values(201103, E'\u5206\u73ed\u4fee\u6539', 20110, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:classQuery:edit', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, 201103
from sys_role r
where r.role_key in ('admin', 'edu_admin')
  and not exists (select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = 201103);


-- -----------------------------------------------------------------------------
-- [34/51] education_class_query_batch_postgresql.sql
-- -----------------------------------------------------------------------------

create table if not exists edu_class_query (
    query_id     bigserial primary key,
    query_name   varchar(100) not null,
    status       char(1) default '1',
    remark       varchar(500),
    create_by    varchar(64) default 'admin',
    create_time  timestamp default CURRENT_TIMESTAMP,
    update_by    varchar(64),
    update_time  timestamp
);

alter table edu_class_assignment add column if not exists query_id bigint;

insert into edu_class_query (query_id, query_name, status, remark, create_by, create_time)
select 1, E'\u9ed8\u8ba4\u5206\u73ed\u67e5\u8be2', '0', E'\u5386\u53f2\u6570\u636e\u8fc1\u79fb', 'admin', CURRENT_TIMESTAMP
where not exists (select 1 from edu_class_query where query_id = 1);

select setval(
    pg_get_serial_sequence('edu_class_query', 'query_id'),
    coalesce((select max(query_id) from edu_class_query), 1),
    true
);

update edu_class_assignment set query_id = 1 where query_id is null;

alter table edu_class_assignment alter column query_id set not null;

alter table edu_class_assignment drop constraint if exists uk_edu_class_assignment_id_card;
alter table edu_class_assignment drop constraint if exists uk_edu_class_query_id_card;
alter table edu_class_assignment add constraint uk_edu_class_query_id_card unique (query_id, id_card_no);
alter table edu_class_assignment drop constraint if exists fk_edu_class_assignment_query;
alter table edu_class_assignment add constraint fk_edu_class_assignment_query
    foreign key (query_id) references edu_class_query (query_id) on delete cascade;

create index if not exists idx_edu_class_assignment_query on edu_class_assignment (query_id);
create index if not exists idx_edu_class_query_status on edu_class_query (status);

insert into sys_menu values(201104, E'\u5206\u73ed\u65b0\u589e', 20110, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:classQuery:add', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, 201104
from sys_role r
where r.role_key in ('admin', 'edu_admin', 'edu_head_teacher', 'edu_grade_leader')
  and not exists (select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = 201104);


-- -----------------------------------------------------------------------------
-- [35/51] education_class_query_dynamic_fields_postgresql.sql
-- -----------------------------------------------------------------------------

alter table edu_class_query add column if not exists field_config text;

alter table edu_class_assignment add column if not exists row_data jsonb;
alter table edu_class_assignment add column if not exists row_key varchar(200);

alter table edu_class_assignment alter column student_name drop not null;
alter table edu_class_assignment alter column id_card_no drop not null;
alter table edu_class_assignment alter column id_card_suffix drop not null;

update edu_class_assignment
set row_data = jsonb_build_object(
        'col_0', student_name,
        'col_1', id_card_no,
        'col_2', grade_name,
        'col_3', class_name,
        'col_4', classroom,
        'col_5', head_teacher,
        'col_6', head_teacher_phone
    ),
    row_key = upper(trim(id_card_no))
where row_data is null
  and student_name is not null
  and id_card_no is not null;

update edu_class_query
set field_config = '[{"key":"col_0","label":"\u59d3\u540d","searchable":true,"uniqueKey":false,"matchMode":"exact","maskMode":"none","orderNum":0},{"key":"col_1","label":"\u8eab\u5206\u8bc1\u53f7\u7801","searchable":true,"uniqueKey":true,"matchMode":"suffix","maskMode":"suffix","orderNum":1},{"key":"col_2","label":"\u5e74\u7ea7","searchable":false,"uniqueKey":false,"matchMode":"exact","maskMode":"none","orderNum":2},{"key":"col_3","label":"\u73ed\u7ea7","searchable":false,"uniqueKey":false,"matchMode":"exact","maskMode":"none","orderNum":3},{"key":"col_4","label":"\u6559\u5ba4","searchable":false,"uniqueKey":false,"matchMode":"exact","maskMode":"none","orderNum":4},{"key":"col_5","label":"\u73ed\u4e3b\u4efb","searchable":false,"uniqueKey":false,"matchMode":"exact","maskMode":"none","orderNum":5},{"key":"col_6","label":"\u73ed\u4e3b\u4efb\u8054\u7cfb\u7535\u8bdd","searchable":false,"uniqueKey":false,"matchMode":"exact","maskMode":"none","orderNum":6}]'
where field_config is null
  and exists (select 1 from edu_class_assignment a where a.query_id = edu_class_query.query_id and a.row_data is not null);

alter table edu_class_assignment drop constraint if exists uk_edu_class_query_id_card;
alter table edu_class_assignment drop constraint if exists uk_edu_class_query_row_key;
alter table edu_class_assignment add constraint uk_edu_class_query_row_key unique (query_id, row_key);

create index if not exists idx_edu_class_assignment_row_data on edu_class_assignment using gin (row_data);


-- -----------------------------------------------------------------------------
-- [36/51] education_class_query_relax_legacy_columns_postgresql.sql
-- -----------------------------------------------------------------------------

-- Relax legacy fixed columns after switching to dynamic row_data storage

alter table edu_class_assignment alter column student_name drop not null;
alter table edu_class_assignment alter column id_card_no drop not null;
alter table edu_class_assignment alter column id_card_suffix drop not null;

alter table edu_class_assignment drop constraint if exists uk_edu_class_assignment_id_card;
alter table edu_class_assignment drop constraint if exists uk_edu_class_query_id_card;


-- -----------------------------------------------------------------------------
-- [37/51] education_exam_knowledge_postgresql.sql
-- -----------------------------------------------------------------------------
