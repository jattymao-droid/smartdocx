-- Pay sys_config rows only (UTF-8 safe unicode escapes)
-- Run: psql -U postgres -d ry_cloud -f patch_edu_pay_sys_config.sql

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 920, E'\u652f\u4ed8-ZPay\u5f00\u5173', 'edu.pay.zpay.enabled', 'false', 'Y', 'admin', current_timestamp, '', null, E'\u662f\u5426\u542f\u7528 ZPay \u6613\u652f\u4ed8'
where not exists (select 1 from sys_config where config_key = 'edu.pay.zpay.enabled');

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 921, E'\u652f\u4ed8-ZPay\u5546\u6237ID', 'edu.pay.zpay.pid', '', 'Y', 'admin', current_timestamp, '', null, E'ZPay \u5546\u6237 PID'
where not exists (select 1 from sys_config where config_key = 'edu.pay.zpay.pid');

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 922, E'\u652f\u4ed8-ZPay\u5bc6\u94a5', 'edu.pay.zpay.key', '', 'Y', 'admin', current_timestamp, '', null, E'ZPay \u5546\u6237\u5bc6\u94a5 KEY'
where not exists (select 1 from sys_config where config_key = 'edu.pay.zpay.key');

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 923, E'\u652f\u4ed8-ZPay\u7f51\u5173', 'edu.pay.zpay.gateway-url', 'https://zpayz.cn', 'Y', 'admin', current_timestamp, '', null, E'ZPay \u7f51\u5173\u5730\u5740'
where not exists (select 1 from sys_config where config_key = 'edu.pay.zpay.gateway-url');

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 924, E'\u652f\u4ed8-ZPay\u56de\u8c03', 'edu.pay.zpay.notify-url', '', 'Y', 'admin', current_timestamp, '', null, E'\u5f02\u6b65\u901a\u77e5\u5730\u5740\uff08\u516c\u7f51\u53ef\u8bbf\u95ee\uff09'
where not exists (select 1 from sys_config where config_key = 'edu.pay.zpay.notify-url');

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 925, E'\u652f\u4ed8-\u7ec4\u5377\u5bfc\u51fa\u8d39\u7528', 'edu.pay.paper-export-fee', '0', 'Y', 'admin', current_timestamp, '', null, E'\u7ec4\u5377\u8bd5\u5377\u5bfc\u51fa/\u4e0b\u8f7d\u8d39\u7528\uff08\u5143\uff09\uff0c0 \u8868\u793a\u514d\u8d39'
where not exists (select 1 from sys_config where config_key = 'edu.pay.paper-export-fee');
