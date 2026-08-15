-- P2: reading progress / continue reading

create table if not exists edu_library_read_log (
    log_id         bigserial primary key,
    document_id    bigint not null references edu_library_document(document_id) on delete cascade,
    user_name      varchar(64) not null,
    read_progress  numeric(5, 2) not null default 0,
    update_time    timestamp default current_timestamp,
    constraint uk_library_read_log unique (document_id, user_name)
);

create index if not exists idx_library_read_log_user on edu_library_read_log (user_name, update_time desc);
