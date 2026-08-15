# -*- coding: utf-8 -*-
"""Fetch list page and parse question 34266545 options."""
import json
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright
from bs4 import BeautifulSoup

from zujuan_collector.parser import parse_list_item, parse_detail_stem

ZID = '34266545'
LIST_URL = 'https://zujuan.xkw.com/13p3334334.html'
DETAIL_URL = f'https://zujuan.xkw.com/13q{ZID}.html'
OUT = Path(__file__).parent / 'data' / 'list_probe_34266545.html'


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        page.goto(LIST_URL, wait_until='networkidle', timeout=120000)
        page.wait_for_timeout(2000)
        html = page.content()
        OUT.write_text(html, encoding='utf-8')

        soup = BeautifulSoup(html, 'lxml')
        node = soup.select_one(f'.quesroot[questionid="{ZID}"]')
        list_item = parse_list_item(node, {}) if node else None

        page.goto(DETAIL_URL, wait_until='networkidle', timeout=120000)
        page.wait_for_timeout(500)
        # simulate early capture (no quest-cnt wait)
        early_html = page.content()
        early_stem = parse_detail_stem(early_html, ZID)

        page.wait_for_selector('.quest-cnt', timeout=30000)
        late_html = page.content()
        late_stem = parse_detail_stem(late_html, ZID)

        browser.close()

    result = {
        'list_found': bool(node),
        'list_options': list_item.get('options') if list_item else None,
        'list_content_has_table': 'optionsTable' in (list_item.get('content') or '') if list_item else False,
        'early_has_quest_cnt': bool(BeautifulSoup(early_html, 'lxml').select_one('.quest-cnt')),
        'early_options': early_stem.get('options'),
        'late_options': late_stem.get('options'),
    }
    out_json = Path(__file__).parent / 'data' / 'list_probe_result.json'
    out_json.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
    print(out_json)


if __name__ == '__main__':
    main()
