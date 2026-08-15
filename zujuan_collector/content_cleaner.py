# -*- coding: utf-8 -*-
"""Convert zujuan HTML to question-bank friendly text and extract media."""

import html as html_module
import re
from urllib.parse import urljoin

from bs4 import BeautifulSoup, NavigableString, Tag

ZUJUAN_BASE = 'https://zujuan.xkw.com'
SKIP_CRUMBS = {'\u7ec4\u5377\u7f51', '\u7ae0\u8282\u9009\u9898'}
ALLOWED_HTML_TAGS = frozenset({
    'table', 'tbody', 'thead', 'tr', 'td', 'th',
    'p', 'div', 'span', 'br', 'img', 'sub', 'sup',
    'i', 'em', 'b', 'strong', 'ul', 'li',
})
TABLE_STYLE = 'border-collapse:collapse;margin:8px 0;max-width:100%;'
CELL_STYLE = 'border:1px solid #333;padding:4px 8px;text-align:center;vertical-align:middle;'
OPTION_LIST_STYLE = 'list-style:none;margin:10px 0 0;padding:0;'
OPTION_ITEM_STYLE = 'display:flex;align-items:flex-start;gap:8px;margin:0 0 8px;padding:0;'
OPTION_LABEL_STYLE = 'flex-shrink:0;font-weight:600;line-height:1.75;'
OPTION_TEXT_STYLE = 'flex:1;line-height:1.75;text-align:justify;'
IMG_STYLE = 'vertical-align:middle;max-width:100%;height:auto;'
_OPTION_LABEL_RE = re.compile(
    r'^\s*([A-Ha-h])[\.\u3001\u3002\uff0e\)\uff09:\uff1a\s]+(.*)$',
    re.S,
)


def _normalize_url(src):
    src = (src or '').strip()
    if not src:
        return ''
    if src.startswith('//'):
        src = 'https:' + src
    elif not src.startswith('http'):
        src = urljoin(ZUJUAN_BASE, src)
    from .image_cleaner import normalize_image_download_url
    return normalize_image_download_url(src)


def _strip_number_prefix(text):
    return re.sub(r'^\s*\d+\s*[\.\u3001\u3002\uff0e\)\uff09:\uff1a\s]*', '', text or '').strip()


def _img_cell_context(img_tag):
    td = img_tag.find_parent(['td', 'th'])
    if td:
        return td.get_text(' ', strip=True)
    parent = img_tag.find_parent('p')
    return parent.get_text(' ', strip=True) if parent else ''


def _formula_img_to_latex(img_tag):
    ctx = _img_cell_context(img_tag)
    if '\u5012\u6570' in ctx:
        return r' $\frac{1}{m}/\mathrm{kg}^{-1}$'
    if '\u52a0\u901f\u5ea6' in ctx:
        return r' $a/(\mathrm{m}\cdot\mathrm{s}^{-2})$'
    return ''


def _img_to_inline(node, img_state):
    src = _normalize_url(node.get('src') or node.get('data-src'))
    if not src or is_answer_image_url(src):
        return ''
    if '/formula/' in src or 'quesimg' in src:
        latex = _formula_img_to_latex(node)
        if latex:
            return latex
    if src in img_state['seen']:
        idx = img_state['urls'].index(src) + 1
    else:
        img_state['urls'].append(src)
        img_state['seen'].add(src)
        idx = len(img_state['urls'])
    return f'![\u56fe{idx}]'


