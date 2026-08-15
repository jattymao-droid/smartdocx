# -*- coding: utf-8 -*-
"""Probe zujuan.xkw.com network APIs (dev only)."""
import json
import re
import sys
from playwright.sync_api import sync_playwright

URL = sys.argv[1] if len(sys.argv) > 1 else 'https://zujuan.xkw.com/gzwl/zj136248/'
hits = []


def main():
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
            if 'json' not in ct and 'javascript' not in ct:
                return
            if not re.search(r'question|zujuan|paper|shiti|exam|ques', url, re.I):
                return
            try:
                body = resp.text()
                if len(body) > 500000:
                    body = body[:500000]
                hits.append({'url': url, 'status': resp.status, 'sample': body[:2000]})
            except Exception:
                pass

        page.on('response', on_response)
        page.goto(URL, wait_until='networkidle', timeout=90000)
        page.wait_for_timeout(3000)
        title = page.title()
        html = page.content()
        print('TITLE:', title)
        print('HTML_LEN:', len(html))
        print('API_HITS:', len(hits))
        for i, h in enumerate(hits[:20]):
            print('\n---', i, h['status'], h['url'])
            print(h['sample'][:500])
        browser.close()


if __name__ == '__main__':
    main()
