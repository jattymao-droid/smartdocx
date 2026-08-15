-- Auto-concat may install_all.sql

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
        (201070),
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


-- ===== education_question_bank_postgresql.sql =====
-- -----------------------------------------------------------------------------
-- education_question_bank_postgresql.sql
-- 题库模块 P0：表结构 + 菜单权限
-- -----------------------------------------------------------------------------

create table if not exists edu_qb_question (
    question_id       bigserial primary key,
    question_code     varchar(32) not null,
    content           text not null,
    options           jsonb,
    correct_answer    jsonb not null,
    subject_id        bigint not null references edu_subject(subject_id),
    chapter_id        bigint,
    chapter_text      varchar(200),
    knowledge_points  jsonb not null default '[]',
    difficulty        numeric(3,2) not null default 0.50,
    question_type     varchar(20) not null,
    source_type       varchar(20) not null default 'manual',
    status            char(1) not null default '0',
    images            jsonb,
    analysis          text,
    content_hash      varchar(64),
    import_task_id    bigint,
    del_flag          char(1) not null default '0',
    create_by         varchar(64),
    create_time       timestamp default current_timestamp,
    update_by         varchar(64),
    update_time       timestamp,
    remark            varchar(500),
    constraint uk_edu_qb_question_code unique (question_code)
);

create index if not exists idx_qb_question_subject on edu_qb_question (subject_id, del_flag);
create index if not exists idx_qb_question_type_diff on edu_qb_question (question_type, difficulty);
create index if not exists idx_qb_question_create_time on edu_qb_question (create_time desc);

create table if not exists edu_qb_knowledge_tag (
    tag_id       bigserial primary key,
    subject_id   bigint not null references edu_subject(subject_id),
    tag_name     varchar(50) not null,
    use_count    integer default 0,
    create_time  timestamp default current_timestamp,
    constraint uk_qb_tag_subject_name unique (subject_id, tag_name)
);

create table if not exists edu_qb_paper (
    paper_id       bigserial primary key,
    paper_title    varchar(200) not null,
    template_code  varchar(20) default 'A4_1COL',
    total_score    numeric(6,2),
    sort_rule      varchar(50),
    export_config  jsonb,
    create_by      varchar(64),
    create_time    timestamp default current_timestamp,
    remark         varchar(500)
);

create table if not exists edu_qb_paper_item (
    item_id        bigserial primary key,
    paper_id       bigint not null references edu_qb_paper(paper_id) on delete cascade,
    question_id    bigint not null references edu_qb_question(question_id),
    order_num      integer not null,
    score_value    numeric(5,2) not null default 5,
    section_name   varchar(100)
);

create index if not exists idx_qb_paper_item_paper on edu_qb_paper_item (paper_id, order_num);

-- 菜单：教学管理(20201) 下题库管理
insert into sys_menu values(20120, E'\u9898\u5e93\u7ba1\u7406', 20201, 6, 'question-bank', 'education/question-bank/index', '', '', 1, 0, 'C', '0', '0', 'education:question:list', 'education', 'admin', CURRENT_TIMESTAMP, '', null, E'\u9898\u5e93\u5f55\u5165\u3001\u68c0\u7d22\u3001\u7ec4\u5377')
on conflict (menu_id) do nothing;

insert into sys_menu values(201201, E'\u9898\u5e93\u67e5\u8be2', 20120, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:question:query', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201202, E'\u9898\u5e93\u65b0\u589e', 20120, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:question:add', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201203, E'\u9898\u5e93\u4fee\u6539', 20120, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:question:edit', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201204, E'\u9898\u5e93\u5220\u9664', 20120, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:question:remove', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201205, E'\u9898\u5e93\u5bfc\u51fa', 20120, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:question:export', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201206, E'\u8bd5\u5377\u9884\u89c8', 20120, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:paper:preview', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, x.menu_id
from sys_role r
cross join (values (20120), (201201), (201202), (201203), (201204), (201205), (201206)) as x(menu_id)
where r.role_key in ('admin', 'edu_admin', 'edu_teacher')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = x.menu_id
  );


-- ===== education_question_bank_textbook_postgresql.sql =====
-- -----------------------------------------------------------------------------
-- education_question_bank_textbook_postgresql.sql
-- �̲İ汾 / �̲� / �½�Ŀ¼
-- -----------------------------------------------------------------------------

