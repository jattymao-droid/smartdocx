# -*- coding: utf-8 -*-
"""Regression tests for answer extraction using collected fixtures."""

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from zujuan_collector.answer_extractor import (
    enrich_answer_from_analysis,
    extract_choice_answer,
    extract_fill_answer,
    extract_judge_answer,
)
from zujuan_collector.item_validator import finalize_item, normalize_item, validate_item
from zujuan_collector.content_cleaner import is_answer_image_url
from zujuan_collector.ocr_client import (
    extract_essay_answer_from_text,
    is_incomplete_answer,
    parse_answer_analysis_text,
)
from zujuan_collector.parser import (
    parse_detail_answer,
    parse_detail_stem,
    parse_list_item,
    infer_question_type,
    map_question_type,
    detect_question_type,
)
from zujuan_collector.subject_resolver import match_subject_id, short_subject_name

DATA = Path(__file__).resolve().parent / 'data'


def test_judge_from_fixture():
    raw = json.loads((DATA / 'collect_test_fix.json').read_text(encoding='utf-8'))
    judge_items = [x for x in raw if x.get('question_type') == 'judge']
    assert judge_items, 'no judge items in fixture'
    filled = 0
    for item in judge_items:
        item = dict(item)
        item['answer'] = ''
        enrich_answer_from_analysis(item)
        if item.get('answer'):
            filled += 1
    assert filled >= len(judge_items) // 2, f'only {filled}/{len(judge_items)} judge answers extracted'


def test_fill_from_fixture():
    raw = json.loads((DATA / 'collect_test_fix.json').read_text(encoding='utf-8'))
    fill_items = [x for x in raw if x.get('question_type') in ('fill', 'knowledge_fill')]
    assert fill_items, 'no fill items'
    filled = 0
    for item in fill_items:
        item = dict(item)
        item['answer'] = ''
        enrich_answer_from_analysis(item)
        if item.get('answer'):
            filled += 1
    assert filled >= 1, 'no fill answers extracted'


def test_comprehensive_options():
    html = (DATA / 'list_probe_34266545.html').read_text(encoding='utf-8')
    from bs4 import BeautifulSoup
    soup = BeautifulSoup(html, 'lxml')
    node = soup.select_one('.quesroot[questionid="34266545"]')
    list_item = parse_list_item(node, {})
    detail = parse_detail_stem(html, '34266545', 'comprehensive')
    assert list_item.get('options'), 'list options empty'
    assert detail.get('options'), 'detail options empty'
    stem = list_item.get('content') or ''
    assert 'optionsTable' not in stem
    assert 'qb-options' in stem, 'comprehensive options should render as list in stem'
    assert '<table' not in stem


def test_fill_chained_markers_fixture():
    raw = json.loads((DATA / 'collect_test_fix.json').read_text(encoding='utf-8'))
    item = next(x for x in raw if x.get('zujuan_id') == '34246038')
    item = dict(item)
    item['answer'] = ''
    enrich_answer_from_analysis(item)
    ans = item.get('answer') or ''
    assert '\u901f\u5ea6\u53d8\u5316' in ans, ans
    assert '\u76f8\u540c' in ans, ans
    assert '\u76f8\u53cd' in ans, ans


def test_judge_block_conclusions_fixture():
    raw = json.loads((DATA / 'collect_test_fix.json').read_text(encoding='utf-8'))
    item = next(x for x in raw if x.get('zujuan_id') == '34245256')
    item = dict(item)
    item['answer'] = ''
    enrich_answer_from_analysis(item)
    assert item.get('answer') == '\u221a\u00d7\u221a\u221a\u00d7', item.get('answer')


def test_choice_keeps_api_answer_without_conclusion():
    item = {
        'question_type': 'single',
        'answer': 'C',
        'analysis': 'A.\u6545A\u9519\u8bef\uff1bB.\u6545B\u6b63\u786e\uff1bC.\u6545C\u6b63\u786e\u3002',
    }
    enrich_answer_from_analysis(item)
    assert item['answer'] == 'C'


def test_finalize_strips_answer_images_from_stem():
    raw = json.loads((DATA / 'collect_test_fix.json').read_text(encoding='utf-8'))
    item = dict(raw[0])
    item['image_urls'] = list(item.get('image_urls') or []) + list(item.get('answer_image_urls') or [])
    finalized = finalize_item(item)
    for url in finalized.get('image_urls') or []:
        assert not is_answer_image_url(url), url


