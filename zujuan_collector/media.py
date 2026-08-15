# -*- coding: utf-8 -*-
"""Download remote images and upload to RuoYi server."""

import json
import mimetypes
import os
import re
from urllib.parse import urlparse

import requests

from .cloud_api import extract_upload_path

USER_AGENT = (
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
    '(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36'
)


def _guess_name(url, index):
    path = urlparse(url).path
    base = os.path.basename(path) or f'zujuan_{index}.png'
    base = re.sub(r'[^\w.\-]+', '_', base)
    if '.' not in base:
        base += '.png'
    return base[:120]


def download_image(url, session=None, timeout=30, media_options=None):
    from .image_cleaner import normalize_image_download_url, process_downloaded_image

    opts = media_options or {}
    if opts.get('prefer_resized_images', True):
        url = normalize_image_download_url(url)
    sess = session or requests.Session()
    headers = {'User-Agent': USER_AGENT, 'Referer': 'https://zujuan.xkw.com/'}
    resp = sess.get(url, headers=headers, timeout=timeout)
    resp.raise_for_status()
    content_type = resp.headers.get('Content-Type', 'image/png')
    blob = process_downloaded_image(url, resp.content, opts)
    return blob, content_type


def upload_image(api_client, image_bytes, filename, content_type='image/png'):
    files = {'file': (filename, image_bytes, content_type)}
    headers = {'Authorization': f'Bearer {api_client.token}'}
    # In RuoYi-Cloud gateway, ruoyi-system is routed under /system/**.
    # Keep /common/upload as fallback for legacy single-service deployments.
    endpoints = (
        f'{api_client.api_base}/system/common/upload',
        f'{api_client.api_base}/common/upload',
    )
    last_err = ''
    for endpoint in endpoints:
        try:
            resp = api_client.session.post(
                endpoint,
                files=files,
                headers=headers,
                timeout=60,
            )
            data = resp.json()
            if data.get('code') != 200:
                last_err = data.get('msg') or f'upload_failed:{endpoint}'
                continue
            path = extract_upload_path(data)
            if path:
                return path
            last_err = '\u56fe\u7247\u4e0a\u4f20\u672a\u8fd4\u56de\u8def\u5f84'
        except Exception as ex:
            last_err = str(ex)
    raise RuntimeError(last_err or '\u56fe\u7247\u4e0a\u4f20\u5931\u8d25')


def localize_images(api_client, image_urls, on_log=None, media_options=None):
    if not image_urls:
        return []
    local_paths = []
    sess = requests.Session()
    sess.headers.update({'User-Agent': USER_AGENT, 'Referer': 'https://zujuan.xkw.com/'})
    for idx, url in enumerate(image_urls, 1):
        try:
            blob, ctype = download_image(url, session=sess, media_options=media_options)
            fname = _guess_name(url, idx)
            if not ctype or ctype == 'application/octet-stream':
                ctype = mimetypes.guess_type(fname)[0] or 'image/png'
            path = upload_image(api_client, blob, fname, ctype)
            local_paths.append(path)
            if on_log:
                on_log(f'    \u56fe\u7247 {idx}/{len(image_urls)} \u5df2\u4e0a\u4f20')
        except Exception as ex:
            if on_log:
                on_log(f'    \u56fe\u7247\u5931\u8d25 {url}: {ex}')
    return local_paths


def images_to_json(paths):
    if not paths:
        return None
    return json.dumps(paths, ensure_ascii=False)


def build_inline_image_html(paths):
    if not paths:
        return ''
    return ''.join(
        '<p><img src="{}" style="vertical-align:middle;max-width:100%;height:auto" alt="" /></p>'.format(p)
        for p in paths
    )


def localize_html_images(api_client, html, on_log=None, media_options=None):
    from .content_cleaner import extract_html_image_urls, is_html_content, replace_html_image_urls

    if not html or not is_html_content(html):
        return html
    urls = extract_html_image_urls(html)
    if not urls:
        return html
    mapping = {}
    sess = requests.Session()
    sess.headers.update({'User-Agent': USER_AGENT, 'Referer': 'https://zujuan.xkw.com/'})
    for idx, url in enumerate(urls, 1):
        if str(url).startswith('/profile/'):
            continue
        try:
            blob, ctype = download_image(url, session=sess, media_options=media_options)
            fname = _guess_name(url, idx)
            if not ctype or ctype == 'application/octet-stream':
                ctype = mimetypes.guess_type(fname)[0] or 'image/png'
            path = upload_image(api_client, blob, fname, ctype)
            mapping[url] = path
            if on_log:
                on_log(f'    \u56fe\u7247 {idx}/{len(urls)} \u5df2\u4e0a\u4f20')
        except Exception as ex:
            if on_log:
                on_log(f'    图片失败 {url}: {ex}')
    if not mapping:
        return html
    return replace_html_image_urls(html, mapping)
