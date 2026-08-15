# -*- coding: utf-8 -*-
"""Fetch and OCR zujuan answer images."""

import re
from urllib.parse import urljoin

import requests

from .answer_extractor import extract_subjective_answer
from .answer_api import fetch_answer_via_api
from .answer_image_mode import (
    is_essay_example_mode,
    looks_like_ocr_garbage,
    should_keep_analysis_as_image,
)
from .content_cleaner import merge_image_urls
from .image_cleaner import (
    OCR_RESIZE_WIDTH,
    normalize_image_download_url,
    process_downloaded_image,
    resize_image_bytes_for_ocr,
)
from .ocr_client import (
    DEFAULT_OCR_URL,
    extract_essay_answer_from_text,
    is_incomplete_answer,
    is_weak_analysis,
    normalize_analysis_text,
    ocr_image_bytes,
    parse_answer_analysis_text,
    pick_best_analysis,
    resolve_ocr_mode,
)
from .parser import parse_detail_answer

USER_AGENT = (
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
    '(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36'
)
ZUJUAN_BASE = 'https://zujuan.xkw.com'


def _cookies_from_context(context):
    jar = {}
    for c in context.cookies():
        jar[c['name']] = c['value']
    return jar


def _resolve_img_src(src):
    src = (src or '').strip()
    if not src:
        return ''
    if src.startswith('//'):
        return 'https:' + src
    if not src.startswith('http'):
        return urljoin(ZUJUAN_BASE, src)
    return src


def _download_answer_image(url, cookies, referer):
    headers = {'User-Agent': USER_AGENT, 'Referer': referer}
    resp = requests.get(url, headers=headers, cookies=cookies, timeout=60)
    resp.raise_for_status()
    return resp.content


def _prepare_image_for_ocr(url, blob):
    blob = process_downloaded_image(
        url,
        blob,
        {'remove_watermark': True, 'auto_clean_fullsize': True},
    )
    return resize_image_bytes_for_ocr(blob)


def _download_answer_image_for_ocr(url, cookies, referer):
    download_url = normalize_image_download_url(url, default_width=OCR_RESIZE_WIDTH)
    blob = _download_answer_image(download_url, cookies, referer)
    return _prepare_image_for_ocr(download_url or url, blob)


def _pick_answer(*candidates):
    for value in candidates:
        text = (value or '').strip()
        if text and not is_incomplete_answer(text):
            return text
    return ''


def _img_src_from_locator(img_locator):
    return _resolve_img_src(
        img_locator.get_attribute('src') or img_locator.get_attribute('data-src')
    )


def _merge_essay_label(partial_answer, ocr_answer):
    answer = (ocr_answer or '').strip()
    if not answer:
        return ''
    partial = (partial_answer or '').strip()
    if not partial or not is_incomplete_answer(partial):
        return answer
    label = partial.rstrip(':：').strip() or '\u4f8b\u6587'
    if answer.startswith(label):
        return answer
    body = re.sub(rf'^{re.escape(label)}\s*[\uff1a:]*', '', answer).strip()
    if body:
        return f'{label}\uff1a\n{body}'
    return answer


def _analysis_image_list(analysis_images, answer_images, all_images):
    urls = merge_image_urls(analysis_images, answer_images, all_images)
    return urls or []


def _ocr_for_answer_only(urls, cookies, detail_url, ocr_base, on_log, item=None, cfg=None):
    answers = []
    mode = resolve_ocr_mode(item, cfg)
    for idx, src in enumerate(urls):
        src = _resolve_img_src(src)
        if not src:
            continue
        try:
            blob = _download_answer_image_for_ocr(src, cookies, detail_url)
            ocr_text = ocr_image_bytes(blob, ocr_base=ocr_base, mode=mode)
            if not ocr_text:
                continue
            essay_ans, _ = extract_essay_answer_from_text(ocr_text)
            ans, _ = parse_answer_analysis_text(ocr_text)
            if essay_ans and not is_incomplete_answer(essay_ans):
                answers.append(essay_ans)
            elif ans and not is_incomplete_answer(ans):
                answers.append(ans)
        except Exception as ex:
            if on_log:
                on_log(f'    \u7b54\u6848\u56fe {idx + 1} \u5904\u7406\u5931\u8d25: {ex}')
    return _pick_answer(*answers)