create table if not exists edu_qb_textbook_version (
    version_id     bigserial primary key,
    subject_id     bigint not null references edu_subject(subject_id),
    school_stage   varchar(20) not null default E'\u9ad8\u4e2d',
    version_name   varchar(50) not null,
    order_num      integer not null default 0,
    status         char(1) not null default '0',
    create_time    timestamp default current_timestamp,
    constraint uk_qb_version_subject_stage_name unique (subject_id, school_stage, version_name)
);

create table if not exists edu_qb_textbook (
    textbook_id    bigserial primary key,
    version_id     bigint not null references edu_qb_textbook_version(version_id) on delete cascade,
    textbook_name  varchar(80) not null,
    order_num      integer not null default 0,
    status         char(1) not null default '0',
    create_time    timestamp default current_timestamp,
    constraint uk_qb_textbook_version_name unique (version_id, textbook_name)
);

create table if not exists edu_qb_chapter (
    chapter_id     bigserial primary key,
    textbook_id    bigint not null references edu_qb_textbook(textbook_id) on delete cascade,
    parent_id      bigint references edu_qb_chapter(chapter_id) on delete cascade,
    chapter_name   varchar(120) not null,
    order_num      integer not null default 0,
    create_time    timestamp default current_timestamp
);

create index if not exists idx_qb_chapter_textbook on edu_qb_chapter (textbook_id, parent_id, order_num);

alter table edu_qb_question add column if not exists textbook_id bigint references edu_qb_textbook(textbook_id);
create index if not exists idx_qb_question_textbook on edu_qb_question (textbook_id, chapter_id);

insert into edu_qb_textbook_version (subject_id, version_name, order_num)
select s.subject_id, v.version_name, v.order_num
from edu_subject s
cross join (values
    ('�˽̰�', 1),
    ('���̰�', 2),
    ('³�ư�', 3),
    ('���ƽ̰�', 4),
    ('�̿ư�', 5)
) as v(version_name, order_num)
where s.subject_name = '����'
on conflict (subject_id, version_name) do nothing;

insert into edu_qb_textbook (version_id, textbook_name, order_num)
select v.version_id, t.textbook_name, t.order_num
from edu_qb_textbook_version v
join edu_subject s on s.subject_id = v.subject_id and s.subject_name = '����'
cross join (values
    ('���� ��һ��', 1),
    ('���� �ڶ���', 2),
    ('���� ������', 3),
    ('ѡ���Ա��� ��һ��', 4),
    ('ѡ���Ա��� �ڶ���', 5),
    ('ѡ���Ա��� ������', 6)
) as t(textbook_name, order_num)
where v.version_name = '�˽̰�'
on conflict (version_id, textbook_name) do nothing;

insert into edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
select tb.textbook_id, null, c.chapter_name, c.order_num
from edu_qb_textbook tb
join edu_qb_textbook_version v on v.version_id = tb.version_id
join edu_subject s on s.subject_id = v.subject_id
cross join (values
    ('��һ�� �˶�������', 1),
    ('�ڶ��� �ȱ���ֱ���˶����о�', 2),
    ('������ �໥���á�����', 3),
    ('������ �˶������Ĺ�ϵ', 4)
) as c(chapter_name, order_num)
where s.subject_name = '����' and v.version_name = '�˽̰�' and tb.textbook_name = '���� ��һ��'
  and not exists (
    select 1 from edu_qb_chapter ec
    where ec.textbook_id = tb.textbook_id and ec.parent_id is null and ec.chapter_name = c.chapter_name
  );

insert into edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
select p.textbook_id, p.chapter_id, s.section_name, s.order_num
from edu_qb_chapter p
join edu_qb_textbook tb on tb.textbook_id = p.textbook_id
join edu_qb_textbook_version v on v.version_id = tb.version_id
join edu_subject sub on sub.subject_id = v.subject_id
cross join (values
    ('��һ�� �˶�������', '1. �ʵ� �ο�ϵ', 1),
    ('��һ�� �˶�������', '2. ʱ�� λ��', 2),
    ('��һ�� �˶�������', '3. λ�ñ仯���������������ٶ�', 3),
    ('��һ�� �˶�������', '4. �ٶȱ仯�����������������ٶ�', 4),
    ('�ڶ��� �ȱ���ֱ���˶����о�', '1. ʵ�飺̽��С���ٶ���ʱ��仯�Ĺ���', 1),
    ('�ڶ��� �ȱ���ֱ���˶����о�', '2. �ȱ���ֱ���˶����ٶ���ʱ��Ĺ�ϵ', 2),
    ('�ڶ��� �ȱ���ֱ���˶����о�', '3. �ȱ���ֱ���˶���λ����ʱ��Ĺ�ϵ', 3),
    ('�ڶ��� �ȱ���ֱ���˶����о�', '4. ���������˶�', 4)
) as s(chapter_name, section_name, order_num)
where sub.subject_name = '����' and v.version_name = '�˽̰�' and tb.textbook_name = '���� ��һ��'
  and p.parent_id is null and p.chapter_name = s.chapter_name
  and not exists (
    select 1 from edu_qb_chapter ec
    where ec.textbook_id = p.textbook_id and ec.parent_id = p.chapter_id and ec.chapter_name = s.section_name
  );

