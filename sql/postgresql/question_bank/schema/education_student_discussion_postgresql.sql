-- Student discussion community tables
create table if not exists edu_qb_discussion_thread (
    thread_id    bigserial primary key,
    subject_id   bigint,
    question_id  bigint references edu_qb_question(question_id),
    title        varchar(200) not null,
    content      text not null,
    user_name    varchar(64) not null,
    nick_name    varchar(64),
    reply_count  int not null default 0,
    status       char(1) not null default '0',
    del_flag     char(1) not null default '0',
    create_time  timestamp default current_timestamp,
    update_time  timestamp
);

create index if not exists idx_qb_discussion_thread_subject on edu_qb_discussion_thread (subject_id, create_time desc);
create index if not exists idx_qb_discussion_thread_question on edu_qb_discussion_thread (question_id, create_time desc);

comment on table edu_qb_discussion_thread is 'Student discussion threads';

create table if not exists edu_qb_discussion_reply (
    reply_id         bigserial primary key,
    thread_id        bigint not null references edu_qb_discussion_thread(thread_id) on delete cascade,
    parent_reply_id  bigint,
    content          text not null,
    user_name        varchar(64) not null,
    nick_name        varchar(64),
    del_flag         char(1) not null default '0',
    create_time      timestamp default current_timestamp
);

create index if not exists idx_qb_discussion_reply_thread on edu_qb_discussion_reply (thread_id, create_time);

comment on table edu_qb_discussion_reply is 'Replies under discussion threads';
