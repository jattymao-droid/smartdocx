#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
\u7ec4\u5377\u7f51\u9898\u5e93\u91c7\u96c6\u5de5\u5177

\u793a\u4f8b:
  python collector.py login
  python collector.py collect --url https://zujuan.xkw.com/gzwl/zj136248/ --pages 2 --fetch-answer -o data/out.json
  python collector.py import --file data/out.json --config config.json
  python collector.py run --url https://zujuan.xkw.com/gzwl/zj136248/ --pages 1 --config config.json
"""

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
if str(ROOT.parent) not in sys.path:
    sys.path.insert(0, str(ROOT.parent))

from zujuan_collector.scraper import collect_chapter, login_interactive
from zujuan_collector.importer import import_questions
from zujuan_collector.config_store import load_config as load_config_file
from zujuan_collector.item_validator import finalize_item


def load_config(path):
    p = Path(path) if path else None
    if p and p.exists():
        return json.loads(p.read_text(encoding='utf-8'))
    cfg, _ = load_config_file(p)
    return cfg


def cmd_login(args):
    login_interactive(headless=args.headless)


def cmd_collect(args):
    items = collect_chapter(
        args.url,
        pages=args.pages,
        fetch_answer=args.fetch_answer,
        fetch_detail=args.fetch_detail,
        delay=args.delay,
        headless=args.headless,
    )
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    items = [finalize_item(i) for i in items]
    out.write_text(json.dumps(items, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'\n\u5171\u91c7\u96c6 {len(items)} \u9053\u9898 -> {out}')


def cmd_import(args):
    cfg = load_config(args.config)
    items = json.loads(Path(args.file).read_text(encoding='utf-8'))
    result = import_questions(items, cfg)
    print(f'\n\u5bfc\u5165\u5b8c\u6210: \u6210\u529f {result["success"]}, \u5931\u8d25 {result["failed"]}')
    if result['errors']:
        print('\u5931\u8d25\u8be6\u60c5:')
        for e in result['errors'][:20]:
            print(' -', e)


def cmd_run(args):
    cfg = load_config(args.config)
    items = collect_chapter(
        args.url,
        pages=args.pages,
        fetch_answer=args.fetch_answer or cfg.get('fetch_answer', False),
        fetch_detail=cfg.get('fetch_detail', True),
        delay=args.delay,
        headless=args.headless if args.headless is not None else cfg.get('headless', True),
    )
    out = Path(args.output) if args.output else ROOT / 'data' / 'last_collect.json'
    out.parent.mkdir(parents=True, exist_ok=True)
    items = [finalize_item(i) for i in items]
    out.write_text(json.dumps(items, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'\n\u91c7\u96c6 {len(items)} \u9053\u9898\uff0c\u5f00\u59cb\u5bfc\u5165...')
    result = import_questions(items, cfg)
    print(f'\u5b8c\u6210: \u91c7\u96c6 {len(items)}, \u6210\u529f {result["success"]}, \u5931\u8d25 {result["failed"]}')


def main():
    parser = argparse.ArgumentParser(description='\u7ec4\u5377\u7f51\u9898\u5e93\u91c7\u96c6\u5de5\u5177')
    sub = parser.add_subparsers(dest='command', required=True)

    p_login = sub.add_parser('login', help='\u6253\u5f00\u6d4f\u89c8\u5668\u767b\u5f55\u7ec4\u5377\u7f51\u5e76\u4fdd\u5b58\u4f1a\u8bdd')
    p_login.add_argument('--headed', dest='headless', action='store_false', help='\u663e\u793a\u6d4f\u89c8\u5668')
    p_login.set_defaults(headless=False, func=cmd_login)

    p_collect = sub.add_parser('collect', help='\u91c7\u96c6\u9898\u76ee\u5230 JSON')
    p_collect.add_argument('--url', required=True, help='\u7ae0\u8282\u5217\u8868\u9875\u5730\u5740')
    p_collect.add_argument('--pages', type=int, default=1, help='\u91c7\u96c6\u9875\u6570')
    p_collect.add_argument('--fetch-answer', action='store_true', help='\u8bbf\u95ee\u8be6\u60c5\u9875\u83b7\u53d6\u7b54\u6848\u89e3\u6790')
    p_collect.add_argument('--no-fetch-detail', dest='fetch_detail', action='store_false', help='\u4e0d\u8bbf\u95ee\u8be6\u60c5\u9875')
    p_collect.add_argument('--delay', type=float, default=0.8, help='\u8bf7\u6c42\u95f4\u9694\u79d2')
    p_collect.add_argument('-o', '--output', default=str(ROOT / 'data' / 'questions.json'))
    p_collect.add_argument('--headed', dest='headless', action='store_false')
    p_collect.set_defaults(headless=True, fetch_detail=True, func=cmd_collect)

    p_import = sub.add_parser('import', help='\u5bfc\u5165 JSON \u5230\u672c\u7cfb\u7edf\u9898\u5e93')
    p_import.add_argument('--file', required=True)
    p_import.add_argument('--config', default=str(ROOT / 'config.json'))
    p_import.set_defaults(func=cmd_import)

    p_run = sub.add_parser('run', help='\u91c7\u96c6\u5e76\u5bfc\u5165')
    p_run.add_argument('--url', required=True)
    p_run.add_argument('--pages', type=int, default=1)
    p_run.add_argument('--fetch-answer', action='store_true')
    p_run.add_argument('--delay', type=float, default=0.8)
    p_run.add_argument('-o', '--output', default='')
    p_run.add_argument('--config', default=str(ROOT / 'config.json'))
    p_run.add_argument('--headed', dest='headless', action='store_false')
    p_run.set_defaults(headless=None, func=cmd_run)

    args = parser.parse_args()
    args.func(args)


if __name__ == '__main__':
    main()