insert into edu_qb_textbook_version (subject_id, version_name, order_num)
select s.subject_id, '�˽̰�', 1
from edu_subject s where s.subject_name = '��ѧ'
on conflict (subject_id, version_name) do nothing;

insert into edu_qb_textbook (version_id, textbook_name, order_num)
select v.version_id, '���� ��һ��', 1
from edu_qb_textbook_version v
join edu_subject s on s.subject_id = v.subject_id
where s.subject_name = '��ѧ' and v.version_name = '�˽̰�'
on conflict (version_id, textbook_name) do nothing;

insert into edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
select tb.textbook_id, null, c.chapter_name, c.order_num
from edu_qb_textbook tb
join edu_qb_textbook_version v on v.version_id = tb.version_id
join edu_subject s on s.subject_id = v.subject_id
cross join (values
    ('��һ�� �����볣���߼�����', 1),
    ('�ڶ��� һԪ���κ��������̺Ͳ���ʽ', 2),
    ('������ �����ĸ���������', 3)
) as c(chapter_name, order_num)
where s.subject_name = '��ѧ' and v.version_name = '�˽̰�' and tb.textbook_name = '���� ��һ��'
  and not exists (
    select 1 from edu_qb_chapter ec
    where ec.textbook_id = tb.textbook_id and ec.parent_id is null and ec.chapter_name = c.chapter_name
  );


-- ===== education_qb_textbook_stage_postgresql.sql =====
-- -----------------------------------------------------------------------------
-- education_qb_textbook_stage_postgresql.sql
-- Add school_stage (学段) to textbook version catalog
-- -----------------------------------------------------------------------------

alter table edu_qb_textbook_version
    add column if not exists school_stage varchar(20) not null default E'\u9ad8\u4e2d';

alter table edu_qb_textbook_version drop constraint if exists uk_qb_version_subject_name;
alter table edu_qb_textbook_version drop constraint if exists uk_qb_version_subject_stage_name;

-- migrate suffix from scraped version names
update edu_qb_textbook_version
set school_stage = E'\u521d\u4e2d',
    version_name = regexp_replace(version_name, E'\uff08\u521d\u4e2d\uff09$', '')
where version_name like E'%\uff08\u521d\u4e2d\uff09';

update edu_qb_textbook_version
set school_stage = E'\u9ad8\u4e2d',
    version_name = regexp_replace(version_name, E'\uff08\u9ad8\u4e2d\uff09$', '')
where version_name like E'%\uff08\u9ad8\u4e2d\uff09';

alter table edu_qb_textbook_version
    add constraint uk_qb_version_subject_stage_name
    unique (subject_id, school_stage, version_name);


-- ===== education_qb_question_type_postgresql.sql =====
-- Question type registry for question bank (题型管理)
create table if not exists edu_qb_question_type (
    type_id         bigserial primary key,
    type_code       varchar(32) not null,
    type_name       varchar(50) not null,
    answer_mode     varchar(20) not null default 'subjective',
    content_max_len integer,
    order_num       integer default 0,
    status          char(1) default '0',
    builtin         char(1) default '0',
    remark          varchar(500),
    create_by       varchar(64),
    create_time     timestamp,
    update_by       varchar(64),
    update_time     timestamp,
    constraint uk_edu_qb_question_type_code unique (type_code)
);

comment on table edu_qb_question_type is E'\u9898\u5e93\u9898\u578b\u914d\u7f6e';
comment on column edu_qb_question_type.type_code is E'\u9898\u578b\u7f16\u7801\uff08\u5b58\u5165\u9898\u76ee question_type\uff09';
comment on column edu_qb_question_type.type_name is E'\u9898\u578b\u540d\u79f0';
comment on column edu_qb_question_type.answer_mode is E'\u7b54\u9898\u6a21\u5f0f\uff1achoice/multi/judge/fill/subjective';
comment on column edu_qb_question_type.content_max_len is E'\u9898\u5e72\u957f\u5ea6\u4e0a\u9650\uff08\u7a7a\u5219\u7528\u9ed8\u8ba4\uff09';
comment on column edu_qb_question_type.builtin is E'1=\u5185\u7f6e\u9898\u578b\uff0c\u4e0d\u53ef\u5220\u9664';