def test_infer_chinese_single_choice_type():
    item = {
        'question_type': 'short',
        'type_label': '\u53e4\u4ee3\u8bd7\u6b4c\u9605\u8bfb-\u5355\u7bc7/1-\u9898',
        'answer': 'C',
        'options': [
            {'label': 'A', 'text': 'opt A'},
            {'label': 'B', 'text': 'opt B'},
            {'label': 'C', 'text': 'opt C'},
            {'label': 'D', 'text': 'opt D'},
        ],
    }
    assert infer_question_type(item) == 'single'


def test_infer_reading_multi_block_options():
    item = {
        'question_type': 'short',
        'type_label': '\u73b0\u4ee3\u6587\u9605\u8bfb-\u4fe1\u606f\u7c7b-\u5355\u6587\u672c/5\u5c0f\u9898',
        'answer': 'B',
        'options': [{'label': 'A', 'text': '1'}, {'label': 'B', 'text': '2'},
                    {'label': 'C', 'text': '3'}, {'label': 'D', 'text': '4'},
                    {'label': 'A', 'text': '5'}, {'label': 'B', 'text': '6'}],
    }
    assert infer_question_type(item) == 'reading'


def test_map_question_type_prefers_qyname():
    assert map_question_type('\u53e4\u4ee3\u8bd7\u6b4c\u9605\u8bfb-\u5355\u7bc7/1-\u9898', '\u5355\u9009\u9898') == 'single'


def test_reading_poem_keeps_subquestions_and_options():
    html = (DATA / 'reading_poem_fixture.html').read_text(encoding='utf-8')
    detail = parse_detail_stem(html, '34293914', '')
    assert detail.get('question_type') == 'reading', detail.get('question_type')
    content = detail.get('content') or ''
    assert '\u9605\u8bfb\u4e0b\u9762\u7684\u8bd7\u6b4c' in content
    assert '\u828b\u82a2' in content
    assert 'qb-options' in content
    assert 'qb-option-item' in content
    assert 'optionsTable' not in content
    assert '<table' not in content
    assert 'A\uff0e' in content or 'A.' in content
    assert '1\uff0e' in content and '2\uff0e' in content
    assert not detail.get('options'), 'reading should keep options in stem only'


def test_finalize_reading_collected_item():
    item = json.loads((DATA / '_item_34293914.json').read_text(encoding='utf-8'))
    fixed = finalize_item(dict(item))
    assert fixed.get('question_type') == 'reading'
    # Legacy collect without option table in stem still keeps options for UI display.
    assert len(fixed.get('options') or []) == 4


def test_normalize_validation():
    raw = json.loads((DATA / 'collect_test_fix.json').read_text(encoding='utf-8'))
    ok_count = 0
    for item in raw[:5]:
        norm = normalize_item(dict(item))
        ok, errors, _ = validate_item(norm)
        if ok:
            ok_count += 1
        else:
            print('validation fail', item.get('zujuan_id'), errors)
    assert ok_count >= 3, f'only {ok_count}/5 passed validation'


def test_single_choice_conclusion_overrides_wrong_answer():
    raw = json.loads((DATA / 'test_answer_one.json').read_text(encoding='utf-8'))
    item = dict(raw[0])
    item['answer'] = 'A'
    enrich_answer_from_analysis(item)
    assert item['answer'] == 'B', f"expected B, got {item['answer']!r}"
    assert '\u2460' not in item['analysis'], 'trailing circled footnote should be removed'
    assert not item['analysis'].endswith('1'), 'trailing footnote digit should be removed'


def test_incomplete_example_essay_answer():
    assert is_incomplete_answer('\u4f8b\u6587\uff1a')
    assert is_incomplete_answer('\u4f8b\u6587')
    assert not is_incomplete_answer('\u4f8b\u6587\uff1a\n\u8fd9\u662f\u8303\u6587\u6b63\u6587')


def test_extract_essay_answer_splits_shenti():
    ocr = (
        '\u4f8b\u6587\uff1a\n\u9752\u5e74\u5e94\u5f53\u6709\u7406\u60f3\u3002\n'
        '\u3010\u5ba1\u9898\u3011\n\u6750\u6599\u9010\u53e5\u62c6\u89e3\uff1a'
    )
    ans, ana = extract_essay_answer_from_text(ocr)
    assert ans.startswith('\u4f8b\u6587')
    assert '\u9752\u5e74\u5e94\u5f53\u6709\u7406\u60f3' in ans
    assert '\u5ba1\u9898' in ana


