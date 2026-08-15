# -*- coding: utf-8 -*-
"""Import collected questions into RuoYi question bank API."""

import json
import subprocess
import time
import uuid

import requests

from .chapter_mapper import ChapterMapper
from .cloud_api import (
    education_url,
    extract_token,
    unwrap_data,
)
from .content_cleaner import filter_question_images, is_html_content
from .item_validator import clamp_difficulty, normalize_item, validate_item
from .media import build_inline_image_html, images_to_json, localize_html_images, localize_images
from .parser import answer_to_json, options_to_json


def _media_options(cfg, *, for_analysis_image=False):
    remove_wm = cfg.get('remove_watermark', True)
    if for_analysis_image:
        remove_wm = True
    return {
        'prefer_resized_images': cfg.get('prefer_resized_images', True),
        'remove_watermark': remove_wm,
        'auto_clean_fullsize': cfg.get('auto_clean_fullsize', True),
    }


def _analysis_image_urls(item):
    urls = item.get('analysis_image_urls') or item.get('answer_image_urls') or []
    return [u for u in urls if u]


class ApiClient:
    def __init__(self, api_base, username, password, redis_cli=None):
        self.api_base = api_base.rstrip('/')
        self.username = username
        self.password = password
        self.redis_cli = redis_cli
        self.token = None
        self.session = requests.Session()

    def _captcha(self):
        resp = self.session.get(
            f'{self.api_base}/code',
            headers={'Accept': 'text/plain, application/json, */*'},
            timeout=30,
        )
        data = resp.json()
        if not data.get('captchaEnabled'):
            return '', ''
        cap_uuid = data.get('uuid') or str(uuid.uuid4())
        code = ''
        if self.redis_cli:
            key = f'captcha_codes:{cap_uuid}'
            proc = subprocess.run(
                [self.redis_cli, 'GET', key],
                capture_output=True,
                text=True,
                timeout=10,
            )
            if proc.returncode == 0:
                code = (proc.stdout or '').strip().strip('"')
        if not code:
            raise RuntimeError(
                '\u767b\u5f55\u9700\u8981\u9a8c\u8bc1\u7801\uff1a\u8bf7\u5728\u914d\u7f6e\u4e2d\u8bbe\u7f6e redis_cli\uff0c'
                '\u6216\u5728\u7f51\u5173\u914d\u7f6e\u4e2d\u5173\u95ed security.captcha.enabled'
            )
        return code, cap_uuid

    def login(self):
        code, cap_uuid = self._captcha()
        payload = {
            'username': self.username,
            'password': self.password,
            'code': code,
            'uuid': cap_uuid,
        }
        resp = self.session.post(f'{self.api_base}/auth/login', json=payload, timeout=30)
        data = resp.json()
        if data.get('code') != 200:
            raise RuntimeError(f'\u767b\u5f55\u5931\u8d25: {data.get("msg")}')
        self.token = extract_token(data)
        if not self.token:
            raise RuntimeError('\u767b\u5f55\u5931\u8d25: \u672a\u8fd4\u56de access_token')
        self.session.headers.update({'Authorization': f'Bearer {self.token}'})
        return self.token

    def fetch_subjects(self):
        resp = self.session.get(
            education_url(self.api_base, '/subject/list'),
            params={'pageNum': 1, 'pageSize': 200},
            timeout=30,
        )
        data = resp.json()
        if data.get('code') != 200:
            raise RuntimeError(data.get('msg') or '\u83b7\u53d6\u5b66\u79d1\u5931\u8d25')
        rows = data.get('rows') or unwrap_data(data).get('rows') or []
        return [
            {'subjectId': r.get('subjectId'), 'subjectName': r.get('subjectName')}
            for r in rows
            if r.get('subjectId') is not None
        ]

    def test_connection(self):
        self.login()
        return True

    def fetch_textbook_versions(self, subject_id, school_stage='\u9ad8\u4e2d'):
        resp = self.session.get(
            education_url(self.api_base, '/textbook/versions'),
            params={'subjectId': subject_id, 'schoolStage': school_stage},
            timeout=30,
        )
        data = resp.json()
        if data.get('code') != 200:
            raise RuntimeError(data.get('msg') or '\u83b7\u53d6\u6559\u6750\u7248\u672c\u5931\u8d25')
        inner = unwrap_data(data)
        return inner.get('data') if isinstance(inner.get('data'), list) else (data.get('data') or [])

    def fetch_textbooks(self, version_id):
        resp = self.session.get(
            education_url(self.api_base, '/textbook/list'),
            params={'versionId': version_id},
            timeout=30,
        )
        data = resp.json()
        if data.get('code') != 200:
            raise RuntimeError(data.get('msg') or '\u83b7\u53d6\u6559\u6750\u5217\u8868\u5931\u8d25')
        inner = unwrap_data(data)
        return inner.get('data') if isinstance(inner.get('data'), list) else (data.get('data') or [])

    def fetch_chapter_tree(self, textbook_id, subject_id):
        resp = self.session.get(
            education_url(self.api_base, '/textbook/chapter/tree'),
            params={'textbookId': textbook_id, 'subjectId': subject_id},
            timeout=30,
        )
        data = resp.json()
        if data.get('code') != 200:
            raise RuntimeError(data.get('msg') or '\u83b7\u53d6\u7ae0\u8282\u6811\u5931\u8d25')
        inner = unwrap_data(data)
        return inner.get('data') if isinstance(inner.get('data'), list) else (data.get('data') or [])

    def create_chapter(self, textbook_id, chapter_name, parent_id=None, order_num=0):
        payload = {
            'textbookId': int(textbook_id),
            'chapterName': (chapter_name or '').strip(),
            'orderNum': int(order_num or 0),
        }
        if parent_id is not None:
            payload['parentId'] = int(parent_id)
        resp = self.session.post(
            education_url(self.api_base, '/textbook/chapter'),
            json=payload,
            timeout=30,
        )
        data = resp.json()
        if data.get('code') != 200:
            raise RuntimeError(data.get('msg') or f'\u521b\u5efa\u7ae0\u8282\u5931\u8d25: {chapter_name}')
        chapter_id = data.get('data')
        if chapter_id is not None:
            return int(chapter_id)
        return self._find_chapter_id(textbook_id, chapter_name, parent_id)

    def _find_chapter_id(self, textbook_id, chapter_name, parent_id=None):
        resp = self.session.get(
            education_url(self.api_base, '/textbook/chapter/list'),
            params={'textbookId': textbook_id},
            timeout=30,
        )
        data = resp.json()
        if data.get('code') != 200:
            return None
        inner = unwrap_data(data)
        rows = inner.get('data') if isinstance(inner.get('data'), list) else (data.get('data') or [])
        name_key = (chapter_name or '').strip()
        for row in rows:
            if (row.get('chapterName') or '').strip() != name_key:
                continue
            row_parent = row.get('parentId')
            if parent_id is None and (row_parent is None or row_parent == 0):
                return int(row['chapterId'])
            if parent_id is not None and row_parent == parent_id:
                return int(row['chapterId'])
        return None

    def create_version(self, subject_id, version_name, school_stage='\u9ad8\u4e2d'):
        payload = {
            'subjectId': int(subject_id),
            'versionName': (version_name or '').strip(),
            'schoolStage': school_stage or '\u9ad8\u4e2d',
            'orderNum': 0,
            'status': '0',
        }
        resp = self.session.post(
            education_url(self.api_base, '/textbook/version'),
            json=payload,
            timeout=30,
        )
        data = resp.json()
        if data.get('code') != 200:
            raise RuntimeError(data.get('msg') or f'\u521b\u5efa\u6559\u6750\u7248\u672c\u5931\u8d25: {version_name}')
        version_id = data.get('data')
        if version_id is not None:
            return int(version_id)
        for row in self.fetch_textbook_versions(subject_id, school_stage):
            if (row.get('versionName') or '').strip() == payload['versionName']:
                return int(row['versionId'])
        return None

    def create_textbook(self, version_id, textbook_name):
        payload = {
            'versionId': int(version_id),
            'textbookName': (textbook_name or '').strip(),
            'orderNum': 0,
            'status': '0',
        }
        resp = self.session.post(
            education_url(self.api_base, '/textbook'),
            json=payload,
            timeout=30,
        )
        data = resp.json()
        if data.get('code') != 200:
            raise RuntimeError(data.get('msg') or f'\u521b\u5efa\u6559\u6750\u5931\u8d25: {textbook_name}')
        textbook_id = data.get('data')
        if textbook_id is not None:
            return int(textbook_id)
        for row in self.fetch_textbooks(version_id):
            if (row.get('textbookName') or '').strip() == payload['textbookName']:
                return int(row['textbookId'])
        return None

    def create_subject(self, subject_name, order_num=99):
        payload = {
            'subjectName': (subject_name or '').strip(),
            'orderNum': int(order_num or 99),
            'status': '0',
        }
        resp = self.session.post(
            education_url(self.api_base, '/subject'),
            json=payload,
            timeout=30,
        )
        data = resp.json()
        if data.get('code') != 200:
            raise RuntimeError(data.get('msg') or f'\u521b\u5efa\u5b66\u79d1\u5931\u8d25: {subject_name}')
        subject_id = data.get('data')
        if subject_id is not None:
            return int(subject_id)
        for row in self.fetch_subjects():
            if (row.get('subjectName') or '').strip() == payload['subjectName']:
                return int(row['subjectId'])
        return None

    def add_question(self, payload):
        resp = self.session.post(
            education_url(self.api_base, '/question'),
            json=payload,
            timeout=60,
        )
        data = resp.json()
        if data.get('code') != 200:
            raise RuntimeError(data.get('msg') or str(data))
        return data

    def check_duplicate(self, subject_id, content, question_id=None):
        body = {'subjectId': subject_id, 'content': content}
        if question_id is not None:
            body['questionId'] = question_id
        resp = self.session.post(
            education_url(self.api_base, '/question/duplicate/check'),
            json=body,
            timeout=30,
        )
        data = resp.json()
        if data.get('code') != 200:
            return None
        return unwrap_data(data) or data.get('data') or {}

    def fetch_chapter_list(self, textbook_id):
        resp = self.session.get(
            education_url(self.api_base, '/textbook/chapter/list'),
            params={'textbookId': textbook_id},
            timeout=30,
        )
        data = resp.json()
        if data.get('code') != 200:
            raise RuntimeError(data.get('msg') or '\u83b7\u53d6\u7ae0\u8282\u5217\u8868\u5931\u8d25')
        inner = unwrap_data(data)
        return inner.get('data') if isinstance(inner.get('data'), list) else (data.get('data') or [])

    def fetch_questions(self, *, subject_id=None, textbook_id=None, chapter_id=None, page_num=1, page_size=100):
        params = {'pageNum': page_num, 'pageSize': page_size}
        if subject_id is not None:
            params['subjectId'] = subject_id
        if textbook_id is not None:
            params['textbookId'] = textbook_id
        if chapter_id is not None:
            params['chapterId'] = chapter_id
        resp = self.session.get(
            education_url(self.api_base, '/question/list'),
            params=params,
            timeout=60,
        )
        data = resp.json()
        if data.get('code') != 200:
            raise RuntimeError(data.get('msg') or '\u83b7\u53d6\u9898\u76ee\u5217\u8868\u5931\u8d25')
        return data.get('rows') or [], int(data.get('total') or 0)

    def delete_questions(self, question_ids):
        if not question_ids:
            return 0
        ids_str = ','.join(str(int(qid)) for qid in question_ids)
        resp = self.session.delete(
            education_url(self.api_base, f'/question/{ids_str}'),
            timeout=60,
        )
        data = resp.json()
        if data.get('code') != 200:
            raise RuntimeError(data.get('msg') or '\u5220\u9664\u9898\u76ee\u5931\u8d25')
        return len(question_ids)

    def delete_chapters(self, chapter_ids):
        if not chapter_ids:
            return 0
        ids_str = ','.join(str(int(cid)) for cid in chapter_ids)
        resp = self.session.delete(
            education_url(self.api_base, f'/textbook/chapter/{ids_str}'),
            timeout=60,
        )
        data = resp.json()
        if data.get('code') != 200:
            raise RuntimeError(data.get('msg') or '\u5220\u9664\u7ae0\u8282\u5931\u8d25')
        return len(chapter_ids)

    def delete_textbooks(self, textbook_ids):
        if not textbook_ids:
            return 0
        ids_str = ','.join(str(int(tid)) for tid in textbook_ids)
        resp = self.session.delete(
            education_url(self.api_base, f'/textbook/{ids_str}'),
            timeout=60,
        )
        data = resp.json()
        if data.get('code') != 200:
            raise RuntimeError(data.get('msg') or '\u5220\u9664\u6559\u6750\u5931\u8d25')
        return len(textbook_ids)

    def delete_versions(self, version_ids):
        if not version_ids:
            return 0
        ids_str = ','.join(str(int(vid)) for vid in version_ids)
        resp = self.session.delete(
            education_url(self.api_base, f'/textbook/version/{ids_str}'),
            timeout=60,
        )
        data = resp.json()
        if data.get('code') != 200:
            raise RuntimeError(data.get('msg') or '\u5220\u9664\u7248\u672c\u5931\u8d25')
        return len(version_ids)


