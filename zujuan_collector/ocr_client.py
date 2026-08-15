# -*- coding: utf-8 -*-
"""OCR client for answer image text extraction."""

import os
import re
import subprocess
import sys
import time
from pathlib import Path

import requests

DEFAULT_OCR_URL = 'http://127.0.0.1:8867'

_OCR_NOISE_PATTERNS = (
    r'zujuan\.xkw',
    r'zujudnk',
    r'zxxk\.com',
    r'kw\.com',
    r'w\.com',
    r'xkw\.com',
)

_INLINE_NOISE_RES = (
    (re.compile(r'zujuan\.xkw\.com', re.I), ''),
    (re.compile(r'zujuan\.xkw', re.I), ''),
    (re.compile(r'zujudnk', re.I), ''),
    (re.compile(r'\bzxxk\.com\b', re.I), ''),
    (re.compile(r'\bwww\.\s*', re.I), ''),
    (re.compile(r'\bxkw\.com\b', re.I), ''),
    (re.compile(r'\b[kw]\.com\b', re.I), ''),
    (re.compile(r'(?<![a-zA-Z0-9])\.com(?![a-zA-Z0-9])', re.I), ''),
)

_ANALYSIS_MARKERS = (
    '\u3010\u8be6\u89e3\u3011',
    '\u3010\u89e3\u6790\u3011',
    '\u3010\u5bfc\u8bed\u3011',
    '\u3010\u5bfc\u8bfb\u3011',
    '\u3010\u70b9\u775b\u3011',
    '\u8be6\u89e3',
    '\u89e3\u6790',
    '\u5bfc\u8bed',
)

_CHINESE_ANALYSIS_MARKERS = (
    '\u3010\u5bfc\u8bed\u3011',
    '\u3010\u5bfc\u8bfb\u3011',
    '\u3010\u89e3\u6790\u3011',
    '\u3010\u8be6\u89e3\u3011',
    '\u3010\u70b9\u775b\u3011',
)

_KEYPOINT_MARKER = '\u3010\u70b9\u775b\u3011'

_CIRCLED_DIGITS = '\u2460\u2461\u2462\u2463\u2464\u2465\u2466\u2467\u2468\u2469'

_CHOICE_CONCLUSION_PATTERNS = (
    r'\u6545\u9009[\u62e9]?\s*[:\uff1a]?\s*([A-H]+)',
    r'\u5e94\u9009[\u62e9]?\s*[:\uff1a]?\s*([A-H]+)',
    r'\u672c\u9898\u9009[\u62e9]?\s*[:\uff1a]?\s*([A-H]+)',
    r'\u7b54\u6848[\u4e3a\u662f]\s*([A-H]+)',
)

_FORMULA_SUBJECT_CODES = (
    'gzwl', 'czwl', 'gzsx', 'czsx', 'gzhx', 'czhx',
)
_FORMULA_SUBJECT_LABELS = (
    '\u7269\u7406', '\u6570\u5b66', '\u5316\u5b66',
)
_FORMULA_CONTENT_HINTS = (
    '\u516c\u5f0f', '\u65b9\u7a0b', '\u51fd\u6570', '\u56fe\u50cf', '\u56fe\u7532',
    '\u725b\u987f', '\u6ed1\u5757', '\u7535\u8def', '\u5149\u7535', '\u7535\u573a',
    '\u78c1\u573a', '\u529b\u5b66', '\u52a8\u80fd', '\u52a8\u91cf', '\u52a0\u901f\u5ea6',
)


def resolve_ocr_mode(item=None, cfg=None):
    """Return OCR mode: mixed (Pix2Text) for formula-heavy subjects, else text."""
    cfg = cfg or {}
    forced = str(cfg.get('ocr_mode') or 'auto').strip().lower()
    if forced in ('text', 'mixed'):
        return forced

    item = item or {}
    code = str(
        item.get('subject_code')
        or (item.get('page_context') or {}).get('subject_code')
        or cfg.get('subject_code')
        or ''
    ).lower()
    label = str(
        item.get('subject_label')
        or (item.get('page_context') or {}).get('subject_label')
        or cfg.get('subject_label')
        or ''
    )
    if any(code.endswith(tag) or code == tag for tag in _FORMULA_SUBJECT_CODES):
        return 'mixed'
    if any(tag in label for tag in _FORMULA_SUBJECT_LABELS):
        return 'mixed'

    content = (item.get('content') or '') + (item.get('content_html') or '')
    if any(hint in content for hint in _FORMULA_CONTENT_HINTS):
        if re.search(r'[=+\-*/^_{}\\]|\\frac|\\sqrt', content):
            return 'mixed'
    return 'text'


