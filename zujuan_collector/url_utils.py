# -*- coding: utf-8 -*-
"""Parse and normalize zujuan.xkw.com URLs."""

import re
from urllib.parse import urlparse

from bs4 import BeautifulSoup

ZUJUAN_ZJ_TEXTBOOK = {
    '135937': '\u5fc5\u4fee \u7b2c\u4e00\u518c',
    '136248': '\u5fc5\u4fee \u7b2c\u4e00\u518c',
    '149857': '\u5fc5\u4fee \u7b2c\u4e8c\u518c',
    '149858': '\u5fc5\u4fee \u7b2c\u4e8c\u518c',
    '149884': '\u5fc5\u4fee \u7b2c\u4e09\u518c',
    '150289': '\u9009\u62e9\u6027\u5fc5\u4fee \u7b2c\u4e00\u518c',
    '150290': '\u9009\u62e9\u6027\u5fc5\u4fee \u7b2c\u4e8c\u518c',
    '150291': '\u9009\u62e9\u6027\u5fc5\u4fee \u7b2c\u4e09\u518c',
}


def infer_chapter_node_from_html(html, list_url):
    """Match chapter tree node label from list URL (/zj{id}/)."""
    m = re.search(r'/zj(\d+)(?:/|$)', list_url or '')
    if not m:
        return ''
    zj_id = m.group(1)
    if zj_id in ZUJUAN_ZJ_TEXTBOOK:
        return ''
    soup = BeautifulSoup(html or '', 'lxml')
    needle = f'/zj{zj_id}'
    for a in soup.select('.tree-anchor, a.tree-anchor, .tree-node-name a'):
        href = (a.get('href') or a.get('data-href') or '').strip()
        if needle in href or href.endswith(f'zj{zj_id}'):
            label = a.get_text(strip=True)
            if label:
                return label
    return ''


def infer_textbook_from_url(url):
    m = re.search(r'/zj(\d+)(?:/|$)', url or '')
    if not m:
        return ''
    return ZUJUAN_ZJ_TEXTBOOK.get(m.group(1), '')


def page_url(base_url, page_num):
    base_url = base_url.rstrip('/')
    if page_num <= 1:
        return base_url + '/'
    return f'{base_url}/o2p{page_num}/'


ZUJUAN_HOST = 'zujuan.xkw.com'

SUBJECT_LABELS = {
    'gzwl': '\u9ad8\u4e2d\u7269\u7406',
    'czwl': '\u521d\u4e2d\u7269\u7406',
    'gzsx': '\u9ad8\u4e2d\u6570\u5b66',
    'czsx': '\u521d\u4e2d\u6570\u5b66',
    'gzhx': '\u9ad8\u4e2d\u5316\u5b66',
    'czhx': '\u521d\u4e2d\u5316\u5b66',
    'gzsw': '\u9ad8\u4e2d\u751f\u7269',
    'czsw': '\u521d\u4e2d\u751f\u7269',
    'gzyw': '\u9ad8\u4e2d\u8bed\u6587',
    'czyw': '\u521d\u4e2d\u8bed\u6587',
    'gzyy': '\u9ad8\u4e2d\u82f1\u8bed',
    'czyy': '\u521d\u4e2d\u82f1\u8bed',
    'gzzz': '\u9ad8\u4e2d\u653f\u6cbb',
    'czzz': '\u521d\u4e2d\u653f\u6cbb',
    'gzls': '\u9ad8\u4e2d\u5386\u53f2',
    'czls': '\u521d\u4e2d\u5386\u53f2',
    'gzdl': '\u9ad8\u4e2d\u5730\u7406',
    'czdl': '\u521d\u4e2d\u5730\u7406',
    'gzxxjs': '\u9ad8\u4e2d\u4fe1\u606f\u6280\u672f',
    'gztyjs': '\u9ad8\u4e2d\u901a\u7528\u6280\u672f',
    'gzry': '\u9ad8\u4e2d\u65e5\u8bed',
}

_LIST_RE = re.compile(
    r'/([a-z]{2,6})/zj(\d+)(?:/o2p(\d+)/?)?/?$',
    re.IGNORECASE,
)
_DETAIL_RE = re.compile(r'/(\d+)q(\d+)\.html', re.IGNORECASE)


def _ensure_scheme(url):
    url = (url or '').strip()
    if not url:
        return ''
    if not url.startswith('http'):
        url = 'https://' + url.lstrip('/')
    return url


