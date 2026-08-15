-- Student daily check-in
create table if not exists edu_qb_student_check_in (
    check_id       bigserial primary key,
    user_name      varchar(64) not null,
    check_date     date not null,
    question_count int not null default 0,
    create_time    timestamp default current_timestamp,
    update_time    timestamp,
    constraint uk_qb_student_check_in unique (user_name, check_date)
);

create index if not exists idx_qb_student_check_in_user on edu_qb_student_check_in (user_name, check_date desc);

comment on table edu_qb_student_check_in is 'Student portal daily check-in and practice-day markers';