def normalize_formula_latex(text):
    """Repair common OCR formula fragments so KaTeX can render them."""
    if not text:
        return ''
    s = str(text)

    s = re.sub(
        r'[A-Za-z]+\{\d+__\\frac\{([A-Za-z]*)(\d+)\}\{([^}]+)\}',
        r'\\frac{\1^{\2}}{\3}',
        s,
    )
    s = re.sub(
        r'^[A-Za-z]+\{\d+__\s*',
        '',
        s,
        flags=re.M,
    )
    s = re.sub(r'\\frac\{([A-Za-z])_(\d+)\}\{\((\d+)\)\}', r'\\frac{\1_\2}{\1_\3}', s)
    s = re.sub(r'\\frac\{t(\d+)\}\{\((\d+)\)\}', r'\\frac{t_\1}{t_\2}', s)
    s = re.sub(r'\\frac\{([A-Za-z]+)(\d+)\}\{\((\d+)\)\}', r'\\frac{\1_\2}{\1_\3}', s)
    s = re.sub(r'\\frac\{([^}]+)\}\{([^}]+)\}___+', r'\\frac{\1}{\2}', s)
    s = re.sub(r'\\frac\{([^}]*?)___([^}]*)\}', r'\\frac{\1\2}', s)
    s = re.sub(
        r'\\frac\{([A-Za-z])(\d+)\}\{([^}]+)\}',
        r'\\frac{\1_\2}{\3}',
        s,
    )
    s = re.sub(
        r'\\frac\{([A-Za-z]{2,})(\d+)\}\{([^}]+)\}',
        lambda m: (
            f'\\frac{{{m.group(1)}^{m.group(2)}}}{{{m.group(3)}}}'
            if not re.search(r'[\^_]', m.group(1))
            else m.group(0)
        ),
        s,
    )
    s = re.sub(r'([A-Za-z])\{(\d+)__', r'\1^{\2}', s)
    s = re.sub(r'\\frac\{(\d+)\}\{(\d+)\}(?=\s*____|\s*$)', '____', s)
    s = re.sub(r'\$+', '', s)
    return s.strip()


def check_ocr_available(ocr_base=DEFAULT_OCR_URL, timeout=3):
    try:
        resp = requests.get(f'{ocr_base.rstrip("/")}/health', timeout=timeout)
        if resp.status_code != 200:
            return False
        data = resp.json()
        return bool(data.get('ready', True))
    except requests.RequestException:
        return False


def _find_ocr_service_dir():
    root = Path(__file__).resolve().parents[1]
    candidates = [
        root / 'packages' / 'question-bank' / '_extracted' / '题库' / 'ocr-service' / 'paddleocr-service',
        root / 'packages' / 'question-bank' / 'ocr-service' / 'paddleocr-service',
    ]
    for candidate in candidates:
        if (candidate / 'start.ps1').is_file():
            return candidate
    extracted = root / 'packages' / 'question-bank' / '_extracted'
    if extracted.is_dir():
        for start_ps1 in extracted.rglob('start.ps1'):
            if start_ps1.parent.name == 'paddleocr-service':
                return start_ps1.parent
    return None


