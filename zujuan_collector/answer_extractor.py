# -*- coding: utf-8 -*-
"""Extract structured answers from OCR/HTML analysis for all question types."""

import re

from .ocr_client import (
    extract_conclusive_choice,
    is_incomplete_answer,
    is_placeholder_answer,
    is_weak_analysis,
    normalize_analysis_text,
)

_FILL_VALUE_PATTERNS = (
    r'\u4e0e(.+?)\u7684\u65b9\u5411(?:\u76f8\u540c|\u4e00\u81f4)',
    r'\u65b9\u5411(?:\u76f8\u540c|\u4e00\u81f4|\u76f8\u53cd)',
    r'\u65b9\u5411(.+?)(?:[\u3002\uFF1B;]|$)',
    r'[:\uff1a]\s*(.+?)(?:[\u3002\uFF1B;]|$)',
)

_WRONG_MARKS = frozenset({
    '\u9519\u8bef', '\u9519', '\u00d7', '\u2717', '\u2718', '\u2716', '\u2715',
    'false', 'F', '\u5426',
})
_RIGHT_MARKS = frozenset({
    '\u6b63\u786e', '\u5bf9', '\u221a', '\u2713', '\u2714',
    'true', 'T', '\u662f',
})


def _mark_to_symbol(mark):
    m = (mark or '').strip()
    if m in _WRONG_MARKS or m in ('\u9519\u8bef', '\u9519'):
        return '\u00d7'
    if m in _RIGHT_MARKS:
        return '\u221a'
    return ''


def _judge_block_mark(block):
    block = (block or '').strip()
    if not block:
        return ''
    if re.search(r'\u6545\u9519\u8bef|\u6545\u9519|\u9519\u8bef|\(\u9519\u8bef\)|\u4e0d\u6b63\u786e', block):
        return '\u00d7'
    if re.search(r'\u6545\u6b63\u786e|\u6545\u5bf9|\(\u6b63\u786e\)|\u6b63\u786e', block):
        return '\u221a'
    if re.search(r'\u800c\u4e0d\u662f|\u4e0d\u80fd|\u65e0\u6cd5', block):
        return '\u00d7'
    if re.search(r'\u56e0\u6b64|\u59cb\u7ec8\u4e00\u81f4|\u786e\u5b9e\u662f|\u5b8c\u5168\u53d6\u51b3\u4e8e', block):
        return '\u221a'
    return ''


def extract_judge_answer(analysis):
    """Build multi-item judge answer string like ????? from analysis text."""
    text = (analysis or '').strip()
    if not text:
        return ''

    blocks = [b.strip() for b in re.split(r'(?=\(\d+\))', text) if b.strip()]
    if blocks:
        per_item = []
        for block in blocks:
            mark = _judge_block_mark(block)
            if mark:
                per_item.append(mark)
        if per_item:
            return ''.join(per_item)

    marks = re.findall(
        r'[\(\uff08](\u6b63\u786e|\u9519\u8bef|\u5bf9|\u9519|\u221a|\u00d7|\u2713|\u2717)[\)\uff09]',
        text,
    )
    if marks:
        return ''.join(_mark_to_symbol(m) or '\u00d7' for m in marks)

    marks = re.findall(r'\u6545(\u6b63\u786e|\u9519\u8bef)', text)
    if marks:
        return ''.join(_mark_to_symbol(m) or '\u00d7' for m in marks)

    return ''


def _extract_fill_value_from_sentence(sentence):
    sentence = (sentence or '').strip().rstrip('\u3002.;' + '\uff1b')
    if not sentence or is_weak_analysis(sentence):
        return ''
    if sentence in ('\u76f8\u540c', '\u76f8\u53cd', '\u4e00\u81f4'):
        return sentence
    for pat in _FILL_VALUE_PATTERNS:
        m = re.search(pat, sentence)
        if m:
            val = (m.group(1) if m.lastindex else m.group(0)).strip()
            val = re.sub(r'^\u7684', '', val).strip()
            if val and len(val) <= 80 and not is_weak_analysis(val):
                return val
    if re.search(r'\u65b9\u5411\u76f8\u540c|\u65b9\u5411\u4e00\u81f4', sentence):
        return '\u76f8\u540c'
    if re.search(r'\u65b9\u5411\u76f8\u53cd', sentence):
        return '\u76f8\u53cd'
    m = re.search(r'\u65b9\u5411(.+?)$', sentence)
    if m:
        val = m.group(1).strip().rstrip('\u3002.;' + '\uff1b')
        if val in ('\u76f8\u540c', '\u76f8\u53cd', '\u4e00\u81f4'):
            return val
    return ''


