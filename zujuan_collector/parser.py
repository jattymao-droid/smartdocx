# -*- coding: utf-8 -*-
"""Parse zujuan.xkw.com question HTML."""

import json
import re
from bs4 import BeautifulSoup

from .content_cleaner import (
    clean_html_fragment,
    clean_option_html,
    collect_answer_parse_image_urls,
    extract_html_image_urls,
    filter_question_images,
    is_html_content,
    merge_image_urls,
    parse_detail_context,
    parse_page_context,
    resolve_chapter_text,
    sanitize_stem_html,
)
from .ocr_client import is_weak_analysis, normalize_analysis_text, strip_watermark_noise
from .answer_extractor import enrich_answer_from_analysis

TYPE_MAP = {
    '\u5355\u9009\u9898': 'single',
    '\u591a\u9009\u9898': 'multi',
    '\u5224\u65ad\u9898': 'judge',
    '\u586b\u7a7a\u9898': 'fill',
    '\u77e5\u8bc6\u70b9\u586b\u7a7a\u9898': 'knowledge_fill',
    '\u5b9e\u9a8c\u9898': 'experiment',
    '\u89e3\u7b54\u9898': 'answer',
    '\u7efc\u5408\u9898': 'comprehensive',
    '\u9605\u8bfb\u9898': 'reading',
    '\u4f5c\u56fe\u9898': 'drawing',
    '\u7b80\u7b54\u9898': 'short',
}

PREFIX_TYPE_MAP = {
    '\u5355\u9009': 'single',
    '\u591a\u9009': 'multi',
    '\u4e0d\u5b9a\u9879': 'multi',
    '\u53cc\u9009': 'multi',
    '\u5224\u65ad': 'judge',
    '\u586b\u7a7a': 'fill',
    '\u77e5\u8bc6\u70b9\u586b\u7a7a': 'knowledge_fill',
    '\u5b9e\u9a8c': 'experiment',
    '\u89e3\u7b54': 'answer',
    '\u7efc\u5408': 'comprehensive',
    '\u9605\u8bfb': 'reading',
    '\u4f5c\u56fe': 'drawing',
    '\u7b80\u7b54': 'short',
}


STEM_TYPES_KEEP_OPTIONS = frozenset({
    'comprehensive', 'experiment', 'reading', 'answer',
})


def _keep_options_in_stem(question_type):
    return (question_type or '') in STEM_TYPES_KEEP_OPTIONS


CHOICE_TYPES = frozenset({'single', 'multi'})


def _select_content_container(root, question_id=''):
    if not root:
        return None
    if question_id:
        ques = root.select_one(f'.quesroot[questionid="{question_id}"]')
        if ques:
            root = ques
    return root.select_one('.exam-item__cnt') or root.select_one('.quest-cnt')


def _select_stem_container(soup, question_id=''):
    return _select_content_container(soup, question_id) or soup.select_one('.quest-cnt')


def _parse_stem_from_raw(raw_html, question_type=None, options_parsed=False):
    """Return (content, stem_image_urls, plain_text) using one consistent pipeline."""
    keep_option_tables = _keep_options_in_stem(question_type)
    if not keep_option_tables and question_type in CHOICE_TYPES and not options_parsed:
        keep_option_tables = True
    stem_html, html_images = sanitize_stem_html(
        raw_html,
        keep_option_tables=keep_option_tables,
    )
    plain_text, plain_images = clean_html_fragment(raw_html)
    has_table = bool(re.search(r'<table\b', raw_html or '', re.I))
    has_formula_img = bool(re.search(r'quesimg/Upload/formula', raw_html or '', re.I))
    if stem_html and (
        is_html_content(stem_html) or html_images or has_table or has_formula_img
    ):
        return stem_html, html_images or plain_images, plain_text
    content = plain_text or stem_html
    images = plain_images or html_images
    return content, images, plain_text


def _normalize_answer_text(text):
    return strip_watermark_noise((text or '').replace('\r\n', '\n').strip())