def _log(msg, on_log=None):
    if on_log:
        on_log(msg)
    else:
        print(msg)


from .subject_resolver import item_subject_context, match_subject_id, short_subject_name


class ImportCatalog:
    def __init__(self):
        self._mappers = {}

    def get_mapper(self, client, subject_id, school_stage, cfg, on_log=None):
        key = (int(subject_id), school_stage or '\u9ad8\u4e2d')
        if key not in self._mappers:
            mapper = ChapterMapper()
            mapper.load(
                client,
                subject_id,
                version_id=cfg.get('version_id'),
                school_stage=school_stage or '\u9ad8\u4e2d',
                cfg=cfg,
                on_log=on_log,
            )
            self._mappers[key] = mapper
        return self._mappers[key]


def build_payload(item, cfg, local_images=None, chapter_map=None, subject_id=None):
    qtype = item.get('question_type') or 'short'
    options = item.get('options') or []
    answer_text = item.get('answer') or ''
    knowledge = item.get('knowledge_points') or []
    if not knowledge and item.get('category_name'):
        knowledge = [item['category_name']]
    if item.get('knowledge_leaf') and item['knowledge_leaf'] not in knowledge:
        knowledge.insert(0, item['knowledge_leaf'])
    detail_path = (item.get('detail_chapter_text') or '').strip()
    if detail_path:
        leaf = detail_path.split('>')[-1].strip()
        if leaf and leaf not in knowledge:
            knowledge.append(leaf)

    if chapter_map:
        chapter_text = chapter_map.get('chapter_text') or '\u7ec4\u5377\u7f51\u91c7\u96c6'
        textbook_id = chapter_map.get('textbook_id') or cfg.get('textbook_id')
        chapter_id = chapter_map.get('chapter_id') or cfg.get('chapter_id')
    else:
        chapter_text = item.get('chapter_text') or item.get('category_name') or '\u7ec4\u5377\u7f51\u91c7\u96c6'
        textbook_id = cfg.get('textbook_id')
        chapter_id = cfg.get('chapter_id')
    remark_parts = [f'zujuan:{item.get("zujuan_id")}']
    if item.get('source_label'):
        remark_parts.append(item['source_label'])
    if item.get('detail_url'):
        remark_parts.append(item['detail_url'])

    content = item.get('content_html') or item.get('content') or ''
    if chapter_map and chapter_map.get('zujuan_path'):
        remark_parts.append(f'zpath:{chapter_map["zujuan_path"]}')
    if is_html_content(content):
        remark_parts.append('fmt:html')

    return {
        'subjectId': subject_id or cfg.get('subject_id'),
        'textbookId': textbook_id,
        'chapterId': chapter_id,
        'chapterText': chapter_text,
        'content': content,
        'questionType': qtype,
        'difficulty': item.get('difficulty') or 0.5,
        'knowledgePoints': json.dumps(knowledge[:10], ensure_ascii=False),
        'options': options_to_json(options),
        'correctAnswer': answer_to_json(qtype, answer_text),
        'analysis': item.get('analysis') or '',
        'images': None if is_html_content(content) else images_to_json(local_images or []),
        'remark': ' | '.join(remark_parts),
    }


