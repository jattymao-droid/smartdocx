-- education library module (P0)

create table if not exists edu_library_category (
    category_id    bigserial primary key,
    parent_id      bigint,
    category_name  varchar(100) not null,
    order_num      integer not null default 0,
    status         char(1) not null default '0',
    create_time    timestamp default current_timestamp
);

create table if not exists edu_library_document (
    document_id       bigserial primary key,
    document_code     varchar(32) not null,
    title             varchar(200) not null,
    summary           varchar(1000),
    file_name         varchar(255) not null,
    file_ext          varchar(16) not null,
    file_size         bigint not null default 0,
    file_url          varchar(500) not null,
    file_storage      varchar(20) not null default 'local',
    cover_url         varchar(500),
    page_count        integer,
    subject_id        bigint references edu_subject(subject_id),
    school_stage      varchar(20),
    version_id        bigint references edu_qb_textbook_version(version_id),
    textbook_id       bigint references edu_qb_textbook(textbook_id),
    chapter_id        bigint references edu_qb_chapter(chapter_id),
    chapter_text      varchar(500),
    category_id       bigint references edu_library_category(category_id),
    tag_names         varchar(500),
    visibility        varchar(20) not null default 'school',
    allow_download    char(1) not null default '1',
    download_count    integer not null default 0,
    view_count        integer not null default 0,
    favorite_count    integer not null default 0,
    convert_status    varchar(20) not null default 'none',
    preview_type      varchar(20),
    preview_url       varchar(500),
    preview_error     varchar(500),
    audit_status      char(1) not null default '1',
    audit_by          varchar(64),
    audit_time        timestamp,
    audit_remark      varchar(500),
    recommend_flag    char(1) not null default '0',
    recommend_order     integer not null default 0,
    status            char(1) not null default '0',
    del_flag          char(1) not null default '0',
    create_by         varchar(64),
    create_time       timestamp default current_timestamp,
    update_by         varchar(64),
    update_time       timestamp,
    remark            varchar(500),
    constraint uk_library_document_code unique (document_code)
);

create index if not exists idx_library_doc_subject on edu_library_document (subject_id, del_flag, audit_status);
create index if not exists idx_library_doc_school_stage on edu_library_document (school_stage, del_flag);
create index if not exists idx_library_doc_version on edu_library_document (version_id, del_flag);
create index if not exists idx_library_doc_textbook on edu_library_document (textbook_id, del_flag);
create index if not exists idx_library_doc_chapter on edu_library_document (chapter_id, del_flag);
create index if not exists idx_library_doc_category on edu_library_document (category_id, del_flag);
create index if not exists idx_library_doc_create_time on edu_library_document (create_time desc);
create index if not exists idx_library_doc_view on edu_library_document (view_count desc);

create table if not exists edu_library_favorite (
    favorite_id   bigserial primary key,
    document_id   bigint not null references edu_library_document(document_id) on delete cascade,
    user_name     varchar(64) not null,
    create_time   timestamp default current_timestamp,
    constraint uk_library_favorite unique (document_id, user_name)
);

insert into edu_library_category (category_id, parent_id, category_name, order_num, status)
values
    (1, null, E'\u8bfe\u4ef6', 1, '0'),
    (2, null, E'\u6559\u6848', 2, '0'),
    (3, null, E'\u8bd5\u5377', 3, '0'),
    (4, null, E'\u8bb2\u4e49', 4, '0'),
    (5, null, E'\u7d20\u6750', 5, '0'),
    (6, null, E'\u5176\u4ed6', 99, '0')
on conflict (category_id) do nothing;

select setval(pg_get_serial_sequence('edu_library_category', 'category_id'), (select coalesce(max(category_id), 1) from edu_library_category));
