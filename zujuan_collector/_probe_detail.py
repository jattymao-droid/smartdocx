# -*- coding: utf-8 -*-
from pathlib import Path
from playwright.sync_api import sync_playwright

URL = 'https://zujuan.xkw.com/13q34216116.html'
OUT = Path(__file__).resolve().parent / 'data' / 'detail.html'

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(user_agent='Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/122.0.0.0 Safari/537.36')
    page.goto(URL, wait_until='networkidle', timeout=90000)
    page.wait_for_timeout(2000)
    OUT.write_text(page.content(), encoding='utf-8')
    ans = page.query_selector('.item.answer, .exam-item__opt .answer, .quesanswer')
    print('answer_el', bool(ans))
    if ans:
        print(ans.inner_html()[:500])
    browser.close()