def _node_to_math_text(node, img_state=None):
    if isinstance(node, NavigableString):
        return str(node)
    if not isinstance(node, Tag):
        return ''
    name = (node.name or '').lower()
    if name == 'img':
        if img_state is not None:
            return _img_to_inline(node, img_state)
        return ''
    if name in ('br',):
        return '\n'
    if name in ('td', 'th'):
        inner = ''.join(_node_to_math_text(c, img_state) for c in node.children).strip()
        return f'{inner}\t' if inner else ''
    if name == 'tr':
        inner = ''.join(_node_to_math_text(c, img_state) for c in node.children).strip()
        inner = re.sub(r'\t+', ' | ', inner).strip(' |')
        return f'{inner}\n' if inner else ''
    if name == 'table':
        if (node.get('name') or '') == 'optionsTable':
            return ''
        inner = ''.join(_node_to_math_text(c, img_state) for c in node.children).strip()
        return f'{inner}\n' if inner else ''
    if name in ('p', 'div', 'li'):
        inner = ''.join(_node_to_math_text(c, img_state) for c in node.children).strip()
        return f'{inner}\n' if inner else ''
    if name == 'sub':
        body = ''.join(_node_to_math_text(c, img_state) for c in node.children).strip()
        return f'_{{{body}}}' if body else ''
    if name == 'sup':
        body = ''.join(_node_to_math_text(c, img_state) for c in node.children).strip()
        return f'^{{{body}}}' if body else ''
    if name == 'bk':
        size = node.get('size') or '6'
        try:
            n = max(4, int(size))
        except ValueError:
            n = 6
        return '_' * n
    if name in ('i', 'em', 'b', 'strong', 'span', 'font'):
        return ''.join(_node_to_math_text(c, img_state) for c in node.children)
    return ''.join(_node_to_math_text(c, img_state) for c in node.children)


def is_answer_image_url(url):
    u = (url or '').lower()
    return 'getanswerandparse' in u or 'imzujuan.xkw.com/getanswer' in u


def collect_answer_parse_image_urls(node):
    """Collect zujuan composite answer/parse image URLs (kept out of stem image_urls)."""
    urls = []
    seen = set()
    if not node:
        return urls
    root = node
    if isinstance(node, str):
        soup = BeautifulSoup(f'<div>{node}</div>', 'lxml')
        root = soup.find('div')
        if not root:
            return urls
    for img in root.find_all('img'):
        src = _normalize_url(img.get('src') or img.get('data-src') or '')
        if src and is_answer_image_url(src) and src not in seen:
            seen.add(src)
            urls.append(src)
    return urls


def filter_question_images(urls):
    return [u for u in (urls or []) if u and not is_answer_image_url(u)]


def _remove_option_tables(root):
    for table in root.find_all('table', attrs={'name': 'optionsTable'}):
        table.decompose()
    for table in root.find_all('table'):
        if table.get('name') == 'optionsTable':
            table.decompose()


_STYLE_KEYS = (
    'text-align', 'vertical-align', 'width', 'height', 'max-width', 'margin',
    'border', 'border-width', 'border-style', 'border-color', 'border-collapse',
)


def _parse_style_string(style_text):
    out = {}
    for chunk in (style_text or '').split(';'):
        piece = chunk.strip()
        if not piece or ':' not in piece:
            continue
        key, val = piece.split(':', 1)
        key = key.strip().lower()
        if key in _STYLE_KEYS:
            out[key] = val.strip()
    return out


def _pick_style(node, defaults):
    merged = _parse_style_string(defaults)
    merged.update(_parse_style_string(node.get('style')))
    if not merged:
        return defaults
    return ';'.join(f'{key}:{val}' for key, val in merged.items())


def _render_option_table_as_list(table, img_urls):
    """Render zujuan optionsTable as a vertical list instead of bordered table cells."""
    letters = 'ABCDEFGH'
    items = []
    td_index = 0
    for td in table.find_all('td'):
        inner = ''.join(_render_stem_node(c, img_urls) for c in td.children).strip()
        plain = td.get_text(' ', strip=True)
        label_m = _OPTION_LABEL_RE.match(plain)
        if label_m:
            letter = label_m.group(1).upper()
            body = re.sub(
                r'^\s*[A-Ha-h][\.\u3001\u3002\uff0e\)\uff09:\uff1a\s]*',
                '',
                inner or label_m.group(2).strip(),
            ).strip()
        elif td_index < len(letters):
            letter = letters[td_index]
            body = inner
        else:
            td_index += 1
            continue
        td_index += 1
        if not body:
            continue
        items.append(
            f'<li class="qb-option-item" style="{OPTION_ITEM_STYLE}">'
            f'<span class="qb-option-label" style="{OPTION_LABEL_STYLE}">{letter}.</span>'
            f'<span class="qb-option-text" style="{OPTION_TEXT_STYLE}">{body}</span>'
            f'</li>'
        )
    if not items:
        return ''
    return f'<ul class="qb-options" style="{OPTION_LIST_STYLE}">{"".join(items)}</ul>'


