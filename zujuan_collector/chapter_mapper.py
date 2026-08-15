# -*- coding: utf-8 -*-
"""Map zujuan breadcrumb paths to project textbook chapter tree."""

import re

ZUJUAN_SKIP_SEGMENTS = {
    '\u7ec4\u5377\u7f51',
    '\u9ad8\u4e2d\u7269\u7406\u7efc\u5408\u5e93',
    '\u9ad8\u4e2d\u6570\u5b66\u7efc\u5408\u5e93',
    '\u9ad8\u4e2d\u5316\u5b66\u7efc\u5408\u5e93',
    '\u9ad8\u4e2d\u751f\u7269\u7efc\u5408\u5e93',
    '\u9ad8\u4e2d\u5386\u53f2\u7efc\u5408\u5e93',
    '\u9ad8\u4e2d\u5730\u7406\u7efc\u5408\u5e93',
    '\u9ad8\u4e2d\u653f\u6cbb\u7efc\u5408\u5e93',
    '\u9ad8\u4e2d\u82f1\u8bed\u7efc\u5408\u5e93',
    '\u9ad8\u4e2d\u8bed\u6587\u7efc\u5408\u5e93',
    '\u529b\u5b66',
    '\u7535\u78c1\u5b66',
    '\u70ed\u5b66',
    '\u5149\u5b66',
    '\u539f\u5b50\u7269\u7406',
    '\u632f\u52a8\u548c\u6ce2',
    '\u58f0\u5b66',
}

SEGMENT_ALIASES = {
    '\u725b\u987f\u8fd0\u52a8\u5b9a\u5f8b': '\u8fd0\u52a8\u548c\u529b\u7684\u5173\u7cfb',
    '\u5300\u53d8\u901f\u76f4\u7ebf\u8fd0\u52a8': '\u5300\u53d8\u901f\u76f4\u7ebf\u8fd0\u52a8\u7684\u7814\u7a76',
    '\u8fd0\u52a8\u7684\u63cf\u8ff0': '\u8fd0\u52a8\u7684\u63cf\u8ff0',
    '\u76f8\u4e92\u4f5c\u7528': '\u76f8\u4e92\u4f5c\u7528--\u529b',
    '\u76f8\u4e92\u4f5c\u7528--\u529b': '\u76f8\u4e92\u4f5c\u7528--\u529b',
    '\u7535\u573a': '\u7535\u573a',
    '\u7535\u8def': '\u7535\u8def',
    '\u78c1\u573a': '\u78c1\u573a',
    '\u7535\u78c1\u611f\u5e94': '\u7535\u78c1\u611f\u5e94',
    '\u4e07\u6709\u5f15\u529b': '\u4e07\u6709\u5f15\u529b\u4e0e\u5b87\u5b99\u822a\u884c',
    '\u673a\u68b0\u80fd': '\u673a\u68b0\u80fd',
    '\u66f2\u7ebf\u8fd0\u52a8': '\u629b\u4f53\u8fd0\u52a8',
    '\u5706\u5468\u8fd0\u52a8': '\u5706\u5468\u8fd0\u52a8',
    '\u529b\u5b66\u5b9e\u9a8c': '\u63a2\u7a76\u52a0\u901f\u5ea6\u4e0e\u529b\u3001\u8d28\u91cf\u7684\u5173\u7cfb',
    '\u9a8c\u8bc1\u52a0\u901f\u5ea6\u4e0e\u8d28\u91cf\u6210\u53cd\u6bd4\u7684\u5b9e\u9a8c': '\u63a2\u7a76\u52a0\u901f\u5ea6\u4e0e\u529b\u3001\u8d28\u91cf\u7684\u5173\u7cfb',
}


def norm_key(text):
    t = (text or '').strip()
    t = re.sub(r'\s+', '', t)
    t = re.sub(r'[\u2014\u2013\u002d\uff0d~]+', '', t)
    t = re.sub(
        r'^\u7b2c[\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d\u5341\u767e\d]+\u7ae0',
        '',
        t,
    )
    t = re.sub(r'^\d+[\.\u3001\uff0e\)\uff09]', '', t)
    t = re.sub(r'[\uff1a:]', '', t)
    return t


