# -*- coding: utf-8 -*-
"""RuoYi-Cloud gateway API helpers for zujuan_collector."""

from urllib.parse import urlparse

SYSTEM_PREFIX = '/system'
EDUCATION_PREFIX = '/system/education'


def education_url(api_base, suffix):
    """Build full education API URL, e.g. /subject/list."""
    base = api_base.rstrip('/')
    if not suffix.startswith('/'):
        suffix = '/' + suffix
    if suffix.startswith('/education/'):
        suffix = suffix[len('/education'):]
    return f'{base}{EDUCATION_PREFIX}{suffix}'


def unwrap_data(resp_json):
    data = resp_json.get('data')
    if isinstance(data, dict):
        return data
    return resp_json


def extract_token(resp_json):
    data = unwrap_data(resp_json)
    token = data.get('access_token') or resp_json.get('token')
    if token:
        return token
    if isinstance(resp_json.get('data'), str):
        return resp_json['data']
    return None


def extract_upload_path(resp_json):
    """Normalize upload response to a gateway-relative image path."""
    if resp_json.get('fileName'):
        return resp_json['fileName']
    data = resp_json.get('data')
    if isinstance(data, dict):
        if data.get('fileName'):
            return data['fileName']
        url = data.get('url') or ''
        if url:
            return _normalize_upload_url(url)
    url = resp_json.get('url') or ''
    if url:
        return _normalize_upload_url(url)
    return ''


def _normalize_upload_url(url):
    if not url:
        return ''
    if url.startswith('/profile/'):
        return url
    if url.startswith('/file/statics/'):
        return url
    if url.startswith('http://') or url.startswith('https://'):
        path = urlparse(url).path or ''
        if path.startswith('/statics/'):
            return '/file' + path
        if path.startswith('/profile/'):
            return path
        return path or url
    if url.startswith('/statics/'):
        return '/file' + url
    return url