def try_start_ocr_service(ocr_base=DEFAULT_OCR_URL, on_log=None, wait_seconds=120):
    """Start local OCR sidecar if down; wait until /health is ready."""
    if check_ocr_available(ocr_base):
        return True

    ocr_dir = _find_ocr_service_dir()
    if not ocr_dir:
        if on_log:
            on_log('    未找到 OCR 服务目录，无法自动启动')
        return False

    if on_log:
        on_log('    正在启动 OCR 服务（首次启动约需 1-2 分钟）...')

    creationflags = 0
    if sys.platform == 'win32':
        creationflags = getattr(subprocess, 'CREATE_NEW_CONSOLE', 0)
        shell_cmd = [
            os.path.join(os.environ.get('SystemRoot', r'C:\Windows'),
                         'System32', 'WindowsPowerShell', 'v1.0', 'powershell.exe'),
            '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', str(ocr_dir / 'start.ps1'),
        ]
    else:
        shell_cmd = ['pwsh', '-NoProfile', '-File', str(ocr_dir / 'start.ps1')]

    try:
        subprocess.Popen(
            shell_cmd,
            cwd=str(ocr_dir),
            creationflags=creationflags,
        )
    except Exception as ex:
        if on_log:
            on_log(f'    OCR 自动启动失败: {ex}')
        return False

    deadline = time.time() + max(10, wait_seconds)
    while time.time() < deadline:
        if check_ocr_available(ocr_base):
            if on_log:
                on_log('    OCR 服务已就绪')
            return True
        time.sleep(2)

    if on_log:
        on_log('    OCR 服务启动超时，请手动运行 scripts\\start-ocr-service.ps1')
    return False


def ocr_image_bytes(image_bytes, ocr_base=DEFAULT_OCR_URL, mode='text', timeout=180):
    url = f'{ocr_base.rstrip("/")}/ocr/upload'
    files = {'file': ('answer.jpg', image_bytes, 'image/jpeg')}
    resp = None
    for attempt in range(2):
        try:
            resp = requests.post(url, files=files, params={'mode': mode}, timeout=timeout)
            resp.raise_for_status()
            break
        except requests.ConnectionError:
            if attempt == 0:
                time.sleep(2)
                continue
            return ''
        except requests.RequestException:
            return ''
    if resp is None:
        return ''
    data = resp.json()
    lines = data.get('lines') or []
    parts = []
    for line in lines:
        if isinstance(line, dict):
            parts.append((line.get('text') or '').strip())
        else:
            parts.append(str(line).strip())
    text = '\n'.join(p for p in parts if p)
    return normalize_formula_latex(text)


def strip_watermark_noise(text):
    if not text:
        return ''
    result = str(text)
    for pat in _OCR_NOISE_PATTERNS:
        result = re.sub(pat, '', result, flags=re.I)
    for pat, repl in _INLINE_NOISE_RES:
        result = pat.sub(repl, result)
    result = re.sub(r'\s{2,}', ' ', result)
    return result.strip()


def strip_placeholder_tokens(text):
    if not text:
        return ''
    result = str(text).strip()
    result = re.sub(r'^[\s\u3011\]\)\uff09\uff08]+', '', result)
    result = re.sub(
        r'(^|\n)\s*[\(\uff08]?\s*\d+\s*[\)\uff09]?\s*\u89c1\u7b54\u6848\s*',
        r'\1',
        result,
    )
    result = re.sub(r'(^|\n)\s*\u89c1\u7b54\u6848\s*', r'\1', result)
    result = re.sub(r'(^|\n)\s*\u89c1\u89e3\u6790\s*', r'\1', result)
    return result.strip()


def is_weak_analysis(text):
    if not text or not str(text).strip():
        return True
    t = strip_watermark_noise(text)
    t = strip_placeholder_tokens(t)
    t = re.sub(r'\u3010[^\u3011]{0,10}\u3011', '', t).strip()
    if not t:
        return True
    if _KEYPOINT_MARKER in text:
        kp = text.split(_KEYPOINT_MARKER, 1)[-1].strip()
        kp = strip_watermark_noise(strip_placeholder_tokens(kp))
        kp = re.sub(r'^[\s\u3011\]\)\uff09]+', '', kp)
        if len(kp) >= 4:
            return False
    if len(t) < 10:
        return True
    if re.fullmatch(r'[\u89c1\u7565\u7b54\u89e3\u6790\s\.\u3002\u3001\uff0c,\uff1b;\uff1a:\[\]\u3010\u3011]+', t):
        return True
    if re.fullmatch(r'\u7565[\s\.\u3002]*', t):
        return True
    if re.fullmatch(r'\u89c1\u7b54\u6848[\s\.\u3002]*', t):
        return True
    placeholder_hits = len(re.findall(r'\u89c1\u7b54\u6848', t))
    if placeholder_hits and len(re.sub(r'\u89c1\u7b54\u6848', '', t).strip()) < 8:
        return True
    return False


