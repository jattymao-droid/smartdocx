#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Entry point for PyInstaller desktop build."""

from zujuan_collector.paths import browsers_ready, setup_playwright_env
from zujuan_collector.desktop_app import main

if __name__ == '__main__':
    setup_playwright_env()
    if not browsers_ready():
        import tkinter as tk
        from tkinter import messagebox

        root = tk.Tk()
        root.withdraw()
        messagebox.showerror(
            '\u9898\u5e93\u91c7\u96c6\u5de5\u5177',
            '\u672a\u627e\u5230\u5185\u7f6e Chromium\u6d4f\u89c8\u5668\u3002\n'
            '\u8bf7\u91cd\u65b0\u8fd0\u884c build_desktop.ps1 \u6253\u5305\uff0c'
            '\u6216\u5c06 playwright-browsers \u76ee\u5f55\u653e\u5728\u7a0b\u5e8f\u540c\u7ea7\u76ee\u5f55\u3002',
        )
        raise SystemExit(1)
    main()
