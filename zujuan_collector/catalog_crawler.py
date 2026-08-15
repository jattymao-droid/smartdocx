# -*- coding: utf-8 -*-
"""Crawl zujuan.xkw.com chapter catalog: subject / version / textbook / chapters."""

import argparse
import json
import time
from datetime import datetime, timezone
from pathlib import Path

from playwright.sync_api import sync_playwright

from .catalog_parser import (
    count_tree_nodes,
    flatten_tree_paths,
    parse_chapter_tree,
    parse_subject_menu,
    parse_textbooks,
    parse_versions,
)
from .config_store import session_path
from .paths import setup_playwright_env
from .scraper import USER_AGENT, session_exists
from .url_utils import parse_zujuan_url

setup_playwright_env()

DEFAULT_OUT_DIR = Path(__file__).resolve().parent / 'data' / 'catalog'
API_TEXTBOOKS = 'https://zujuan.xkw.com/zujuan-api/chaptertextbooks'

SENIOR_SUBJECT_SEEDS = {
    'gzyw': 'https://zujuan.xkw.com/gzyw/zj135937/',
    'gzsx': 'https://zujuan.xkw.com/gzsx/zj135303/',
    'gzyy': 'https://zujuan.xkw.com/gzyy/zj150288/',
    'gzwl': 'https://zujuan.xkw.com/gzwl/zj136248/',
    'gzhx': 'https://zujuan.xkw.com/gzhx/zj145077/',
    'gzsw': 'https://zujuan.xkw.com/gzsw/zj150293/',
    'gzzz': 'https://zujuan.xkw.com/gzzz/zj136278/',
    'gzls': 'https://zujuan.xkw.com/gzls/zj136098/',
    'gzdl': 'https://zujuan.xkw.com/gzdl/zj145352/',
    'gzxxjs': 'https://zujuan.xkw.com/gzxxjs/zj159653/',
    'gztyjs': 'https://zujuan.xkw.com/gztyjs/zj154816/',
    'gzry': 'https://zujuan.xkw.com/gzry/zj191126/',
}


def _log(msg, on_log=None):
    if on_log:
        on_log(msg)
    else:
        print(msg, flush=True)


def _new_context(p, headless=True):
    browser = p.chromium.launch(headless=headless)
    kwargs = dict(user_agent=USER_AGENT, locale='zh-CN')
    storage = session_path()
    if storage.exists():
        kwargs['storage_state'] = str(storage)
    return browser, browser.new_context(**kwargs)


def fetch_textbooks_html(page, version_id, url_template):
    resp = page.request.get(
        API_TEXTBOOKS,
        params={'textbookVersionId': str(version_id), 'url': url_template},
        headers={'content-type': 'application/x-www-form-urlencoded'},
        timeout=60000,
    )
    if not resp.ok:
        raise RuntimeError(f'chaptertextbooks HTTP {resp.status} version={version_id}')
    return resp.text()


def _is_access_blocked(html, page_title=''):
    title = (page_title or '').strip()
    if '\u7981\u6b62\u8bbf\u95ee' in title or '\u8bbf\u95ee\u88ab\u62d2' in title:
        return True
    text = (html or '')[:8000]
    if '\u7981\u6b62\u8bbf\u95ee' in text or '405' in title:
        return True
    if 'potential threats' in text.lower():
        return True
    return False


