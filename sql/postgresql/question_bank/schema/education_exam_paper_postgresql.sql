-- Exam paper catalog (admin upload, portal pick questions)

alter table edu_qb_paper add column if not exists paper_type varchar(20) not null default 'user';
alter table edu_qb_paper add column if not exists subject_id bigint;
alter table edu_qb_paper add column if not exists exam_category varchar(50);
alter table edu_qb_paper add column if not exists exam_year varchar(10);
alter table edu_qb_paper add column if not exists region varchar(50);
alter table edu_qb_paper add column if not exists grade varchar(20);
alter table edu_qb_paper add column if not exists publish_status char(1) not null default '0';
alter table edu_qb_paper add column if not exists source_file varchar(500);

create index if not exists idx_qb_paper_exam on edu_qb_paper (paper_type, publish_status, exam_category);

comment on column edu_qb_paper.paper_type is 'user=�ҵ��Ծ�, exam=�Ծ�ѡ��Ŀ¼';
comment on column edu_qb_paper.publish_status is '0=�ѷ���, 1=�ݸ�';

-- Menu: �Ծ�ѡ����� under �������(20300)
insert into sys_menu values(
  20125, E'\u8bd5\u5377\u9009\u9898\u7ba1\u7406', 20300, 8,
  'exam-paper', 'education/exam-paper/index', '', '', 1, 0, 'C', '0', '0',
  'education:exam-paper:list', 'documentation', 'admin', CURRENT_TIMESTAMP, '', null,
  E'\u4e0a\u4f20\u8bd5\u5377\u3001\u667a\u80fd\u6807\u8bb0\u9898\u76ee\u3001\u53d1\u5e03\u5230\u95e8\u6237'
) on conflict (menu_id) do nothing;

insert into sys_menu values(201251, E'\u8bd5\u5377\u67e5\u8be2', 20125, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:exam-paper:query', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201252, E'\u8bd5\u5377\u4e0a\u4f20', 20125, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:exam-paper:add', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201253, E'\u8bd5\u5377\u4fee\u6539', 20125, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:exam-paper:edit', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201254, E'\u8bd5\u5377\u5220\u9664', 20125, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:exam-paper:remove', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;
insert into sys_menu values(201255, E'\u95e8\u6237\u8bd5\u5377\u67e5\u8be2', 20125, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'education:exam-paper:portal', '#', 'admin', CURRENT_TIMESTAMP, '', null, '')
on conflict (menu_id) do nothing;

insert into sys_role_menu (role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
cross join (values (20125),(201251),(201252),(201253),(201254),(201255)) as m(menu_id)
where r.role_key in ('admin', 'edu_admin', 'edu_teacher')
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = m.menu_id
  );