def split_path(path):
    if not path:
        return []
    parts = re.split(r'\s*>\s*', str(path).strip())
    out = []
    for p in parts:
        p = p.strip()
        if not p or p in ZUJUAN_SKIP_SEGMENTS:
            continue
        if p.endswith('\u7efc\u5408\u5e93'):
            continue
        out.append(p)
    return out


def _alias_key(segment):
    return norm_key(SEGMENT_ALIASES.get(segment, segment))


def segment_score(segment, label):
    seg_k = _alias_key(segment)
    lab_k = norm_key(label)
    if not seg_k or not lab_k:
        return 0.0
    if seg_k == lab_k:
        return 100.0
    if seg_k in lab_k:
        return 85.0 + min(10.0, len(seg_k) / max(len(lab_k), 1) * 10.0)
    if lab_k in seg_k:
        return 75.0 + min(10.0, len(lab_k) / max(len(seg_k), 1) * 10.0)
    common = 0
    for i in range(min(len(seg_k), len(lab_k))):
        if seg_k[i] == lab_k[i]:
            common += 1
        else:
            break
    if common >= 3:
        return 50.0 + common
    return 0.0


def flatten_tree(nodes, parent=None):
    rows = []
    for node in nodes or []:
        nid = node.get('id')
        if not nid or nid == 'all':
            continue
        label = node.get('label') or ''
        row = {
            'id': int(nid),
            'label': label,
            'parent_id': parent['id'] if parent else None,
            'parent_label': parent['label'] if parent else '',
            'depth': (parent['depth'] + 1) if parent else 0,
        }
        rows.append(row)
        rows.extend(flatten_tree(node.get('children') or [], row))
    return rows