def _crawl_catalog_on_page(
    page,
    seed_url,
    *,
    delay=0.6,
    on_log=None,
    versions_limit=None,
    textbooks_limit=None,
):
    meta = parse_zujuan_url(seed_url)
    if not meta.get('valid') or meta.get('kind') != 'list':
        raise ValueError(f'unsupported URL: {seed_url}')

    subject_code = meta.get('subject_code') or ''
    subject_label = meta.get('subject_label') or ''

    catalog = {
        'source_url': seed_url.rstrip('/') + '/',
        'crawled_at': datetime.now(timezone.utc).isoformat(),
        'subject': {
            'code': subject_code,
            'label': subject_label,
            'school_stage': '\u9ad8\u4e2d' if subject_code.startswith('gz') else (
                '\u521d\u4e2d' if subject_code.startswith('cz') else ''
            ),
        },
        'versions': [],
    }

    _log(f'\u52a0\u8f7d\u76ee\u5f55\u9875: {seed_url}', on_log)
    page.goto(seed_url, wait_until='networkidle', timeout=120000)
    page.wait_for_timeout(int(delay * 1000))
    html = page.content()
    if _is_access_blocked(html, page.title()):
        raise RuntimeError(
            '\u7ec4\u5377\u7f51\u8fd4\u56de\u201c\u7981\u6b62\u8bbf\u95ee\u201d\uff0c'
            '\u8bf7\u7b49\u5f85 10~30 \u5206\u949f\u540e\u5355\u79d1\u722c\u53d6\uff0c'
            '\u6216\u6362\u7f51\u7edc/\u91cd\u65b0\u767b\u5f55\u540e\u518d\u8bd5'
        )

    menu = parse_subject_menu(html)
    if menu.get('title'):
        catalog['subject']['menu_title'] = menu['title']
    for item in menu.get('subjects') or []:
        if item.get('selected'):
            catalog['subject']['subject_id'] = item.get('subject_id')
            catalog['subject']['phase'] = item.get('phase')
            if item.get('name'):
                catalog['subject']['nav_name'] = item.get('name')
            break

    versions = parse_versions(html)
    if not versions:
        page.wait_for_timeout(2000)
        html = page.content()
        versions = parse_versions(html)
    if not versions:
        raise RuntimeError(
            '\u672a\u89e3\u6790\u5230\u7248\u672c\u5217\u8868\uff0c'
            '\u8bf7\u786e\u8ba4\u5df2\u767b\u5f55\u7ec4\u5377\u7f51\u4e14 URL \u6b63\u786e'
        )

    if versions_limit:
        versions = versions[:versions_limit]

    _log(f'\u7248\u672c\u6570: {len(versions)}', on_log)

    for v_idx, version in enumerate(versions, 1):
        v_name = version.get('name') or version.get('version_id')
        _log(f'  [{v_idx}/{len(versions)}] \u7248\u672c: {v_name}', on_log)
        version_entry = {
            'version_id': version.get('version_id'),
            'name': version.get('name'),
            'url_template': version.get('url_template'),
            'textbooks': [],
        }

        try:
            tb_html = fetch_textbooks_html(
                page,
                version.get('version_id'),
                version.get('url_template') or f'/{subject_code}/zj{{0}}/',
            )
        except Exception as ex:
            _log(f'    \u6559\u6750\u5217\u8868\u83b7\u53d6\u5931\u8d25: {ex}', on_log)
            catalog['versions'].append(version_entry)
            continue

        textbooks = parse_textbooks(tb_html)
        if not textbooks:
            textbooks = parse_textbooks(html)

        if textbooks_limit:
            textbooks = textbooks[:textbooks_limit]

        _log(f'    \u6559\u6750\u6570: {len(textbooks)}', on_log)

        for t_idx, textbook in enumerate(textbooks, 1):
            tb_url = textbook.get('url') or ''
            if not tb_url:
                zj_id = textbook.get('zj_id')
                tb_url = f'https://zujuan.xkw.com/{subject_code}/zj{zj_id}/'
            _log(
                f'    [{t_idx}/{len(textbooks)}] \u6559\u6750: '
                f'{textbook.get("name")} ({tb_url})',
                on_log,
            )

            page.goto(tb_url, wait_until='networkidle', timeout=120000)
            page.wait_for_timeout(int(delay * 1000))
            tb_page_html = page.content()
            chapters = parse_chapter_tree(tb_page_html)
            node_count = count_tree_nodes(chapters)
            _log(f'      \u7ae0\u8282\u8282\u70b9: {node_count}', on_log)

            version_entry['textbooks'].append({
                'zj_id': textbook.get('zj_id'),
                'name': textbook.get('name'),
                'url': tb_url,
                'chapter_count': node_count,
                'chapters': chapters,
            })
            time.sleep(delay)

        catalog['versions'].append(version_entry)
        time.sleep(delay)

    return catalog