insert into edu_qb_question_type (type_code, type_name, answer_mode, content_max_len, order_num, status, builtin, create_by, create_time)
select v.type_code, v.type_name, v.answer_mode, v.content_max_len, v.order_num, '0', '1', 'admin', CURRENT_TIMESTAMP
from (values
    ('single', E'\u5355\u9009', 'choice', null, 1),
    ('multi', E'\u591a\u9009', 'multi', null, 2),
    ('fill', E'\u586b\u7a7a', 'fill', null, 3),
    ('experiment', E'\u5b9e\u9a8c', 'subjective', null, 4),
    ('answer', E'\u89e3\u7b54', 'subjective', null, 5),
    ('comprehensive', E'\u7efc\u5408', 'subjective', null, 6),
    ('reading', E'\u9605\u8bfb', 'subjective', 10000, 7),
    ('judge', E'\u5224\u65ad', 'judge', null, 8),
    ('drawing', E'\u4f5c\u56fe', 'subjective', null, 9),
    ('knowledge_fill', E'\u77e5\u8bc6\u586b\u7a7a', 'fill', null, 10),
    ('short', E'\u7b80\u7b54', 'subjective', null, 11)
) as v(type_code, type_name, answer_mode, content_max_len, order_num)
where not exists (select 1 from edu_qb_question_type t where t.type_code = v.type_code);


-- ===== education_question_bank_p1_import_postgresql.sql =====
-- -----------------------------------------------------------------------------
-- education_question_bank_p1_import_postgresql.sql
-- P1: DOCX 导入任务表 + 导入权限
-- -----------------------------------------------------------------------------

create table if not exists edu_qb_import_task (
    task_id          bigserial primary key,
    file_name        varchar(200) not null,
    file_path        varchar(500) not null,
    subject_id       bigint references edu_subject(subject_id),
    status           varchar(20) not null default 'parsed',
    block_count      integer default 0,
    imported_count   integer default 0,
    parse_result     jsonb,
    create_by        varchar(64),
    create_time      timestamp default current_timestamp,
    update_time      timestamp,
    remark           varchar(500)
);

create index if not exists idx_qb_import_task_create on edu_qb_import_task (create_time desc);

insert into sys_menu values(201207, E'\u9898\u5e93\u5bfc\u5165', 20120, 7, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:question:import', '#', 'admin', CURRENT_TIMESTAMP, '', null, E'DOCX \u6bb5\u843d\u5bfc\u5165')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, 201207
from sys_role r
where r.role_key in ('admin', 'edu_admin', 'edu_teacher')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = 201207
  );


-- ===== education_question_bank_p2_ocr_postgresql.sql =====
-- -----------------------------------------------------------------------------
-- education_question_bank_p2_ocr_postgresql.sql
-- P2: OCR draft table for mobile / web photo import
-- -----------------------------------------------------------------------------

create table if not exists edu_qb_ocr_draft (
    draft_id              bigserial primary key,
    image_path            varchar(500) not null,
    ocr_text              text,
    ocr_lines             jsonb,
    confidence            numeric(5,4),
    predicted_type        varchar(20),
    predicted_difficulty  numeric(3,2),
    predicted_options     jsonb,
    subject_id            bigint references edu_subject(subject_id),
    status                varchar(20) not null default 'draft',
    question_id           bigint references edu_qb_question(question_id),
    create_by             varchar(64),
    create_time           timestamp default current_timestamp,
    update_time           timestamp,
    remark                varchar(500)
);

create index if not exists idx_qb_ocr_draft_create on edu_qb_ocr_draft (create_time desc);
create index if not exists idx_qb_ocr_draft_status on edu_qb_ocr_draft (status, create_by);


-- ===== education_question_bank_p2_ocr_figure_patch.sql =====
-- Persist manually cropped figure path on OCR draft (fallback when commit body omits images)
alter table edu_qb_ocr_draft
    add column if not exists figure_path varchar(500);


-- ===== education_question_bank_p3_dedup_postgresql.sql =====
-- -----------------------------------------------------------------------------
-- education_question_bank_p3_dedup_postgresql.sql
-- P3 Sprint 3: content_hash index for duplicate detection
-- -----------------------------------------------------------------------------

