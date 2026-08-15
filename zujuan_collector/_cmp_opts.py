# -*- coding: utf-8 -*-
import json
from bs4 import BeautifulSoup

from zujuan_collector.parser import parse_detail_stem, parse_list_item

html = open('zujuan_collector/data/probe_34266545.html', encoding='utf-8').read()
stem = parse_detail_stem(html, '34266545')
out = {'detail_options': stem['options']}
soup = BeautifulSoup(html, 'lxml')
node = soup.select_one('.quesroot[questionid="34266545"]')
if node:
    item = parse_list_item(node, {})
    out['list_options'] = item['options']
    out['content_has_table'] = 'optionsTable' in (item.get('content') or '')
open('zujuan_collector/data/cmp_opts.json', 'w', encoding='utf-8').write(
    json.dumps(out, ensure_ascii=False, indent=2)
)