def _parse_multi_answer(text):
    raw = (text or '').strip().upper()
    if not raw:
        return []
    for pat in (
        r'(?:\u7b54\u6848|\u6545\u9009|\u9009)\s*[:\uff1a]?\s*([A-H]+)',
        r'\u3010\u7b54\u6848\u3011\s*([A-H]+)',
    ):
        m = re.search(pat, raw)
        if m:
            return sorted(set(m.group(1)))
    compact = re.sub(r'\s+', '', raw)
    if re.fullmatch(r'[A-H]+', compact):
        return sorted(set(compact))
    return []


SEP = r'[\.\u3001\u3002\uff0e\)\uff09:\uff1a\s]'

_INLINE_OPTION_RE = re.compile(
    r'([A-H])\s*[．\.、。\uff0e\)\uff09:：]\s*(.+?)(?=\s*[A-H]\s*[．\.、。\uff0e\)\uff09:：]|$)',
    re.S,
)


def parse_options_from_table(table):
    options = []
    option_images = []
    letters = 'ABCDEFGH'
    td_index = 0
    for td in table.find_all('td'):
        raw_html = ''.join(str(x) for x in td.contents)
        plain = td.get_text(' ', strip=True)
        label_m = re.search(r'^\s*([A-Ha-h])' + SEP, plain)
        if not label_m:
            label_m = re.search(r'([A-Ha-h])' + SEP, re.sub(r'<[^>]+>', '', raw_html))
        if label_m:
            letter = label_m.group(1).upper()
        elif td_index < len(letters):
            letter = letters[td_index]
        else:
            td_index += 1
            continue
        td_index += 1
        body = clean_option_html(raw_html) or clean_option_html(plain)
        if is_html_content(body):
            imgs = extract_html_image_urls(body)
        else:
            body, imgs = clean_html_fragment(raw_html)
            body = re.sub(
                r'^\s*[A-Ha-h][\.\u3001\u3002\uff0e\)\uff09:\uff1a\s]*',
                '',
                body or '',
            ).strip()
        options.append({'label': letter, 'text': body})
        option_images.extend(imgs)
    return options, option_images


def parse_inline_options_from_content(soup_cnt):
    if not soup_cnt:
        return [], []
    clone = BeautifulSoup(str(soup_cnt), 'lxml')
    for table in clone.select('table[name="optionsTable"]'):
        table.decompose()
    text = clone.get_text('\n', strip=True)
    if not re.search(r'[A-H][．\.、。\uff0e\)\uff09:：]', text):
        return [], []
    matches = list(_INLINE_OPTION_RE.finditer(text))
    if len(matches) < 2:
        return [], []
    options = []
    seen = set()
    for m in matches:
        letter = m.group(1).upper()
        if letter in seen:
            continue
        seen.add(letter)
        body = (m.group(2) or '').strip()
        if body:
            options.append({'label': letter, 'text': body})
    return (options, []) if len(options) >= 2 else ([], [])


def parse_options_from_content(soup_cnt, question_type=''):
    if not soup_cnt:
        return [], []
    tables = soup_cnt.find_all('table', attrs={'name': 'optionsTable'})
    if not tables:
        for tbl in soup_cnt.find_all('table'):
            if tbl.find('td') and re.search(r'[A-Ha-h]' + SEP, tbl.get_text(' ', strip=True)[:80]):
                tables = [tbl]
                break
    options = []
    option_images = []
    best_opts = []
    best_imgs = []
    for table in tables:
        opts, imgs = parse_options_from_table(table)
        if not opts:
            continue
        if question_type in CHOICE_TYPES:
            if len(opts) > len(best_opts):
                best_opts = opts
                best_imgs = imgs
            continue
        options.extend(opts)
        option_images.extend(imgs)
    if question_type in CHOICE_TYPES and best_opts:
        return best_opts, best_imgs
    if not options:
        options, option_images = parse_inline_options_from_content(soup_cnt)
    return options, option_images


