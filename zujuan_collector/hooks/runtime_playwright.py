# -*- coding: utf-8 -*-
"""PyInstaller runtime hook: configure Playwright browser path."""

import os
import sys
from pathlib import Path

if getattr(sys, 'frozen', False):
    base = Path(sys.executable).resolve().parent
    browsers = base / 'playwright-browsers'
    if browsers.is_dir():
        os.environ['PLAYWRIGHT_BROWSERS_PATH'] = str(browsers)
    os.environ.setdefault('PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD', '1')
