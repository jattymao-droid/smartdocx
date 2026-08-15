# -*- coding: utf-8 -*-
"""Fetch answer/parse via zujuan internal API when available."""

import json
import re

import requests
from bs4 import BeautifulSoup

from .content_cleaner import clean_html_fragment, collect_answer_parse_image_urls, merge_image_urls
from .ocr_client import normalize_analysis_text, strip_watermark_noise
from .parser import extract_labeled_sections, parse_detail_answer

API_URL = 'https://zujuan.xkw.com/zujuan-api/check_ques_parse'
USER_AGENT = (
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
    '(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36'
)


def _cookies_from_context(context):
    jar = {}
    for c in context.cookies():
        jar[c['name']] = c['value']
    return jar


def _unwrap_api_payload(data):
    if not isinstance(data, dict):
        return {}
    for key in ('data', 'result', 'body'):
        inner = data.get(key)
        if isinstance(inner, dict):
            return inner
    return data


def _pick_text(*values):
    for value in values:
        text = (value or '').strip()
        if text:
            return text
    return ''


def _html_to_text(html):
    if not html:
        return ''
    text, _ = clean_html_fragment(html)
    return strip_watermark_noise(text)


def _parse_api_fields(payload):
    answer = _pick_text(
        payload.get('answer'),
        payload.get('answerText'),
        payload.get('rightAnswer'),
        payload.get('correctAnswer'),
    )
    analysis = _pick_text(
        payload.get('analysis'),
        payload.get('parse'),
        payload.get('parseText'),
        payload.get('explanation'),
    )
    if not answer:
        answer = _html_to_text(
            payload.get('answerHtml') or payload.get('answerContent') or payload.get('answerStr')
        )
    if not analysis:
        analysis = _html_to_text(
            payload.get('parseHtml') or payload.get('analysisHtml') or payload.get('parseStr')
        )
    html_blob = payload.get('html') or payload.get('content') or ''
    images = collect_answer_parse_image_urls(html_blob) if html_blob else []
    if html_blob and (not answer or not analysis):
        soup = BeautifulSoup(str(html_blob), 'lxml')
        root = soup.select_one('.answer-txt') or soup.select_one('.answer-box') or soup
        ans, ana, _, ana_imgs = extract_labeled_sections(root)
        answer = answer or ans
        analysis = analysis or ana
        images = merge_image_urls(images, ana_imgs, collect_answer_parse_image_urls(root))
    answer = strip_watermark_noise(answer)
    analysis = normalize_analysis_text(analysis)
    return answer, analysis


def fetch_answer_via_api(page, item, timeout=30):
    """
    Call check_ques_parse API. Returns (answer, analysis, image_urls, ok, note).
  """
    zid = item.get('zujuan_id')
    bank_id = item.get('bank_id') or '13'
    if not zid:
        return '', '', [], False, 'missing_zujuan_id'

    cookies = _cookies_from_context(page.context)
    headers = {
        'User-Agent': USER_AGENT,
        'Referer': item.get('detail_url') or page.url,
        'Content-Type': 'application/json',
        'Accept': 'application/json, text/plain, */*',
    }
    payloads = [
        {'quesId': int(zid), 'bankId': int(bank_id)},
        {'questionId': int(zid), 'bankId': int(bank_id)},
        {'quesId': str(zid), 'bankId': str(bank_id)},
    ]

    last_err = ''
    for body in payloads:
        try:
            resp = requests.post(
                API_URL, json=body, headers=headers, cookies=cookies, timeout=timeout,
            )
            if resp.status_code != 200:
                last_err = f'HTTP {resp.status_code}'
                continue
            raw = resp.json()
            if isinstance(raw, dict) and raw.get('code') not in (None, 0, 200, '200'):
                last_err = str(raw.get('msg') or raw.get('message') or raw.get('code'))
                continue
            payload = _unwrap_api_payload(raw)
            answer, analysis = _parse_api_fields(payload)
            images = collect_answer_parse_image_urls(payload.get('html') or payload.get('content') or '')
            for key in ('answerImg', 'parseImg', 'answerImage', 'imgUrl', 'parseImage'):
                url = payload.get(key)
                if url:
                    images.append(str(url))
            images = merge_image_urls(images)
            if answer or analysis or images:
                return answer, analysis, images, True, ''
            last_err = 'empty_payload'
        except (requests.RequestException, json.JSONDecodeError, ValueError) as ex:
            last_err = str(ex)

    return '', '', [], False, last_err or 'api_no_data'
