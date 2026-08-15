# -*- coding: utf-8 -*-
"""Resolve zujuan URL subject metadata to RuoYi subject_id."""

import re

CODE_TO_SHORT = {
    'gzyw': '\u8bed\u6587',
    'czyw': '\u8bed\u6587',
    'gzwl': '\u7269\u7406',
    'czwl': '\u7269\u7406',
    'gzsx': '\u6570\u5b66',
    'czsx': '\u6570\u5b66',
    'gzhx': '\u5316\u5b66',
    'czhx': '\u5316\u5b66',
    'gzsw': '\u751f\u7269',
    'czsw': '\u751f\u7269',
    'gzzz': '\u653f\u6cbb',
    'czzz': '\u653f\u6cbb',
    'gzls': '\u5386\u53f2',
    'czls': '\u5386\u53f2',
    'gzdl': '\u5730\u7406',
    'czdl': '\u5730\u7406',
    'gzyy': '\u82f1\u8bed',
    'czyy': '\u82f1\u8bed',
    'gzxxjs': '\u4fe1\u606f\u6280\u672f',
    'gztyjs': '\u901a\u7528\u6280\u672f',
    'gzry': '\u65e5\u8bed',
}


def school_stage_from_code(subject_code):
    code = (subject_code or '').lower()
    if code.startswith('cz'):
        return '\u521d\u4e2d'
    return '\u9ad8\u4e2d'


def short_subject_name(subject_label='', subject_code=''):
    code = (subject_code or '').lower()
    if code in CODE_TO_SHORT:
        return CODE_TO_SHORT[code]
    label = (subject_label or '').strip()
    label = re.sub(r'^(\u9ad8\u4e2d|\u521d\u4e2d)', '', label)
    return label or code


def match_subject_id(subjects, subject_label='', subject_code='', fallback_id=None):
    if not subjects:
        return fallback_id

    short = short_subject_name(subject_label, subject_code)
    full = (subject_label or '').strip()

    for s in subjects:
        name = (s.get('subjectName') or '').strip()
        if short and name == short:
            return s.get('subjectId')

    for s in subjects:
        name = (s.get('subjectName') or '').strip()
        if short and short in name:
            return s.get('subjectId')
        if name and name in short:
            return s.get('subjectId')

    if full:
        for s in subjects:
            name = (s.get('subjectName') or '').strip()
            if name and (full.endswith(name) or name in full):
                return s.get('subjectId')

    return fallback_id


def item_subject_context(item, cfg=None):
    cfg = cfg or {}
    page_ctx = item.get('page_ctx') or {}
    return {
        'subject_code': (
            item.get('subject_code')
            or page_ctx.get('subject_code')
            or cfg.get('subject_code')
            or ''
        ),
        'subject_label': (
            item.get('subject_label')
            or page_ctx.get('subject_label')
            or cfg.get('subject_label')
            or ''
        ),
        'school_stage': (
            item.get('school_stage')
            or page_ctx.get('school_stage')
            or cfg.get('school_stage')
            or school_stage_from_code(page_ctx.get('subject_code') or cfg.get('subject_code'))
        ),
    }


def subject_context_from_url(url):
    from .url_utils import parse_zujuan_url

    info = parse_zujuan_url(url)
    code = info.get('subject_code') or ''
    return {
        'subject_code': code,
        'subject_label': info.get('subject_label') or '',
        'school_stage': school_stage_from_code(code),
        'textbook_hint': info.get('textbook_hint') or '',
    }
