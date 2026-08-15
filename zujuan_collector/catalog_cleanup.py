# -*- coding: utf-8 -*-
"""Remove default zujuan placeholder catalog (version/textbook/chapters/questions)."""

import argparse
import json
import time

from .chapter_mapper import norm_key
from .config_store import load_config
from .importer import ApiClient

DEFAULT_DB = {
    'host': 'localhost',
    'dbname': 'ry_cloud',
    'user': 'postgres',
    'password': 'mm5621528',
}

QUESTION_CHILD_TABLES = (
    'edu_qb_paper_item',
    'edu_qb_question_feedback',
    'edu_qb_ocr_draft',
)


def _log(msg, on_log=None):
    if on_log:
        on_log(msg)
    else:
        try:
            print(msg, flush=True)
        except UnicodeEncodeError:
            safe = str(msg).encode('gbk', errors='replace').decode('gbk')
            print(safe, flush=True)


def _name_match(actual, expected):
    return norm_key(actual) == norm_key(expected)


def _collect_question_ids(client, *, subject_id, textbook_id, chapter_ids):
    seen = set()
    page_size = 200
    page_num = 1
    while True:
        rows, total = client.fetch_questions(
            subject_id=subject_id,
            textbook_id=textbook_id,
            page_num=page_num,
            page_size=page_size,
        )
        for row in rows:
            qid = row.get('questionId')
            if qid is not None:
                seen.add(int(qid))
        if page_num * page_size >= total or not rows:
            break
        page_num += 1

    for chapter_id in chapter_ids:
        page_num = 1
        while True:
            rows, total = client.fetch_questions(
                subject_id=subject_id,
                chapter_id=chapter_id,
                page_num=page_num,
                page_size=page_size,
            )
            for row in rows:
                qid = row.get('questionId')
                if qid is not None:
                    seen.add(int(qid))
            if page_num * page_size >= total or not rows:
                break
            page_num += 1
    return sorted(seen)


def _delete_questions(client, question_ids, *, dry_run, batch_size, on_log):
    deleted = 0
    for i in range(0, len(question_ids), batch_size):
        batch = question_ids[i:i + batch_size]
        if dry_run:
            _log(
                f'  [dry-run] delete questions {len(batch)} '
                f'({i + len(batch)}/{len(question_ids)})',
                on_log,
            )
            deleted += len(batch)
            continue
        client.delete_questions(batch)
        deleted += len(batch)
        _log(f'  [question] deleted {deleted}/{len(question_ids)}', on_log)
        time.sleep(0.1)
    return deleted


def _delete_chapters_leaf_first(client, chapters, *, dry_run, on_log):
    remaining = {int(c['chapterId']): c for c in chapters if c.get('chapterId') is not None}
    deleted = 0
    while remaining:
        child_counts = {}
        for cid, row in remaining.items():
            parent_id = row.get('parentId')
            if parent_id is not None and int(parent_id) != 0:
                child_counts[int(parent_id)] = child_counts.get(int(parent_id), 0) + 1
        leaves = [cid for cid in remaining if child_counts.get(cid, 0) == 0]
        if not leaves:
            raise RuntimeError(
                'chapter tree cycle or unresolved nodes: ' + ', '.join(map(str, remaining.keys()))
            )
        if dry_run:
            _log(f'  [dry-run] delete chapters {len(leaves)} (left {len(remaining) - len(leaves)})', on_log)
            for cid in leaves:
                remaining.pop(cid)
            deleted += len(leaves)
            continue
        client.delete_chapters(leaves)
        for cid in leaves:
            remaining.pop(cid)
        deleted += len(leaves)
        _log(f'  [chapter] deleted {deleted}', on_log)
        time.sleep(0.05)
    return deleted


def _db_connect(db_cfg):
    import psycopg2
    return psycopg2.connect(
        host=db_cfg.get('host', 'localhost'),
        dbname=db_cfg.get('dbname', 'ry_cloud'),
        user=db_cfg.get('user', 'postgres'),
        password=db_cfg.get('password', ''),
        port=db_cfg.get('port', 5432),
    )


def _hard_purge_textbook_questions(db_cfg, textbook_id, *, dry_run, on_log):
    """Hard-delete questions (incl. soft-deleted) so textbook FK can be removed."""
    import psycopg2

    conn = _db_connect(db_cfg)
    conn.autocommit = False
    cur = conn.cursor()
    try:
        cur.execute(
            'SELECT question_id FROM edu_qb_question WHERE textbook_id = %s',
            (int(textbook_id),),
        )
        question_ids = [row[0] for row in cur.fetchall()]
        if not question_ids:
            conn.commit()
            return 0

        if dry_run:
            _log(f'  [dry-run] hard purge questions {len(question_ids)} via DB', on_log)
            conn.rollback()
            return len(question_ids)

        for table in QUESTION_CHILD_TABLES:
            cur.execute(
                f'DELETE FROM {table} WHERE question_id = ANY(%s)',
                (question_ids,),
            )
        cur.execute(
            'DELETE FROM edu_qb_question WHERE textbook_id = %s',
            (int(textbook_id),),
        )
        deleted = cur.rowcount
        conn.commit()
        _log(f'  [db] hard deleted questions {deleted}', on_log)
        return deleted
    except Exception:
        conn.rollback()
        raise
    finally:
        cur.close()
        conn.close()