def _format_subquestion_blocks(html):
    """Wrap inline 1．/2． sub-questions into paragraphs for reading passages."""
    if not html:
        return html
    p_style = 'margin:6px 0;text-align:justify'
    text = html
    text = re.sub(r'(</p>)\s*(\d+[\．\.])', rf'\1<p style="{p_style}">\2', text)
    text = re.sub(r'(</table>)\s*(\d+[\．\.])', rf'\1<p style="{p_style}">\2', text)
    text = re.sub(r'(</ul>)\s*(\d+[\．\.])', rf'\1<p style="{p_style}">\2', text)
    text = re.sub(
        r'(<p style="[^"]*text-align:center[^"]*">[^<]*</span></p>)\s*(\d+[\．\.])',
        rf'\1<p style="{p_style}">\2',
        text,
        count=1,
    )
    text = re.sub(r'(<p style="[^"]*">\d+[\．\.].*?)(<table)', r'\1</p>\2', text, flags=re.S)
    text = re.sub(
        r'(<p style="[^"]*">\d+[\．\.].*?)(<ul class="qb-options")',
        r'\1</p>\2',
        text,
        flags=re.S,
    )
    if re.search(r'\d+[\．\.][^<]+$', text) and not text.rstrip().endswith('</p>'):
        text = re.sub(
            r'(\d+[\．\.][^<]+)$',
            rf'<p style="{p_style}">\1</p>',
            text.strip(),
        )
    return text


def _parse_stem_and_options(raw_html, soup_cnt, qtype):
    """Parse stem/options; reading passages keep option tables inside the stem."""
    if qtype in STEM_TYPES_KEEP_OPTIONS:
        content, stem_images, _ = _parse_stem_from_raw(
            raw_html,
            question_type=qtype,
            options_parsed=True,
        )
        content = _format_subquestion_blocks(content)
        if qtype == 'reading':
            return content, stem_images, [], []
        options, option_images = (
            parse_options_from_content(soup_cnt, qtype) if soup_cnt else ([], [])
        )
        return content, stem_images, options, option_images
    options, option_images = (
        parse_options_from_content(soup_cnt, qtype) if soup_cnt else ([], [])
    )
    content, stem_images, _ = _parse_stem_from_raw(
        raw_html,
        question_type=qtype,
        options_parsed=bool(options),
    )
    return content, stem_images, options, option_images


def _multi_subquestion_label(label):
    text = (label or '').strip()
    m = re.search(r'/(\d+)-题', text)
    if m and int(m.group(1)) >= 2:
        return True
    m = re.search(r'(\d+)小题', text)
    if m and int(m.group(1)) >= 2:
        return True
    return False


def detect_question_type(type_label='', qyname='', qyisselect=None, raw_html=''):
    """Detect question type using labels and HTML structure (e.g. 阅读+小题)."""
    qtype = map_question_type(type_label, qyname, qyisselect)
    full_label = (type_label or '').strip()
    html = raw_html or ''

    if _multi_subquestion_label(full_label):
        return 'reading'

    if html:
        n_sub = len(re.findall(r'\d+[\．\.]', html))
        reading_hint = (
            re.search(r'\u9605\u8bfb\u4e0b\u9762|\u5b8c\u6210.*\u5c0f\u9898', html)
            or _multi_subquestion_label(full_label)
            or re.search(r'\u9605\u8bfb', full_label)
        )
        if n_sub >= 2 and reading_hint and (
            'optionsTable' in html
            or re.search(r'[A-Ha-h][\．\.]', html)
        ):
            return 'reading'
        if qtype == 'short' and reading_hint:
            if n_sub >= 1 and ('optionsTable' in html or '<table' in html):
                return 'reading'

    return qtype


