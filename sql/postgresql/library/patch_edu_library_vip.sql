-- Library VIP membership module
-- Run: psql -U postgres -d ry_cloud -f patch_edu_library_vip.sql

create table if not exists edu_library_vip_member (
    vip_id       bigserial primary key,
    username     varchar(64) not null,
    plan_code    varchar(32) not null default 'monthly',
    status       char(1) not null default '0',
    start_time   timestamp not null,
    expire_time  timestamp not null,
    source       varchar(16) not null default 'pay',
    order_no     varchar(64),
    remark       varchar(500),
    create_by    varchar(64),
    create_time  timestamp default current_timestamp,
    update_by    varchar(64),
    update_time  timestamp,
    constraint uk_edu_library_vip_username unique (username)
);

create index if not exists idx_edu_library_vip_expire on edu_library_vip_member (status, expire_time);

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 930, E'\u6587\u5e93-VIP\u5f00\u5173', 'edu.library.vip.enabled', 'false', 'Y', 'admin', current_timestamp, '', null, E'\u662f\u5426\u5f00\u653e\u6587\u5e93 VIP \u4f1a\u5458'
where not exists (select 1 from sys_config where config_key = 'edu.library.vip.enabled');

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 931, E'\u6587\u5e93-VIP\u4ef7\u683c', 'edu.library.vip.price', '29.00', 'Y', 'admin', current_timestamp, '', null, E'VIP \u4f1a\u5458\u4ef7\u683c\uff08\u5143\uff09'
where not exists (select 1 from sys_config where config_key = 'edu.library.vip.price');

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 932, E'\u6587\u5e93-VIP\u65f6\u957f', 'edu.library.vip.duration-days', '30', 'Y', 'admin', current_timestamp, '', null, E'VIP \u6709\u6548\u5929\u6570'
where not exists (select 1 from sys_config where config_key = 'edu.library.vip.duration-days');

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 933, E'\u6587\u5e93-VIP\u514d\u8d39\u4e0b\u8f7d', 'edu.library.vip.free-download', 'true', 'Y', 'admin', current_timestamp, '', null, E'VIP \u4f1a\u5458\u662f\u5426\u514d\u8d39\u4e0b\u8f7d\u4ed8\u8d39\u6587\u6863'
where not exists (select 1 from sys_config where config_key = 'edu.library.vip.free-download');

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 934, E'\u6587\u5e93-VIP\u9884\u89c8\u9875\u6570', 'edu.library.vip.preview-pages', '0', 'Y', 'admin', current_timestamp, '', null, E'VIP \u9884\u89c8\u9875\u6570\u4e0a\u9650\uff080 \u8868\u793a\u4f7f\u7528\u9ed8\u8ba4\u503c\uff09'
where not exists (select 1 from sys_config where config_key = 'edu.library.vip.preview-pages');

insert into sys_menu values(20132, E'VIP\u4f1a\u5458\u7ba1\u7406', 20300, 11, 'libraryVip', 'education/library/vip', '', '', 1, 0, 'C', '0', '0', 'education:library:vip', 'peoples', 'admin', CURRENT_TIMESTAMP, '', null, E'\u6587\u5e93 VIP \u4f1a\u5458\u7ba1\u7406')
on conflict (menu_id) do nothing;

insert into sys_menu values(201321, E'VIP\u67e5\u8be2', 20132, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:library:vip', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_menu values(201322, E'VIP\u6388\u4e88', 20132, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:library:vip', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_menu values(201323, E'VIP\u7eed\u671f', 20132, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:library:vip', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_menu values(201324, E'VIP\u505c\u7528', 20132, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:library:vip', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, x.menu_id
from sys_role r
cross join (values (20132), (201321), (201322), (201323), (201324)) as x(menu_id)
where r.role_key in ('admin', 'edu_admin', 'edu_teacher')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = x.menu_id
  );
