-- -----------------------------------------------------------------------------
-- education_qb_textbook_stage_postgresql.sql
-- Add school_stage (学段) to textbook version catalog
-- -----------------------------------------------------------------------------

alter table edu_qb_textbook_version
    add column if not exists school_stage varchar(20) not null default E'\u9ad8\u4e2d';

alter table edu_qb_textbook_version drop constraint if exists uk_qb_version_subject_name;
alter table edu_qb_textbook_version drop constraint if exists uk_qb_version_subject_stage_name;

-- migrate suffix from scraped version names
update edu_qb_textbook_version
set school_stage = E'\u521d\u4e2d',
    version_name = regexp_replace(version_name, E'\uff08\u521d\u4e2d\uff09$', '')
where version_name like E'%\uff08\u521d\u4e2d\uff09';

update edu_qb_textbook_version
set school_stage = E'\u9ad8\u4e2d',
    version_name = regexp_replace(version_name, E'\uff08\u9ad8\u4e2d\uff09$', '')
where version_name like E'%\uff08\u9ad8\u4e2d\uff09';

alter table edu_qb_textbook_version
    add constraint uk_qb_version_subject_stage_name
    unique (subject_id, school_stage, version_name);