create index if not exists idx_qb_question_subject_hash
    on edu_qb_question (subject_id, content_hash)
    where del_flag = '0' and content_hash is not null;

-- Backfill content_hash for existing rows (normalized stem SHA-256 computed in app on next edit;
-- optional one-time update can be run via admin tool later).


-- ===== education_question_bank_p3_audit_postgresql.sql =====
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


-- ===== education_question_bank_create_menu_postgresql.sql =====
-- -----------------------------------------------------------------------------
-- education_question_bank_create_menu_postgresql.sql
-- 左侧菜单：教学管理 -> 新增试题
-- -----------------------------------------------------------------------------

insert into sys_menu values(
  20122,
  E'\u65b0\u589e\u8bd5\u9898',
  20201,
  7,
  'question-create',
  'education/question-bank/QuestionCreate',
  '',
  '',
  1,
  0,
  'C',
  '0',
  '0',
  'education:question:add',
  'edit',
  'admin',
  CURRENT_TIMESTAMP,
  '',
  null,
  E'\u624b\u52a8\u5f55\u5165\u9898\u5e72\u3001\u9009\u9879\u4e0e\u7b54\u6848'
)
on conflict (menu_id) do update set
  menu_name = excluded.menu_name,
  parent_id = excluded.parent_id,
  order_num = excluded.order_num,
  path = excluded.path,
  component = excluded.component,
  perms = excluded.perms,
  icon = excluded.icon,
  remark = excluded.remark,
  update_by = 'admin',
  update_time = CURRENT_TIMESTAMP;

-- 教材目录排在「新增试题」之后
update sys_menu
set order_num = 8, update_by = 'admin', update_time = CURRENT_TIMESTAMP
where menu_id = 20121 and order_num < 8;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, 20122
from sys_role r
where r.role_key in ('admin', 'edu_admin', 'edu_teacher')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = 20122
  );


-- ===== education_question_bank_create_ownership_postgresql.sql =====
-- -----------------------------------------------------------------------------
-- education_question_bank_create_ownership_postgresql.sql
-- 导入/审核权限归属「新增试题」(20122)
-- -----------------------------------------------------------------------------

update sys_menu
set parent_id = 20122,
    order_num = 1,
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP
where menu_id = 201207;

update sys_menu
set parent_id = 20122,
    order_num = 2,
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP
where menu_id = 201208;

-- 按钮权限排序：查询/新增/修改等仍挂在题库管理下
update sys_menu
set order_num = 3, update_by = 'admin', update_time = CURRENT_TIMESTAMP
where menu_id = 201201;
update sys_menu
set order_num = 4, update_by = 'admin', update_time = CURRENT_TIMESTAMP
where menu_id = 201202;
update sys_menu
set order_num = 5, update_by = 'admin', update_time = CURRENT_TIMESTAMP
where menu_id = 201203;
update sys_menu
set order_num = 6, update_by = 'admin', update_time = CURRENT_TIMESTAMP
where menu_id = 201204;
update sys_menu
set order_num = 7, update_by = 'admin', update_time = CURRENT_TIMESTAMP
where menu_id = 201205;
update sys_menu
set order_num = 8, update_by = 'admin', update_time = CURRENT_TIMESTAMP
where menu_id = 201206;


-- ===== education_qb_feedback_postgresql.sql =====
-- Question feedback / error report for compose workflow
create table if not exists edu_qb_question_feedback (
    feedback_id   bigserial primary key,
    question_id   bigint not null references edu_qb_question(question_id),
    feedback_type varchar(32) not null,
    content       text,
    paper_title   varchar(200),
    status        char(1) default '0',
    create_by     varchar(64),
    create_time   timestamp default current_timestamp
);

create index if not exists idx_qb_feedback_question on edu_qb_question_feedback (question_id, create_time desc);


-- ===== education_textbook_catalog_menu_postgresql.sql =====
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


-- ===== education_renjiao_physics_catalog_import.sql =====
-- auto-generated from dzkbw.com renjiao high-school physics
DO $$
DECLARE
  v_subject_id bigint;
  v_version_id bigint;
  v_textbook_id bigint;
  v_chapter_id bigint;
