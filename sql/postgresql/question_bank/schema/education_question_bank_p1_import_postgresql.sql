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
