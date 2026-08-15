# -*- coding: utf-8 -*-
"""Windows desktop GUI for zujuan question bank collector."""

import json
import queue
import threading
import tkinter as tk
from pathlib import Path
from tkinter import filedialog, messagebox

import customtkinter as ctk

from .config_store import data_dir, load_config, save_config, user_config_path
from .importer import ApiClient, import_questions
from .item_validator import finalize_item
from .scraper import collect_chapter, login_interactive, session_exists
from .subject_resolver import match_subject_id, short_subject_name
from .url_utils import list_page_url, parse_zujuan_url

APP_TITLE = '\u9898\u5e93\u91c7\u96c6\u5de5\u5177'
DEFAULT_URL = 'https://zujuan.xkw.com/gzwl/zj136248/'

# UI palette (aligned with portal teal theme)
COLOR_PRIMARY = '#0d9488'
COLOR_PRIMARY_HOVER = '#0f766e'
COLOR_HEADER = '#115e59'
COLOR_HEADER_SUB = '#ccfbf1'
COLOR_BG = '#f1f5f9'
COLOR_CARD = '#ffffff'
COLOR_CARD_BORDER = '#e2e8f0'
COLOR_MUTED = '#64748b'
COLOR_SUCCESS = '#059669'
COLOR_DANGER = '#ef4444'
COLOR_DANGER_HOVER = '#dc2626'
COLOR_ACCENT = '#1e6fff'
COLOR_ACCENT_HOVER = '#1558cc'

FONT_TITLE = ('Microsoft YaHei UI', 20, 'bold')
FONT_SECTION = ('Microsoft YaHei UI', 15, 'bold')
FONT_LABEL = ('Microsoft YaHei UI', 13)
FONT_SMALL = ('Microsoft YaHei UI', 11)
FONT_LOG = ('Consolas', 12)


