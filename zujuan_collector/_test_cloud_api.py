#!/usr/bin/env python
# -*- coding: utf-8 -*-
import subprocess
import sys

import requests

from zujuan_collector.importer import ApiClient

REDIS_CLI = r'C:\Program Files\Redis\redis-cli.exe'
BASE = 'http://localhost:8080'


def main():
    s = requests.Session()
    cap = s.get(f'{BASE}/code', headers={'Accept': 'text/plain, */*'}, timeout=10).json()
    uuid = cap.get('uuid', '')
    code = ''
    if cap.get('captchaEnabled') and uuid:
        proc = subprocess.run(
            [REDIS_CLI, 'GET', f'captcha_codes:{uuid}'],
            capture_output=True,
            text=True,
            timeout=10,
        )
        code = (proc.stdout or '').strip().strip('"')
    login = s.post(
        f'{BASE}/auth/login',
        json={'username': 'admin', 'password': 'admin123', 'code': code, 'uuid': uuid},
        timeout=10,
    ).json()
    print('login', login.get('code'), login.get('msg'))
    if login.get('code') != 200:
        sys.exit(1)
    token = login['data']['access_token']
    subj = s.get(
        f'{BASE}/system/education/subject/list',
        params={'pageNum': 1, 'pageSize': 5},
        headers={'Authorization': f'Bearer {token}'},
        timeout=10,
    ).json()
    print('subjects', subj.get('code'), 'rows', len(subj.get('rows') or []))

    client = ApiClient(BASE, 'admin', 'admin123', redis_cli=REDIS_CLI)
    client.login()
    subs = client.fetch_subjects()
    print('collector subjects', len(subs))

    from pathlib import Path
    from zujuan_collector.media import upload_image

    sample = Path(__file__).resolve().parent / 'data' / 'answer_sample.png'
    if sample.exists():
        path = upload_image(client, sample.read_bytes(), 'answer_sample.png', 'image/png')
        print('upload path', path)
        img_resp = s.get(f'{BASE}{path}', timeout=10)
        print('profile get', img_resp.status_code, img_resp.headers.get('Content-Type', ''))

    print('ok')


if __name__ == '__main__':
    main()
