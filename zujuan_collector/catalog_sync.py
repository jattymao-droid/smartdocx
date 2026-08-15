# -*- coding: utf-8 -*-
"""Sync zujuan catalog JSON into RuoYi textbook version / textbook / chapter tree."""

import argparse
import json
from pathlib import Path

from .chapter_mapper import flatten_tree, norm_key
from .config_store import load_config
from .importer import ApiClient
from .subject_resolver import match_subject_id, school_stage_from_code, short_subject_name

DEFAULT_CATALOG_DIR = Path(__file__).resolve().parent / 'data' / 'catalog'


def _log(msg, on_log=None):
    if on_log:
        on_log(msg)
    else:
        try:
            print(msg, flush=True)
        except UnicodeEncodeError:
            safe = str(msg).encode('gbk', errors='replace').decode('gbk')
            print(safe, flush=True)


class CatalogSyncer:
    def __init__(self, client, cfg=None, on_log=None):
        self.client = client
        self.cfg = cfg or {}
        self.on_log = on_log
        self.subjects = client.fetch_subjects()
        self._version_cache = {}
        self._textbook_cache = {}
        self._chapter_cache = {}

    def _ensure_subject_id(self, catalog):
        subject = catalog.get('subject') or {}
        code = subject.get('code') or ''
        label = subject.get('label') or ''
        subject_id = match_subject_id(self.subjects, label, code, fallback_id=None)
        if subject_id:
            return int(subject_id)

        if not self.cfg.get('auto_create_subjects', True):
            raise RuntimeError(
                f'\u672a\u627e\u5230\u5b66\u79d1: {label or code}\uff0c'
                f'\u8bf7\u5728\u7cfb\u7edf\u4e2d\u521b\u5efa\u6216\u5f00\u542f auto_create_subjects'
            )

        short_name = short_subject_name(label, code)
        subject_id = self.client.create_subject(short_name)
        self.subjects = self.client.fetch_subjects()
        _log(f'[\u5b66\u79d1] \u521b\u5efa: {short_name} (id:{subject_id})', self.on_log)
        return int(subject_id)

    def _find_version(self, subject_id, version_name, school_stage):
        cache_key = (subject_id, school_stage, norm_key(version_name))
        if cache_key in self._version_cache:
            return self._version_cache[cache_key]

        for row in self.client.fetch_textbook_versions(subject_id, school_stage):
            if norm_key(row.get('versionName')) == norm_key(version_name):
                vid = int(row['versionId'])
                self._version_cache[cache_key] = vid
                return vid
        return None

    def _ensure_version(self, subject_id, version_name, school_stage):
        hit = self._find_version(subject_id, version_name, school_stage)
        if hit:
            return hit
        if not self.cfg.get('auto_create_catalog', True):
            raise RuntimeError(f'\u672a\u627e\u5230\u6559\u6750\u7248\u672c: {version_name}')

        version_id = self.client.create_version(subject_id, version_name, school_stage)
        cache_key = (subject_id, school_stage, norm_key(version_name))
        self._version_cache[cache_key] = version_id
        _log(f'  [\u7248\u672c] \u521b\u5efa: {version_name} (id:{version_id})', self.on_log)
        return version_id

    def _find_textbook(self, version_id, textbook_name):
        cache_key = (version_id, norm_key(textbook_name))
        if cache_key in self._textbook_cache:
            return self._textbook_cache[cache_key]

        for row in self.client.fetch_textbooks(version_id):
            if norm_key(row.get('textbookName')) == norm_key(textbook_name):
                tid = int(row['textbookId'])
                self._textbook_cache[cache_key] = tid
                return tid
        return None

    def _ensure_textbook(self, version_id, textbook_name):
        hit = self._find_textbook(version_id, textbook_name)
        if hit:
            return hit
        if not self.cfg.get('auto_create_catalog', True):
            raise RuntimeError(f'\u672a\u627e\u5230\u6559\u6750: {textbook_name}')

        textbook_id = self.client.create_textbook(version_id, textbook_name)
        cache_key = (version_id, norm_key(textbook_name))
        self._textbook_cache[cache_key] = textbook_id
        _log(f'    [\u6559\u6750] \u521b\u5efa: {textbook_name} (id:{textbook_id})', self.on_log)
        return textbook_id

    def _load_chapter_index(self, textbook_id, subject_id):
        cache_key = (textbook_id, subject_id)
        if cache_key in self._chapter_cache:
            return self._chapter_cache[cache_key]

        tree = self.client.fetch_chapter_tree(textbook_id, subject_id)
        rows = flatten_tree(tree)
        index = {}
        for row in rows:
            parent_key = row.get('parent_id') or 0
            index[(parent_key, norm_key(row.get('label')))] = int(row['id'])
        self._chapter_cache[cache_key] = index
        return index

    def _find_chapter_id(self, index, parent_id, label):
        parent_key = parent_id or 0
        return index.get((parent_key, norm_key(label)))

    def _sync_chapter_nodes(self, textbook_id, subject_id, nodes, parent_id=None, stats=None):
        stats = stats if stats is not None else {'created': 0, 'skipped': 0}
        if not nodes:
            return stats

        index = self._load_chapter_index(textbook_id, subject_id)
        siblings = 0
        for node in nodes:
            label = (node.get('label') or '').strip()
            if not label:
                continue
            siblings += 1
            chapter_id = self._find_chapter_id(index, parent_id, label)
            if not chapter_id:
                if not self.cfg.get('auto_create_chapters', True):
                    stats['skipped'] += 1
                else:
                    chapter_id = self.client.create_chapter(
                        textbook_id, label, parent_id, siblings,
                    )
                    parent_key = parent_id or 0
                    index[(parent_key, norm_key(label))] = int(chapter_id)
                    stats['created'] += 1
                    if stats['created'] <= 5 or stats['created'] % 100 == 0:
                        _log(f'      [\u7ae0\u8282] \u521b\u5efa: {label} (id:{chapter_id})', self.on_log)
            else:
                stats['skipped'] += 1

            children = node.get('children') or []
            if children and chapter_id:
                self._sync_chapter_nodes(
                    textbook_id, subject_id, children, chapter_id, stats,
                )

        cache_key = (textbook_id, subject_id)
        self._chapter_cache[cache_key] = index
        return stats

    def sync_catalog(self, catalog):
        subject = catalog.get('subject') or {}
        code = subject.get('code') or ''
        school_stage = subject.get('school_stage') or school_stage_from_code(code)
        subject_id = self._ensure_subject_id(catalog)

        result = {
            'subject_code': code,
            'subject_id': subject_id,
            'versions': 0,
            'textbooks': 0,
            'chapters_created': 0,
            'chapters_skipped': 0,
        }

        label = subject.get('label') or code
        _log(f'[\u540c\u6b65] {label} (subjectId:{subject_id})', self.on_log)

        for version in catalog.get('versions') or []:
            version_name = version.get('name') or ''
            if not version_name:
                continue
            version_id = self._ensure_version(subject_id, version_name, school_stage)
            result['versions'] += 1

            for textbook in version.get('textbooks') or []:
                textbook_name = textbook.get('name') or ''
                if not textbook_name:
                    continue
                textbook_id = self._ensure_textbook(version_id, textbook_name)
                result['textbooks'] += 1
                _log(
                    f'    [\u6559\u6750] {textbook_name} (id:{textbook_id}) '
                    f'\u7ae0\u8282\u8282\u70b9: {textbook.get("chapter_count") or len(textbook.get("chapters") or [])}',
                    self.on_log,
                )
                stats = self._sync_chapter_nodes(
                    textbook_id,
                    subject_id,
                    textbook.get('chapters') or [],
                )
                result['chapters_created'] += stats['created']
                result['chapters_skipped'] += stats['skipped']

        return result