class ZujuanDesktopApp(ctk.CTk):
    def __init__(self):
        super().__init__()
        ctk.set_appearance_mode('light')
        ctk.set_default_color_theme('blue')

        self.title(APP_TITLE)
        self.geometry('1040x760')
        self.minsize(920, 680)
        self.configure(fg_color=COLOR_BG)

        self.cfg, self.cfg_path = load_config()
        self.log_queue = queue.Queue()
        self.worker_thread = None
        self.worker_kind = None
        self.cancel_flag = threading.Event()
        self.login_wait = threading.Event()
        self.last_items = []
        self.last_output = None
        self.subjects = []
        self.continue_event = threading.Event()
        self.continue_choice = False
        self.url_info = {}

        self._build_ui()
        self._load_form_from_config()
        self._poll_log()
        self.protocol('WM_DELETE_WINDOW', self._on_close)

    def _build_ui(self):
        self.grid_columnconfigure(0, weight=1)
        self.grid_rowconfigure(1, weight=3)
        self.grid_rowconfigure(2, weight=2)

        header = ctk.CTkFrame(self, fg_color=COLOR_HEADER, corner_radius=0, height=56)
        header.grid(row=0, column=0, sticky='ew')
        header.grid_columnconfigure(1, weight=1)
        header.grid_propagate(False)

        title_wrap = ctk.CTkFrame(header, fg_color='transparent')
        title_wrap.grid(row=0, column=0, sticky='w', padx=20, pady=10)
        ctk.CTkLabel(
            title_wrap,
            text=APP_TITLE,
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=20, weight='bold'),
            text_color='white',
        ).pack(anchor='w')
        ctk.CTkLabel(
            title_wrap,
            text='SchoolManagement \u9898\u5e93\u7cfb\u7edf',
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=12),
            text_color=COLOR_HEADER_SUB,
        ).pack(anchor='w')

        header_right = ctk.CTkFrame(header, fg_color='transparent')
        header_right.grid(row=0, column=1, sticky='e', padx=20)

        self.login_btn = ctk.CTkButton(
            header_right, text='\u767b\u5f55\u7ec4\u5377\u7f51', height=32, width=108,
            fg_color='white', hover_color='#e2e8f0', text_color=COLOR_HEADER,
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=12, weight='bold'),
            command=self._start_zujuan_login,
        )
        self.login_btn.pack(side='right', padx=(8, 0))

        self.save_session_btn = ctk.CTkButton(
            header_right, text='\u4fdd\u5b58\u4f1a\u8bdd', height=32, width=92,
            fg_color='#14b8a6', hover_color=COLOR_PRIMARY_HOVER, text_color='white',
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=12, weight='bold'),
            command=self._save_zujuan_session, state='disabled',
        )
        self.save_session_btn.pack(side='right', padx=(8, 0))

        self.header_session = ctk.CTkLabel(
            header_right,
            text='',
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=12),
            text_color=COLOR_HEADER_SUB,
        )
        self.header_session.pack(side='right', padx=(0, 8))
        self.session_status = self.header_session

        body = ctk.CTkFrame(self, fg_color='transparent')
        body.grid(row=1, column=0, sticky='nsew', padx=16, pady=(12, 6))
        body.grid_columnconfigure(0, weight=2)
        body.grid_columnconfigure(1, weight=3)
        body.grid_rowconfigure(0, weight=1)
        body.grid_rowconfigure(1, weight=0)

        left_col = ctk.CTkFrame(body, fg_color='transparent')
        left_col.grid(row=0, column=0, sticky='nsew', padx=(0, 8))
        left_col.grid_columnconfigure(0, weight=1)

        right_col = ctk.CTkFrame(body, fg_color='transparent')
        right_col.grid(row=0, column=1, sticky='nsew', padx=(8, 0))
        right_col.grid_columnconfigure(0, weight=1)

        self._build_config_section(left_col)
        self._build_collect_section(right_col)

        import_wrap = ctk.CTkFrame(body, fg_color='transparent')
        import_wrap.grid(row=1, column=0, columnspan=2, sticky='ew', pady=(10, 0))
        import_wrap.grid_columnconfigure(0, weight=1)
        self._build_import_section(import_wrap)

        log_frame = ctk.CTkFrame(self, fg_color=COLOR_CARD, corner_radius=10, border_width=1, border_color=COLOR_CARD_BORDER)
        log_frame.grid(row=2, column=0, sticky='nsew', padx=16, pady=(6, 14))
        log_frame.grid_columnconfigure(0, weight=1)
        log_frame.grid_rowconfigure(2, weight=1)

        toolbar = ctk.CTkFrame(log_frame, fg_color='transparent')
        toolbar.grid(row=0, column=0, sticky='ew', padx=14, pady=(10, 4))
        toolbar.grid_columnconfigure(1, weight=1)

        ctk.CTkLabel(
            toolbar,
            text='\u8fd0\u884c\u65e5\u5fd7',
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=14, weight='bold'),
        ).grid(row=0, column=0, sticky='w')

        self.status_label = ctk.CTkLabel(
            toolbar, text='\u5c31\u7eea', text_color=COLOR_MUTED,
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=12),
        )
        self.status_label.grid(row=0, column=1, sticky='e', padx=8)

        self.cancel_btn = ctk.CTkButton(
            toolbar, text='\u53d6\u6d88\u4efb\u52a1', fg_color=COLOR_DANGER, hover_color=COLOR_DANGER_HOVER,
            command=self._cancel_task, state='disabled', width=96, height=30,
        )
        self.cancel_btn.grid(row=0, column=2, padx=(4, 0))
        ctk.CTkButton(
            toolbar, text='\u6e05\u7a7a\u65e5\u5fd7', width=96, height=30,
            fg_color='#e2e8f0', hover_color='#cbd5e1', text_color='#334155',
            command=self._clear_log,
        ).grid(row=0, column=3, padx=(8, 0))

        progress_row = ctk.CTkFrame(log_frame, fg_color='transparent')
        progress_row.grid(row=1, column=0, sticky='ew', padx=14, pady=(0, 6))
        progress_row.grid_columnconfigure(0, weight=1)
        ctk.CTkLabel(
            progress_row, text='\u8fdb\u5ea6', font=ctk.CTkFont(family='Microsoft YaHei UI', size=11),
            text_color=COLOR_MUTED,
        ).grid(row=0, column=0, sticky='w', pady=(0, 4))
        self.progress = ctk.CTkProgressBar(progress_row, height=8, progress_color=COLOR_PRIMARY)
        self.progress.grid(row=1, column=0, sticky='ew')
        self.progress.set(0)

        self.log_box = ctk.CTkTextbox(
            log_frame, height=180, font=ctk.CTkFont(family='Consolas', size=12),
            fg_color='#f8fafc', border_width=1, border_color=COLOR_CARD_BORDER,
        )
        self.log_box.grid(row=2, column=0, sticky='nsew', padx=14, pady=(0, 12))
        self.log_box.configure(state='disabled')

    def _section(self, parent, title, subtitle='', step=''):
        frame = ctk.CTkFrame(
            parent, fg_color=COLOR_CARD, corner_radius=10,
            border_width=1, border_color=COLOR_CARD_BORDER,
        )
        frame.grid(sticky='nsew', pady=(0, 0))
        frame.grid_columnconfigure(1, weight=1)

        head = ctk.CTkFrame(frame, fg_color='transparent')
        head.grid(row=0, column=0, columnspan=4, sticky='ew', padx=14, pady=(12, 10))
        head.grid_columnconfigure(1, weight=1)

        if step:
            step_badge = ctk.CTkLabel(
                head, text=step, width=28, height=28, corner_radius=14,
                fg_color=COLOR_PRIMARY, text_color='white',
                font=ctk.CTkFont(family='Microsoft YaHei UI', size=13, weight='bold'),
            )
            step_badge.grid(row=0, column=0, rowspan=2, sticky='nw', padx=(0, 10))

        title_col = 1 if step else 0
        ctk.CTkLabel(
            head, text=title, font=ctk.CTkFont(family='Microsoft YaHei UI', size=15, weight='bold'),
        ).grid(row=0, column=title_col, sticky='w')
        if subtitle:
            ctk.CTkLabel(
                head, text=subtitle, font=ctk.CTkFont(family='Microsoft YaHei UI', size=11),
                text_color=COLOR_MUTED, wraplength=360, justify='left',
            ).grid(row=1, column=title_col, sticky='w', pady=(2, 0))

        ctk.CTkFrame(frame, height=1, fg_color=COLOR_CARD_BORDER).grid(
            row=1, column=0, columnspan=4, sticky='ew', padx=14, pady=(0, 4),
        )
        return frame

    def _label(self, parent, text, row, column=0):
        ctk.CTkLabel(
            parent, text=text, anchor='e', width=88,
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=13),
            text_color='#475569',
        ).grid(row=row, column=column, sticky='e', padx=(14, 8), pady=6)

    def _build_config_section(self, parent):
        sec = self._section(
            parent, '\u7cfb\u7edf\u914d\u7f6e',
            subtitle='\u8fde\u63a5\u672c\u7cfb\u7edf API\u4e0e\u5bfc\u5165\u76ee\u6807\u5b66\u79d1',
            step='1',
        )
        r = 2
        self._label(sec, 'API \u5730\u5740', r)
        self.api_base = ctk.CTkEntry(sec, placeholder_text='http://localhost:8080', height=34)
        self.api_base.grid(row=r, column=1, columnspan=3, sticky='ew', padx=(0, 14), pady=6)

        r += 1
        self._label(sec, '\u7528\u6237\u540d', r)
        self.username = ctk.CTkEntry(sec, height=34)
        self.username.grid(row=r, column=1, sticky='ew', padx=(0, 8), pady=6)
        self._label(sec, '\u5bc6\u7801', r, column=2)
        self.password = ctk.CTkEntry(sec, show='*', height=34)
        self.password.grid(row=r, column=3, sticky='ew', padx=(0, 14), pady=6)
        sec.grid_columnconfigure(1, weight=1)
        sec.grid_columnconfigure(3, weight=1)

        r += 1
        self._label(sec, 'Redis CLI', r)
        self.redis_cli = ctk.CTkEntry(sec, placeholder_text=r'D:\Tools\Redis\redis-cli.exe', height=34)
        self.redis_cli.grid(row=r, column=1, columnspan=2, sticky='ew', padx=(0, 8), pady=6)
        ctk.CTkButton(
            sec, text='\u6d4b\u8bd5\u8fde\u63a5', width=100, height=34,
            fg_color=COLOR_PRIMARY, hover_color=COLOR_PRIMARY_HOVER,
            command=self._test_connection,
        ).grid(row=r, column=3, sticky='e', padx=(0, 14), pady=6)

        r += 1
        self._label(sec, '\u5b66\u79d1', r)
        self.subject_var = tk.StringVar()
        self.subject_menu = ctk.CTkOptionMenu(sec, variable=self.subject_var, values=['\u52a0\u8f7d\u4e2d...'], height=34)
        self.subject_menu.grid(row=r, column=1, sticky='ew', padx=(0, 8), pady=6)
        ctk.CTkButton(
            sec, text='\u5237\u65b0', width=72, height=34,
            fg_color='#e2e8f0', hover_color='#cbd5e1', text_color='#334155',
            command=self._refresh_subjects,
        ).grid(row=r, column=2, columnspan=2, sticky='w', padx=(0, 14), pady=6)

        r += 1
        self._label(sec, '\u6559\u6750', r)
        self.textbook_var = tk.StringVar(value='\u81ea\u52a8\u5339\u914d')
        self.textbook_menu = ctk.CTkOptionMenu(
            sec, variable=self.textbook_var, values=['\u81ea\u52a8\u5339\u914d'], height=34,
        )
        self.textbook_menu.grid(row=r, column=1, columnspan=2, sticky='ew', padx=(0, 8), pady=6)
        ctk.CTkButton(
            sec, text='\u5237\u65b0', width=72, height=34,
            fg_color='#e2e8f0', hover_color='#cbd5e1', text_color='#334155',
            command=self._refresh_textbooks,
        ).grid(row=r, column=3, sticky='e', padx=(0, 14), pady=6)
        self.textbooks = []

        r += 1
        self._label(sec, '\u7ae0\u8282\u6587\u672c', r)
        self.chapter_text = ctk.CTkEntry(sec, height=34)
        self.chapter_text.grid(row=r, column=1, columnspan=3, sticky='ew', padx=(0, 14), pady=6)

        r += 1
        btn_row = ctk.CTkFrame(sec, fg_color='transparent')
        btn_row.grid(row=r, column=0, columnspan=4, sticky='ew', padx=14, pady=(8, 14))
        ctk.CTkButton(
            btn_row, text='\u4fdd\u5b58\u914d\u7f6e', height=34,
            fg_color=COLOR_PRIMARY, hover_color=COLOR_PRIMARY_HOVER,
            command=self._save_config,
        ).pack(side='left')
        ctk.CTkLabel(
            btn_row, text=str(user_config_path()), text_color=COLOR_MUTED,
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=11),
        ).pack(side='left', padx=12)

        parent.grid_columnconfigure(0, weight=1)
        sec.grid(row=0, column=0, sticky='nsew')

    def _build_collect_section(self, parent):
        sec = self._section(
            parent, '\u7ec4\u5377\u7f51\u91c7\u96c6',
            subtitle='\u8f93\u5165\u7ae0\u8282\u5217\u8868\u5730\u5740\uff0c\u767b\u5f55\u540e\u5f00\u59cb\u91c7\u96c6',
            step='2',
        )
        r = 2
        self._label(sec, '\u91c7\u96c6\u5730\u5740', r)
        self.collect_url = ctk.CTkEntry(sec, height=34)
        self.collect_url.grid(row=r, column=1, columnspan=3, sticky='ew', padx=(0, 14), pady=6)
        self.collect_url.bind('<KeyRelease>', self._on_url_changed)

        r += 1
        url_hint_frame = ctk.CTkFrame(sec, fg_color='#f0fdfa', corner_radius=8, border_width=1, border_color='#99f6e4')
        url_hint_frame.grid(row=r, column=1, columnspan=3, sticky='ew', padx=(0, 14), pady=(0, 8))
        self.url_info_label = ctk.CTkLabel(
            url_hint_frame,
            text='\u8bf7\u8f93\u5165\u7ec4\u5377\u7f51\u7ae0\u8282\u5217\u8868\u5730\u5740',
            anchor='w',
            text_color=COLOR_MUTED,
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=12),
            wraplength=480,
            justify='left',
        )
        self.url_info_label.pack(fill='x', padx=12, pady=8)

        r += 1
        params = ctk.CTkFrame(sec, fg_color='#f8fafc', corner_radius=8)
        params.grid(row=r, column=0, columnspan=4, sticky='ew', padx=14, pady=(0, 8))
        params.grid_columnconfigure(1, weight=1)
        params.grid_columnconfigure(3, weight=1)

        ctk.CTkLabel(
            params, text='\u91c7\u96c6\u9875\u6570', font=ctk.CTkFont(family='Microsoft YaHei UI', size=12),
            text_color='#475569',
        ).grid(row=0, column=0, sticky='e', padx=(12, 8), pady=10)
        self.pages = ctk.CTkEntry(params, width=72, height=32)
        self.pages.insert(0, '1')
        self.pages.grid(row=0, column=1, sticky='w', pady=10)
        ctk.CTkLabel(
            params, text='\u95f4\u9694(\u79d2)', font=ctk.CTkFont(family='Microsoft YaHei UI', size=12),
            text_color='#475569',
        ).grid(row=0, column=2, sticky='e', padx=(16, 8), pady=10)
        self.delay = ctk.CTkEntry(params, width=72, height=32)
        self.delay.insert(0, '0.8')
        self.delay.grid(row=0, column=3, sticky='w', padx=(0, 12), pady=10)

        r += 1
        opts = ctk.CTkFrame(sec, fg_color='transparent')
        opts.grid(row=r, column=0, columnspan=4, sticky='ew', padx=14, pady=4)
        opts.grid_columnconfigure(0, weight=1)
        opts.grid_columnconfigure(1, weight=1)

        self.step_by_page = ctk.CTkCheckBox(
            opts,
            text='\u9010\u9875\u91c7\u96c6',
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=12),
            command=self._on_step_mode_changed,
        )
        self.step_by_page.select()
        self.step_by_page.grid(row=0, column=0, sticky='w', pady=4)
        self.fetch_answer = ctk.CTkCheckBox(
            opts, text='\u83b7\u53d6\u7b54\u6848\u89e3\u6790',
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=12),
        )
        self.fetch_answer.grid(row=0, column=1, sticky='w', pady=4)
        self.fetch_detail = ctk.CTkCheckBox(
            opts, text='\u8bbf\u95ee\u8be6\u60c5\u9875',
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=12),
        )
        self.fetch_detail.select()
        self.fetch_detail.grid(row=1, column=0, sticky='w', pady=4)
        self.headless = ctk.CTkCheckBox(
            opts, text='\u65e0\u5934\u6a21\u5f0f\u91c7\u96c6',
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=12),
        )
        self.headless.select()
        self.headless.grid(row=1, column=1, sticky='w', pady=4)

        r += 1
        btn_row = ctk.CTkFrame(sec, fg_color='transparent')
        btn_row.grid(row=r, column=0, columnspan=4, sticky='ew', padx=14, pady=(8, 14))

        ctk.CTkLabel(
            btn_row,
            text='\u83b7\u53d6\u7b54\u6848\u524d\u8bf7\u5728\u9876\u90e8\u300c\u767b\u5f55\u7ec4\u5377\u7f51\u300d\u2192\u300c\u4fdd\u5b58\u4f1a\u8bdd\u300d',
            text_color=COLOR_MUTED,
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=11),
        ).pack(side='left')

        ctk.CTkButton(
            btn_row, text='\u5f00\u59cb\u91c7\u96c6', height=38, width=120,
            fg_color=COLOR_ACCENT, hover_color=COLOR_ACCENT_HOVER,
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=14, weight='bold'),
            command=self._start_collect,
        ).pack(side='right')

        sec.grid(row=0, column=0, sticky='nsew')

    def _build_import_section(self, parent):
        sec = self._section(
            parent, '\u5bfc\u5165\u9898\u5e93',
            subtitle='\u5c06\u91c7\u96c6\u7684 JSON \u5bfc\u5165\u672c\u7cfb\u7edf\u9898\u5e93',
            step='3',
        )
        r = 2
        self._label(sec, 'JSON \u6587\u4ef6', r)
        self.import_file = ctk.CTkEntry(sec, height=34)
        self.import_file.grid(row=r, column=1, columnspan=2, sticky='ew', padx=(0, 8), pady=6)
        ctk.CTkButton(
            sec, text='\u6d4f\u89c8...', width=80, height=34,
            fg_color='#e2e8f0', hover_color='#cbd5e1', text_color='#334155',
            command=self._pick_file,
        ).grid(row=r, column=3, sticky='e', padx=(0, 14), pady=6)

        r += 1
        btn_row = ctk.CTkFrame(sec, fg_color='transparent')
        btn_row.grid(row=r, column=0, columnspan=4, sticky='ew', padx=14, pady=(8, 14))

        ctk.CTkButton(
            btn_row, text='\u5bfc\u5165 JSON', height=36,
            fg_color='#e2e8f0', hover_color='#cbd5e1', text_color='#334155',
            command=self._start_import,
        ).pack(side='left')
        ctk.CTkButton(
            btn_row, text='\u91c7\u96c6\u5e76\u5bfc\u5165', height=38, width=130,
            fg_color=COLOR_PRIMARY, hover_color=COLOR_PRIMARY_HOVER,
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=14, weight='bold'),
            command=self._start_run,
        ).pack(side='left', padx=10)

        result_wrap = ctk.CTkFrame(btn_row, fg_color='#f0fdf4', corner_radius=8)
        result_wrap.pack(side='left', fill='x', expand=True, padx=(8, 0))
        self.result_label = ctk.CTkLabel(
            result_wrap, text='\u5c1a\u672a\u6267\u884c\u4efb\u52a1', text_color=COLOR_MUTED,
            font=ctk.CTkFont(family='Microsoft YaHei UI', size=12), anchor='w',
        )
        self.result_label.pack(fill='x', padx=12, pady=8)

        sec.grid(row=0, column=0, sticky='ew')

    def _load_form_from_config(self):
        c = self.cfg
        self._set_entry(self.api_base, c.get('api_base', ''))
        self._set_entry(self.username, c.get('username', ''))
        self._set_entry(self.password, c.get('password', ''))
        self._set_entry(self.redis_cli, c.get('redis_cli', ''))
        self._set_entry(self.chapter_text, c.get('chapter_text', ''))
        self._set_entry(self.collect_url, DEFAULT_URL)
        if c.get('fetch_answer') or session_exists():
            self.fetch_answer.select()
        if c.get('fetch_detail', True):
            self.fetch_detail.select()
        if c.get('headless', True):
            self.headless.select()
        else:
            self.headless.deselect()
        self._update_session_status()
        self.after(300, self._refresh_subjects)
        self.after(400, self._on_url_changed)
        self.after(500, self._on_step_mode_changed)

    def _on_step_mode_changed(self):
        if self.step_by_page.get():
            self.pages.configure(state='disabled')
        else:
            self.pages.configure(state='normal')

    def _on_url_changed(self, event=None):
        info = parse_zujuan_url(self.collect_url.get())
        self.url_info = info
        if info.get('valid'):
            color = COLOR_SUCCESS if info.get('kind') == 'list' else '#d97706'
        else:
            color = COLOR_MUTED
        self.url_info_label.configure(text=info.get('summary') or '\u8bf7\u8f93\u5165\u7ec4\u5377\u7f51\u7ae0\u8282\u5217\u8868\u5730\u5740', text_color=color)
        if info.get('kind') == 'list':
            self._sync_subject_from_url(info)

    def _sync_subject_from_url(self, info):
        if not self.subjects:
            return
        short = short_subject_name(info.get('subject_label'), info.get('subject_code'))
        if not short:
            return
        for s in self.subjects:
            name = (s.get('subjectName') or '').strip()
            if name == short or short in name or name in short:
                label = f"{name} (ID:{s['subjectId']})"
                self.subject_var.set(label)
                self.cfg['subject_id'] = s['subjectId']
                if info.get('subject_code', '').startswith('cz'):
                    self.cfg['school_stage'] = '\u521d\u4e2d'
                else:
                    self.cfg['school_stage'] = '\u9ad8\u4e2d'
                break

    def _ask_continue_page(self, page_num, page_count, total_count, pager):
        if self.cancel_flag.is_set():
            return False

        def ask():
            lines = [
                f'\u7b2c {page_num} \u9875\u91c7\u96c6\u5b8c\u6210',
                f'\u672c\u9875 {page_count} \u9898\uff0c\u7d2f\u8ba1 {total_count} \u9898',
            ]
            if pager.get('total_pages'):
                lines.append(f'\u5171\u7ea6 {pager["total_pages"]} \u9875')
            if pager.get('has_next'):
                nxt = pager.get('next_page') or (page_num + 1)
                lines.append(f'\u662f\u5426\u7ee7\u7eed\u91c7\u96c6\u7b2c {nxt} \u9875\uff1f')
            else:
                lines.append('\u5df2\u5230\u6700\u540e\u4e00\u9875')
            if pager.get('has_next'):
                self.continue_choice = messagebox.askyesno(APP_TITLE, '\n'.join(lines))
            else:
                messagebox.showinfo(APP_TITLE, '\n'.join(lines))
                self.continue_choice = False
            self.continue_event.set()

        self.continue_event.clear()
        self.after(0, ask)
        while not self.continue_event.wait(timeout=0.2):
            if self.cancel_flag.is_set():
                return False
        return self.continue_choice and pager.get('has_next')

    def _resolve_collect_target(self):
        info = parse_zujuan_url(self.collect_url.get().strip())
        if not info.get('valid'):
            raise ValueError(info.get('summary') or '\u91c7\u96c6\u5730\u5740\u65e0\u6548')
        if info.get('kind') != 'list':
            raise ValueError(info.get('summary') or '\u8bf7\u4f7f\u7528\u7ae0\u8282\u5217\u8868\u5730\u5740')
        return info

    def _run_collect(self, cfg, on_progress=None):
        info = self._resolve_collect_target()
        base_url = info['base_url']
        current_page = info['page_num']
        delay = float(self.delay.get().strip() or '0.8')
        fetch = bool(self.fetch_answer.get())
        detail = bool(self.fetch_detail.get())
        headless = bool(self.headless.get())
        step_mode = bool(self.step_by_page.get())
        pages = 1 if step_mode else max(1, int(self.pages.get().strip() or '1'))

        all_items = []
        seen = set()
        pager_holder = {}

        def on_page_done(page_num, page_added, pager, total):
            pager_holder['pager'] = pager
            return None

        while True:
            if self.cancel_flag.is_set():
                break
            self._append_log(
                f'[\u5f00\u59cb] \u7b2c {current_page} \u9875 '
                f'{list_page_url(base_url, current_page)}'
            )
            batch = collect_chapter(
                base_url,
                pages=pages,
                start_page=current_page,
                fetch_answer=fetch,
                fetch_detail=detail,
                delay=delay,
                headless=headless,
                ocr_base=cfg.get('ocr_base'),
                prefer_analysis_image=cfg.get('prefer_analysis_image', False),
                cfg=cfg,
                on_log=self._append_log,
                should_cancel=self.cancel_flag.is_set,
                on_progress=on_progress,
                on_page_done=on_page_done,
            )
            for item in batch:
                zid = item.get('zujuan_id')
                if zid and zid not in seen:
                    seen.add(zid)
                    all_items.append(item)

            pager = pager_holder.get('pager') or {}
            self._append_log(
                f'\u7b2c {current_page} \u9875\u5b8c\u6210: \u672c\u9875 {len(batch)} \u9898, '
                f'\u7d2f\u8ba1 {len(all_items)} \u9898'
            )

            if not step_mode:
                break
            if not pager.get('has_next'):
                self._append_log('\u5df2\u5230\u6700\u540e\u4e00\u9875')
                break
            if not self._ask_continue_page(current_page, len(batch), len(all_items), pager):
                self._append_log('\u7528\u6237\u7ed3\u675f\u9010\u9875\u91c7\u96c6')
                break
            current_page = pager.get('next_page') or (current_page + 1)
            next_url = list_page_url(base_url, current_page)
            self.after(0, lambda u=next_url: self._set_entry(self.collect_url, u))
            self.after(0, self._on_url_changed)
            pages = 1

        return all_items

    def _set_entry(self, entry, value):
        entry.delete(0, 'end')
        if value is not None:
            entry.insert(0, str(value))

    def _form_to_config(self):
        sid = self._selected_subject_id()
        info = getattr(self, 'url_info', None) or parse_zujuan_url(self.collect_url.get())
        return {
            'api_base': self.api_base.get().strip(),
            'username': self.username.get().strip(),
            'password': self.password.get(),
            'redis_cli': self.redis_cli.get().strip() or None,
            'subject_id': sid,
            'subject_code': info.get('subject_code') if info else self.cfg.get('subject_code'),
            'subject_label': info.get('subject_label') if info else self.cfg.get('subject_label'),
            'textbook_id': self._selected_textbook_id(),
            'chapter_id': self.cfg.get('chapter_id'),
            'chapter_text': self.chapter_text.get().strip() or '\u7ec4\u5377\u7f51\u91c7\u96c6',
            'fetch_answer': bool(self.fetch_answer.get()),
            'fetch_detail': bool(self.fetch_detail.get()),
            'ocr_base': self.cfg.get('ocr_base', 'http://127.0.0.1:8867'),
            'import_delay': self.cfg.get('import_delay', 0.35),
            'headless': bool(self.headless.get()),
            'version_id': self.cfg.get('version_id'),
            'school_stage': self.cfg.get('school_stage', '\u9ad8\u4e2d'),
            'auto_create_chapters': self.cfg.get('auto_create_chapters', True),
            'auto_create_catalog': self.cfg.get('auto_create_catalog', True),
            'auto_create_subjects': self.cfg.get('auto_create_subjects', False),
            'default_version_name': self.cfg.get('default_version_name', '\u7ec4\u5377\u7f51\u540c\u6b65'),
            'default_textbook_name': self.cfg.get('default_textbook_name', '\u7ec4\u5377\u7f51\u7efc\u5408\u5e93'),
        }

    def _selected_textbook_id(self):
        label = self.textbook_var.get()
        if label == '\u81ea\u52a8\u5339\u914d':
            return None
        for tb in self.textbooks:
            if f"{tb['textbookName']} (ID:{tb['textbookId']})" == label:
                return tb['textbookId']
        return self.cfg.get('textbook_id')

    def _selected_subject_id(self):
        label = self.subject_var.get()
        for s in self.subjects:
            if f"{s['subjectName']} (ID:{s['subjectId']})" == label:
                return s['subjectId']
        return self.cfg.get('subject_id')

    def _save_config(self):
        self.cfg = self._form_to_config()
        path = save_config(self.cfg, self.cfg_path)
        self.cfg_path = path
        self._append_log(f'\u914d\u7f6e\u5df2\u4fdd\u5b58: {path}')
        messagebox.showinfo(APP_TITLE, f'\u914d\u7f6e\u5df2\u4fdd\u5b58\n{path}')

    def _update_session_status(self):
        if session_exists():
            text = '\u2713 \u5df2\u4fdd\u5b58\u7ec4\u5377\u7f51\u4f1a\u8bdd'
            color = COLOR_HEADER_SUB
        else:
            text = '\u25cb \u672a\u767b\u5f55\u7ec4\u5377\u7f51'
            color = '#94a3b8'
        self.session_status.configure(text=text, text_color=color)

    def _append_log(self, msg):
        self.log_queue.put(msg)

    def _poll_log(self):
        try:
            while True:
                msg = self.log_queue.get_nowait()
                self.log_box.configure(state='normal')
                self.log_box.insert('end', msg + '\n')
                self.log_box.see('end')
                self.log_box.configure(state='disabled')
        except queue.Empty:
            pass
        self.after(120, self._poll_log)

    def _clear_log(self):
        self.log_box.configure(state='normal')
        self.log_box.delete('1.0', 'end')
        self.log_box.configure(state='disabled')

    def _set_busy(self, busy, status=''):
        state = 'disabled' if busy else 'normal'
        self.cancel_btn.configure(state='normal' if busy else 'disabled')
        if status:
            self.status_label.configure(text=status)
        elif not busy:
            self.status_label.configure(text='\u5c31\u7eea')
            self.progress.set(0)

    def _cancel_task(self):
        self.cancel_flag.set()
        self.login_wait.set()
        self.continue_choice = False
        self.continue_event.set()
        self._append_log('\u6b63\u5728\u53d6\u6d88...')

    def _run_worker(self, target, status, kind='generic'):
        if self.worker_thread and self.worker_thread.is_alive():
            messagebox.showwarning(APP_TITLE, '\u5f53\u524d\u6709\u4efb\u52a1\u6b63\u5728\u8fd0\u884c')
            return
        self.cancel_flag.clear()
        self.worker_kind = kind
        self._set_busy(True, status)
        self.worker_thread = threading.Thread(target=target, daemon=True)
        self.worker_thread.start()

    def _worker_done(self, on_ok=None):
        def _finish():
            self.worker_kind = None
            self.worker_thread = None
            self._set_busy(False)
            self._update_session_status()
            if on_ok:
                on_ok()
        self.after(0, _finish)

    def _test_connection(self):
        def task():
            try:
                cfg = self._form_to_config()
                client = ApiClient(cfg['api_base'], cfg['username'], cfg['password'], cfg.get('redis_cli'))
                client.test_connection()
                self._append_log('\u7cfb\u7edf\u8fde\u63a5\u6210\u529f')
                self.after(0, lambda: messagebox.showinfo(APP_TITLE, '\u8fde\u63a5\u6210\u529f'))
            except Exception as ex:
                self._append_log(f'\u8fde\u63a5\u5931\u8d25: {ex}')
                self.after(0, lambda: messagebox.showerror(APP_TITLE, str(ex)))
            finally:
                self._worker_done()
        self._run_worker(task, '\u6d4b\u8bd5\u8fde\u63a5...')

    def _refresh_subjects(self):
        def task():
            try:
                cfg = self._form_to_config()
                client = ApiClient(cfg['api_base'], cfg['username'], cfg['password'], cfg.get('redis_cli'))
                client.login()
                subjects = client.fetch_subjects()
                self.after(0, lambda: self._apply_subjects(subjects))
                self._append_log(f'\u5df2\u52a0\u8f7d {len(subjects)} \u4e2a\u5b66\u79d1')
            except Exception as ex:
                self._append_log(f'\u52a0\u8f7d\u5b66\u79d1\u5931\u8d25: {ex}')
            finally:
                self._worker_done()
        self._run_worker(task, '\u52a0\u8f7d\u5b66\u79d1...')

    def _apply_subjects(self, subjects):
        self.subjects = subjects
        labels = [f"{s['subjectName']} (ID:{s['subjectId']})" for s in subjects]
        if not labels:
            labels = ['\u65e0\u5b66\u79d1\u6570\u636e']
        self.subject_menu.configure(values=labels)
        target = self.cfg.get('subject_id')
        picked = labels[0]
        for s in subjects:
            if s['subjectId'] == target:
                picked = f"{s['subjectName']} (ID:{s['subjectId']})"
                break
        self.subject_var.set(picked)
        info = getattr(self, 'url_info', None)
        if info and info.get('kind') == 'list':
            self._sync_subject_from_url(info)
        self._refresh_textbooks()

    def _refresh_textbooks(self):
        def task():
            try:
                cfg = self._form_to_config()
                sid = cfg.get('subject_id')
                if not sid:
                    return
                client = ApiClient(cfg['api_base'], cfg['username'], cfg['password'], cfg.get('redis_cli'))
                client.login()
                versions = client.fetch_textbook_versions(sid, cfg.get('school_stage') or '\u9ad8\u4e2d')
                if not versions:
                    return
                version_id = cfg.get('version_id') or versions[0].get('versionId')
                books = client.fetch_textbooks(version_id)
                self.after(0, lambda: self._apply_textbooks(books))
                self._append_log(f'\u5df2\u52a0\u8f7d {len(books)} \u518c\u6559\u6750')
            except Exception as ex:
                self._append_log(f'\u52a0\u8f7d\u6559\u6750\u5931\u8d25: {ex}')
            finally:
                self._worker_done()
        if self.worker_thread and self.worker_thread.is_alive():
            return
        self._run_worker(task, '\u52a0\u8f7d\u6559\u6750...')

    def _apply_textbooks(self, textbooks):
        self.textbooks = textbooks
        labels = ['\u81ea\u52a8\u5339\u914d'] + [
            f"{tb['textbookName']} (ID:{tb['textbookId']})" for tb in textbooks
        ]
        self.textbook_menu.configure(values=labels)
        target = self.cfg.get('textbook_id')
        picked = '\u81ea\u52a8\u5339\u914d'
        if target:
            for tb in textbooks:
                if tb['textbookId'] == target:
                    picked = f"{tb['textbookName']} (ID:{tb['textbookId']})"
                    break
        self.textbook_var.set(picked)

    def _start_zujuan_login(self):
        if self.worker_thread and self.worker_thread.is_alive():
            if self.worker_kind == 'login':
                messagebox.showinfo(
                    APP_TITLE,
                    '\u767b\u5f55\u6d4f\u89c8\u5668\u5df2\u6253\u5f00\u3002\n'
                    '\u8bf7\u5728\u6d4f\u89c8\u5668\u4e2d\u5b8c\u6210\u767b\u5f55\uff0c\u7136\u540e\u70b9\u300c\u4fdd\u5b58\u4f1a\u8bdd\u300d\u3002\n\n'
                    '\u5982\u9700\u91cd\u65b0\u6253\u5f00\u6d4f\u89c8\u5668\uff0c\u8bf7\u5148\u70b9\u300c\u53d6\u6d88\u4efb\u52a1\u300d\u3002',
                )
                return
            messagebox.showwarning(
                APP_TITLE,
                '\u5f53\u524d\u6709\u4efb\u52a1\u6b63\u5728\u8fd0\u884c\uff0c\u8bf7\u5148\u53d6\u6d88\u6216\u7b49\u5f85\u5b8c\u6210',
            )
            return

        self.login_wait.clear()
        self.after(0, lambda: self.save_session_btn.configure(state='normal'))
        self._append_log('\u6b63\u5728\u6253\u5f00\u6d4f\u89c8\u5668\uff0c\u767b\u5f55\u540e\u70b9\u300c\u4fdd\u5b58\u4f1a\u8bdd\u300d')

        def task():
            try:
                login_interactive(
                    headless=False,
                    on_log=self._append_log,
                    wait_event=self.login_wait,
                )
            except Exception as ex:
                self._append_log(f'\u767b\u5f55\u5931\u8d25: {ex}')
            finally:
                self.after(0, lambda: self.save_session_btn.configure(state='disabled'))
                self._worker_done()
        self._run_worker(task, '\u7ec4\u5377\u7f51\u767b\u5f55...', kind='login')

    def _save_zujuan_session(self):
        self.login_wait.set()
        self._append_log('\u6b63\u5728\u4fdd\u5b58\u4f1a\u8bdd...')

    def _start_collect(self):
        def task():
            try:
                cfg = self._form_to_config()

                def on_progress(count, page_num, total_pages):
                    pct = page_num / max(total_pages, 1)
                    self.after(0, lambda: self.progress.set(pct))

                items = self._run_collect(cfg, on_progress=on_progress)
                items = [finalize_item(i) for i in items]
                out = data_dir() / 'last_collect.json'
                out.write_text(json.dumps(items, ensure_ascii=False, indent=2), encoding='utf-8')
                self.last_items = items
                self.last_output = out
                self.after(0, lambda: self._set_entry(self.import_file, str(out)))
                self._append_log(f'\u91c7\u96c6\u5b8c\u6210: {len(items)} \u9053\u9898 -> {out}')
                self.after(0, lambda: self.result_label.configure(
                    text=f'\u6700\u8fd1\u91c7\u96c6 {len(items)} \u9898',
                ))
            except Exception as ex:
                self._append_log(f'\u91c7\u96c6\u5931\u8d25: {ex}')
                self.after(0, lambda: messagebox.showerror(APP_TITLE, str(ex)))
            finally:
                self._worker_done(lambda: self.progress.set(1 if not self.cancel_flag.is_set() else 0))
        self._run_worker(task, '\u91c7\u96c6\u4e2d...')

    def _pick_file(self):
        path = filedialog.askopenfilename(
            title='\u9009\u62e9\u91c7\u96c6 JSON',
            filetypes=[('JSON', '*.json'), ('All', '*.*')],
            initialdir=str(data_dir()),
        )
        if path:
            self._set_entry(self.import_file, path)

    def _start_import(self):
        path = self.import_file.get().strip()
        if not path:
            messagebox.showwarning(APP_TITLE, '\u8bf7\u9009\u62e9 JSON \u6587\u4ef6')
            return

        def task():
            try:
                cfg = self._form_to_config()
                items = json.loads(Path(path).read_text(encoding='utf-8'))
                result = import_questions(
                    items, cfg, on_log=self._append_log, should_cancel=self.cancel_flag.is_set,
                )
                msg = f"\u5bfc\u5165\u5b8c\u6210: \u6210\u529f {result['success']}, \u5931\u8d25 {result['failed']}"
                self._append_log(msg)
                self.after(0, lambda: self.result_label.configure(text=msg))
            except Exception as ex:
                self._append_log(f'\u5bfc\u5165\u5931\u8d25: {ex}')
                self.after(0, lambda: messagebox.showerror(APP_TITLE, str(ex)))
            finally:
                self._worker_done()
        self._run_worker(task, '\u5bfc\u5165\u4e2d...')

    def _start_run(self):
        def task():
            try:
                cfg = self._form_to_config()
                items = self._run_collect(cfg)
                items = [finalize_item(i) for i in items]
                out = data_dir() / 'last_collect.json'
                out.write_text(json.dumps(items, ensure_ascii=False, indent=2), encoding='utf-8')
                self.last_items = items
                self.after(0, lambda: self._set_entry(self.import_file, str(out)))
                self._append_log(f'\u91c7\u96c6 {len(items)} \u9898\uff0c\u5f00\u59cb\u5bfc\u5165...')
                result = import_questions(
                    items, cfg, on_log=self._append_log, should_cancel=self.cancel_flag.is_set,
                )
                msg = f"\u5b8c\u6210: \u91c7\u96c6 {len(items)}, \u6210\u529f {result['success']}, \u5931\u8d25 {result['failed']}"
                self._append_log(msg)
                self.after(0, lambda: self.result_label.configure(text=msg))
            except Exception as ex:
                self._append_log(f'\u4efb\u52a1\u5931\u8d25: {ex}')
                self.after(0, lambda: messagebox.showerror(APP_TITLE, str(ex)))
            finally:
                self._worker_done()
        self._run_worker(task, '\u91c7\u96c6\u5e76\u5bfc\u5165...')

    def _on_close(self):
        if self.worker_thread and self.worker_thread.is_alive():
            if not messagebox.askyesno(APP_TITLE, '\u4efb\u52a1\u8fd0\u884c\u4e2d\uff0c\u786e\u5b9a\u9000\u51fa\uff1f'):
                return
            self.cancel_flag.set()
            self.login_wait.set()
        self.destroy()


def main():
    app = ZujuanDesktopApp()
    app.mainloop()


if __name__ == '__main__':
    main()