def infer_question_type(item):
    """Reclassify mislabeled zujuan items (e.g. 古代诗歌阅读 -> short) using options/answer."""
    qtype = item.get('question_type') or 'short'
    if qtype in CHOICE_TYPES:
        return qtype

    options = item.get('options') or []
    answer_raw = re.sub(r'[\s,，、;；]+', '', (item.get('answer') or '').strip().upper())
    content = item.get('content') or item.get('content_html') or ''
    type_label = item.get('type_label') or ''

    if _multi_subquestion_label(type_label):
        return 'reading'
    n_sub = len(re.findall(r'\d+[\．\.]', content))
    reading_hint = (
        re.search(r'\u9605\u8bfb\u4e0b\u9762|\u5b8c\u6210.*\u5c0f\u9898', content)
        or re.search(r'\u9605\u8bfb', type_label)
    )
    if n_sub >= 2 and reading_hint:
        return 'reading'
    if reading_hint and ('<table' in content or n_sub >= 1):
        return 'reading'

    if len(options) >= 2:
        labels = [str(opt.get('label') or '').upper() for opt in options if opt.get('label')]
        if labels.count('A') > 1 and len(labels) >= 6:
            return 'reading'
        if re.fullmatch(r'[A-H]{2,}', answer_raw):
            return 'multi'
        if re.fullmatch(r'[A-H]', answer_raw):
            return 'single'
        return 'single'

    type_label = item.get('type_label') or ''
    if re.search(r'\u591a\u9009', type_label):
        return 'multi'
    if re.search(r'\u5355\u9009|\u5355\u9898', type_label):
        return 'single'
    if re.search(r'\u9605\u8bfb|\d+\u5c0f\u9898', type_label):
        return 'reading'
    if re.search(r'\u7efc\u5408', type_label):
        return 'comprehensive'

    return qtype


def _map_label_text(text):
    text = (text or '').strip()
    if not text:
        return ''
    head = text.split('-')[0].strip()
    if head in TYPE_MAP:
        return TYPE_MAP[head]
    for prefix, code in PREFIX_TYPE_MAP.items():
        if head.startswith(prefix):
            return code
    return ''


def map_question_type(raw_label, qyname='', qyisselect=None):
    """Map zujuan type labels; qyname (e.g. 单选题) is more reliable than category label."""
    for candidate in (qyname, raw_label):
        mapped = _map_label_text(candidate)
        if mapped:
            return mapped

    full = (raw_label or qyname or '').strip()
    if re.search(r'\u591a\u9009', full):
        return 'multi'
    if re.search(r'\u5355\u9009|\u5355\u9898', full):
        return 'single'
    if re.search(r'\u5224\u65ad', full):
        return 'judge'
    if re.search(r'\u586b\u7a7a', full):
        return 'fill'
    if re.search(r'\u9605\u8bfb|\u5c0f\u9898', full):
        return 'reading'
    if re.search(r'\u7efc\u5408', full):
        return 'comprehensive'

    if str(qyisselect or '').lower() in ('true', '1', 'yes'):
        return 'single'

    return 'short'


def parse_list_item(item_soup, page_ctx=None):
    question_id = item_soup.get('questionid') or ''
    bank_id = item_soup.get('bankid') or '13'
    add_btn = item_soup.select_one('a.addques, a[data-btn-type="quesAdd"]')
    qyname = add_btn.get('qyname', '') if add_btn else ''
    qyisselect = add_btn.get('qyisselect', '') if add_btn else ''
    qdvalue = add_btn.get('qdvalue', '') if add_btn else ''
    category = add_btn.get('categoryname', '') if add_btn else ''
    source = add_btn.get('titleabbreviation', '') if add_btn else ''

    type_label = ''
    info_cnt = item_soup.select_one('.addi-info .info-cnt')
    if info_cnt:
        type_label = info_cnt.get_text(strip=True)

    knowledge = [a.get_text(strip=True) for a in item_soup.select('.knowledge-item')]
    cnt = _select_content_container(item_soup, question_id)
    raw_html = ''.join(str(x) for x in cnt.contents) if cnt else ''
    qtype = detect_question_type(type_label, qyname, qyisselect, raw_html)
    content, stem_images, options, option_images = _parse_stem_and_options(
        raw_html, cnt, qtype,
    )

    detail_path = f'/{bank_id}q{question_id}.html'
    detail_url = f'https://zujuan.xkw.com{detail_path}'

    return {
        'zujuan_id': question_id,
        'bank_id': bank_id,
        'detail_url': detail_url,
        'question_type': qtype,
        'type_label': type_label or qyname,
        'difficulty': float(qdvalue) if qdvalue else 0.5,
        'knowledge_points': knowledge,
        'category_name': category,
        'source_label': source,
        'content': content,
        'content_html': content,
        'image_urls': filter_question_images(merge_image_urls(stem_images, option_images)),
        'options': options,
        'answer': '',
        'analysis': '',
        'chapter_text': resolve_chapter_text({'category_name': category}, page_ctx),
        'page_ctx': page_ctx or {},
    }