def test_parse_answer_analysis_essay():
    body = '\u8fd9\u662f\u4e00\u7bc7\u5b8c\u6574\u7684\u4f5c\u6587\u8303\u6587\u5185\u5bb9\u3002' * 5
    ocr = f'\u4f8b\u6587\uff1a\n{body}\n\u3010\u5ba1\u9898\u3011\n\u89e3\u6790\u5185\u5bb9'
    answer, analysis = parse_answer_analysis_text(ocr)
    assert '\u4f8b\u6587' in answer
    assert '\u5ba1\u9898' in analysis


def test_parse_detail_answer_extracts_parse_image():
    html = (DATA / 'probe_34293914.html').read_text(encoding='utf-8')
    _, _, _, _, all_images, ok = parse_detail_answer(html)
    assert ok
    assert len(all_images) == 1
    assert 'getAnswerAndParse' in all_images[0]


def test_importer_keeps_answer_parse_image_urls():
    from zujuan_collector.importer import _analysis_image_urls
    urls = ['https://imzujuan.xkw.com/getAnswerAndParse/1/10/abc']
    item = {'answer_image_urls': urls, 'analysis': ''}
    assert _analysis_image_urls(item) == urls


def test_process_downloaded_analysis_image_forces_watermark_cleanup(monkeypatch):
    from zujuan_collector import image_cleaner

    called = {}

    def fake_remove(blob):
        called['blob'] = blob
        return b'cleaned'

    monkeypatch.setattr(image_cleaner, 'remove_xkw_watermark', fake_remove)
    out = image_cleaner.process_downloaded_image(
        'https://tkpic.zujuan.xkw.com/2026/07/sample.png',
        b'raw-image',
        {'remove_watermark': True, 'auto_clean_fullsize': True},
    )
    assert out == b'cleaned'
    assert called['blob'] == b'raw-image'


def test_physics_analysis_prefers_image():
    from zujuan_collector.answer_image_mode import looks_like_ocr_garbage, should_keep_analysis_as_image
    item = {
        'content': '\u5982\u56fe\u6240\u793a\uff0c\u7ec6\u7ebf\u6813\u7740\u5c0f\u7403',
        'question_type': 'single',
    }
    bad = '(1)A\n(2)B\n(4) ook\n\u8054\u7acb\u53ef\u5f97a= \u2014 \u2014 \u00b7m+ / g'
    assert looks_like_ocr_garbage(bad)
    assert should_keep_analysis_as_image(
        item, bad, ['https://imzujuan.xkw.com/getAnswerAndParse/1/10/x'],
        prefer_analysis_image=True,
    )
    assert not should_keep_analysis_as_image(
        item, bad, ['https://imzujuan.xkw.com/getAnswerAndParse/1/10/x'],
        prefer_analysis_image=False,
    )


def test_chinese_subjective_answer_parse():
    from zujuan_collector.ocr_client import parse_answer_analysis_text, is_weak_analysis

    ocr = (
        '\u3010\u7b54\u6848\u3011\n'
        '\u2460 \u77e5\u4eba\u8bba\u4e16\uff0c\u4e86\u89e3\u95fb\u4e00\u591a\u53ca\u300a\u7ea2\u70db\u300b\u521b\u4f5c\u80cc\u666f\uff1b\n'
        '\u2461 \u8bf5\u8bfb\u5168\u8bd7\uff0c\u628a\u63e1\u201c\u7ea2\u70db\u201d\u610f\u8c61\u4e0e\u60c5\u611f\u53d8\u5316\uff1b\n'
        '\u2462 \u6b23\u8d4f\u5173\u952e\u8bd7\u53e5\uff0c\u4f53\u4f1a\u8c61\u5f81\u624b\u6cd5\u4e0e\u6298\u60c5\u65b9\u5f0f\uff1b\n'
        '\u2463 \u6bd4\u8f83\u9605\u8bfb\uff0c\u8054\u7cfb\u6750\u6599\u4e2d\u7684\u73b0\u4ee3\u8bd7\u9605\u8bfb\u65b9\u6cd5\u8fdb\u884c\u5f52\u7eb3\u3002\n'
        '\u3010\u8be6\u89e3\u3011\u672c\u9898\u8003\u67e5\u73b0\u4ee3\u8bd7\u6b4c\u9605\u8bfb\u65b9\u6cd5\u7684\u7efc\u5408\u8fd0\u7528\u3002'
        '\u4f5c\u7b54\u65f6\u5e94\u7ed3\u5408\u6750\u6599\u4e00\u3001\u6750\u6599\u4e8c\u63d0\u4f9b\u7684\u65b9\u6cd5\uff0c'
        '\u9488\u5bf9\u300a\u7ea2\u70db\u300b\u8bbe\u8ba1\u5b8c\u6574\u9605\u8bfb\u6d41\u7a0b\u3002'
    )
    answer, analysis = parse_answer_analysis_text(ocr)
    assert not is_weak_analysis(analysis), 'subjective analysis should not be weak'
    assert '\u7ea2\u70db' in analysis
    assert '\u8be6\u89e3' in analysis or '\u77e5\u4eba\u8bba\u4e16' in analysis
    assert answer, 'subjective answer should be extracted'


