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