BEGIN
  SELECT subject_id INTO v_subject_id FROM edu_subject WHERE subject_name = '物理' LIMIT 1;
  IF v_subject_id IS NULL THEN
    RAISE EXCEPTION 'subject physics not found';
  END IF;
  UPDATE edu_qb_question q
  SET textbook_id = NULL, chapter_id = NULL
  FROM edu_qb_textbook t, edu_qb_textbook_version v
  WHERE q.textbook_id = t.textbook_id
    AND t.version_id = v.version_id
    AND v.subject_id = v_subject_id;
  DELETE FROM edu_qb_chapter c
  USING edu_qb_textbook t, edu_qb_textbook_version v
  WHERE c.textbook_id = t.textbook_id
    AND t.version_id = v.version_id
    AND v.subject_id = v_subject_id;
  DELETE FROM edu_qb_textbook t
  USING edu_qb_textbook_version v
  WHERE t.version_id = v.version_id AND v.subject_id = v_subject_id;
  DELETE FROM edu_qb_textbook_version WHERE subject_id = v_subject_id;
  INSERT INTO edu_qb_textbook_version (subject_id, school_stage, version_name, order_num, status)
  VALUES (v_subject_id, '高中', '人教版(2019)', 1, '0')
  RETURNING version_id INTO v_version_id;
  INSERT INTO edu_qb_textbook (version_id, textbook_name, order_num, status)
  VALUES (v_version_id, '必修 第一册', 1, '0')
  RETURNING textbook_id INTO v_textbook_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '序言 物理学：研究物质及其运动规律的科学', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第一章 运动的描述', 2)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.质点 参考系', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.时间 位移', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.位置变化快慢的描述--速度', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.速度变化快慢的描述--加速度', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第二章 匀变速直线运动的研究', 3)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.实验：探究小车速度随时间变化的规律', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.匀变速直线运动的速度与时间的关系', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.匀变速直线运动的位移与时间的关系', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.自由落体运动', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第三章 相互作用--力', 4)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.重力与弹力', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.摩擦力', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.牛顿第三定律', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.力的合成和分解', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.共点力的平衡', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第四章 运动和力的关系', 5)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.牛顿第一定律', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.实验：探究加速度与力、质量的关系', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.牛顿第二定律', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.力学单位制', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.牛顿运动定律的应用', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '6.超重和失重', 6);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '课题研究', 6);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '学生实验', 7);
  INSERT INTO edu_qb_textbook (version_id, textbook_name, order_num, status)
  VALUES (v_version_id, '必修 第二册', 2, '0')
  RETURNING textbook_id INTO v_textbook_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第五章 抛体运动', 1)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.曲线运动', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.运动的合成与分解', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.实验：探究平抛运动的特点', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.抛体运动的规律', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第六章 圆周运动', 2)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.圆周运动', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.向心力', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.向心加速度', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.生活中的圆周运动', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第七章 万有引力与宇宙航行', 3)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.行星的运动', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.万有引力定律', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.万有引力理论的成就', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.宇宙航行', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.相对论时空观与牛顿力学的局限性', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第八章 机械能守恒定律', 4)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.功与功率', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.重力势能', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.动能和动能定理', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.机械能守恒定律', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.实验：验证机械能守恒定律', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '课题研究', 5);
  INSERT INTO edu_qb_textbook (version_id, textbook_name, order_num, status)
  VALUES (v_version_id, '必修 第三册', 3, '0')
  RETURNING textbook_id INTO v_textbook_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第九章 静电场及其应用', 1)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.电荷', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.库仑定律', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.电场 电场强度', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.静电的防止与利用', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第十章 静电场中的能量', 2)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.电势能和电势', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.电势差', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.电势差与电场强度的关系', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.电容器的电容', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.带电粒子在电场中的运动', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第十一章 电路及其应用', 3)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.电源和电流', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.导体的电阻', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.实验：导体电阻率的测量', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.串联电路和并联电路', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.实验：练习使用多用电表', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第十二章 电能 能量守恒定律', 4)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.电路中的能量转化', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.闭合电路的欧姆定律', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.实验：电池电动势和内阻的测量', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.能源与可持续发展', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第十三章 电磁感应与电磁波初步', 5)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.磁场 磁感线', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.磁感应强度 磁通量', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.电磁感应现象及应用', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.电磁波的发现及应用', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.能量量子化', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '课题研究', 6);
  INSERT INTO edu_qb_textbook (version_id, textbook_name, order_num, status)
  VALUES (v_version_id, '选择性必修 第一册', 4, '0')
  RETURNING textbook_id INTO v_textbook_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第一章 动量守恒定律', 1)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.动量', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.动量定理', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.动量守恒定律', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.实验：验证动量守恒定律', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.弹性碰撞和非弹性碰撞', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '6.反冲现象 火箭', 6);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第二章 机械振动', 2)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.简谐运动', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.简谐运动的描述', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.简谐运动的回复力和能量', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.单摆', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.实验：用单摆测量重力加速度', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '6.受迫振动 共振', 6);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第三章 机械波', 3)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.波的形成', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.波的描述', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.波的反射、折射和衍射', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.波的干涉', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.多普勒效应', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第四章 光', 4)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.光的折射', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.全反射', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.光的干涉', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.实验：用双缝干涉测量光的波长', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.光的衍射', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '6.光的偏振 激光', 6);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '课题研究', 5);
  INSERT INTO edu_qb_textbook (version_id, textbook_name, order_num, status)
  VALUES (v_version_id, '选择性必修 第二册', 5, '0')
  RETURNING textbook_id INTO v_textbook_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第一章 安培力与洛伦兹力', 1)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.磁场对通电导线的作用力', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.磁场对运动电荷的作用力', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.带电粒子在匀强磁场中的运动', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.质谱仪与回旋加速器', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第二章 电磁感应', 2)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.楞次定律', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.法拉第电磁感应定律', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.涡流、电磁阻尼和电磁驱动', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.互感和自感', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第三章 交变电流', 3)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.交变电流', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.交变电流的描述', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.变压器', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.电能的输送', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第四章 电磁振荡与电磁波', 4)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.电磁振荡', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.电磁场与电磁波', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.无线电波的发射和接收', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.电磁波谱', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第五章 传感器', 5)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.认识传感器', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.常见传感器的工作原理及应用', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.利用传感器制作简单的自动控制装置', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '课题研究', 6);
  INSERT INTO edu_qb_textbook (version_id, textbook_name, order_num, status)
  VALUES (v_version_id, '选择性必修 第三册', 6, '0')
  RETURNING textbook_id INTO v_textbook_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第一章 分子动理论', 1)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.分子动理论的基本内容', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.实验：用油膜法估测油酸分子的大小', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.分子运动速率分布规律', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.分子动能和分子势能', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第二章 气体、固体和液体', 2)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.温度和温标', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.气体的等温变化', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.气体的等压变化和等容变化', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.固体', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.液体', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第三章 热力学定律', 3)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.功、热和内能的改变', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.热力学第一定律', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.能量守恒定律', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.热力学第二定律', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第四章 原子结构和波粒二象性', 4)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.普朗克黑体辐射理论', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.光电效应', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.原子的核式结构模型', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.氢原子光谱和玻尔的原子模型', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.粒子的波动性和量子力学的建立', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '第五章 原子核', 5)
  RETURNING chapter_id INTO v_chapter_id;
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '1.原子核的组成', 1);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '2.放射性元素的衰变', 2);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '3.核力与结合能', 3);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '4.核裂变与核聚变', 4);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, v_chapter_id, '5.“基本”粒子', 5);
  INSERT INTO edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
  VALUES (v_textbook_id, NULL, '课题研究', 6);