def crawl_catalog(
    seed_url,
    *,
    headless=True,
    delay=0.6,
    on_log=None,
    versions_limit=None,
    textbooks_limit=None,
):
    """Crawl catalog tree starting from a chapter list URL like /gzyw/zj135948/."""
    with sync_playwright() as p:
        browser, context = _new_context(p, headless=headless)
        page = context.new_page()
        catalog = _crawl_catalog_on_page(
            page,
            seed_url,
            delay=delay,
            on_log=on_log,
            versions_limit=versions_limit,
            textbooks_limit=textbooks_limit,
        )
        browser.close()
    return catalog


def save_catalog(catalog, out_dir=None, filename=None):
    out_dir = Path(out_dir or DEFAULT_OUT_DIR)
    out_dir.mkdir(parents=True, exist_ok=True)
    code = (catalog.get('subject') or {}).get('code') or 'catalog'
    path = out_dir / (filename or f'{code}_catalog.json')
    path.write_text(json.dumps(catalog, ensure_ascii=False, indent=2), encoding='utf-8')

    flat = []
    for version in catalog.get('versions') or []:
        for textbook in version.get('textbooks') or []:
            for row in flatten_tree_paths(textbook.get('chapters') or []):
                flat.append({
                    'subject_code': (catalog.get('subject') or {}).get('code'),
                    'version_name': version.get('name'),
                    'textbook_name': textbook.get('name'),
                    'textbook_zj_id': textbook.get('zj_id'),
                    **row,
                })
    paths_file = out_dir / f'{code}_catalog_paths.json'
    paths_file.write_text(json.dumps(flat, ensure_ascii=False, indent=2), encoding='utf-8')
    return path


def crawl_all_senior_subjects(
    *,
    headless=True,
    delay=0.6,
    on_log=None,
    out_dir=None,
    skip_existing=False,
    seeds=None,
):
    """Crawl all high-school subjects and save one JSON per subject code."""
    seeds = seeds or SENIOR_SUBJECT_SEEDS
    out_dir = Path(out_dir or DEFAULT_OUT_DIR)
    results = []

    for code, seed_url in seeds.items():
        out_path = out_dir / f'{code}_catalog.json'
        if skip_existing and out_path.exists():
            _log(f'[\u8df3\u8fc7] {code} \u5df2\u5b58\u5728: {out_path}', on_log)
            results.append({'subject_code': code, 'skipped': True, 'path': str(out_path)})
            continue

        _log(f'[\u5f00\u59cb] {code} <- {seed_url}', on_log)
        try:
            with sync_playwright() as p:
                browser, context = _new_context(p, headless=headless)
                page = context.new_page()
                catalog = _crawl_catalog_on_page(
                    page,
                    seed_url,
                    delay=delay,
                    on_log=on_log,
                )
                browser.close()
            saved = save_catalog(catalog, out_dir=out_dir)
            versions = len(catalog.get('versions') or [])
            textbooks = sum(len(v.get('textbooks') or []) for v in catalog.get('versions') or [])
            chapters = sum(
                tb.get('chapter_count') or count_tree_nodes(tb.get('chapters'))
                for v in catalog.get('versions') or []
                for tb in v.get('textbooks') or []
            )
            _log(
                f'[\u5b8c\u6210] {code}: \u7248\u672c {versions}, '
                f'\u6559\u6750 {textbooks}, \u7ae0\u8282 {chapters} -> {saved}',
                on_log,
            )
            results.append({
                'subject_code': code,
                'path': str(saved),
                'versions': versions,
                'textbooks': textbooks,
                'chapters': chapters,
            })
        except Exception as ex:
            _log(f'[\u5931\u8d25] {code}: {ex}', on_log)
            results.append({'subject_code': code, 'error': str(ex)})
        time.sleep(max(delay * 3, 15))

    return results


