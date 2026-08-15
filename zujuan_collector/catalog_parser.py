# -*- coding: utf-8 -*-
"""Parse zujuan chapter catalog HTML (subject / version / textbook / chapter tree)."""

import re
from urllib.parse import urljoin

from bs4 import BeautifulSoup

ZUJUAN_BASE = 'https://zujuan.xkw.com'

_PHASE_LABELS = {
    'primary': '\u5c0f\u5b66',
    'junior': '\u521d\u4e2d',
    'senior': '\u9ad8\u4e2d',
    'zhijiao': '\u4e2d\u804c',
}

_SUBJECT_ID_TO_CODE = {
    ('senior', '10'): 'gzyw',
    ('senior', '11'): 'gzsx',
    ('senior', '12'): 'gzyy',
    ('senior', '13'): 'gzwl',
    ('senior', '14'): 'gzhx',
    ('senior', '15'): 'gzsw',
    ('senior', '16'): 'gzzz',
    ('senior', '17'): 'gzls',
    ('senior', '18'): 'gzdl',
    ('junior', '1'): 'czyw',
    ('junior', '2'): 'czsx',
    ('junior', '3'): 'czyy',
    ('junior', '4'): 'czwl',
    ('junior', '5'): 'czhx',
    ('junior', '6'): 'czsw',
    ('senior', '27'): 'gzxxjs',
    ('senior', '28'): 'gztyjs',
    ('senior', '33'): 'gzry',
}


def _zj_id_from_href(href):
    m = re.search(r'/zj(\d+)(?:/|$)', href or '', re.I)
    return m.group(1) if m else ''


def _abs_url(href):
    href = (href or '').strip()
    if not href or href.startswith('javascript'):
        return ''
    return urljoin(ZUJUAN_BASE, href)


def parse_subject_menu(html):
    """Parse phase tabs and subject nav items from chapter list page."""
    soup = BeautifulSoup(html or '', 'lxml')
    phases = []
    for tab in soup.select('.subject-nav .tabs .tab[data-tab]'):
        key = tab.get('data-tab', '').strip()
        phases.append({
            'phase': key,
            'label': _PHASE_LABELS.get(key, tab.get_text(strip=True)),
            'selected': 'selected' in (tab.get('class') or []),
        })

    subjects = []
    for box in soup.select('.subject-nav .tab-box.grade-item__list[data-tab]'):
        phase = box.get('data-tab', '').strip()
        for a in box.select('a.subject-nav-item[data-subject-id]'):
            sid = a.get('data-subject-id', '').strip()
            subjects.append({
                'phase': phase,
                'subject_id': sid,
                'name': a.get_text(strip=True),
                'subject_code': _SUBJECT_ID_TO_CODE.get((phase, sid), ''),
                'selected': 'selected' in (a.get('class') or []),
            })

    title = ''
    title_el = soup.select_one('.subject-menu__title .title-txt')
    if title_el:
        title = title_el.get_text(strip=True)
    return {'title': title, 'phases': phases, 'subjects': subjects}


def parse_versions(html):
    soup = BeautifulSoup(html or '', 'lxml')
    versions = []
    for a in soup.select('#chapter_textbook_version a[versionid]'):
        versions.append({
            'version_id': a.get('versionid', '').strip(),
            'name': a.get_text(strip=True),
            'url_template': (a.get('url') or '').strip(),
            'selected': 'selected' in (a.get('class') or []),
        })
    return versions


def parse_textbooks(html):
    """Parse textbook tabs from full page or API HTML fragment."""
    soup = BeautifulSoup(html or '', 'lxml')
    root = soup.select_one('#chapter_textbooks') or soup
    textbooks = []
    for a in root.select('a[chapter-id], a.font-item[href*="/zj"]'):
        chapter_id = (a.get('chapter-id') or '').strip()
        href = (a.get('href') or '').strip()
        zj_id = chapter_id or _zj_id_from_href(href)
        if not zj_id:
            continue
        name = a.get_text(strip=True)
        if not name or name in ('\u52a0\u8f7d\u4e2d...', '\u52a0\u8f7d\u4e2err...'):
            continue
        textbooks.append({
            'zj_id': zj_id,
            'name': name,
            'url': _abs_url(href),
            'selected': 'selected' in (a.get('class') or []),
        })
    return textbooks


def _parse_tree_node(li):
    anchor = li.select_one('.tree-anchor')
    label = anchor.get_text(strip=True) if anchor else ''
    href = ''
    if anchor:
        href = (anchor.get('data-href') or anchor.get('href') or '').strip()
    zj_id = (li.get('tree-id') or '').strip() or _zj_id_from_href(href)
    try:
        level = int(li.get('data-level') or 0)
    except (TypeError, ValueError):
        level = 0

    children = []
    child_ul = li.select_one(':scope > ul.tree-ul')
    if child_ul:
        for child_li in child_ul.select(':scope > li.tree-node'):
            child = _parse_tree_node(child_li)
            if child:
                children.append(child)

    if not label and not children:
        return None

    return {
        'zj_id': zj_id,
        'label': label,
        'level': level,
        'url': _abs_url(href),
        'children': children,
    }


def parse_chapter_tree(html):
    soup = BeautifulSoup(html or '', 'lxml')
    tree_root = soup.select_one('.tree-box .tk-tree')
    if not tree_root:
        return []

    chapters = []
    top_ul = tree_root.select_one(':scope > ul.tree-ul')
    if not top_ul:
        return chapters
    for li in top_ul.select(':scope > li.tree-node'):
        node = _parse_tree_node(li)
        if node:
            chapters.append(node)
    return chapters


def count_tree_nodes(nodes):
    total = 0
    for node in nodes or []:
        total += 1
        total += count_tree_nodes(node.get('children'))
    return total


def flatten_tree_paths(nodes, prefix=None):
    prefix = prefix or []
    paths = []
    for node in nodes or []:
        path = prefix + [node.get('label') or '']
        paths.append({
            'path': ' > '.join(p for p in path if p),
            'zj_id': node.get('zj_id', ''),
            'url': node.get('url', ''),
            'level': node.get('level', 0),
        })
        paths.extend(flatten_tree_paths(node.get('children'), path))
    return paths