END $$;

-- -----------------------------------------------------------------------------
-- [52/52] education_exam_paper_postgresql.sql + portal banner menu
-- -----------------------------------------------------------------------------

alter table edu_qb_paper add column if not exists paper_type varchar(20) not null default 'user';
alter table edu_qb_paper add column if not exists subject_id bigint;
alter table edu_qb_paper add column if not exists exam_category varchar(50);
alter table edu_qb_paper add column if not exists exam_year varchar(10);
alter table edu_qb_paper add column if not exists region varchar(50);
alter table edu_qb_paper add column if not exists grade varchar(20);
alter table edu_qb_paper add column if not exists publish_status char(1) not null default '0';
alter table edu_qb_paper add column if not exists source_file varchar(500);

create index if not exists idx_qb_paper_exam on edu_qb_paper (paper_type, publish_status, exam_category);

insert into sys_menu values(
  20125, E'\u8bd5\u5377\u9009\u9898\u7ba1\u7406', 20300, 8,
  'exam-paper', 'education/exam-paper/index', '', '', 1, 0, 'C', '0', '0',
  'education:exam-paper:list', 'documentation', 'admin', CURRENT_TIMESTAMP, '', null,
  E'\u4e0a\u4f20\u8bd5\u5377\u3001\u667a\u80fd\u6807\u8bb0\u9898\u76ee\u3001\u53d1\u5e03\u5230\u95e8\u6237'
) on conflict (menu_id) do nothing;

