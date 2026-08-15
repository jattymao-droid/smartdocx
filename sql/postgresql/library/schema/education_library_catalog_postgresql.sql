-- Add textbook catalog fields to library documents

alter table edu_library_document add column if not exists school_stage varchar(20);
alter table edu_library_document add column if not exists version_id bigint references edu_qb_textbook_version(version_id);
alter table edu_library_document add column if not exists textbook_id bigint references edu_qb_textbook(textbook_id);
alter table edu_library_document add column if not exists chapter_id bigint references edu_qb_chapter(chapter_id);
alter table edu_library_document add column if not exists chapter_text varchar(500);

create index if not exists idx_library_doc_school_stage on edu_library_document (school_stage, del_flag);
create index if not exists idx_library_doc_version on edu_library_document (version_id, del_flag);
create index if not exists idx_library_doc_textbook on edu_library_document (textbook_id, del_flag);
create index if not exists idx_library_doc_chapter on edu_library_document (chapter_id, del_flag);
