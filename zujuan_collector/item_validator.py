# -*- coding: utf-8 -*-
"""Validate and normalize collected question items before import."""

import re

from .answer_extractor import enrich_answer_from_analysis
from .content_cleaner import filter_question_images
from .ocr_client import is_placeholder_answer, is_incomplete_answer
from .parser import answer_to_json, options_to_json, infer_question_type, detect_question_type

CHOICE_TYPES = frozenset({'single', 'multi'})
SUBJECTIVE_TYPES = frozenset({
    'short', 'answer', 'experiment', 'comprehensive', 'reading', 'drawing',
    'fill', 'knowledge_fill', 'judge',
})


def _ensure_knowledge_points(item):
    knowledge = list(item.get('knowledge_points') or [])
    if item.get('knowledge_leaf') and item['knowledge_leaf'] not in knowledge:
        knowledge.insert(0, item['knowledge_leaf'])
    if item.get('category_name') and item['category_name'] not in knowledge:
        knowledge.append(item['category_name'])
    detail = (item.get('detail_chapter_text') or '').strip()
    if detail:
        leaf = detail.split('>')[-1].strip()
        if leaf and leaf not in knowledge:
            knowledge.append(leaf)
    if not knowledge and item.get('chapter_text'):
        knowledge = [item['chapter_text']]
    item['knowledge_points'] = [k for k in knowledge if k][:10]
    return item


def normalize_item(item):
    """Fill missing metadata and backfill answers from analysis."""
    item = dict(item)
    _ensure_knowledge_points(item)

    content = (item.get('content_html') or item.get('content') or '').strip()
    if content:
        item['content'] = content
        item['content_html'] = content

    enrich_answer_from_analysis(item)

    item['question_type'] = infer_question_type(item)

    content = (item.get('content_html') or item.get('content') or '').strip()
    qtype = item.get('question_type') or 'short'
    if qtype in ('reading',):
        if ('<table' in content or 'qb-options' in content) and re.search(
            r'[A-Ha-h][\uFF0E\.]', content
        ):
            item['options'] = []

    answer = (item.get('answer') or '').strip()
    if is_incomplete_answer(answer) and answer:
        pass
    elif not answer or is_placeholder_answer(answer):
        analysis = (item.get('analysis') or '').strip()
        if analysis and item.get('question_type') in SUBJECTIVE_TYPES:
            item['answer'] = analysis[:500]

    qtype = item.get('question_type') or 'short'
    if qtype in CHOICE_TYPES and not item.get('options'):
        item['_validation_warning'] = 'missing_options'

    return item


def finalize_item(item):
    """Normalize metadata/answers and strip answer-image URLs from stem images."""
    item = normalize_item(item)
    item['image_urls'] = filter_question_images(item.get('image_urls'))
    return item


def validate_item(item):
    """
    Return (ok, errors, warnings).
    ok=False blocks import; warnings are logged but import may proceed.
    """
    errors = []
    warnings = []

    content = (item.get('content_html') or item.get('content') or '').strip()
    if not content:
        errors.append('\u9898\u5e72\u4e3a\u7a7a')

    qtype = item.get('question_type') or 'short'
    if qtype in CHOICE_TYPES:
        options = item.get('options') or []
        if not options:
            errors.append('\u9009\u62e9\u9898\u7f3a\u5c11\u9009\u9879')
        elif qtype == 'single' and len(options) < 2:
            warnings.append('\u5355\u9009\u9898\u9009\u9879\u8f83\u5c11')

    knowledge = item.get('knowledge_points') or []
    if not knowledge:
        errors.append('\u7f3a\u5c11\u77e5\u8bc6\u70b9')

    answer = (item.get('answer') or '').strip()
    analysis = (item.get('analysis') or '').strip()
    has_answer_image = bool(item.get('answer_image_urls') or item.get('analysis_image_urls'))
    if not answer and not analysis and not has_answer_image:
        errors.append('\u7f3a\u5c11\u7b54\u6848\u4e0e\u89e3\u6790')

    if answer and is_placeholder_answer(answer) and not analysis:
        warnings.append('\u7b54\u6848\u4ec5\u5360\u4f4d\u6587\u672c')

    try:
        answer_to_json(qtype, answer or '\u89c1\u89e3\u6790')
        if qtype in CHOICE_TYPES:
            options_to_json(item.get('options') or [])
    except Exception as ex:
        warnings.append(f'\u7b54\u6848\u683c\u5f0f: {ex}')

    diff = item.get('difficulty')
    if diff is not None:
        try:
            d = float(diff)
            if d < 0.1 or d > 1.0:
                warnings.append('\u96be\u5ea6\u8d85\u51fa 0.1~1.0\uff0c\u5c06\u88c1\u526a')
        except (TypeError, ValueError):
            warnings.append('\u96be\u5ea6\u683c\u5f0f\u5f02\u5e38')

    return len(errors) == 0, errors, warnings


def clamp_difficulty(item):
    try:
        d = float(item.get('difficulty') or 0.5)
    except (TypeError, ValueError):
        d = 0.5
    item['difficulty'] = max(0.1, min(1.0, d))
    return item