def parse_list_page(html, page_ctx=None, on_log=None):
    if page_ctx is None:
        page_ctx = parse_page_context(html)
    soup = BeautifulSoup(html, 'lxml')
    items = []
    for node in soup.select('.tk-quest-item.quesroot'):
        try:
            items.append(parse_list_item(node, page_ctx))
        except Exception as ex:
            zid = node.get('questionid') or '?'
            if on_log:
                on_log(f'  [\u8b66\u544a] \u89e3\u6790\u5217\u8868\u9898\u5931\u8d25 zujuan:{zid} {ex}')
    return items, page_ctx


def _clean_answer_analysis_html(html):
    text, imgs = clean_html_fragment(html or '')
    return text.strip(), imgs


def _extract_labeled_sections(root):
    answer_text = ''
    analysis_text = ''
    answer_images = []
    analysis_images = []

    if not root:
        return answer_text, analysis_text, answer_images, analysis_images

    blocks = root.select(
        '.item, .answer-item, .quesanswer, .parse-item, .exam-item__parse, .analysis, .qml-explanation'
    )
    if not blocks:
        blocks = [root]

    for block in blocks:
        label = ''
        title = block.select_one('.item-hd, .title, .name, .label, h3, h4')
        if title:
            label = title.get_text(' ', strip=True)
        block_text = block.get_text(' ', strip=True)
        label_hint = label or block_text[:20]

        body_el = block.select_one('.item-bd, .content, .txt, .qml-an, .parse-cnt') or block
        body_html = ''.join(str(x) for x in body_el.contents)
        body, imgs = _clean_answer_analysis_html(body_html)

        if not body and block_text:
            body = re.sub(r'^\u3010?(\u7b54\u6848|\u89e3\u6790|\u8be6\u89e3)\u3011?[: \uff1a\s]*', '', block_text).strip()

        if not body:
            continue

        if '\u7b54\u6848' in label_hint and not answer_text:
            answer_text = re.sub(r'^\u3010?\u7b54\u6848\u3011?[: \uff1a\s]*', '', body).strip()
            answer_images.extend(imgs)
        elif ('\u89e3\u6790' in label_hint or '\u8be6\u89e3' in label_hint) and not analysis_text:
            analysis_text = re.sub(r'^\u3010?(\u89e3\u6790|\u8be6\u89e3)\u3011?[: \uff1a\s]*', '', body).strip()
            analysis_images.extend(imgs)

    if not answer_text:
        for sel in ('.qml-an', '.right-answer', '.answer em', '.answer .txt', '.correct-answer'):
            em = root.select_one(sel)
            if em:
                answer_text, imgs = _clean_answer_analysis_html(''.join(str(x) for x in em.contents) or em.get_text())
                answer_images.extend(imgs)
                if answer_text:
                    break

    if not analysis_text:
        for sel in ('.qml-explanation', '.parse-cnt', '.exam-item__parse', '.analysis'):
            em = root.select_one(sel)
            if em:
                analysis_text, imgs = _clean_answer_analysis_html(''.join(str(x) for x in em.contents) or em.get_text())
                analysis_images.extend(imgs)
                if analysis_text:
                    break

    if not answer_text and not analysis_text:
        plain = root.get_text('\n', strip=True)
        m_ans = re.search(r'\u3010\u7b54\u6848\u3011[: \uff1a\s]*(.+?)(?=\u3010\u89e3\u6790\u3011|\u3010\u8be6\u89e3\u3011|$)', plain, re.S)
        m_parse = re.search(r'\u3010(?:\u89e3\u6790|\u8be6\u89e3)\u3011[: \uff1a\s]*(.+)$', plain, re.S)
        if m_ans:
            answer_text = m_ans.group(1).strip()
        if m_parse:
            analysis_text = m_parse.group(1).strip()

    answer_text = _normalize_answer_text(answer_text)
    analysis_text = normalize_analysis_text(analysis_text)
    if is_weak_analysis(analysis_text):
        analysis_text = ''

    return answer_text, analysis_text, answer_images, analysis_images


