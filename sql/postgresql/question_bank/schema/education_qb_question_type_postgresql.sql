-- Question type registry for question bank (题型管理)
create table if not exists edu_qb_question_type (
    type_id         bigserial primary key,
    type_code       varchar(32) not null,
    type_name       varchar(50) not null,
    answer_mode     varchar(20) not null default 'subjective',
    content_max_len integer,
    order_num       integer default 0,
    status          char(1) default '0',
    builtin         char(1) default '0',
    remark          varchar(500),
    create_by       varchar(64),
    create_time     timestamp,
    update_by       varchar(64),
    update_time     timestamp,
    constraint uk_edu_qb_question_type_code unique (type_code)
);

comment on table edu_qb_question_type is E'\u9898\u5e93\u9898\u578b\u914d\u7f6e';
comment on column edu_qb_question_type.type_code is E'\u9898\u578b\u7f16\u7801\uff08\u5b58\u5165\u9898\u76ee question_type\uff09';
comment on column edu_qb_question_type.type_name is E'\u9898\u578b\u540d\u79f0';
comment on column edu_qb_question_type.answer_mode is E'\u7b54\u9898\u6a21\u5f0f\uff1achoice/multi/judge/fill/subjective';
comment on column edu_qb_question_type.content_max_len is E'\u9898\u5e72\u957f\u5ea6\u4e0a\u9650\uff08\u7a7a\u5219\u7528\u9ed8\u8ba4\uff09';
comment on column edu_qb_question_type.builtin is E'1=\u5185\u7f6e\u9898\u578b\uff0c\u4e0d\u53ef\u5220\u9664';

insert into edu_qb_question_type (type_code, type_name, answer_mode, content_max_len, order_num, status, builtin, create_by, create_time)
select v.type_code, v.type_name, v.answer_mode, v.content_max_len, v.order_num, '0', '1', 'admin', CURRENT_TIMESTAMP
from (values
    ('single', E'\u5355\u9009', 'choice', null, 1),
    ('multi', E'\u591a\u9009', 'multi', null, 2),
    ('fill', E'\u586b\u7a7a', 'fill', null, 3),
    ('experiment', E'\u5b9e\u9a8c', 'subjective', null, 4),
    ('answer', E'\u89e3\u7b54', 'subjective', null, 5),
    ('comprehensive', E'\u7efc\u5408', 'subjective', null, 6),
    ('reading', E'\u9605\u8bfb', 'subjective', 10000, 7),
    ('judge', E'\u5224\u65ad', 'judge', null, 8),
    ('drawing', E'\u4f5c\u56fe', 'subjective', null, 9),
    ('knowledge_fill', E'\u77e5\u8bc6\u586b\u7a7a', 'fill', null, 10),
    ('short', E'\u7b80\u7b54', 'subjective', null, 11)
) as v(type_code, type_name, answer_mode, content_max_len, order_num)
where not exists (select 1 from edu_qb_question_type t where t.type_code = v.type_code);
