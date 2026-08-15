-- -----------------------------------------------------------------------------
-- education_question_bank_p2_ocr_postgresql.sql
-- P2: OCR draft table for mobile / web photo import
-- -----------------------------------------------------------------------------

create table if not exists edu_qb_ocr_draft (
    draft_id              bigserial primary key,
    image_path            varchar(500) not null,
    ocr_text              text,
    ocr_lines             jsonb,
    confidence            numeric(5,4),
    predicted_type        varchar(20),
    predicted_difficulty  numeric(3,2),
    predicted_options     jsonb,
    subject_id            bigint references edu_subject(subject_id),
    status                varchar(20) not null default 'draft',
    question_id           bigint references edu_qb_question(question_id),
    create_by             varchar(64),
    create_time           timestamp default current_timestamp,
    update_time           timestamp,
    remark                varchar(500)
);

create index if not exists idx_qb_ocr_draft_create on edu_qb_ocr_draft (create_time desc);
create index if not exists idx_qb_ocr_draft_status on edu_qb_ocr_draft (status, create_by);
