# -*- coding: utf-8 -*-
import json
from playwright.sync_api import sync_playwright

from zujuan_collector.config_store import session_path
from zujuan_collector.parser import parse_detail_stem, parse_options_from_content
from zujuan_collector.paths import setup_playwright_env
from bs4 import BeautifulSoup

setup_playwright_env()
url = 'https://zujuan.xkw.com/13q34266545.html'
storage = session_path()

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    kwargs = {'locale': 'zh-CN'}
    if storage.exists():
        kwargs['storage_state'] = str(storage)
    ctx = browser.new_context(**kwargs)
    page = ctx.new_page()
    page.goto(url, wait_until='networkidle', timeout=120000)
    page.wait_for_timeout(2000)
    html = page.content()
    browser.close()

stem = parse_detail_stem(html, '34266545')
print('=== parse_detail_stem options ===')
print(json.dumps(stem.get('options'), ensure_ascii=False, indent=2))
print('options count', len(stem.get('options') or []))

soup = BeautifulSoup(html, 'lxml')
cnt = soup.select_one('.quest-cnt') or soup.select_one('.exam-item__cnt')
if cnt:
    table = cnt.find('table', attrs={'name': 'optionsTable'})
    print('has optionsTable', bool(table))
    if table:
        for i, td in enumerate(table.find_all('td')):
            print(f'--- td {i} raw ---')
            print(''.join(str(x) for x in td.contents)[:500])
            print('plain:', td.get_text(' ', strip=True)[:200])

# save snippet
out = __file__.replace('_probe_q.py', 'data/probe_34266545.html')
with open(out, 'w', encoding='utf-8') as f:
    f.write(html)
print('saved', out)
