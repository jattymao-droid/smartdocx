# -*- coding: utf-8 -*-
"""Decide when to keep answer/parse as images instead of OCR text."""

import re

from .ocr_client import (
    _ESSAY_ANSWER_LABELS,
    is_incomplete_answer,
    is_weak_analysis,
    strip_watermark_noise,
)

_OCR_GARBAGE_PATTERNS = (
    r'\b[vV]\.com\b',
    r'zujuan\.xkw',
    r'zxxk\.com',
    r'xkw\.com',
    r'\book\b',
    r'\u2014\s*\u2014\s*\u00b7',
    r'/\s*g\b',
    r'm\+\s*/',
    r'[A-Za-z]\s*/\s*[A-Za-z]',
)

_NOISE_CHAR_RE = re.compile(
    r'[^\u4e00-\u9fff\w\s\.,;:\-\(\)\[\]'
    r'\u3010\u3011\uff08\uff09\u3001\u3002\uff1b\uff1a\uff01\uff1f%+\=<>/\\]'
)


def is_essay_example_mode(item, html_answer='', partial_answer=''):
    """True when answer area is a sample essay image that should be OCR'd."""
    content = (item.get('content') or '') + (item.get('content_html') or '')
    label = (html_answer or partial_answer or '').strip()
    if is_incomplete_answer(partial_answer) or is_incomplete_answer(html_answer):
        if any(tag in label for tag in _ESSAY_ANSWER_LABELS):
            return True
        if any(tag in content for tag in ('\u5199\u4f5c', '\u6839\u636e\u8981\u6c42\u5199\u4f5c', '\u4f5b\u6587', '\u8bae\u8bba\u6587')):
            return True
    return False


def looks_like_ocr_garbage(text):
    text = strip_watermark_noise((text or '').replace('\r\n', '\n').strip())
    if not text:
        return False
    hits = sum(1 for pat in _OCR_GARBAGE_PATTERNS if re.search(pat, text, re.I))
    if hits >= 2:
        return True
    if hits >= 1 and len(text) > 120:
        return True
    noisy = len(_NOISE_CHAR_RE.findall(text))
    if len(text) > 80 and noisy / max(len(text), 1) > 0.08:
        return True
    return False


def should_keep_analysis_as_image(
    item,
    analysis_text,
    image_urls,
    *,
    prefer_analysis_image=True,
    html_answer='',
    partial_answer='',
):
    if not prefer_analysis_image or not image_urls:
        return False
    if is_essay_example_mode(item, html_answer=html_answer, partial_answer=partial_answer):
        return False
    if not (analysis_text or '').strip():
        return True
    if is_weak_analysis(analysis_text):
        return True
    if looks_like_ocr_garbage(analysis_text):
        return True
    # Analysis text looks usable; prefer using extracted text instead of
    # forcing image-only mode (which can lead to missing "���" when images fail to upload/display).
    return False