def _render_stem_node(node, img_urls):
    if isinstance(node, NavigableString):
        text = str(node).replace('\xa0', ' ')
        if not text.strip():
            return text.replace('\xa0', ' ')
        return html_module.escape(text)
    if not isinstance(node, Tag):
        return ''
    name = (node.name or '').lower()
    if name == 'bk':
        size = node.get('size') or '6'
        try:
            width = max(3, int(size)) * 0.55
        except ValueError:
            width = 3.5
        return (
            f'<span class="qb-blank" style="display:inline-block;min-width:{width}em;'
            f'border-bottom:1px solid #303133;padding:0 2px;">&nbsp;</span>'
        )
    if name == 'img':
        src = _normalize_url(node.get('src') or node.get('data-src'))
        if not src or is_answer_image_url(src):
            return ''
        if src not in img_urls:
            img_urls.append(src)
        style = _pick_style(node, IMG_STYLE)
        return f'<img src="{html_module.escape(src, quote=True)}" style="{style}" alt="" />'
    if name == 'br':
        return '<br/>'
    if name == 'table':
        if (node.get('name') or '') == 'optionsTable':
            return _render_option_table_as_list(node, img_urls)
        inner = ''.join(_render_stem_node(c, img_urls) for c in node.children)
        style = _pick_style(node, TABLE_STYLE)
        return f'<table style="{style}">{inner}</table>'
    if name in ('tbody', 'thead'):
        inner = ''.join(_render_stem_node(c, img_urls) for c in node.children)
        return f'<{name}>{inner}</{name}>'
    if name == 'tr':
        inner = ''.join(_render_stem_node(c, img_urls) for c in node.children)
        return f'<tr>{inner}</tr>'
    if name in ('td', 'th'):
        inner = ''.join(_render_stem_node(c, img_urls) for c in node.children)
        style = _pick_style(node, CELL_STYLE)
        tag = 'th' if name == 'th' else 'td'
        return f'<{tag} style="{style}">{inner}</{tag}>'
    if name == 'p':
        inner = ''.join(_render_stem_node(c, img_urls) for c in node.children).strip()
        if not inner:
            return ''
        style = _pick_style(node, 'margin:6px 0;text-align:justify;')
        return f'<p style="{style}">{inner}</p>'
    if name == 'div':
        return ''.join(_render_stem_node(c, img_urls) for c in node.children)
    if name in ALLOWED_HTML_TAGS:
        inner = ''.join(_render_stem_node(c, img_urls) for c in node.children)
        return f'<{name}>{inner}</{name}>' if inner else ''
    return ''.join(_render_stem_node(c, img_urls) for c in node.children)


def sanitize_stem_html(html, keep_option_tables=False):
    """Return sanitized HTML preserving tables/images layout, plus image url list."""
    if not html:
        return '', []
    soup = BeautifulSoup(f'<div>{html}</div>', 'lxml')
    root = soup.find('div')
    if not root:
        return '', []
    if not keep_option_tables:
        _remove_option_tables(root)
    if (
        len(root.contents) == 1
        and isinstance(root.contents[0], Tag)
        and (root.contents[0].name or '').lower() == 'div'
    ):
        root = root.contents[0]
    img_urls = []
    body = ''.join(_render_stem_node(child, img_urls) for child in root.children)
    body = re.sub(r'\n{3,}', '\n', body).strip()
    return body, filter_question_images(img_urls)


def is_html_content(text):
    return bool(re.search(
        r'<(table|img|p|div|span|tbody|tr|td|th|sub|sup|br|i|em|b|strong|bk)\b',
        str(text or ''),
        re.I,
    ))


def extract_html_image_urls(html):
    if not html:
        return []
    urls = []
    seen = set()
    soup = BeautifulSoup(html, 'lxml')
    for img in soup.find_all('img'):
        src = _normalize_url(img.get('src') or img.get('data-src') or '')
        if src and src not in seen and not is_answer_image_url(src):
            seen.add(src)
            urls.append(src)
    return urls


def replace_html_image_urls(html, mapping):
    if not html or not mapping:
        return html
    result = html
    for remote, local in sorted(mapping.items(), key=lambda item: -len(item[0])):
        if remote and local:
            result = result.replace(remote, local)
    return result