def clean_ocr_analysis(analysis):
    if not analysis:
        return ''
    lines = []
    for raw in analysis.split('\n'):
        s = strip_watermark_noise(raw.strip())
        s = re.sub(r'^[\s\u3011\]\)\uff09]+', '', s)
        if not s:
            continue
        if any(re.search(pat, s, re.I) for pat in _OCR_NOISE_PATTERNS):
            continue
        if re.fullmatch(r'[mM](\s+[mM])+', s):
            continue
        if re.fullmatch(r'[\d\.]+', s) and len(s) <= 6:
            continue
        if re.fullmatch(r'[a-zA-Z]/[^\s]+', s) and len(s) < 25:
            continue
        if re.fullmatch(r'\\frac\{\d\}\{\d\}', s):
            continue
        if s in ('/kg-1', 'kg-1', 'com'):
            continue
        if re.fullmatch(r'[\d\.\s]+', s) and len(s) < 35:
            continue
        if re.fullmatch(r'[\u89c1\u7565\u7b54\u6848\s\.\u3002]+', s):
            continue
        if re.fullmatch(rf'[{_CIRCLED_DIGITS}]', s):
            continue
        lines.append(s)
    text = '\n'.join(lines)
    text = re.sub(r'a\u4e0e\u6210\u6b63\u6bd4\u5173\u7cfb', 'a\u4e0e1/m\u6210\u6b63\u6bd4\u5173\u7cfb', text)
    text = re.sub(r'a\u4e0e\u6210\u6b63\u6bd4', 'a\u4e0e1/m\u6210\u6b63\u6bd4', text)
    text = re.sub(r'\u3001\s*\u4e3a\u6a2a\u5750\u6807', '\u30011/m\u4e3a\u6a2a\u5750\u6807', text)
    text = re.sub(r'F\u4e00\u5b9a\u65f6\uff0ca\u4e0e1\u6210\u6b63\u6bd4', 'F\u4e00\u5b9a\u65f6\uff0ca\u4e0e1/m\u6210\u6b63\u6bd4', text)
    text = strip_placeholder_tokens(text)
    return re.sub(r'\n{3,}', '\n\n', text).strip()


def strip_trailing_ocr_footnotes(text):
    """Remove circled-number footnote noise often appended by OCR."""
    text = (text or '').strip()
    while True:
        new = re.sub(rf'[\s\n]*[{_CIRCLED_DIGITS}]\s*$', '', text)
        if new == text:
            break
        text = new
    text = re.sub(
        rf'(?<=[\u3002\.\!\?\uff01\uff1f])\s*\n\s*[{_CIRCLED_DIGITS}1-9]\s*$',
        '',
        text,
    )
    return text.strip()


def extract_conclusive_choice(text, multi=False):
    """Pick the last conclusion phrase such as 故选B / 故选 ACD."""
    if not text:
        return ''
    best = ''
    best_pos = -1
    for pat in _CHOICE_CONCLUSION_PATTERNS:
        for m in re.finditer(pat, text, re.I):
            if m.start() >= best_pos:
                best_pos = m.start()
                best = re.sub(r'\s+', '', m.group(1).upper())
    if not best or not re.fullmatch(r'[A-H]+', best):
        return ''
    if multi:
        return ''.join(sorted(set(best)))
    return best if len(best) == 1 else best[0]


def normalize_analysis_text(text):
    text = strip_watermark_noise((text or '').replace('\r\n', '\n').strip())
    text = re.sub(r'\n*\u4ee5\u4e0a\u90e8\u5206\u5185\u5bb9\u7531AI\u751f\u6210.*$', '', text, flags=re.S).strip()
    text = re.sub(r'\u58f0\u660e[:\uff1a].*$', '', text, flags=re.S).strip()
    text = clean_ocr_analysis(text)
    text = strip_trailing_ocr_footnotes(text)
    text = normalize_formula_latex(text)
    return text.strip()


def is_placeholder_answer(text):
    t = strip_placeholder_tokens(strip_watermark_noise(text))
    if not t:
        return True
    if re.fullmatch(r'[\u89c1\u7565\u7b54\u6848\s\.\u3002]+', t):
        return True
    if t in ('\u89c1\u7b54\u6848', '\u7565', '\u7565\u3002'):
        return True
    return False


_ESSAY_ANSWER_LABELS = (
    '\u4f8b\u6587',
    '\u8303\u6587',
    '\u793a\u4f8b\u4f5c\u6587',
    '\u53c2\u8003\u8303\u6587',
    '\u793a\u4f8b',
)

