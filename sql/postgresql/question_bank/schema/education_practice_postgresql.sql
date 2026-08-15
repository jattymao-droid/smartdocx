-- Phase 3: online practice records, wrong book, weak-point analytics

create table if not exists edu_qb_practice_session (
    session_id       bigserial primary key,
    user_name        varchar(64) not null,
    subject_id       bigint,
    paper_title      varchar(200),
    share_id         varchar(64),
    total_count      int not null default 0,
    correct_count    int not null default 0,
    choice_count     int not null default 0,
    subjective_count int not null default 0,
    duration_sec     int,
    create_time      timestamp default now()
);

create index if not exists idx_qb_practice_session_user on edu_qb_practice_session (user_name, create_time desc);
create index if not exists idx_qb_practice_session_subject on edu_qb_practice_session (user_name, subject_id);

comment on table edu_qb_practice_session is 'Online practice session summary per user';

create table if not exists edu_qb_practice_record (
    record_id        bigserial primary key,
    session_id       bigint not null references edu_qb_practice_session(session_id) on delete cascade,
    user_name        varchar(64) not null,
    question_id      bigint not null,
    subject_id       bigint,
    chapter_id       bigint,
    chapter_text     varchar(500),
    question_type    varchar(32),
    picked_answer    varchar(500),
    correct_flag     char(1) not null default '0',
    create_time      timestamp default now()
);

create index if not exists idx_qb_practice_record_session on edu_qb_practice_record (session_id);
create index if not exists idx_qb_practice_record_user_q on edu_qb_practice_record (user_name, question_id);
create index if not exists idx_qb_practice_record_chapter on edu_qb_practice_record (user_name, subject_id, chapter_id);

comment on table edu_qb_practice_record is 'Per-question practice result within a session';
comment on column edu_qb_practice_record.correct_flag is '1=correct, 0=wrong, 2=subjective/ungraded';

create table if not exists edu_qb_wrong_book (
    wrong_id         bigserial primary key,
    user_name        varchar(64) not null,
    question_id      bigint not null,
    subject_id       bigint,
    chapter_id       bigint,
    chapter_text     varchar(500),
    question_type    varchar(32),
    wrong_count      int not null default 1,
    last_wrong_time  timestamp default now(),
    mastered         char(1) not null default '0',
    create_time      timestamp default now(),
    update_time      timestamp,
    constraint uk_qb_wrong_book_user_q unique (user_name, question_id)
);

create index if not exists idx_qb_wrong_book_user on edu_qb_wrong_book (user_name, mastered, last_wrong_time desc);
create index if not exists idx_qb_wrong_book_chapter on edu_qb_wrong_book (user_name, subject_id, chapter_id);

comment on table edu_qb_wrong_book is 'User wrong-question book aggregated from practice';
comment on column edu_qb_wrong_book.mastered is '0=active wrong, 1=mastered/removed from active list';
