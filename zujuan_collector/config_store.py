# -*- coding: utf-8 -*-
"""Load/save collector config for CLI and desktop app."""

import json
import os
from pathlib import Path

from .paths import package_dir

ROOT = package_dir()
DEFAULT_CONFIG = ROOT / 'config.json'
EXAMPLE_CONFIG = ROOT / 'config.example.json'


def user_config_dir():
    appdata = os.environ.get('APPDATA')
    if appdata:
        return Path(appdata) / 'SchoolManagement' / 'ZujuanCollector'
    return Path.home() / '.zujuan_collector'


def user_config_path():
    return user_config_dir() / 'config.json'


def default_config():
    if DEFAULT_CONFIG.exists():
        return json.loads(DEFAULT_CONFIG.read_text(encoding='utf-8'))
    if EXAMPLE_CONFIG.exists():
        return json.loads(EXAMPLE_CONFIG.read_text(encoding='utf-8'))
    return {
        'api_base': 'http://localhost:8080',
        'username': 'admin',
        'password': 'admin123',
        'redis_cli': r'C:\Program Files\Redis\redis-cli.exe',
        'subject_id': 4,
        'textbook_id': None,
        'version_id': None,
        'school_stage': '\u9ad8\u4e2d',
        'chapter_id': None,
        'chapter_text': '\u7ec4\u5377\u7f51\u91c7\u96c6',
        'fetch_answer': False,
        'import_delay': 0.35,
        'headless': True,
        'prefer_resized_images': True,
        'remove_watermark': True,
        'prefer_analysis_image': False,
        'ocr_mode': 'auto',
        'auto_create_chapters': True,
        'auto_create_catalog': True,
        'auto_create_subjects': False,
        'default_version_name': '\u7ec4\u5377\u7f51\u540c\u6b65',
        'default_textbook_name': '\u7ec4\u5377\u7f51\u7efc\u5408\u5e93',
        'prefer_resized_images': True,
        'remove_watermark': True,
        'prefer_analysis_image': False,
        'ocr_mode': 'auto',
        'auto_clean_fullsize': True,
    }


def load_config(path=None):
    path = Path(path) if path else user_config_path()
    if path.exists():
        data = json.loads(path.read_text(encoding='utf-8'))
        merged = default_config()
        merged.update(data)
        return merged, path
    if DEFAULT_CONFIG.exists():
        data = json.loads(DEFAULT_CONFIG.read_text(encoding='utf-8'))
        merged = default_config()
        merged.update(data)
        return merged, user_config_path()
    return default_config(), user_config_path()


def save_config(cfg, path=None):
    path = Path(path) if path else user_config_path()
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(cfg, ensure_ascii=False, indent=2), encoding='utf-8')
    return path


def session_path():
    d = user_config_dir() / 'data'
    d.mkdir(parents=True, exist_ok=True)
    return d / 'zujuan_storage.json'


def data_dir():
    d = user_config_dir() / 'data'
    d.mkdir(parents=True, exist_ok=True)
    return d
