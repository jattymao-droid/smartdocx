-- ZPay payment module (library download + paper export)
-- Run: psql -U postgres -d ry_cloud -f patch_edu_pay_zpay.sql

alter table edu_library_document
    add column if not exists download_price numeric(10, 2) not null default 0;

create table if not exists edu_pay_order (
    order_id      bigserial primary key,
    order_no      varchar(64) not null,
    username      varchar(64) not null,
    biz_type      varchar(32) not null,
    biz_id        bigint not null default 0,
    biz_ref       varchar(64),
    product_name  varchar(200) not null,
    amount        numeric(10, 2) not null,
    pay_type      varchar(16) not null,
    status        varchar(16) not null default 'pending',
    trade_no      varchar(64),
    pay_url       varchar(500),
    qrcode_url    varchar(500),
    client_ip     varchar(64),
    notify_time   timestamp,
    pay_time      timestamp,
    create_time   timestamp default current_timestamp,
    update_time   timestamp,
    remark        varchar(500),
    constraint uk_edu_pay_order_no unique (order_no)
);

create index if not exists idx_edu_pay_order_user on edu_pay_order (username, biz_type, status);
create index if not exists idx_edu_pay_order_biz on edu_pay_order (biz_type, biz_id, biz_ref);

create table if not exists edu_pay_entitlement (
    entitlement_id bigserial primary key,
    username       varchar(64) not null,
    biz_type       varchar(32) not null,
    biz_id         bigint not null default 0,
    biz_ref        varchar(64),
    order_no       varchar(64) not null,
    create_time    timestamp default current_timestamp,
    constraint uk_edu_pay_entitlement unique (username, biz_type, biz_id, biz_ref)
);

\i patch_edu_pay_sys_config.sql