_ESSAY_ANALYSIS_MARKERS = (
    '\u3010\u5ba1\u9898\u3011',
    '\u3010\u89e3\u6790\u3011',
    '\u3010\u8be6\u89e3\u3011',
    '\u3010\u70b9\u775b\u3011',
    '\u3010\u8be6\u7ec6\u89e3\u6790\u3011',
    '\u5ba1\u9898',
    '\u89e3\u6790',
    '\u8be6\u89e3',
)


def is_incomplete_answer(text):
    """True when answer is empty, placeholder, or only a label like \u4f8b\u6587\uff1a."""
    t = strip_placeholder_tokens(strip_watermark_noise(text or ''))
    if not t or is_placeholder_answer(t):
        return True
    if re.fullmatch(r'(?:' + '|'.join(_ESSAY_ANSWER_LABELS) + r')\s*[\uff1a:]*', t):
        return True
    for label in _ESSAY_ANSWER_LABELS:
        if t == label or t == f'{label}\uff1a' or t == f'{label}:':
            return True
    if len(t) <= 8 and any(label in t for label in _ESSAY_ANSWER_LABELS):
        body = re.sub(r'^(?:' + '|'.join(_ESSAY_ANSWER_LABELS) + r')\s*[\uff1a:]*', '', t).strip()
        if not body:
            return True
    return False


def _chinese_char_count(text):
    return len(re.findall(r'[\u4e00-\u9fff]', text or ''))


def extract_essay_answer_from_text(text):
    """Split OCR/HTML into \u4f8b\u6587 answer body and analysis sections."""
    text = strip_watermark_noise((text or '').replace('\r\n', '\n').strip())
    if not text:
        return '', ''

    if re.search(r'\u3010\u7b54\u6848\u3011', text) and not extract_conclusive_choice(text):
        answer, analysis = _parse_chinese_answer_analysis(text)
        if analysis and not is_weak_analysis(analysis):
            return answer, analysis
        if answer and not is_incomplete_answer(answer):
            return answer, analysis or ''

    label_pattern = r'(?:' + '|'.join(_ESSAY_ANSWER_LABELS) + r')'
    m = re.search(rf'({label_pattern})\s*[\uff1a:]\s*', text)
    if m:
        rest = text[m.start():]
        split_at = len(rest)
        for marker in _ESSAY_ANALYSIS_MARKERS:
            idx = rest.find(marker)
            if idx > 0 and idx < split_at:
                split_at = idx
        answer_part = rest[:split_at].strip()
        analysis_part = normalize_analysis_text(rest[split_at:].strip())
        if len(answer_part) > len(m.group(0)):
            return answer_part, analysis_part
        return '', analysis_part

    if any(marker in text for marker in _ESSAY_ANALYSIS_MARKERS):
        return '', normalize_analysis_text(text)

    if len(text) >= 80 and not extract_conclusive_choice(text):
        if _chinese_char_count(text) >= 30:
            analysis = normalize_analysis_text(text)
            if analysis and not is_weak_analysis(analysis):
                first = analysis.split('\n')[0].strip()[:200]
                return first or '', analysis
        return f'\u4f8b\u6587\uff1a\n{text}', ''
    return '', ''


def _extract_chinese_answer_block(text):
    """Extract 【答案】 block including 【小题N】 lines."""
    m = re.search(
        r'\u3010\u7b54\u6848\u3011([\s\S]*?)(?=\u3010\u5bfc\u8bed\u3011|\u3010\u5bfc\u8bfb\u3011|'
        r'\u3010\u89e3\u6790\u3011|\u3010\u8be6\u89e3\u3011|\u3010\u70b9\u775b\u3011|$)',
        text,
    )
    if m:
        block = m.group(1).strip()
        if block:
            return f'\u3010\u7b54\u6848\u3011{block}'
    return ''


def _extract_chinese_analysis_body(text):
    """Collect 导语/解析/详解 sections typical in Chinese reading items."""
    parts = []
    for marker in _CHINESE_ANALYSIS_MARKERS:
        start = 0
        while True:
            idx = text.find(marker, start)
            if idx < 0:
                break
            rest = text[idx:]
            end = len(rest)
            for other in _CHINESE_ANALYSIS_MARKERS:
                if other == marker:
                    continue
                nxt = rest.find(other, len(marker))
                if nxt > 0 and nxt < end:
                    end = nxt
            chunk = rest[:end].strip()
            if chunk and chunk not in parts:
                parts.append(chunk)
            start = idx + len(marker)
    return '\n'.join(parts).strip()