def test_chinese_reading_answer_parse():
    from zujuan_collector.ocr_client import parse_answer_analysis_text, is_weak_analysis

    ocr = (
        '\u3010\u7b54\u6848\u3011\u3010\u5c0f\u98981\u3011 D\n'
        '\u3010\u5c0f\u98982\u3011 \u793a\u4f8b\u4e00\uff1a\u5173\u8054\u300a\u79bb\u9a9a\u300b\n'
        '\u3010\u5bfc\u8bed\u3011\u8fd9\u9996\u5b8b\u8bd7\u4ee5\u5bfb\u6885\u7684\u6ce2\u6298\u5165\u7b14\u3002\n'
        '\u3010\u8be6\u89e3\u3011\u3010\u5c0f\u98981\u3011 D\u3002\u9519\u8bef\u3002\u6545\u9009D\u3002'
    )
    answer, analysis = parse_answer_analysis_text(ocr)
    assert answer == 'D', f'expected D, got {answer!r}'
    assert not is_weak_analysis(analysis), 'analysis should not be weak'
    assert '\u5bfc\u8bed' in analysis
    assert '\u8be6\u89e3' in analysis or '\u9519\u8bef' in analysis


def test_normalize_formula_latex_repairs_physics():
    from zujuan_collector.ocr_client import normalize_formula_latex, resolve_ocr_mode

    assert normalize_formula_latex('md{2__\\frac{md2}{2F}') == '\\frac{md^{2}}{2F}'
    assert normalize_formula_latex('\\frac{t1}{(3)}___') == '\\frac{t_1}{t_3}'
    assert resolve_ocr_mode({'subject_code': 'gzwl'}) == 'mixed'
    assert resolve_ocr_mode({'subject_label': '\u9ad8\u4e2d\u7269\u7406'}) == 'mixed'
    assert resolve_ocr_mode({'subject_code': 'gzyw'}) == 'text'


def test_chinese_analysis_ocr_strips_zxxk_watermark():
    ocr = (
        'www.zxxk.com\n\u3010\u7b54\u6848\u3011\u3010\u5c0f\u98981\u3011 D\n'
        '\u3010\u89e3\u6790\u3011\u3010\u5bfc\u8bed\u3011\u8fd9\u9996\u5b8b\u8bd7\u4ee5\u5bfb\u6885\u7684\u6ce2\u6298\u5165\u7b14\u3002\n'
        'www.zxxk.com\n\u3010\u8be6\u89e3\u3011\u3010\u5c0f\u98981\u3011 D\u3002\u201c\u8868\u8fbe\u8bd7\u4eba\u5bf9\u529f\u540d\u5bcc\u8d35\u7684\u5411\u5f80\u201d\u9519\u8bef\u3002'
    )
    _, analysis = parse_answer_analysis_text(ocr)
    assert 'zxxk' not in analysis.lower()
    assert 'www.' not in analysis.lower()
    assert '\u9519\u8bef' in analysis
    assert len(analysis) >= 10


def test_essay_still_allows_ocr():
    from zujuan_collector.answer_image_mode import is_essay_example_mode, should_keep_analysis_as_image
    item = {'content': '\u6839\u636e\u8981\u6c42\u5199\u4f5c\u3002', 'question_type': 'short'}
    assert is_essay_example_mode(item, partial_answer='\u4f8b\u6587\uff1a')
    assert not should_keep_analysis_as_image(
        item,
        '\u3010\u5ba1\u9898\u3011\u6750\u6599\u9010\u53e5\u62c6\u89e3',
        ['https://imzujuan.xkw.com/getAnswerAndParse/1/10/x'],
        html_answer='\u4f8b\u6587\uff1a',
        partial_answer='\u4f8b\u6587\uff1a',
    )