def extract_fill_answer(stem, analysis):
    """Extract fill-in answers from numbered analysis sections."""
    text = (analysis or '').strip()
    if not text:
        return ''

    section_values = []
    for block in re.split(r'(?=\(\d+\))', text):
        block = block.strip()
        if not block:
            continue
        m = re.match(r'\((\d+)\)\s*(.+)', block, re.S)
        if not m:
            continue
        body = re.sub(r'(?:\[\d+\])+', '', m.group(2)).strip()
        body = re.sub(r'\u3010\u70b9\u775b\u3011.*$', '', body, flags=re.S).strip()
        if not body:
            continue
        subparts = re.split(r'[;\uff1b]', body)
        if len(subparts) > 1:
            for part in subparts:
                val = _extract_fill_value_from_sentence(part.strip())
                if val:
                    section_values.append(val)
        else:
            val = _extract_fill_value_from_sentence(body)
            if val:
                section_values.append(val)
    if section_values:
        return '; '.join(section_values)

    bracket_parts = re.findall(r'\[\d+\]\s*([^[\n]+)', text)
    if bracket_parts:
        values = []
        for part in bracket_parts:
            part = part.strip().rstrip('\u3002.').strip()
            if part and not is_weak_analysis(part):
                values.append(part)
        if values:
            return '; '.join(values)

    numbered = []
    for m in re.finditer(r'\((\d+)\)\s*([^\n(]+)', text):
        body = m.group(2).strip()
        body = re.sub(r'(?:\[\d+\])+', '', body).strip()
        body = re.sub(r'\u3010\u70b9\u775b\u3011.*$', '', body).strip()
        if body and len(body) <= 200 and not is_weak_analysis(body):
            numbered.append(body.rstrip('\u3002.'))
    if numbered:
        return '; '.join(numbered)

    blank_count = len(re.findall(r'_{2,}|________', stem or ''))
    if blank_count and numbered:
        short_vals = []
        for seg in numbered:
            m = re.search(r'[:\uff1a]\s*(.+)$', seg)
            short_vals.append((m.group(1) if m else seg).strip())
        if short_vals:
            return '; '.join(short_vals[:blank_count] or short_vals)

    return ''


def extract_subjective_answer(analysis, max_len=500):
    """Use first substantive analysis block as reference answer for subjective types."""
    text = normalize_analysis_text(analysis)
    if not text or is_weak_analysis(text):
        return ''

    blocks = re.split(r'\n\s*\n', text)
    for block in blocks:
        block = block.strip()
        if not block or is_weak_analysis(block):
            continue
        block = re.sub(r'^\(\d+\)\s*', '', block)
        block = re.sub(r'^\[\d+\]\s*', '', block)
        if len(block) >= 8:
            return block[:max_len]
    return text[:max_len]


def extract_choice_answer(analysis, multi=False):
    """Extract A-H answer letters from analysis."""
    text = (analysis or '').strip()
    if not text:
        return ''
    conclusive = extract_conclusive_choice(text, multi=multi)
    if conclusive:
        return conclusive
    patterns = (
        r'\u6545\u9009\s*[:\uff1a]?\s*([A-H]+)',
        r'\u7b54\u6848\s*[:\uff1a]?\s*([A-H]+)',
        r'\u3010\u7b54\u6848\u3011\s*([A-H]+)',
        r'\u3010\u5c0f\u9898\s*\d+\u3011\s*([A-Ha-h])\b',
    )
    best = ''
    best_pos = -1
    for pat in patterns:
        for m in re.finditer(pat, text, re.I):
            if m.start() >= best_pos:
                best_pos = m.start()
                best = m.group(1).upper()
    if not best:
        return ''
    if multi:
        return ''.join(sorted(set(best)))
    return best[0] if best else ''


def enrich_answer_from_analysis(item):
    """Backfill answer field from analysis when direct extraction failed."""
    analysis = normalize_analysis_text(item.get('analysis') or '')
    if analysis:
        item['analysis'] = analysis

    qtype = item.get('question_type') or 'short'

    if qtype in ('single', 'multi', 'reading') or (
        qtype == 'short' and (item.get('options') or [])
    ):
        answer = (item.get('answer') or '').strip()
        extracted = extract_choice_answer(analysis, multi=(qtype == 'multi'))
        conclusive = extract_conclusive_choice(analysis, multi=(qtype == 'multi'))
        if extracted:
            if not answer or is_placeholder_answer(answer):
                item['answer'] = extracted
            elif conclusive and answer.upper() != extracted.upper():
                item['answer'] = extracted
        return item

    answer = (item.get('answer') or '').strip()
    if answer and not is_placeholder_answer(answer) and not is_incomplete_answer(answer):
        return item

    if not analysis:
        return item

    if qtype == 'judge':
        extracted = extract_judge_answer(analysis)
        if extracted:
            item['answer'] = extracted
            return item

    if qtype in ('fill', 'knowledge_fill'):
        extracted = extract_fill_answer(item.get('content') or '', analysis)
        if extracted:
            item['answer'] = extracted
            return item

    if qtype in (
        'short', 'answer', 'experiment', 'comprehensive', 'reading', 'drawing',
    ):
        for pat in (
            r'\u7b54\u6848\s*[:\uff1a]\s*([^\n\u3010]{1,200})',
            r'\u6545\u586b\s*[:\uff1a]?\s*([^\n]+)',
            r'\u6545\u9009\s*[:\uff1a]?\s*([^\n]+)',
        ):
            m = re.search(pat, analysis)
            if m:
                ans = m.group(1).strip()
                if ans and not is_placeholder_answer(ans) and not is_weak_analysis(ans):
                    item['answer'] = ans
                    return item
        extracted = extract_subjective_answer(analysis)
        if extracted:
            item['answer'] = extracted
            return item

    return item
