# -*- coding: utf-8 -*-
"""Resolve app paths for development and PyInstaller frozen builds."""

import os
import sys
from pathlib import Path


def is_frozen():
    return bool(getattr(sys, 'frozen', False))


def install_dir():
    """Directory containing the executable (distribution root)."""
    if is_frozen():
        return Path(sys.executable).resolve().parent
    return Path(__file__).resolve().parent.parent


def package_dir():
    """Directory containing package assets (config templates, etc.)."""
    if is_frozen():
        meipass = Path(getattr(sys, '_MEIPASS', install_dir()))
        nested = meipass / 'zujuan_collector'
        if nested.is_dir():
            return nested
        return meipass
    return Path(__file__).resolve().parent


def playwright_browsers_dir():
    return install_dir() / 'playwright-browsers'


def setup_playwright_env():
    """Configure Playwright for frozen executable."""
    if not is_frozen():
        return
    browsers = playwright_browsers_dir()
    if browsers.is_dir():
        os.environ['PLAYWRIGHT_BROWSERS_PATH'] = str(browsers)
    os.environ.setdefault('PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD', '1')


def browsers_ready():
    if not is_frozen():
        return True
    root = playwright_browsers_dir()
    if not root.is_dir():
        return False
    return any(root.glob('chromium-*'))