def _extract_subquestion_choice_answer(text):
    """Pick choice letter from 【小题N】 D style lines."""
    letters = []
    for m in re.finditer(r'\u3010\u5c0f\u9898\s*\d+\u3011\s*([A-Ha-h])\b', text):
        letters.append(m.group(1).upper())
    if len(letters) == 1:
        return letters[0]
    if letters:
        return '/'.join(letters)
    return ''


def _looks_like_chinese_answer_parse(text):
    t = text or ''
    if any(marker in t for marker in (
        '\u3010\u5c0f\u9898', '\u3010\u5bfc\u8bed\u3011', '\u3010\u5bfc\u8bfb\u3011',
    )):
        return True
    if '\u3010\u7b54\u6848\u3011' not in t:
        return False
    if any(marker in t for marker in ('\u3010\u5bfc\u8bed\u3011', '\u3010\u5bfc\u8bfb\u3011')):
        return True
    if _chinese_char_count(t) >= 30 and not extract_conclusive_choice(t):
        answer_block = _extract_chinese_answer_block(t) or t
        ans_body = re.sub(r'^\u3010\u7b54\u6848\u3011', '', answer_block).strip()
        if ans_body and not re.fullmatch(r'[A-Ha-h]', ans_body) and _chinese_char_count(ans_body) >= 15:
            return True
    return False


def _parse_chinese_answer_analysis(text):
    answer_block = _extract_chinese_answer_block(text)
    analysis_body = _extract_chinese_analysis_body(text)
    if not analysis_body:
        analysis_body = _extract_analysis_section(text)
    if is_weak_analysis(analysis_body):
        analysis_body = _extract_analysis_fallback(text, '') or analysis_body

    answer = _extract_subquestion_choice_answer(answer_block or text)
    if not answer:
        answer = _extract_answer(answer_block or text)
    if not answer and analysis_body:
        answer = extract_conclusive_choice(analysis_body)
    if not answer and analysis_body:
        answer = _extract_subquestion_choice_answer(analysis_body)

    if not answer and answer_block:
        body = re.sub(r'^\u3010\u7b54\u6848\u3011', '', answer_block).strip()
        if body and not is_weak_analysis(body):
            if len(body) <= 400:
                answer = body
            elif not analysis_body:
                analysis_body = body
                first_line = body.split('\n')[0].strip()
                if first_line and len(first_line) >= 4:
                    answer = first_line[:200]

    analysis = normalize_analysis_text(analysis_body)
    if not analysis and answer_block:
        tail = re.sub(r'^\u3010\u7b54\u6848\u3011', '', answer_block).strip()
        if tail and len(tail) > 20:
            analysis = normalize_analysis_text(tail)
    return answer, analysis


def _extract_answer(text):
    conclusive = extract_conclusive_choice(text)
    if conclusive:
        return conclusive

    ans_patterns = [
        r'\u3010\u5c0f\u9898\s*\d+\u3011\s*([A-Ha-h])\b',
        r'[\u3010\u3011\[\]]?\s*\u7b54\u6848\s*[\u3010\u3011\[\]]?\s*([A-H]{2,})',
        r'\u7b54\u6848\s*[:\uff1a]?\s*([A-H]{2,})',
        r'[\u3010\u3011\[\]]?\s*\u7b54\u6848\s*[\u3010\u3011\[\]]?\s*([A-Ha-h])',
        r'\u7b54\u6848\s*[:\uff1a]?\s*([A-Ha-h])',
        r'\u3010\u7b54\u6848\u3011\s*([^\n\u3010]{1,120})',
        r'\u7b54\u6848\s*[:\uff1a]\s*([^\n\u3010]{1,120})',
        r'\u6545\u586b\s*[:\uff1a]?\s*([^\n]+)',
        r'^\s*([A-H]{2,})\s*$',
        r'^\s*([A-Ha-h])\s*$',
    ]
    sub_ans = _extract_subquestion_choice_answer(text)
    if sub_ans and len(sub_ans) == 1:
        return sub_ans
    for pat in ans_patterns:
        m = re.search(pat, text, re.M)
        if m:
            ans = m.group(1).strip()
            if re.fullmatch(r'[A-H]+', ans.upper().replace(' ', '')):
                return ans.upper().replace(' ', '')
            if ans and not is_placeholder_answer(ans):
                return ans
    return ''


