-- Server-side paper share snapshots (cross-device share links)

create table if not exists edu_qb_paper_share (
    share_id     varchar(32) primary key,
    snapshot     text not null,
    create_by    varchar(64),
    create_time  timestamp default current_timestamp,
    expire_time  timestamp
);

create index if not exists idx_qb_paper_share_expire on edu_qb_paper_share (expire_time);

comment on table edu_qb_paper_share is 'Shared paper preview snapshots';