extract_labeled_sections = _extract_labeled_sections


def parse_detail_answer(html):
    soup = BeautifulSoup(html, 'lxml')
    if soup.select_one('.answer-box .need-login'):
        return '', '', [], [], False

    answer_box = soup.select_one('.answer-txt') or soup.select_one('.answer-box')
    if not answer_box:
        return '', '', [], [], True

    answer_text, analysis_text, answer_images, analysis_images = _extract_labeled_sections(answer_box)
    parse_img_urls = collect_answer_parse_image_urls(answer_box)
    if parse_img_urls:
        analysis_images = merge_image_urls(analysis_images, parse_img_urls)
        if not answer_text:
            answer_images = merge_image_urls(answer_images, parse_img_urls)
    all_images = merge_image_urls(answer_images, analysis_images, parse_img_urls)
    ok = bool(answer_text or analysis_text or parse_img_urls or soup.select_one('.answer-txt'))
    return answer_text, analysis_text, answer_images, analysis_images, all_images, ok


def parse_detail_stem(html, question_id='', question_type=''):
    soup = BeautifulSoup(html, 'lxml')
    cnt = _select_stem_container(soup, question_id)
    type_label = ''
    qyname = ''
    qyisselect = ''
    add_btn = None
    if question_id:
        add_btn = soup.select_one(
            f'a.addques[quesid="{question_id}"], a#quesselect{question_id}'
        )
    if not add_btn:
        add_btn = soup.select_one('a.addques, a[data-btn-type="quesAdd"]')
    if add_btn:
        qyname = add_btn.get('qyname', '') or ''
        qyisselect = add_btn.get('qyisselect', '') or ''
    info_cnt = soup.select_one('.addi-info .info-cnt')
    if info_cnt:
        type_label = info_cnt.get_text(strip=True)
    raw_html = ''.join(str(x) for x in cnt.contents) if cnt else ''
    if not question_type or question_type == 'short':
        question_type = detect_question_type(type_label, qyname, qyisselect, raw_html)
    content, stem_images, options, option_images = _parse_stem_and_options(
        raw_html, cnt, question_type,
    )
    plain_text, _ = clean_html_fragment(raw_html)
    detail_ctx = parse_detail_context(html)
    return {
        'content': content,
        'content_html': content,
        'content_plain': plain_text,
        'options': options,
        'image_urls': filter_question_images(merge_image_urls(stem_images, option_images)),
        'question_type': question_type,
        'type_label': type_label or qyname,
        'detail_chapter_text': detail_ctx.get('chapter_text') or '',
        'knowledge_leaf': detail_ctx.get('knowledge_leaf') or '',
    }


def options_to_json(options):
    if not options:
        return None
    arr = []
    for opt in options:
        letter = opt.get('label') or 'A'
        text = opt.get('text') or ''
        arr.append(f'{letter}.{text}' if text else f'{letter}.')
    return json.dumps(arr, ensure_ascii=False)


def answer_to_json(question_type, answer_text):
    answer_text = (answer_text or '').strip()
    if not answer_text:
        return json.dumps('')
    if question_type == 'multi':
        letters = _parse_multi_answer(answer_text)
        if letters:
            return json.dumps(letters)
        return json.dumps(answer_text)
    if question_type == 'judge':
        if re.fullmatch(r'[\u221a\u00d7\u2713\u2717]+', answer_text):
            return json.dumps(answer_text)
        if answer_text in ('\u9519', '\u9519\u8bef', 'false', 'F', '\u00d7'):
            return json.dumps('false')
        if answer_text in ('\u5bf9', '\u6b63\u786e', 'true', 'T', '\u221a'):
            return json.dumps('true')
        return json.dumps(answer_text)
    return json.dumps(answer_text)