def _merge_keypoint(analysis_part, rest):
    key_idx = rest.find(_KEYPOINT_MARKER)
    if key_idx < 0:
        return strip_placeholder_tokens(analysis_part or rest)
    body = strip_placeholder_tokens((analysis_part or '') + rest[:key_idx].strip())
    keypoint = strip_placeholder_tokens(rest[key_idx + len(_KEYPOINT_MARKER):].strip())
    keypoint = re.sub(r'^[\s\u3011\]\)\uff09]+', '', keypoint)
    if keypoint and not is_weak_analysis(keypoint):
        if body:
            return f'{body}\n{_KEYPOINT_MARKER}{keypoint}'
        return f'{_KEYPOINT_MARKER}{keypoint}'
    return body


def _extract_analysis_section(text):
    for marker in _ANALYSIS_MARKERS:
        idx = text.find(marker)
        if idx < 0:
            continue
        rest = text[idx + len(marker):].lstrip(' \t:\uff1a\n')
        return _merge_keypoint('', rest)
    return ''


def _extract_analysis_fallback(text, answer):
    t = text
    t = re.sub(
        r'[\u3010\u3011\[\]]?\s*\u7b54\u6848\s*[\u3010\u3011\[\]]?\s*[^\n\u3010\u89e3\u8be6\u70b9]{0,80}',
        '',
        t,
        count=1,
        flags=re.S,
    )
    for marker in _ANALYSIS_MARKERS:
        t = t.replace(marker, '\n', 1)
    t = strip_placeholder_tokens(t)
    t = strip_watermark_noise(t)
    if answer:
        t = re.sub(rf'^\s*{re.escape(answer)}\s*', '', t, flags=re.M)
    t = clean_ocr_analysis(t)
    return t.strip()


def pick_best_analysis(*candidates):
    best = ''
    best_score = -1
    for raw in candidates:
        if not raw:
            continue
        cleaned = normalize_analysis_text(raw)
        if not cleaned:
            continue
        score = len(cleaned)
        if not is_weak_analysis(cleaned):
            score += 100000
        if score > best_score:
            best_score = score
            best = cleaned
    return best


def parse_answer_analysis_text(text):
    text = (text or '').replace('\r\n', '\n').strip()
    if not text:
        return '', ''

    text = strip_watermark_noise(text)
    text = re.sub(r'_+', ' ', text)

    if _looks_like_chinese_answer_parse(text):
        answer, analysis = _parse_chinese_answer_analysis(text)
        if answer or (analysis and not is_weak_analysis(analysis)):
            return answer, analysis

    essay_ans, essay_ana = extract_essay_answer_from_text(text)
    if essay_ans and not is_incomplete_answer(essay_ans):
        analysis = essay_ana or _extract_analysis_section(text)
        if is_weak_analysis(analysis):
            analysis = _extract_analysis_fallback(text, '') or essay_ana
        return essay_ans, normalize_analysis_text(analysis)

    answer = _extract_answer(text)
    analysis = _extract_analysis_section(text)

    if not analysis:
        parts = re.split(r'\u7b54\u6848[^\n]*\n?', text, maxsplit=1)
        if len(parts) > 1:
            analysis = _merge_keypoint('', parts[1].strip())

    if is_weak_analysis(analysis):
        fallback = _extract_analysis_fallback(text, answer)
        if fallback and not is_weak_analysis(fallback):
            analysis = fallback
        elif fallback and len(fallback) > len(analysis or ''):
            analysis = fallback

    if not answer and analysis:
        answer = extract_conclusive_choice(analysis)
        if not answer:
            m = re.search(r'\u7b54\u6848\s*[:\uff1a]?\s*([A-H]{2,})', analysis)
            if m:
                answer = m.group(1).upper()

    if not analysis and text and not is_weak_analysis(text):
        analysis = _extract_analysis_fallback(text, answer) or text

    analysis = normalize_analysis_text(analysis)
    return answer, analysis