class ChapterMapper:
    def __init__(self):
        self.textbooks = []
        self.trees = {}
        self.version_id = None

    def _find_textbook_by_name(self, name):
        z_name = (name or '').strip()
        if not z_name:
            return None
        for tb in self.textbooks:
            if (tb.get('textbookName') or '').strip() == z_name:
                return tb
        z_norm = norm_key(z_name)
        for tb in self.textbooks:
            if norm_key(tb.get('textbookName')) == z_norm:
                return tb
        return None

    def _ensure_textbook(self, client, version_id, textbook_name, cfg, on_log=None):
        if not textbook_name:
            return None
        hit = self._find_textbook_by_name(textbook_name)
        if hit:
            return int(hit['textbookId'])
        if not cfg.get('auto_create_catalog', True):
            return None
        textbook_id = client.create_textbook(version_id, textbook_name)
        self.textbooks = client.fetch_textbooks(version_id)
        if on_log:
            on_log(f'  [\u6559\u6750] \u81ea\u52a8\u521b\u5efa: {textbook_name} (id:{textbook_id})')
        return textbook_id

    def load(self, client, subject_id, version_id=None, school_stage='\u9ad8\u4e2d', cfg=None, on_log=None):
        cfg = cfg or {}
        self.subject_id = subject_id
        versions = client.fetch_textbook_versions(subject_id, school_stage)
        if not versions and cfg.get('auto_create_catalog', True):
            version_name = cfg.get('default_version_name') or '\u7ec4\u5377\u7f51\u540c\u6b65'
            version_id = client.create_version(subject_id, version_name, school_stage)
            versions = client.fetch_textbook_versions(subject_id, school_stage)
            if on_log:
                on_log(f'  [\u7248\u672c] \u81ea\u52a8\u521b\u5efa: {version_name} (id:{version_id})')
        if not versions:
            raise RuntimeError('\u672a\u627e\u5230\u6559\u6750\u7248\u672c\uff0c\u8bf7\u5148\u5728\u7cfb\u7edf\u4e2d\u5bfc\u5165\u6559\u6750\u76ee\u5f55')
        if version_id is None:
            version_id = cfg.get('version_id') or versions[0].get('versionId')
        self.version_id = version_id
        self.textbooks = client.fetch_textbooks(version_id)
        default_tb = cfg.get('default_textbook_name') or '\u7ec4\u5377\u7f51\u7efc\u5408\u5e93'
        if cfg.get('auto_create_catalog', True) and not self.textbooks:
            self._ensure_textbook(client, version_id, default_tb, cfg, on_log=on_log)
        self.trees = {}
        for tb in self.textbooks:
            tid = tb.get('textbookId')
            if tid is None:
                continue
            tree = client.fetch_chapter_tree(tid, subject_id)
            self.trees[tid] = flatten_tree(tree)
        if on_log:
            on_log(
                f'\u5df2\u52a0\u8f7d {len(self.textbooks)} \u518c\u6559\u6750\u3001'
                f'{sum(len(v) for v in self.trees.values())} \u4e2a\u7ae0\u8282\u8282\u70b9'
            )

    def _resolve_textbook_id(self, item, cfg, client=None, on_log=None):
        cfg_tid = cfg.get('textbook_id')
        if cfg_tid:
            return int(cfg_tid)

        page_ctx = item.get('page_ctx') or {}
        z_name = (page_ctx.get('textbook') or page_ctx.get('textbook_hint') or '').strip()
        if z_name:
            hit = self._find_textbook_by_name(z_name)
            if hit:
                return int(hit['textbookId'])
            if client and cfg.get('auto_create_catalog', True) and self.version_id:
                return self._ensure_textbook(client, self.version_id, z_name, cfg, on_log=on_log)

        if cfg.get('auto_create_catalog', True) and client and self.version_id:
            default_tb = cfg.get('default_textbook_name') or '\u7ec4\u5377\u7f51\u7efc\u5408\u5e93'
            return self._ensure_textbook(client, self.version_id, default_tb, cfg, on_log=on_log)
        return None

    def _chapter_match_path(self, item):
        """Prefer textbook chapter path over knowledge category labels."""
        detail = (item.get('detail_chapter_text') or '').strip()
        if detail:
            return detail

        page_ctx = item.get('page_ctx') or {}
        ct = (item.get('chapter_text') or '').strip()
        if ct and ' > ' in ct and '\u7efc\u5408\u5e93' not in ct:
            return ct

        parts = []
        if page_ctx.get('textbook'):
            parts.append(page_ctx['textbook'])
        if page_ctx.get('chapter_node'):
            parts.append(page_ctx['chapter_node'])
        elif item.get('category_name'):
            cat = item['category_name'].strip()
            if cat and (not parts or cat != parts[-1]):
                parts.append(cat)
        if parts:
            return ' > '.join(parts)
        return ct

    def _match_in_tree(self, segments, rows):
        if not segments or not rows:
            return None

        parent_row = None
        section_row = None
        matched_count = 0

        for seg in segments:
            if parent_row:
                candidates = [r for r in rows if r['parent_id'] == parent_row['id']]
            else:
                candidates = [r for r in rows if r['parent_id'] is None]

            best = None
            best_score = 0.0
            for row in candidates:
                score = segment_score(seg, row['label'])
                if score > best_score:
                    best_score = score
                    best = row

            if not best or best_score < 55.0:
                return None

            matched_count += 1
            if best['parent_id'] is None:
                parent_row = best
            else:
                section_row = best
                parent_row = next((r for r in rows if r['id'] == best['parent_id']), parent_row)

        if matched_count < len(segments):
            return None

        target = section_row or parent_row
        if not target:
            return None

        if section_row:
            parent_label = section_row['parent_label'] or (parent_row['label'] if parent_row else '')
            chapter_text = (
                f"{parent_label} > {section_row['label']}" if parent_label else section_row['label']
            )
            return {
                'chapter_id': section_row['id'],
                'chapter_text': chapter_text,
                'matched_label': section_row['label'],
            }

        return {
            'chapter_id': parent_row['id'],
            'chapter_text': parent_row['label'],
            'matched_label': parent_row['label'],
        }

    def _candidate_rows(self, rows, parent_id):
        if parent_id is None:
            return [r for r in rows if r['parent_id'] is None]
        return [r for r in rows if r['parent_id'] == parent_id]

    def _best_row_match(self, segment, candidates):
        best = None
        best_score = 0.0
        for row in candidates:
            score = segment_score(segment, row['label'])
            if row['parent_id'] is None:
                score -= 1.0
            else:
                score += 2.0
            if score > best_score:
                best_score = score
                best = row
        if best and best_score >= 55.0:
            return best, best_score
        return None, best_score

    def _append_row(self, textbook_id, chapter_id, label, parent_row):
        parent_id = parent_row['id'] if parent_row else None
        row = {
            'id': int(chapter_id),
            'label': label,
            'parent_id': parent_id,
            'parent_label': parent_row['label'] if parent_row else '',
            'depth': (parent_row['depth'] + 1) if parent_row else 0,
        }
        rows = self.trees.setdefault(textbook_id, [])
        rows.append(row)
        return row

    def _ensure_chapter_path(self, client, textbook_id, segments, on_log=None):
        """Create missing chapter nodes along zujuan breadcrumb path."""
        if not segments or not client:
            return None

        rows = self.trees.get(textbook_id) or []
        parent_row = None
        path_labels = []
        created_any = False

        for seg in segments:
            candidates = self._candidate_rows(rows, parent_row['id'] if parent_row else None)
            best, _ = self._best_row_match(seg, candidates)
            if best:
                parent_row = best
                path_labels.append(best['label'])
                continue

            parent_id = parent_row['id'] if parent_row else None
            order_num = len(candidates) + 1
            chapter_id = client.create_chapter(textbook_id, seg, parent_id, order_num)
            parent_row = self._append_row(textbook_id, chapter_id, seg, parent_row)
            path_labels.append(seg)
            created_any = True
            if on_log:
                parent_hint = f' <- {parent_id}' if parent_id else ''
                on_log(f'  [\u7ae0\u8282] \u81ea\u52a8\u521b\u5efa: {seg} (id:{chapter_id}{parent_hint})')

        if not parent_row:
            return None

        chapter_text = ' > '.join(path_labels)
        return {
            'chapter_id': parent_row['id'],
            'chapter_text': chapter_text,
            'matched_label': parent_row['label'],
            'created': created_any,
        }

    def match(self, item, cfg=None, client=None, on_log=None):
        cfg = cfg or {}
        textbook_id = self._resolve_textbook_id(item, cfg, client=client, on_log=on_log)
        if textbook_id and textbook_id not in self.trees and client:
            tree = client.fetch_chapter_tree(textbook_id, self.subject_id)
            self.trees[textbook_id] = flatten_tree(tree)
        if not textbook_id or textbook_id not in self.trees:
            return {
                'textbook_id': textbook_id,
                'chapter_id': cfg.get('chapter_id'),
                'chapter_text': self._fallback_text(item, cfg),
                'mapped': False,
            }

        path = self._chapter_match_path(item)
        page_ctx = item.get('page_ctx') or {}
        if not path:
            parts = []
            if page_ctx.get('chapter_node'):
                parts.append(page_ctx['chapter_node'])
            elif item.get('category_name'):
                parts.append(item['category_name'])
            path = ' > '.join(parts)

        segments = split_path(path)
        hit = self._match_in_tree(segments, self.trees.get(textbook_id) or [])

        if hit:
            return {
                'textbook_id': textbook_id,
                'chapter_id': hit['chapter_id'],
                'chapter_text': hit['chapter_text'],
                'mapped': True,
                'zujuan_path': path,
            }

        auto_create = cfg.get('auto_create_chapters', True)
        if auto_create and client and segments:
            try:
                ensured = self._ensure_chapter_path(client, textbook_id, segments, on_log=on_log)
            except Exception as ex:
                if on_log:
                    on_log(f'  [\u7ae0\u8282] \u81ea\u52a8\u521b\u5efa\u5931\u8d25: {ex}')
                ensured = None
            if ensured:
                return {
                    'textbook_id': textbook_id,
                    'chapter_id': ensured['chapter_id'],
                    'chapter_text': ensured['chapter_text'],
                    'mapped': True,
                    'created': ensured.get('created', False),
                    'zujuan_path': path,
                }

        fallback = self._fallback_text(item, cfg)
        if page_ctx.get('chapter_node') and fallback in ('', '\u7ec4\u5377\u7f51\u91c7\u96c6'):
            node = page_ctx['chapter_node']
            parent = page_ctx.get('textbook') or ''
            fallback = f'{parent} > {node}' if parent else node

        return {
            'textbook_id': textbook_id,
            'chapter_id': cfg.get('chapter_id'),
            'chapter_text': fallback,
            'mapped': False,
            'zujuan_path': path,
        }

    def _fallback_text(self, item, cfg):
        page_ctx = item.get('page_ctx') or {}
        if page_ctx.get('chapter_node'):
            return page_ctx['chapter_node']
        if item.get('category_name'):
            return item['category_name']
        cfg_text = (cfg.get('chapter_text') or '').strip()
        if cfg_text and cfg_text not in ('\u7ec4\u5377\u7f51\u91c7\u96c6',):
            return cfg_text
        return '\u7ec4\u5377\u7f51\u91c7\u96c6'
