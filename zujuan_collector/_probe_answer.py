# -*- coding: utf-8 -*-
import json
from pathlib import Path

import requests
from playwright.sync_api import sync_playwright

storage = Path.home() / 'AppData/Roaming/SchoolManagement/ZujuanCollector/data/zujuan_storage.json'
url = 'https://zujuan.xkw.com/13q34216116.html'

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    ctx = browser.new_context(storage_state=str(storage))
    page = ctx.new_page()
    captured = {}

    def on_resp(resp):
        u = resp.url
        if 'check_ques_parse' in u:
            try:
                captured[u] = resp.text()
            except Exception as ex:
                captured[u] = str(ex)

    page.on('response', on_resp)
    page.goto(url, wait_until='networkidle', timeout=120000)
    page.wait_for_timeout(6000)

    img = page.locator('.answer-txt img')
    src = img.first.get_attribute('src') if img.count() else ''
    print('img', src)
    print('captured', json.dumps(captured, ensure_ascii=False)[:800])

    cookies = {c['name']: c['value'] for c in ctx.cookies()}
    s = requests.Session()
    s.headers['User-Agent'] = 'Mozilla/5.0'
    s.headers['Referer'] = url
    for k, v in cookies.items():
        s.cookies.set(k, v, domain='.xkw.com')

    for payload in [
        {'quesId': 34216116, 'bankId': 13},
        {'questionId': 34216116, 'bankId': 13},
    ]:
        r = s.post('https://zujuan.xkw.com/zujuan-api/check_ques_parse', json=payload, timeout=30)
        print('POST', payload, r.status_code, r.text[:400])

    if src:
        r2 = s.get(src, timeout=30)
        print('img download', r2.status_code, len(r2.content), r2.headers.get('content-type'))
        out = Path(__file__).parent / 'data' / 'answer_sample.png'
        out.write_bytes(r2.content)
        print('saved', out)

    browser.close()
