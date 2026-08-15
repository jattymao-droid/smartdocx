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
