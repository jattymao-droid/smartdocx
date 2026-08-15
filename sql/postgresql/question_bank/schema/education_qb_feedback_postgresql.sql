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