def parse_zujuan_url(url):
    """Return metadata for a zujuan URL."""
    raw = (url or '').strip()
    empty = {
        'valid': False,
        'kind': 'unknown',
        'raw': raw,
        'subject_code': '',
        'subject_label': '',
        'zj_id': '',
        'page_num': 1,
        'base_url': '',
        'page_url': '',
        'textbook_hint': '',
        'summary': '\u8bf7\u8f93\u5165\u7ec4\u5377\u7f51\u7ae0\u8282\u5217\u8868\u5730\u5740',
    }
    if not raw:
        return empty

    full = _ensure_scheme(raw)
    parsed = urlparse(full)
    host = (parsed.netloc or '').lower()
    if ZUJUAN_HOST not in host:
        empty['summary'] = '\u975e\u7ec4\u5377\u7f51\u5730\u5740'
        return empty

    path = parsed.path or ''

    m_detail = _DETAIL_RE.search(path)
    if m_detail:
        return {
            'valid': True,
            'kind': 'detail',
            'raw': raw,
            'subject_code': '',
            'subject_label': '',
            'zj_id': m_detail.group(2),
            'page_num': 1,
            'base_url': '',
            'page_url': full.split('?')[0],
            'textbook_hint': '',
            'summary': f'\u9898\u76ee\u8be6\u60c5\u9875\uff08ID:{m_detail.group(2)}\uff09\uff0c\u8bf7\u6539\u7528\u7ae0\u8282\u5217\u8868\u5730\u5740',
        }

    m_list = _LIST_RE.search(path)
    if not m_list:
        empty['summary'] = '\u65e0\u6cd5\u8bc6\u522b\u7684\u7ec4\u5377\u7f51\u5730\u5740\u683c\u5f0f'
        return empty

    subject_code = m_list.group(1).lower()
    zj_id = m_list.group(2)
    page_num = int(m_list.group(3) or 1)
    base_url = f'https://{ZUJUAN_HOST}/{subject_code}/zj{zj_id}'
    current_page_url = page_url(base_url, page_num)
    subject_label = SUBJECT_LABELS.get(subject_code, subject_code)
    textbook_hint = infer_textbook_from_url(base_url)

    parts = [subject_label, f'zj{zj_id}', f'\u7b2c{page_num}\u9875']
    if textbook_hint:
        parts.insert(1, textbook_hint)
    summary = ' \u00b7 '.join(parts)

    return {
        'valid': True,
        'kind': 'list',
        'raw': raw,
        'subject_code': subject_code,
        'subject_label': subject_label,
        'zj_id': zj_id,
        'page_num': page_num,
        'base_url': base_url,
        'page_url': current_page_url,
        'textbook_hint': textbook_hint,
        'summary': summary,
    }


def normalize_list_url(url):
    """Normalize to chapter list base URL (page 1)."""
    info = parse_zujuan_url(url)
    if info.get('kind') == 'list' and info.get('base_url'):
        return info['base_url'] + '/'
    return (url or '').strip()


def list_page_url(base_url, page_num):
    return page_url((base_url or '').rstrip('/'), page_num)


def parse_pager_from_html(html):
    """Extract pagination info from a list page."""
    soup = BeautifulSoup(html or '', 'lxml')
    pager = soup.select_one('.tk-pager')
    if not pager:
        return {
            'current_page': 1,
            'has_next': False,
            'next_page': None,
            'total_pages': None,
            'page_size': None,
            'total_items': None,
        }

    active = soup.select_one('.tk-pager a.page-num.active')
    current_page = 1
    if active:
        try:
            current_page = int(active.get('data-num') or 1)
        except (TypeError, ValueError):
            current_page = 1

    next_btn = soup.select_one('.tk-pager a.next-page')
    classes = ' '.join(next_btn.get('class') or []) if next_btn else ''
    has_next = bool(next_btn and 'disabled' not in classes)

    total_pages = None
    last_btn = soup.select_one('.tk-pager a.to-last')
    if last_btn and last_btn.get('lastid'):
        try:
            total_pages = int(last_btn['lastid'])
        except (TypeError, ValueError):
            total_pages = None

    page_size = None
    total_items = None
    if pager.get('data-size'):
        try:
            page_size = int(pager['data-size'])
        except (TypeError, ValueError):
            page_size = None
    if pager.get('data-sum'):
        try:
            total_items = int(pager['data-sum'])
        except (TypeError, ValueError):
            total_items = None

    return {
        'current_page': current_page,
        'has_next': has_next,
        'next_page': current_page + 1 if has_next else None,
        'total_pages': total_pages,
        'page_size': page_size,
        'total_items': total_items,
    }
