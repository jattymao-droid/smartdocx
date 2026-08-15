-- Phase 2: compose template library
create table if not exists edu_qb_compose_template (
    template_id      bigserial primary key,
    template_name    varchar(100) not null,
    template_code    varchar(50) not null,
    scope            varchar(20) not null default 'user',
    subject_id       bigint,
    paper_title      varchar(200),
    difficulty_min   numeric(4,2) default 0.30,
    difficulty_max   numeric(4,2) default 0.70,
    easy_percent     int default 30,
    medium_percent   int default 50,
    hard_percent     int default 20,
    type_rules       jsonb not null,
    status           char(1) not null default '0',
    remark           varchar(500),
    create_by        varchar(64) default '',
    create_time      timestamp default now(),
    update_time      timestamp
);

create index if not exists idx_qb_compose_tpl_scope on edu_qb_compose_template (scope, status);
create index if not exists idx_qb_compose_tpl_user on edu_qb_compose_template (create_by, status);

comment on table edu_qb_compose_template is 'Smart compose paper templates (system + user)';
