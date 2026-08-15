-- Persist manually cropped figure path on OCR draft (fallback when commit body omits images)
alter table edu_qb_ocr_draft
    add column if not exists figure_path varchar(500);
