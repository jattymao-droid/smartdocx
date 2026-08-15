# -*- coding: utf-8 -*-
"""Verify comprehensive question options are kept in stem and parsed."""
import json
from pathlib import Path

from bs4 import BeautifulSoup

from zujuan_collector.parser import parse_detail_stem, parse_list_item, options_to_json

html = Path(__file__).parent.joinpath('data', 'list_probe_34266545.html').read_text(encoding='utf-8')
soup = BeautifulSoup(html, 'lxml')
node = soup.select_one('.quesroot[questionid="34266545"]')
list_item = parse_list_item(node, {})
detail_stem = parse_detail_stem(html, '34266545', 'comprehensive')

result = {
    'list_options': options_to_json(list_item['options']),
    'list_content_has_table': '<table' in (list_item.get('content') or ''),
    'list_content_has_option_text': '\u53d7\u529b' in (list_item.get('content') or ''),
    'detail_options': options_to_json(detail_stem['options']),
    'detail_content_has_table': '<table' in (detail_stem.get('content') or ''),
    'detail_content_has_option_text': '\u8d28\u91cf' in (detail_stem.get('content') or ''),
}
out = Path(__file__).parent / 'data' / 'verify_34266545.json'
out.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
assert result['list_options'] and '\u53d7\u529b' in result['list_options']
assert result['detail_options'] and '\u8d28\u91cf' in result['detail_options']
assert result['list_content_has_table'] and result['list_content_has_option_text']
assert result['detail_content_has_table'] and result['detail_content_has_option_text']
print('OK', out)
