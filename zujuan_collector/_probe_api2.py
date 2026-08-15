# -*- coding: utf-8 -*-
import json
import re
import sys
from pathlib import Path
from playwright.sync_api import sync_playwright

URL = sys.argv[1] if len(sys.argv) > 1 else 'https://zujuan.xkw.com/gzwl/zj136248/'
OUT = Path(__file__).resolve().parent / 'data'
OUT.mkdir(exist_ok=True)


def main():
    apis = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(
            user_agent='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
            locale='zh-CN',
        )

        def on_response(resp):
            url = resp.url
            if 'xkw.com' not in url:
                return
            ct = (resp.headers.get('content-type') or '').lower()
            if 'json' not in ct:
                return
            try:
                text = resp.text()
                apis.append({'url': url, 'status': resp.status, 'body': text[:8000]})
            except Exception:
                pass

        page.on('response', on_response)
        page.goto(URL, wait_until='networkidle', timeout=90000)
        page.wait_for_timeout(4000)
        html = page.content()
        (OUT / 'page.html').write_text(html, encoding='utf-8')
        (OUT / 'apis.json').write_text(json.dumps(apis, ensure_ascii=False, indent=2), encoding='utf-8')

        # DOM question blocks
        cards = page.query_selector_all('.tk-quest-item, .ques-item, [data-question-id], .question-item, .exam-item')
        print('cards', len(cards))
        browser.close()

    # search inline JSON in html
    html = (OUT / 'page.html').read_text(encoding='utf-8')
    for pat in [r'questionList\s*[:=]\s*(\[[\s\S]{0,5000}?\])', r'"questions"\s*:\s*(\[[\s\S]{0,5000}?\])', r'quesList\s*[:=]\s*(\[[\s\S]{0,5000}?\])']:
        m = re.search(pat, html)
        if m:
            (OUT / 'inline_match.txt').write_text(m.group(0)[:10000], encoding='utf-8')
            print('inline', pat[:20])
    print('apis', len(apis))
    for a in apis:
        print(a['url'])


if __name__ == '__main__':
    main()