def cleanup_placeholder_catalog(
    client,
    *,
    version_name='\u7ec4\u5377\u7f51\u540c\u6b65',
    textbook_name='\u7ec4\u5377\u7f51\u7efc\u5408\u5e93',
    school_stage='\u9ad8\u4e2d',
    dry_run=False,
    batch_size=50,
    db_cfg=None,
    on_log=None,
):
    stats = {
        'subjects_scanned': 0,
        'versions_found': 0,
        'textbooks_found': 0,
        'questions_deleted': 0,
        'chapters_deleted': 0,
        'textbooks_deleted': 0,
        'versions_deleted': 0,
        'details': [],
    }

    subjects = client.fetch_subjects()
    for subject in subjects:
        subject_id = subject.get('subjectId')
        subject_name = subject.get('subjectName') or ''
        if subject_id is None:
            continue
        stats['subjects_scanned'] += 1

        try:
            versions = client.fetch_textbook_versions(subject_id, school_stage)
        except Exception as ex:
            _log(f'[{subject_name}] fetch versions failed: {ex}', on_log)
            continue

        target_versions = [v for v in versions if _name_match(v.get('versionName'), version_name)]
        if not target_versions:
            continue

        for version in target_versions:
            version_id = int(version['versionId'])
            stats['versions_found'] += 1
            _log(f'\n[{subject_name}] version: {version.get("versionName")} (id:{version_id})', on_log)

            textbooks = client.fetch_textbooks(version_id)
            target_textbooks = [t for t in textbooks if _name_match(t.get('textbookName'), textbook_name)]
            if not target_textbooks:
                _log('  no target textbook, skip', on_log)
                continue

            for textbook in target_textbooks:
                textbook_id = int(textbook['textbookId'])
                stats['textbooks_found'] += 1
                _log(f'  textbook: {textbook.get("textbookName")} (id:{textbook_id})', on_log)

                chapters = client.fetch_chapter_list(textbook_id)
                chapter_ids = [int(c['chapterId']) for c in chapters if c.get('chapterId') is not None]
                _log(f'  chapters: {len(chapter_ids)}', on_log)

                question_ids = _collect_question_ids(
                    client,
                    subject_id=subject_id,
                    textbook_id=textbook_id,
                    chapter_ids=chapter_ids,
                )
                _log(f'  questions: {len(question_ids)}', on_log)

                q_deleted = _delete_questions(
                    client, question_ids, dry_run=dry_run, batch_size=batch_size, on_log=on_log,
                )
                c_deleted = _delete_chapters_leaf_first(
                    client, chapters, dry_run=dry_run, on_log=on_log,
                )

                if db_cfg:
                    _hard_purge_textbook_questions(
                        db_cfg, textbook_id, dry_run=dry_run, on_log=on_log,
                    )

                tb_deleted = 0
                if dry_run:
                    _log('  [dry-run] delete textbook', on_log)
                    tb_deleted = 1
                else:
                    client.delete_textbooks([textbook_id])
                    tb_deleted = 1
                    _log('  [textbook] deleted', on_log)

                stats['questions_deleted'] += q_deleted
                stats['chapters_deleted'] += c_deleted
                stats['textbooks_deleted'] += tb_deleted
                stats['details'].append({
                    'subject_id': subject_id,
                    'subject_name': subject_name,
                    'version_id': version_id,
                    'textbook_id': textbook_id,
                    'questions': q_deleted,
                    'chapters': c_deleted,
                })

            if dry_run:
                remaining_textbooks = [
                    t for t in textbooks
                    if not _name_match(t.get('textbookName'), textbook_name)
                ]
            else:
                remaining_textbooks = [
                    t for t in client.fetch_textbooks(version_id)
                    if not _name_match(t.get('textbookName'), textbook_name)
                ]
            if not remaining_textbooks:
                if dry_run:
                    _log('  [dry-run] delete version (no other textbooks)', on_log)
                    stats['versions_deleted'] += 1
                else:
                    client.delete_versions([version_id])
                    stats['versions_deleted'] += 1
                    _log('  [version] deleted', on_log)

    return stats


def main(argv=None):
    parser = argparse.ArgumentParser(
        description='Clean zujuan placeholder catalog and related questions',
    )
    parser.add_argument(
        '--version-name',
        default='\u7ec4\u5377\u7f51\u540c\u6b65',
        help='version name to remove',
    )
    parser.add_argument(
        '--textbook-name',
        default='\u7ec4\u5377\u7f51\u7efc\u5408\u5e93',
        help='textbook name to remove',
    )
    parser.add_argument('--school-stage', default='\u9ad8\u4e2d', help='school stage')
    parser.add_argument('--dry-run', action='store_true', help='scan only, no delete')
    parser.add_argument('--batch-size', type=int, default=50, help='question delete batch size')
    parser.add_argument('--no-db-purge', action='store_true', help='skip DB hard purge for soft-deleted questions')
    args = parser.parse_args(argv)

    cfg, _ = load_config()
    db_cfg = None
    if not args.no_db_purge:
        db_cfg = dict(DEFAULT_DB)
        db_cfg.update(cfg.get('db') or {})
    client = ApiClient(cfg['api_base'], cfg['username'], cfg['password'], redis_cli=cfg.get('redis_cli'))
    _log('logging in to RuoYi...', None)
    client.login()

    stats = cleanup_placeholder_catalog(
        client,
        version_name=args.version_name,
        textbook_name=args.textbook_name,
        school_stage=args.school_stage,
        dry_run=args.dry_run,
        batch_size=args.batch_size,
        db_cfg=db_cfg,
    )

    _log('\n=== done ===', None)
    _log(json.dumps(stats, ensure_ascii=False, indent=2), None)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
