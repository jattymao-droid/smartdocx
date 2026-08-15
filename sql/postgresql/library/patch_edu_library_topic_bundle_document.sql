-- Link a library zip document to a hot topic (portal list opens topic page instead of archive preview)

alter table edu_library_topic
    add column if not exists bundle_document_id bigint references edu_library_document(document_id);

create index if not exists idx_library_topic_bundle_doc
    on edu_library_topic (bundle_document_id)
    where bundle_document_id is not null and del_flag = '0';

-- ����������ҵ topic -> zip document
update edu_library_topic
set bundle_document_id = 6,
    update_time = current_timestamp
where topic_id = 2
  and bundle_document_id is distinct from 6;