def _ocr_essay_answer_and_analysis(urls, cookies, detail_url, ocr_base, on_log, item=None, cfg=None):
    ocr_chunks = []
    per_image_answers = []
    per_image_analyses = []
    ocr_warned = False
    mode = resolve_ocr_mode(item, cfg)

    for idx, src in enumerate(urls):
        src = _resolve_img_src(src)
        if not src:
            continue
        try:
            blob = _download_answer_image_for_ocr(src, cookies, detail_url)
            ocr_text = ocr_image_bytes(blob, ocr_base=ocr_base, mode=mode)
            if ocr_text:
                ocr_chunks.append(ocr_text)
                essay_ans, essay_ana = extract_essay_answer_from_text(ocr_text)
                ans, analysis = parse_answer_analysis_text(ocr_text)
                if essay_ans and not is_incomplete_answer(essay_ans):
                    per_image_answers.append(essay_ans)
                elif ans and not is_incomplete_answer(ans):
                    per_image_answers.append(ans)
                if essay_ana:
                    per_image_analyses.append(essay_ana)
                if analysis:
                    per_image_analyses.append(analysis)
            elif not ocr_warned and on_log:
                ocr_warned = True
                on_log(
                    '    OCR\u670d\u52a1\u672a\u54cd\u5e94\uff0c\u5df2\u4fdd\u7559\u7b54\u6848\u56fe\u7247'
                    '\uff08\u8bf7\u542f\u52a8 scripts\\start-ocr-service.ps1\uff09'
                )
        except Exception as ex:
            if on_log:
                on_log(f'    \u7b54\u6848\u56fe {idx + 1} \u5904\u7406\u5931\u8d25: {ex}')

    combined_text = '\n'.join(chunk for chunk in ocr_chunks if chunk)
    comb_answer, comb_analysis = parse_answer_analysis_text(combined_text)
    essay_ans, essay_ana = extract_essay_answer_from_text(combined_text)
    answer = _pick_answer(essay_ans, comb_answer, *per_image_answers)
    analysis = pick_best_analysis(essay_ana, comb_analysis, *per_image_analyses)
    return answer, analysis, combined_text


_SUBJECTIVE_TYPES = frozenset({
    'short', 'answer', 'experiment', 'comprehensive', 'reading', 'drawing',
    'fill', 'knowledge_fill', 'judge',
})


def _needs_answer_image_ocr(item, partial_answer, partial_analysis, image_urls):
    if not image_urls:
        return False
    if (
        is_incomplete_answer(partial_answer)
        or not partial_analysis
        or is_weak_analysis(partial_analysis)
    ):
        return True
    qtype = item.get('question_type') or 'short'
    if qtype in _SUBJECTIVE_TYPES and len((partial_analysis or '').strip()) < 80:
        return True
    return False


def _mark_analysis_image_fallback(item, image_urls):
    if not image_urls:
        return
    item['analysis_from_image'] = True
    item['analysis_image_urls'] = image_urls


