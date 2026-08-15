# -*- coding: utf-8 -*-
"""Playwright scraper for zujuan.xkw.com."""

import time
from pathlib import Path

from playwright.sync_api import sync_playwright

from .answer_fetcher import fetch_answer_from_page
from .config_store import session_path
from .ocr_client import DEFAULT_OCR_URL, check_ocr_available, try_start_ocr_service
from .parser import parse_detail_stem, parse_list_page, enrich_answer_from_analysis
from .content_cleaner import merge_image_urls, filter_question_images, resolve_chapter_text, is_html_content
from .item_validator import finalize_item
from .paths import setup_playwright_env
from .url_utils import infer_chapter_node_from_html, infer_textbook_from_url, page_url, parse_pager_from_html
from .subject_resolver import subject_context_from_url

setup_playwright_env()

STORAGE = session_path()
USER_AGENT = (
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
    '(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36'
)


def _log(msg, on_log=None):
    if on_log:
        on_log(msg)
    else:
        print(msg)


def login_interactive(headless=False, on_log=None, wait_event=None):
    STORAGE.parent.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=headless)
        context = browser.new_context(user_agent=USER_AGENT, locale='zh-CN')
        page = context.new_page()
        page.goto('https://zujuan.xkw.com/', wait_until='domcontentloaded', timeout=120000)
        _log('\u7ec4\u5377\u7f51\u767b\u5f55\uff1a\u8bf7\u5728\u6d4f\u89c8\u5668\u4e2d\u5b8c\u6210\u767b\u5f55', on_log)
        if wait_event is not None:
            wait_event.wait()
        else:
            _log('\u767b\u5f55\u6210\u529f\u540e\u6309 Enter \u4fdd\u5b58\u4f1a\u8bdd...', on_log)
            input()
        context.storage_state(path=str(STORAGE))
        browser.close()
    _log(f'\u4f1a\u8bdd\u5df2\u4fdd\u5b58: {STORAGE}', on_log)
    return str(STORAGE)


def session_exists():
    return STORAGE.exists()


def _new_context(p, headless=True):
    browser = p.chromium.launch(headless=headless)
    kwargs = dict(user_agent=USER_AGENT, locale='zh-CN')
    if STORAGE.exists():
        kwargs['storage_state'] = str(STORAGE)
    context = browser.new_context(**kwargs)
    return browser, context


def collect_chapter(
    url,
    pages=1,
    start_page=1,
    fetch_answer=False,
    fetch_detail=True,
    delay=0.8,
    headless=True,
    on_log=None,
    should_cancel=None,
    on_progress=None,
    on_page_done=None,
    ocr_base=None,
    prefer_analysis_image=False,
    cfg=None,
):
    url = url.rstrip('/')
    # Strip /o2pN suffix so start_page controls pagination.
    import re
    url = re.sub(r'/o2p\d+/?$', '', url)
    url_subject = subject_context_from_url(url)
    all_items = []
    seen = set()
    fetch_detail = fetch_detail or fetch_answer
    last_pager = None

    if fetch_answer:
        ocr_url = ocr_base or DEFAULT_OCR_URL
        if not check_ocr_available(ocr_url):
            ocr_logger = (lambda msg: on_log(msg)) if on_log else None
            if not try_start_ocr_service(ocr_url, on_log=ocr_logger):
                _log(
                    f'[\u8b66\u544a] OCR \u670d\u52a1\u672a\u542f\u52a8 ({ocr_url})\uff0c'
                    '\u7b54\u6848\u56fe\u7247\u5c06\u4fdd\u7559\u4f46\u65e0\u6cd5\u8bc6\u522b\u6587\u5b57\u3002'
                    '\u8bf7\u8fd0\u884c scripts\\start-ocr-service.ps1',
                    on_log,
                )

    with sync_playwright() as p:
        browser, context = _new_context(p, headless=headless)
        page = context.new_page()
        page_ctx = {}

        end_page = start_page + max(1, pages) - 1
        for page_num in range(start_page, end_page + 1):
            if should_cancel and should_cancel():
                _log('\u91c7\u96c6\u5df2\u53d6\u6d88', on_log)
                break
            target = page_url(url, page_num)
            _log(f'[\u91c7\u96c6] \u7b2c {page_num}/{end_page} \u9875 {target}', on_log)
            page.goto(target, wait_until='networkidle', timeout=120000)
            page.wait_for_timeout(int(delay * 1000))
            html = page.content()
            items, page_ctx = parse_list_page(html, page_ctx, on_log=on_log)
            page_ctx = {**url_subject, **(page_ctx or {})}
            pager = parse_pager_from_html(html)
            pager['current_page'] = page_num
            if pager.get('next_page') is None and page_num < end_page:
                pager['next_page'] = page_num + 1
                pager['has_next'] = True
            last_pager = pager
            if not page_ctx.get('textbook'):
                tb = infer_textbook_from_url(target)
                if tb:
                    page_ctx['textbook'] = tb
            if not page_ctx.get('textbook') and page_ctx.get('textbook_hint'):
                page_ctx['textbook'] = page_ctx.get('textbook_hint')
            if not page_ctx.get('chapter_node'):
                chapter_node = infer_chapter_node_from_html(html, target)
                if chapter_node:
                    page_ctx['chapter_node'] = chapter_node
            for item in items:
                item.setdefault('page_ctx', {}).update(page_ctx)
                item.update({
                    'subject_code': page_ctx.get('subject_code', ''),
                    'subject_label': page_ctx.get('subject_label', ''),
                    'school_stage': page_ctx.get('school_stage', ''),
                })
                item['chapter_text'] = resolve_chapter_text(item, page_ctx)
            _log(f'  \u627e\u5230 {len(items)} \u9053\u9898', on_log)
            if page_ctx.get('textbook'):
                _log(f'  \u6559\u6750: {page_ctx.get("textbook")}', on_log)
            if pager.get('total_pages'):
                _log(
                    f'  \u5206\u9875: \u7b2c {page_num}/{pager["total_pages"]} \u9875',
                    on_log,
                )
            page_added = 0
            for item in items:
                if should_cancel and should_cancel():
                    break
                zid = item.get('zujuan_id')
                if zid in seen:
                    continue
                seen.add(zid)
                if fetch_detail:
                    _fill_detail(
                        page, item, delay, fetch_answer, on_log, ocr_base,
                        prefer_analysis_image=prefer_analysis_image,
                        cfg=cfg,
                    )
                all_items.append(item)
                page_added += 1
                if on_progress:
                    on_progress(len(all_items), page_num, end_page)
            if on_page_done:
                cont = on_page_done(page_num, page_added, pager, len(all_items))
                if cont is False:
                    break

        browser.close()

    return all_items


