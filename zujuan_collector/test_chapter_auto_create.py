# -*- coding: utf-8 -*-
"""Tests for chapter auto-creation during import."""

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from zujuan_collector.chapter_mapper import ChapterMapper, split_path


class _MockClient:
    def __init__(self):
        self._next_id = 1000
        self.created = []

    def create_chapter(self, textbook_id, chapter_name, parent_id=None, order_num=0):
        self._next_id += 1
        self.created.append({
            'textbookId': textbook_id,
            'chapterName': chapter_name,
            'parentId': parent_id,
            'orderNum': order_num,
        })
        return self._next_id


def test_split_path_skips_comprehensive_lib():
    path = '\u9ad8\u4e2d\u7269\u7406\u7efc\u5408\u5e93 > \u529b\u5b66 > \u76f8\u4e92\u4f5c\u7528 > \u5171\u70b9\u529b\u7684\u5e73\u8861'
    segs = split_path(path)
    assert '\u9ad8\u4e2d\u7269\u7406\u7efc\u5408\u5e93' not in segs
    assert '\u529b\u5b66' not in segs
    assert '\u76f8\u4e92\u4f5c\u7528' in segs


def test_auto_create_missing_segments():
    mapper = ChapterMapper()
    mapper.textbooks = [{'textbookId': 1, 'textbookName': 'Book A'}]
    mapper.trees = {
        1: [
            {'id': 10, 'label': '\u76f8\u4e92\u4f5c\u7528', 'parent_id': None, 'parent_label': '', 'depth': 0},
        ],
    }
    client = _MockClient()
    item = {
        'detail_chapter_text': '\u9ad8\u4e2d\u7269\u7406\u7efc\u5408\u5e93 > \u529b\u5b66 > \u76f8\u4e92\u4f5c\u7528 > \u5171\u70b9\u529b\u7684\u5e73\u8861 > \u5e73\u8861\u72b6\u6001',
        'page_ctx': {},
    }
    cfg = {'textbook_id': 1, 'auto_create_chapters': True}
    result = mapper.match(item, cfg, client=client, on_log=None)
    assert result['mapped'] is True
    assert result.get('chapter_id')
    assert result.get('created') is True
    assert len(client.created) >= 2
    names = [c['chapterName'] for c in client.created]
    assert '\u5171\u70b9\u529b\u7684\u5e73\u8861' in names


def test_match_existing_without_create():
    mapper = ChapterMapper()
    mapper.textbooks = [{'textbookId': 1, 'textbookName': 'Book A'}]
    mapper.trees = {
        1: [
            {'id': 10, 'label': '\u76f8\u4e92\u4f5c\u7528', 'parent_id': None, 'parent_label': '', 'depth': 0},
            {'id': 11, 'label': '\u5171\u70b9\u529b\u7684\u5e73\u8861', 'parent_id': 10, 'parent_label': '\u76f8\u4e92\u4f5c\u7528', 'depth': 1},
            {'id': 12, 'label': '\u5e73\u8861\u72b6\u6001', 'parent_id': 11, 'parent_label': '\u5171\u70b9\u529b\u7684\u5e73\u8861', 'depth': 2},
        ],
    }
    client = _MockClient()
    item = {
        'detail_chapter_text': '\u76f8\u4e92\u4f5c\u7528 > \u5171\u70b9\u529b\u7684\u5e73\u8861 > \u5e73\u8861\u72b6\u6001',
    }
    result = mapper.match(item, {'textbook_id': 1}, client=client)
    assert result['mapped'] is True
    assert result['chapter_id'] == 12
    assert not client.created


def main():
    test_split_path_skips_comprehensive_lib()
    test_auto_create_missing_segments()
    test_match_existing_without_create()
    print('chapter auto-create tests: OK')


if __name__ == '__main__':
    main()
