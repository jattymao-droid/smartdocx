-- Hot topic bundles: group library documents for portal display and zip download

create table if not exists edu_library_topic (
    topic_id        bigserial primary key,
    title           varchar(200) not null,
    summary         varchar(1000),
    cover_url       varchar(500),
    bundle_price    numeric(10, 2) not null default 0,
    download_count  integer not null default 0,
    order_num       integer not null default 0,
    status          char(1) not null default '0',
    del_flag        char(1) not null default '0',
    create_by       varchar(64),
    create_time     timestamp default current_timestamp,
    update_by       varchar(64),
    update_time     timestamp,
    remark          varchar(500)
);

create table if not exists edu_library_topic_document (
    id              bigserial primary key,
    topic_id        bigint not null references edu_library_topic(topic_id) on delete cascade,
    document_id     bigint not null references edu_library_document(document_id) on delete cascade,
    order_num       integer not null default 0,
    constraint uk_library_topic_document unique (topic_id, document_id)
);

create index if not exists idx_library_topic_status on edu_library_topic (status, del_flag, order_num);
create index if not exists idx_library_topic_doc_topic on edu_library_topic_document (topic_id, order_num);

-- Admin menu: hot topics under education (menu id 20133 avoids conflict with VIP 20132)
insert into sys_menu values(20133, E'\u70ed\u95e8\u4e13\u9898', 20300, 12, 'libraryTopic', 'education/library/topic', '', '', 1, 0, 'C', '0', '0', 'education:library:topic', 'skill', 'admin', CURRENT_TIMESTAMP, '', null, E'\u6587\u6863\u4e13\u9898\u6253\u5305\u4e0b\u8f7d')
on conflict (menu_id) do update set
    parent_id = 20300,
    order_num = 12,
    path = 'libraryTopic',
    component = 'education/library/topic',
    perms = 'education:library:topic',
    update_by = 'admin',
    update_time = CURRENT_TIMESTAMP;

insert into sys_menu values(201331, E'\u4e13\u9898\u67e5\u8be2', 20133, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:library:topic:query', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201332, E'\u4e13\u9898\u65b0\u589e', 20133, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:library:topic:add', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201333, E'\u4e13\u9898\u4fee\u6539', 20133, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:library:topic:edit', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201334, E'\u4e13\u9898\u5220\u9664', 20133, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:library:topic:remove', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, x.menu_id
from sys_role r
cross join (values (20133), (201331), (201332), (201333), (201334)) as x(menu_id)
where r.role_key in ('admin', 'edu_admin', 'edu_teacher')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = x.menu_id
  );