def fetch_answer_from_page(
    page,
    item,
    ocr_base=DEFAULT_OCR_URL,
    on_log=None,
    prefer_analysis_image=False,
    cfg=None,
):
    """Return (answer, analysis, image_urls, ok, note)."""
    detail_url = item.get('detail_url') or page.url

    api_answer, api_analysis, api_images, api_ok, api_note = fetch_answer_via_api(page, item)

    try:
        page.wait_for_selector(
            '.answer-txt img, .answer-txt, .answer-box .need-login',
            timeout=25000,
        )
    except Exception:
        pass
    try:
        page.wait_for_function(
            """() => {
                const box = document.querySelector('.answer-txt');
                if (!box) return false;
                return box.querySelectorAll('img[src], img[data-src]').length > 0
                    || (box.textContent || '').trim().length > 0;
            }""",
            timeout=20000,
        )
    except Exception:
        pass
    page.wait_for_timeout(2000)

    if page.locator('.answer-box .need-login').count() > 0:
        return '', '', [], False, '\u9700\u767b\u5f55\u7ec4\u5377\u7f51\u540e\u624d\u80fd\u67e5\u770b\u7b54\u6848\u89e3\u6790'

    html = page.content()
    html_answer, html_analysis, ans_images, parse_images, all_images, html_ok = parse_detail_answer(html)
    html_analysis = normalize_analysis_text(html_analysis)

    partial_answer = _pick_answer(api_answer, html_answer) or (api_answer or html_answer or '').strip()
    partial_analysis = pick_best_analysis(api_analysis, html_analysis)

    imgs = page.locator('.answer-txt img')
    page_image_urls = []
    for idx in range(imgs.count()):
        src = _img_src_from_locator(imgs.nth(idx))
        if src:
            page_image_urls.append(src)

    image_urls = _analysis_image_list(parse_images, ans_images, merge_image_urls(page_image_urls, all_images, api_images))
    essay_mode = is_essay_example_mode(item, html_answer=html_answer, partial_answer=partial_answer)
    cookies = _cookies_from_context(page.context)

    if api_ok and (partial_answer or partial_analysis) and not essay_mode:
        needs_image_ocr = _needs_answer_image_ocr(
            item, partial_answer, partial_analysis, image_urls,
        )
        if not needs_image_ocr:
            if on_log:
                on_log(f'    API\u7b54\u6848: {partial_answer or "-"}')
            return partial_answer, partial_analysis, image_urls, True, ''

    if not image_urls:
        if partial_answer and not is_incomplete_answer(partial_answer):
            return partial_answer, partial_analysis, image_urls, True, ''
        if partial_analysis and not is_weak_analysis(partial_analysis):
            return partial_answer, partial_analysis, image_urls, True, ''
        if partial_analysis:
            return partial_answer, partial_analysis, image_urls, bool(partial_answer), 'analysis_placeholder'
        return '', '', image_urls, False, '\u672a\u83b7\u53d6\u5230\u7b54\u6848\u89e3\u6790'

    ocr_answer, ocr_analysis, combined_text = _ocr_essay_answer_and_analysis(
        image_urls, cookies, detail_url, ocr_base, on_log, item=item, cfg=cfg,
    )

    if is_incomplete_answer(partial_answer):
        answer = _merge_essay_label(partial_answer, ocr_answer)
    else:
        answer = _pick_answer(partial_answer, ocr_answer)

    analysis = pick_best_analysis(partial_analysis, ocr_analysis)

    if is_weak_analysis(analysis) and combined_text:
        retry_answer, retry_analysis = parse_answer_analysis_text(combined_text)
        analysis = pick_best_analysis(analysis, retry_analysis)
        if not answer or is_incomplete_answer(answer):
            answer = _pick_answer(retry_answer, answer)

    if (not analysis or is_weak_analysis(analysis)) and combined_text:
        raw_fallback = normalize_analysis_text(combined_text)
        if (
            raw_fallback
            and not is_weak_analysis(raw_fallback)
            and not looks_like_ocr_garbage(raw_fallback)
        ):
            analysis = raw_fallback
            if not answer or is_incomplete_answer(answer):
                subj = extract_subjective_answer(analysis)
                if subj:
                    answer = subj

    has_good_analysis = (
        analysis
        and not is_weak_analysis(analysis)
        and not looks_like_ocr_garbage(analysis)
    )
    final_answer = answer if answer and not is_incomplete_answer(answer) else partial_answer

    if has_good_analysis and not (final_answer and not is_incomplete_answer(final_answer)):
        if on_log:
            on_log(f'    OCR\u89e3\u6790\u5b57\u6570:{len(analysis)} \u7b54\u6848: {final_answer or "-"}')
        return final_answer or '', analysis, image_urls, True, ''

    if should_keep_analysis_as_image(
        item,
        analysis,
        image_urls,
        prefer_analysis_image=prefer_analysis_image,
        html_answer=html_answer,
        partial_answer=partial_answer,
    ):
        if not final_answer or is_incomplete_answer(final_answer):
            final_answer = _ocr_for_answer_only(
                image_urls, cookies, detail_url, ocr_base, on_log, item=item, cfg=cfg,
            ) or final_answer
        item['analysis_from_image'] = True
        item['analysis_image_urls'] = image_urls
        if on_log:
            on_log(
                f'    \u89e3\u6790\u4fdd\u7559\u56fe\u7247\uff08{len(image_urls)}\u5f20\uff09'
                f' \u7b54\u6848: {final_answer or "-"}'
            )
        return final_answer, '', image_urls, True, 'analysis_image'

    if final_answer and not is_incomplete_answer(final_answer):
        if not analysis and image_urls and not combined_text:
            if on_log:
                on_log('    OCR\u672a\u8bc6\u522b\uff0c\u4fdd\u7559\u89e3\u6790\u56fe\u7247\u5f85\u5bfc\u5165')
            _mark_analysis_image_fallback(item, image_urls)
        if on_log:
            head = final_answer[:80] + ('...' if len(final_answer) > 80 else '')
            on_log(f'    OCR\u7b54\u6848: {head}')
        return final_answer, analysis, image_urls, True, ''

    if image_urls and prefer_analysis_image:
        if on_log and not combined_text:
            on_log('    \u4fdd\u7559\u7b54\u6848\u56fe\u7247\uff08OCR\u672a\u8bc6\u522b\u6216\u670d\u52a1\u672a\u542f\u52a8\uff09')
        _mark_analysis_image_fallback(item, image_urls)
        return final_answer, analysis, image_urls, True, 'answer_image_only'

    if image_urls and not analysis and not has_good_analysis:
        if on_log and not combined_text:
            on_log('    OCR\u672a\u8bc6\u522b\uff0c\u4fdd\u7559\u89e3\u6790\u56fe\u7247\u5f85\u5bfc\u5165')
        _mark_analysis_image_fallback(item, image_urls)

    if partial_analysis and is_weak_analysis(partial_analysis):
        if on_log:
            on_log('    \u89e3\u6790\u4ec5\u5360\u4f4d\u6587\u672c\uff08\u89c1\u7b54\u6848/\u7565\uff09')
        return partial_answer, analysis, image_urls, bool(final_answer), 'analysis_placeholder'

    return final_answer or '', analysis, image_urls, bool(final_answer or analysis), ''