def test_ocr_parse_does_not_match_gu_a_cuowu():
    analysis = (
        '\u3010\u7b54\u6848\u3011A\n\u3010\u89e3\u6790\u3011'
        '\u3011A.\u82e5\u62c9\u529b\u6cbfAa\u65b9\u5411\uff0c\u6545A\u9519\u8bef\uff1b'
        'B.\u6728\u68d2\u53d7\u91cd\u529bG\u3001\u62c9\u529bFR\u548c\u62c9\u529bF4\u4e09\u4e2a\u529b\u4f5c\u7528\u5904\u4e8e\u5e73\u8861\u72b6\u6001\u3002\u6545B\u6b63\u786e\uff1b'
        '\u6545\u9009B\u3002\n\u2460'
    )
    answer, parsed_analysis = parse_answer_analysis_text(analysis)
    assert answer == 'B', f"expected B from \u6545\u9009B, got {answer!r}"
    assert '\u2460' not in parsed_analysis


def test_multi_choice_conclusion():
    analysis = 'A.\u6545A\u6b63\u786e\uff1bB.\u6545B\u9519\u8bef\uff1b\u6545\u9009 ACD\u3002'
    assert extract_choice_answer(analysis, multi=True) == 'ACD'


def test_detail_container_options():
    html = (DATA / 'probe_34266545.html').read_text(encoding='utf-8')
    detail = parse_detail_stem(html, '34266545', 'comprehensive')
    assert detail.get('options'), 'detail options empty'


def test_list_container_fallback():
    html = (DATA / 'probe_34266545.html').read_text(encoding='utf-8')
    from bs4 import BeautifulSoup
    soup = BeautifulSoup(html, 'lxml')
    node = soup.select_one('.quesroot[questionid="34266545"]')
    item = parse_list_item(node, {})
    assert item.get('options'), 'list parser should read quest-cnt fallback'


def test_subject_match_chinese_not_physics():
    subjects = [
        {'subjectId': 1, 'subjectName': '\u8bed\u6587'},
        {'subjectId': 4, 'subjectName': '\u7269\u7406'},
    ]
    sid = match_subject_id(subjects, '\u9ad8\u4e2d\u8bed\u6587', 'gzyw', fallback_id=4)
    assert sid == 1
    assert short_subject_name('\u9ad8\u4e2d\u8bed\u6587', 'gzyw') == '\u8bed\u6587'


def main():
    test_detail_container_options()
    print('detail options: OK')
    test_list_container_fallback()
    print('list quest-cnt fallback: OK')
    test_subject_match_chinese_not_physics()
    print('subject match: OK')
    test_single_choice_conclusion_overrides_wrong_answer()
    print('single choice conclusion: OK')
    test_incomplete_example_essay_answer()
    print('essay incomplete answer: OK')
    test_extract_essay_answer_splits_shenti()
    print('essay split shenti: OK')
    test_parse_answer_analysis_essay()
    print('essay parse answer: OK')
    test_physics_analysis_prefers_image()
    print('analysis image mode: OK')
    test_chinese_analysis_ocr_strips_zxxk_watermark()
    print('chinese analysis ocr watermark: OK')
    test_chinese_subjective_answer_parse()
    print('chinese subjective answer parse: OK')
    test_chinese_reading_answer_parse()
    print('chinese reading answer parse: OK')
    test_normalize_formula_latex_repairs_physics()
    print('formula latex repair: OK')
    test_essay_still_allows_ocr()
    print('essay ocr kept: OK')
    test_ocr_parse_does_not_match_gu_a_cuowu()
    print('ocr parse gu-a-cuowu: OK')
    test_multi_choice_conclusion()
    print('multi choice conclusion: OK')
    test_judge_from_fixture()
    print('judge: OK')
    test_fill_from_fixture()
    print('fill: OK')
    test_fill_chained_markers_fixture()
    print('fill chained markers: OK')
    test_judge_block_conclusions_fixture()
    print('judge blocks: OK')
    test_choice_keeps_api_answer_without_conclusion()
    print('choice api keep: OK')
    test_finalize_strips_answer_images_from_stem()
    print('finalize images: OK')
    test_infer_chinese_single_choice_type()
    print('infer chinese single: OK')
    test_infer_reading_multi_block_options()
    print('infer reading multi: OK')
    test_map_question_type_prefers_qyname()
    print('map type qyname: OK')
    test_reading_poem_keeps_subquestions_and_options()
    print('reading poem subquestions: OK')
    test_finalize_reading_collected_item()
    print('finalize reading item: OK')
    test_comprehensive_options()
    print('comprehensive options: OK')
    test_normalize_validation()
    print('validation: OK')
    print('ALL TESTS PASSED')


if __name__ == '__main__':
    main()