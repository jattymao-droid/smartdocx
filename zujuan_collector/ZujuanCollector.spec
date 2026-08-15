# -*- mode: python ; coding: utf-8 -*-
"""PyInstaller spec for ZujuanCollector desktop app."""

import os
from pathlib import Path

from PyInstaller.utils.hooks import collect_data_files, collect_submodules

block_cipher = None

repo_root = Path(SPECPATH).parent
pkg_dir = Path(SPECPATH)

datas = []
datas += collect_data_files('customtkinter')
datas.append((str(pkg_dir / 'config.example.json'), 'zujuan_collector'))

try:
    import playwright

    driver_src = Path(playwright.__file__).resolve().parent / 'driver'
    if driver_src.is_dir():
        datas.append((str(driver_src), 'playwright' + os.sep + 'driver'))
except Exception:
    pass

hiddenimports = collect_submodules('playwright')
hiddenimports += collect_submodules('zujuan_collector')
hiddenimports += [
    'zujuan_collector',
    'zujuan_collector.app_entry',
    'zujuan_collector.desktop_app',
    'zujuan_collector.scraper',
    'zujuan_collector.parser',
    'zujuan_collector.importer',
    'zujuan_collector.cloud_api',
    'zujuan_collector.config_store',
    'zujuan_collector.paths',
    'zujuan_collector.image_cleaner',
    'zujuan_collector.media',
    'zujuan_collector.answer_extractor',
    'zujuan_collector.answer_api',
    'zujuan_collector.answer_fetcher',
    'zujuan_collector.item_validator',
    'zujuan_collector.ocr_client',
    'bs4',
    'lxml',
    'lxml.etree',
    'lxml._elementpath',
    'requests',
    'urllib3',
    'certifi',
]

a = Analysis(
    [str(pkg_dir / 'app_entry.py')],
    pathex=[str(repo_root)],
    binaries=[],
    datas=datas,
    hiddenimports=hiddenimports,
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[str(pkg_dir / 'hooks' / 'runtime_playwright.py')],
    excludes=['matplotlib', 'numpy', 'pandas', 'scipy', 'PIL'],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    [],
    exclude_binaries=True,
    name='ZujuanCollector',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    console=False,
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
)

coll = COLLECT(
    exe,
    a.binaries,
    a.zipfiles,
    a.datas,
    strip=False,
    upx=True,
    upx_exclude=[],
    name='ZujuanCollector',
)