insert into sys_menu values(201251, E'\u8bd5\u5377\u67e5\u8be2', 20125, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:exam-paper:query', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201252, E'\u8bd5\u5377\u4e0a\u4f20', 20125, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:exam-paper:add', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201253, E'\u8bd5\u5377\u4fee\u6539', 20125, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:exam-paper:edit', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201254, E'\u8bd5\u5377\u5220\u9664', 20125, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:exam-paper:remove', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201255, E'\u95e8\u6237\u8bd5\u5377\u67e5\u8be2', 20125, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:exam-paper:portal', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
cross join (values (20125),(201251),(201252),(201253),(201254),(201255)) as m(menu_id)
where r.role_key in ('admin', 'edu_admin', 'edu_teacher')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = m.menu_id
  );

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
select r.role_id, m.menu_id
from sys_role r
cross join (values (20124), (201241), (201242)) as m(menu_id)
where r.role_key in ('admin', 'edu_admin', 'edu_teacher')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = m.menu_id
  );

-- -----------------------------------------------------------------------------
-- [53/53] education_paper_share_postgresql.sql
-- -----------------------------------------------------------------------------

create table if not exists edu_qb_paper_share (
    share_id     varchar(32) primary key,
    snapshot     text not null,
    create_by    varchar(64),
    create_time  timestamp default current_timestamp,
    expire_time  timestamp
);

create index if not exists idx_qb_paper_share_expire on edu_qb_paper_share (expire_time);

-- -----------------------------------------------------------------------------
-- [54/54] edu_portal_banner_config_postgresql.sql
-- -----------------------------------------------------------------------------

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 9, E'\u95e8\u6237\u9996\u9875-Banner\u6a21\u5f0f', 'portal.home.banner.mode', 'none', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'none=\u9ed8\u8ba4\u6e10\u53d8, image=\u80cc\u666f\u56fe, video=\u80cc\u666f\u89c6\u9891'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.mode');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 10, E'\u95e8\u6237\u9996\u9875-Banner\u56fe\u7247', 'portal.home.banner.imageUrl', '', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'\u56fe\u7247URL\uff0c\u5982 /profile/upload/... \u6216\u5b8c\u6574 http \u5730\u5740'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.imageUrl');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 11, E'\u95e8\u6237\u9996\u9875-Banner\u89c6\u9891', 'portal.home.banner.videoUrl', '', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'mp4/webm \u89c6\u9891\u5730\u5740'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.videoUrl');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 12, E'\u95e8\u6237\u9996\u9875-Banner\u89c6\u9891\u5c01\u9762', 'portal.home.banner.videoPoster', '', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'\u89c6\u9891\u52a0\u8f7d\u524d\u5c01\u9762\u56fe'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.videoPoster');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT 13, E'\u95e8\u6237\u9996\u9875-Banner\u906e\u7f69', 'portal.home.banner.overlay', '0.42', 'Y', 'admin', CURRENT_TIMESTAMP, '', NULL, E'0-1\uff0c\u8d8a\u5927\u6587\u5b57\u5bf9\u6bd4\u8d8a\u5f3a\uff0c\u5efa\u8bae 0.3-0.55'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'portal.home.banner.overlay');

-- -----------------------------------------------------------------------------
-- [55/55] patch_portal_teacher_subject.sql (create edu_teacher + portal menus)
-- -----------------------------------------------------------------------------

DO $$
DECLARE
    role_id_teacher bigint;
BEGIN
    SELECT role_id INTO role_id_teacher
    FROM sys_role
    WHERE role_key = 'edu_teacher' AND del_flag = '0';

    IF role_id_teacher IS NULL THEN
        INSERT INTO sys_role (
            role_name, role_key, role_sort, data_scope,
            menu_check_strictly, dept_check_strictly,
            status, del_flag, create_by, create_time, remark
        )
        VALUES (
            E'\u4efb\u8bfe\u6559\u5e08', 'edu_teacher', 4, '1',
            1, 1, '0', '0', 'admin', CURRENT_TIMESTAMP,
            E'\u95e8\u6237\u6559\u5e08\u89d2\u8272'
        )
        RETURNING role_id INTO role_id_teacher;
    END IF;

    INSERT INTO sys_role_menu (role_id, menu_id)
    SELECT role_id_teacher, m.menu_id
    FROM (VALUES
        (201070),
        (20120), (201201), (201202), (201203), (201204), (201205), (201206),
        (20125), (201251), (201252), (201253), (201254), (201255)
    ) AS m(menu_id)
    WHERE NOT EXISTS (
        SELECT 1 FROM sys_role_menu rm
        WHERE rm.role_id = role_id_teacher AND rm.menu_id = m.menu_id
    );
END $$;

