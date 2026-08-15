-- -----------------------------------------------------------------------------
-- education_question_bank_textbook_postgresql.sql
-- �̲İ汾 / �̲� / �½�Ŀ¼
-- -----------------------------------------------------------------------------

create table if not exists edu_qb_textbook_version (
    version_id     bigserial primary key,
    subject_id     bigint not null references edu_subject(subject_id),
    school_stage   varchar(20) not null default E'\u9ad8\u4e2d',
    version_name   varchar(50) not null,
    order_num      integer not null default 0,
    status         char(1) not null default '0',
    create_time    timestamp default current_timestamp,
    constraint uk_qb_version_subject_stage_name unique (subject_id, school_stage, version_name)
);

create table if not exists edu_qb_textbook (
    textbook_id    bigserial primary key,
    version_id     bigint not null references edu_qb_textbook_version(version_id) on delete cascade,
    textbook_name  varchar(80) not null,
    order_num      integer not null default 0,
    status         char(1) not null default '0',
    create_time    timestamp default current_timestamp,
    constraint uk_qb_textbook_version_name unique (version_id, textbook_name)
);

create table if not exists edu_qb_chapter (
    chapter_id     bigserial primary key,
    textbook_id    bigint not null references edu_qb_textbook(textbook_id) on delete cascade,
    parent_id      bigint references edu_qb_chapter(chapter_id) on delete cascade,
    chapter_name   varchar(120) not null,
    order_num      integer not null default 0,
    create_time    timestamp default current_timestamp
);

create index if not exists idx_qb_chapter_textbook on edu_qb_chapter (textbook_id, parent_id, order_num);

alter table edu_qb_question add column if not exists textbook_id bigint references edu_qb_textbook(textbook_id);
create index if not exists idx_qb_question_textbook on edu_qb_question (textbook_id, chapter_id);

insert into edu_qb_textbook_version (subject_id, version_name, order_num)
select s.subject_id, v.version_name, v.order_num
from edu_subject s
cross join (values
    ('�˽̰�', 1),
    ('���̰�', 2),
    ('³�ư�', 3),
    ('���ƽ̰�', 4),
    ('�̿ư�', 5)
) as v(version_name, order_num)
where s.subject_name = '����'
on conflict (subject_id, version_name) do nothing;

insert into edu_qb_textbook (version_id, textbook_name, order_num)
select v.version_id, t.textbook_name, t.order_num
from edu_qb_textbook_version v
join edu_subject s on s.subject_id = v.subject_id and s.subject_name = '����'
cross join (values
    ('���� ��һ��', 1),
    ('���� �ڶ���', 2),
    ('���� ������', 3),
    ('ѡ���Ա��� ��һ��', 4),
    ('ѡ���Ա��� �ڶ���', 5),
    ('ѡ���Ա��� ������', 6)
) as t(textbook_name, order_num)
where v.version_name = '�˽̰�'
on conflict (version_id, textbook_name) do nothing;

insert into edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
select tb.textbook_id, null, c.chapter_name, c.order_num
from edu_qb_textbook tb
join edu_qb_textbook_version v on v.version_id = tb.version_id
join edu_subject s on s.subject_id = v.subject_id
cross join (values
    ('��һ�� �˶�������', 1),
    ('�ڶ��� �ȱ���ֱ���˶����о�', 2),
    ('������ �໥���á�����', 3),
    ('������ �˶������Ĺ�ϵ', 4)
) as c(chapter_name, order_num)
where s.subject_name = '����' and v.version_name = '�˽̰�' and tb.textbook_name = '���� ��һ��'
  and not exists (
    select 1 from edu_qb_chapter ec
    where ec.textbook_id = tb.textbook_id and ec.parent_id is null and ec.chapter_name = c.chapter_name
  );

insert into edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
select p.textbook_id, p.chapter_id, s.section_name, s.order_num
from edu_qb_chapter p
join edu_qb_textbook tb on tb.textbook_id = p.textbook_id
join edu_qb_textbook_version v on v.version_id = tb.version_id
join edu_subject sub on sub.subject_id = v.subject_id
cross join (values
    ('��һ�� �˶�������', '1. �ʵ� �ο�ϵ', 1),
    ('��һ�� �˶�������', '2. ʱ�� λ��', 2),
    ('��һ�� �˶�������', '3. λ�ñ仯���������������ٶ�', 3),
    ('��һ�� �˶�������', '4. �ٶȱ仯�����������������ٶ�', 4),
    ('�ڶ��� �ȱ���ֱ���˶����о�', '1. ʵ�飺̽��С���ٶ���ʱ��仯�Ĺ���', 1),
    ('�ڶ��� �ȱ���ֱ���˶����о�', '2. �ȱ���ֱ���˶����ٶ���ʱ��Ĺ�ϵ', 2),
    ('�ڶ��� �ȱ���ֱ���˶����о�', '3. �ȱ���ֱ���˶���λ����ʱ��Ĺ�ϵ', 3),
    ('�ڶ��� �ȱ���ֱ���˶����о�', '4. ���������˶�', 4)
) as s(chapter_name, section_name, order_num)
where sub.subject_name = '����' and v.version_name = '�˽̰�' and tb.textbook_name = '���� ��һ��'
  and p.parent_id is null and p.chapter_name = s.chapter_name
  and not exists (
    select 1 from edu_qb_chapter ec
    where ec.textbook_id = p.textbook_id and ec.parent_id = p.chapter_id and ec.chapter_name = s.section_name
  );

insert into edu_qb_textbook_version (subject_id, version_name, order_num)
select s.subject_id, '�˽̰�', 1
from edu_subject s where s.subject_name = '��ѧ'
on conflict (subject_id, version_name) do nothing;

insert into edu_qb_textbook (version_id, textbook_name, order_num)
select v.version_id, '���� ��һ��', 1
from edu_qb_textbook_version v
join edu_subject s on s.subject_id = v.subject_id
where s.subject_name = '��ѧ' and v.version_name = '�˽̰�'
on conflict (version_id, textbook_name) do nothing;

insert into edu_qb_chapter (textbook_id, parent_id, chapter_name, order_num)
select tb.textbook_id, null, c.chapter_name, c.order_num
from edu_qb_textbook tb
join edu_qb_textbook_version v on v.version_id = tb.version_id
join edu_subject s on s.subject_id = v.subject_id
cross join (values
    ('��һ�� �����볣���߼�����', 1),
    ('�ڶ��� һԪ���κ��������̺Ͳ���ʽ', 2),
    ('������ �����ĸ���������', 3)
) as c(chapter_name, order_num)
where s.subject_name = '��ѧ' and v.version_name = '�˽̰�' and tb.textbook_name = '���� ��һ��'
  and not exists (
    select 1 from edu_qb_chapter ec
    where ec.textbook_id = tb.textbook_id and ec.parent_id is null and ec.chapter_name = c.chapter_name
  );