def clean_html_fragment(html):
    """Return (plain_text, image_urls)."""
    if not html:
        return '', []
    soup = BeautifulSoup(f'<div>{html}</div>', 'lxml')
    root = soup.find('div')
    if not root:
        return '', []
    _remove_option_tables(root)
    img_state = {'urls': [], 'seen': set()}
    text = _node_to_math_text(root, img_state).strip()
    text = text.replace('\xa0', ' ')
    lines = [re.sub(r'[ \t]+', ' ', ln).strip() for ln in text.split('\n')]
    text = '\n'.join(ln for ln in lines if ln)
    text = re.sub(r'\n{3,}', '\n\n', text)
    text = re.sub(r'[\uff08]\s*[\uff09]', '\uff08    \uff09', text)
    return _strip_number_prefix(text.strip()), filter_question_images(img_state['urls'])


def clean_option_html(html):
    def _strip_prefix(text):
        return re.sub(r'^\s*[A-Ha-h][\.\u3001\u3002\uff0e\)\uff09:\uff1a\s]*', '', text or '').strip()

    html_text, _ = sanitize_stem_html(html or '')
    if html_text and is_html_content(html_text):
        soup = BeautifulSoup(f'<div>{html_text}</div>', 'lxml')
        root = soup.find('div')
        if root:
            for node in root.descendants:
                if isinstance(node, NavigableString):
                    raw = str(node)
                    if not raw.strip():
                        continue
                    stripped = _strip_prefix(raw)
                    if stripped != raw.strip():
                        node.replace_with(raw.replace(raw.strip(), stripped, 1))
                    break
            rendered = ''.join(str(c) for c in root.contents).strip()
            return rendered

    text, _ = clean_html_fragment(html)
    return _strip_prefix(text)


def parse_page_context(html):
    soup = BeautifulSoup(html, 'lxml')
    crumbs = []
    for a in soup.select('.bread-nav .bread-nav-item'):
        label = a.get_text(strip=True)
        if label and label not in SKIP_CRUMBS:
            crumbs.append(label)

    textbook = ''
    for a in soup.select('#chapter_textbooks .font-item.selected, #chapter_textbooks a.selected'):
        textbook = a.get_text(strip=True)
        if textbook:
            break
    if not textbook and crumbs:
        textbook = crumbs[-1]
    if not textbook:
        for a in soup.select('.bread-nav .bread-nav-item'):
            href = (a.get('href') or '').strip()
            if '/zj' in href.lower():
                textbook = a.get_text(strip=True)
                if textbook:
                    break

    chapter_node = ''
    for sel in ('.tree-anchor.selected', '.tree-node.selected .tree-anchor', '.tree-node.tree-selected .tree-anchor'):
        node = soup.select_one(sel)
        if node:
            chapter_node = node.get_text(strip=True)
            break

    return {
        'breadcrumb': ' > '.join(crumbs),
        'textbook': textbook,
        'chapter_node': chapter_node,
    }


def parse_detail_context(html):
    soup = BeautifulSoup(html, 'lxml')
    crumbs = []
    for a in soup.select('.bread-nav .bread-nav-item'):
        label = a.get_text(strip=True)
        if label and label not in SKIP_CRUMBS:
            crumbs.append(label)
    if crumbs and crumbs[0] == '\u7ec4\u5377\u7f51':
        crumbs = crumbs[1:]
    return {
        'chapter_text': ' > '.join(crumbs) if crumbs else '',
        'knowledge_leaf': crumbs[-1] if crumbs else '',
    }


def resolve_chapter_text(item, page_ctx=None, cfg=None):
    parts = []
    page_ctx = page_ctx or {}
    cfg = cfg or {}

    if item.get('chapter_text'):
        return item['chapter_text']

    if page_ctx.get('textbook'):
        parts.append(page_ctx['textbook'])
    if page_ctx.get('chapter_node'):
        parts.append(page_ctx['chapter_node'])
    elif item.get('category_name'):
        parts.append(item['category_name'])

    if parts:
        return ' > '.join(parts)

    if item.get('category_name'):
        return item['category_name']

    cfg_chapter = (cfg.get('chapter_text') or '').strip()
    if cfg_chapter and cfg_chapter not in ('\u7ec4\u5377\u7f51\u91c7\u96c6',):
        return cfg_chapter

    return item.get('category_name') or cfg_chapter or '\u7ec4\u5377\u7f51\u91c7\u96c6'


def merge_image_urls(*groups):
    out = []
    seen = set()
    for group in groups:
        for url in group or []:
            if url and url not in seen:
                seen.add(url)
                out.append(url)
    return out
