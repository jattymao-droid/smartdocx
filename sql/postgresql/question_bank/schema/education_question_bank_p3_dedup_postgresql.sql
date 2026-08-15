-- -----------------------------------------------------------------------------
-- education_question_bank_p3_dedup_postgresql.sql
-- P3 Sprint 3: content_hash index for duplicate detection
-- -----------------------------------------------------------------------------

create index if not exists idx_qb_question_subject_hash
    on edu_qb_question (subject_id, content_hash)
    where del_flag = '0' and content_hash is not null;

-- Backfill content_hash for existing rows (normalized stem SHA-256 computed in app on next edit;
-- optional one-time update can be run via admin tool later).