def sync_catalog_file(path, cfg=None, on_log=None):
    catalog = json.loads(Path(path).read_text(encoding='utf-8'))
    client = ApiClient(
        cfg['api_base'],
        cfg['username'],
        cfg['password'],
        redis_cli=cfg.get('redis_cli'),
    )
    client.login()
    syncer = CatalogSyncer(client, cfg=cfg, on_log=on_log)
    return syncer.sync_catalog(catalog)


def sync_catalog_dir(catalog_dir=None, cfg=None, on_log=None, pattern='*_catalog.json'):
    catalog_dir = Path(catalog_dir or DEFAULT_CATALOG_DIR)
    files = sorted(catalog_dir.glob(pattern))
    if not files:
        raise RuntimeError(f'\u672a\u627e\u5230\u76ee\u5f55\u6587\u4ef6: {catalog_dir / pattern}')

    cfg = cfg or load_config()[0]
    client = ApiClient(
        cfg['api_base'],
        cfg['username'],
        cfg['password'],
        redis_cli=cfg.get('redis_cli'),
    )
    client.login()
    syncer = CatalogSyncer(client, cfg=cfg, on_log=on_log)

    summary = []
    for path in files:
        if path.name.endswith('_catalog_paths.json'):
            continue
        catalog = json.loads(path.read_text(encoding='utf-8'))
        try:
            row = syncer.sync_catalog(catalog)
            row['file'] = str(path)
            summary.append(row)
        except Exception as ex:
            _log(f'[\u5931\u8d25] {path.name}: {ex}', on_log)
            summary.append({'file': str(path), 'error': str(ex)})

    return summary


def main(argv=None):
    parser = argparse.ArgumentParser(description='Sync zujuan catalog JSON to RuoYi textbook tree')
    parser.add_argument('--file', help='Single catalog JSON file')
    parser.add_argument('--dir', default=str(DEFAULT_CATALOG_DIR), help='Catalog directory')
    parser.add_argument('--all', action='store_true', help='Sync all *_catalog.json in dir')
    args = parser.parse_args(argv)

    cfg, _ = load_config()
    if args.file:
        result = sync_catalog_file(args.file, cfg=cfg)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return

    if args.all or not args.file:
        summary = sync_catalog_dir(args.dir, cfg=cfg)
        print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