def import_questions(items, cfg, on_log=None, should_cancel=None, upload_images=True):
    client = ApiClient(
        cfg['api_base'],
        cfg['username'],
        cfg['password'],
        redis_cli=cfg.get('redis_cli'),
    )
    _log('\u6b63\u5728\u767b\u5f55\u672c\u7cfb\u7edf...', on_log)
    client.login()

    mapper = ImportCatalog()
    subjects = client.fetch_subjects()
    default_subject_id = cfg.get('subject_id')

    delay = float(cfg.get('import_delay') or 0.3)
    media_opts = _media_options(cfg)
    skip_duplicates = cfg.get('skip_duplicates', True)
    ok, fail, skipped = 0, 0, 0
    errors = []
    total = len(items)
    seen_zujuan = set()
    for idx, item in enumerate(items, 1):
        if should_cancel and should_cancel():
            _log('\u5bfc\u5165\u5df2\u53d6\u6d88', on_log)
            break
        try:
            zid = str(item.get('zujuan_id') or '')
            if zid and zid in seen_zujuan:
                skipped += 1
                _log(f'  [{idx}/{total}] SKIP \u91cd\u590d\u91c7\u96c6 zujuan:{zid}', on_log)
                continue
            if zid:
                seen_zujuan.add(zid)

            item = normalize_item(item)
            clamp_difficulty(item)
            valid, val_errors, val_warnings = validate_item(item)
            for w in val_warnings:
                _log(f'  [{idx}/{total}] WARN zujuan:{zid} {w}', on_log)
            if not valid:
                fail += 1
                msg = f'zujuan:{zid} ' + '; '.join(val_errors)
                errors.append(msg)
                _log(f'  [{idx}/{total}] FAIL {msg}', on_log)
                continue

            item['image_urls'] = filter_question_images(item.get('image_urls'))
            content = item.get('content_html') or item.get('content') or ''
            local_images = []
            if upload_images:
                if is_html_content(content):
                    _log(f'  [{idx}/{total}] \u4e0a\u4f20\u9898\u5e72\u56fe\u7247 zujuan:{item.get("zujuan_id")}', on_log)
                    content = localize_html_images(client, content, on_log=on_log, media_options=media_opts)
                    item['content'] = content
                    item['content_html'] = content
                elif item.get('image_urls'):
                    _log(f'  [{idx}/{total}] \u4e0a\u4f20\u56fe\u7247 zujuan:{item.get("zujuan_id")}', on_log)
                    local_images = localize_images(
                        client, item.get('image_urls'), on_log=on_log, media_options=media_opts,
                    )
                options = item.get('options') or []
                if options:
                    localized_options = []
                    for opt in options:
                        opt_text = opt.get('text') or ''
                        if is_html_content(opt_text):
                            opt_text = localize_html_images(
                                client, opt_text, on_log=on_log, media_options=media_opts,
                            )
                        localized_options.append({**opt, 'text': opt_text})
                    item['options'] = localized_options
                analysis_urls = _analysis_image_urls(item)
                # When OCR returns empty/weak analysis, importer would otherwise fall back
                # to uploading parsing images. Respect `prefer_analysis_image=false`.
                prefer_analysis_image = bool(cfg.get('prefer_analysis_image', False))
                use_analysis_image = bool(analysis_urls) and (
                    item.get('analysis_from_image')
                    or (prefer_analysis_image and not (item.get('analysis') or '').strip())
                )
                if use_analysis_image:
                    _log(
                        f'  [{idx}/{total}] \u4e0a\u4f20\u89e3\u6790\u56fe zujuan:{item.get("zujuan_id")}',
                        on_log,
                    )
                    ans_paths = localize_images(
                        client,
                        analysis_urls,
                        on_log=on_log,
                        media_options=_media_options(cfg, for_analysis_image=True),
                    )
                    if ans_paths:
                        item['analysis'] = build_inline_image_html(ans_paths)
                        item['analysis_from_image'] = True
            ctx = item_subject_context(item, cfg)
            subject_id = match_subject_id(
                subjects,
                ctx.get('subject_label'),
                ctx.get('subject_code'),
                default_subject_id,
            )
            if not subject_id and cfg.get('auto_create_subjects', False):
                short_name = short_subject_name(ctx.get('subject_label'), ctx.get('subject_code'))
                if short_name:
                    subject_id = client.create_subject(short_name)
                    subjects = client.fetch_subjects()
                    _log(f'  [\u5b66\u79d1] \u81ea\u52a8\u521b\u5efa: {short_name} (id:{subject_id})', on_log)
            if not subject_id:
                raise RuntimeError('config.subject_id \u672a\u914d\u7f6e\u4e14\u65e0\u6cd5\u4ece\u91c7\u96c6 URL \u5339\u914d\u5b66\u79d1')
            if (
                default_subject_id
                and subject_id != default_subject_id
                and ctx.get('subject_label')
            ):
                _log(
                    f'  [\u5b66\u79d1] URL\u5339\u914d: {ctx.get("subject_label")} -> id:{subject_id}'
                    f' (\u914d\u7f6e\u4e3a {default_subject_id})',
                    on_log,
                )
            chapter_map = None
            try:
                chapter_mapper = mapper.get_mapper(
                    client,
                    subject_id,
                    ctx.get('school_stage') or cfg.get('school_stage') or '\u9ad8\u4e2d',
                    cfg,
                    on_log=on_log,
                )
                chapter_map = chapter_mapper.match(item, cfg, client=client, on_log=on_log)
            except Exception as ex:
                _log(f'  [\u7ae0\u8282] \u5339\u914d\u5931\u8d25\uff0c\u5c06\u4f7f\u7528\u91c7\u96c6\u6587\u672c: {ex}', on_log)
            payload = build_payload(item, cfg, local_images, chapter_map, subject_id=subject_id)
            if not payload.get('subjectId'):
                raise RuntimeError('config.subject_id \u672a\u914d\u7f6e')
            if not payload.get('content'):
                raise RuntimeError('\u9898\u5e72\u4e3a\u7a7a')

            if skip_duplicates and payload.get('content'):
                dup = client.check_duplicate(payload['subjectId'], payload['content'])
                if dup and (dup.get('duplicate') or dup.get('duplicates')):
                    skipped += 1
                    _log(f'  [{idx}/{total}] SKIP \u5e93\u4e2d\u91cd\u590d zujuan:{zid}', on_log)
                    continue

            client.add_question(payload)
            ok += 1
            if chapter_map and chapter_map.get('created'):
                map_tag = '[+]'
            elif chapter_map and chapter_map.get('mapped'):
                map_tag = '[Y]'
            else:
                map_tag = '[~]'
            _log(
                f'  [{idx}/{total}] OK zujuan:{item.get("zujuan_id")} '
                f'\u7ae0\u8282{map_tag}:{payload.get("chapterText")}'
                + (f' (id:{payload.get("chapterId")})' if payload.get('chapterId') else ''),
                on_log,
            )
        except Exception as ex:
            fail += 1
            msg = f'zujuan:{item.get("zujuan_id")} {ex}'
            errors.append(msg)
            _log(f'  [{idx}/{total}] FAIL {msg}', on_log)
        time.sleep(delay)
    return {'success': ok, 'failed': fail, 'skipped': skipped, 'errors': errors, 'total': total}