def _fill_detail(page, item, delay, fetch_answer, on_log=None, ocr_base=None, prefer_analysis_image=False, cfg=None):
    detail_url = item['detail_url']
    try:
        page.goto(detail_url, wait_until='networkidle', timeout=120000)
        page.wait_for_timeout(int(delay * 1000))
        try:
            page.wait_for_selector(
                f'.quesroot[questionid="{item.get("zujuan_id")}"] .quest-cnt, '
                f'#quesdiv{item.get("zujuan_id")} .exam-item__cnt, .quest-cnt',
                timeout=30000,
            )
        except Exception:
            pass

        html = page.content()
        stem = parse_detail_stem(
            html,
            item.get('zujuan_id'),
            item.get('question_type') or '',
        )
        detected_type = stem.get('question_type') or ''
        if detected_type:
            item['question_type'] = detected_type
        if stem.get('type_label'):
            item['type_label'] = stem['type_label']
        prev_content = item.get('content') or ''
        new_content = stem.get('content') or ''
        prev_html = is_html_content(prev_content)
        new_html = is_html_content(new_content)
        new_images = bool(stem.get('image_urls'))
        prefer_new = (
            not prev_content
            or new_html
            or (new_images and not prev_html)
            or len(new_content) >= len(prev_content)
        )
        if new_content and prefer_new:
            item['content'] = new_content
            item['content_html'] = stem.get('content_html') or new_content
        if stem.get('options'):
            item['options'] = stem['options']
        elif not item.get('options'):
            # Detail page may use a different container; retry regardless of declared type.
            from bs4 import BeautifulSoup
            from .parser import parse_options_from_content as _parse_opts
            qtype = item.get('question_type') or ''
            retry_cnt = BeautifulSoup(html, 'lxml').select_one(
                f'.quesroot[questionid="{item.get("zujuan_id")}"] .quest-cnt, '
                f'.quesroot[questionid="{item.get("zujuan_id")}"] .exam-item__cnt, '
                '.quest-cnt, .exam-item__cnt'
            )
            if retry_cnt:
                retry_opts, retry_imgs = _parse_opts(retry_cnt, qtype)
                if retry_opts:
                    item['options'] = retry_opts
                    item['image_urls'] = filter_question_images(merge_image_urls(
                        item.get('image_urls'),
                        retry_imgs,
                    ))
                    from .parser import infer_question_type as _infer_type
                    item['question_type'] = _infer_type(item)
        if stem.get('detail_chapter_text'):
            item['detail_chapter_text'] = stem['detail_chapter_text']
        if stem.get('knowledge_leaf'):
            item['knowledge_leaf'] = stem['knowledge_leaf']
        item['image_urls'] = filter_question_images(merge_image_urls(
            item.get('image_urls'),
            stem.get('image_urls'),
        ))

        if fetch_answer:
            ocr_url = ocr_base or 'http://127.0.0.1:8867'
            answer, analysis, ans_images, ok, note = fetch_answer_from_page(
                page, item, ocr_base=ocr_url, on_log=on_log,
                prefer_analysis_image=prefer_analysis_image,
                cfg=cfg,
            )
            item['answer'] = answer
            item['analysis'] = analysis
            enrich_answer_from_analysis(item)
            item['answer_available'] = ok and bool(
                item.get('answer') or item.get('analysis') or ans_images
                or item.get('analysis_from_image')
            )
            item['answer_note'] = note
            if ans_images:
                item['answer_image_urls'] = ans_images
            if item.get('analysis_from_image') and not item.get('analysis_image_urls'):
                item['analysis_image_urls'] = ans_images
            if item['answer_available'] and on_log:
                if item.get('analysis_from_image'):
                    _log(
                        f'  \u7b54\u6848 zujuan:{item.get("zujuan_id")} -> {answer or "-"}'
                        f' \u89e3\u6790:\u56fe\u7247({len(item.get("analysis_image_urls") or [])}\u5f20)',
                        on_log,
                    )
                else:
                    _log(
                        f'  \u7b54\u6848 zujuan:{item.get("zujuan_id")} -> {answer or "-"}'
                        + (f' \u89e3\u6790\u5b57\u6570:{len(analysis)}' if analysis else ''),
                        on_log,
                    )
            elif not item['answer_available'] and on_log:
                _log(f'  \u7b54\u6848\u672a\u83b7\u53d6 zujuan:{item.get("zujuan_id")} {note}', on_log)
        finalize_item(item)
    except Exception as ex:
        item['answer_note'] = str(ex)
        _log(f'  \u8be6\u60c5\u5931\u8d25 {item.get("zujuan_id")}: {ex}', on_log)