def main(argv=None):
    parser = argparse.ArgumentParser(description='Crawl zujuan chapter catalog tree')
    parser.add_argument(
        '--url',
        default='https://zujuan.xkw.com/gzyw/zj135948/',
        help='Seed chapter list URL',
    )
    parser.add_argument('--out', default=str(DEFAULT_OUT_DIR), help='Output directory')
    parser.add_argument('--headed', action='store_true', help='Show browser window')
    parser.add_argument('--delay', type=float, default=0.6, help='Delay between requests (seconds)')
    parser.add_argument('--versions-limit', type=int, default=0, help='Limit versions (0=all)')
    parser.add_argument('--textbooks-limit', type=int, default=0, help='Limit textbooks per version (0=all)')
    parser.add_argument('--all-senior', action='store_true', help='Crawl all high-school subjects')
    parser.add_argument('--skip-existing', action='store_true', help='Skip subjects with existing JSON')
    parser.add_argument('--resume', action='store_true', help='Only crawl subjects missing catalog JSON')
    args = parser.parse_args(argv)

    if not session_exists():
        print(
            '\u8b66\u544a: \u672a\u627e\u5230\u7ec4\u5377\u7f51\u767b\u5f55\u4f1a\u8bdd\uff0c'
            '\u90e8\u5206\u76ee\u5f55\u53ef\u80fd\u65e0\u6cd5\u52a0\u8f7d\u3002'
            '\u8bf7\u5148\u8fd0\u884c\u91c7\u96c6\u7aef\u767b\u5f55\u3002'
        )

    if args.all_senior or args.resume:
        seeds = dict(SENIOR_SUBJECT_SEEDS)
        if args.resume:
            out_dir = Path(args.out)
            seeds = {
                code: url for code, url in seeds.items()
                if not (out_dir / f'{code}_catalog.json').exists()
            }
            if not seeds:
                print('\u6240\u6709\u9ad8\u4e2d\u5b66\u79d1\u76ee\u5f55\u5df2\u5b58\u5728\uff0c\u65e0\u9700\u722c\u53d6')
                return
            print(f'\u5f85\u722c\u53d6: {", ".join(seeds.keys())}')
        results = crawl_all_senior_subjects(
            headless=not args.headed,
            delay=max(args.delay, 1.5),
            out_dir=args.out,
            skip_existing=args.skip_existing and not args.resume,
            seeds=seeds,
        )
        ok = [r for r in results if not r.get('error') and not r.get('skipped')]
        fail = [r for r in results if r.get('error')]
        print(json.dumps(results, ensure_ascii=False, indent=2))
        print(f'\u6458\u8981: \u6210\u529f {len(ok)}, \u5931\u8d25 {len(fail)}, \u8df3\u8fc7 {len(results) - len(ok) - len(fail)}')
        return

    catalog = crawl_catalog(
        args.url,
        headless=not args.headed,
        delay=args.delay,
        versions_limit=args.versions_limit or None,
        textbooks_limit=args.textbooks_limit or None,
    )
    out_path = save_catalog(catalog, out_dir=args.out)
    versions = len(catalog.get('versions') or [])
    textbooks = sum(len(v.get('textbooks') or []) for v in catalog.get('versions') or [])
    chapters = sum(
        tb.get('chapter_count') or count_tree_nodes(tb.get('chapters'))
        for v in catalog.get('versions') or []
        for tb in v.get('textbooks') or []
    )
    print(f'\u5df2\u4fdd\u5b58: {out_path}')
    print(f'\u7edf\u8ba1: \u7248\u672c {versions}, \u6559\u6750 {textbooks}, \u7ae0\u8282\u8282\u70b9 {chapters}')


if __name__ == '__main__':
    main()
