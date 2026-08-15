# -*- coding: utf-8 -*-
"""Normalize xkw image URLs and optionally reduce editorImg watermarks."""

from urllib.parse import parse_qs, urlencode, urlparse, urlunparse

DEFAULT_RESIZE_WIDTH = 600
OCR_RESIZE_WIDTH = 1400
OCR_MAX_WIDTH = 2000
OCR_MAX_HEIGHT = 4000
_WATERMARK_ALPHA = 0.32
_WATERMARK_GRAY = 215


def is_xkw_editor_image(url):
    u = (url or '').lower()
    return 'editorimg' in u or '/dksih/' in u


def is_xkw_like_image(url):
    parsed = urlparse(url or '')
    host = (parsed.netloc or '').lower()
    path = (parsed.path or '').lower()
    full = f'{host}{path}'
    return any(token in full for token in ('xkw.com', 'zujuan', 'getanswerandparse', 'editorimg'))


def is_fullsize_xkw_image(url):
    if not is_xkw_editor_image(url):
        return False
    qs = parse_qs(urlparse(url).query)
    return 'resizew' not in qs and 'resizeh' not in qs


def normalize_image_download_url(url, default_width=DEFAULT_RESIZE_WIDTH):
    """Prefer xkw CDN resized URLs; they are usually watermark-free."""
    if not url or not is_xkw_editor_image(url):
        return url
    parsed = urlparse(url)
    qs = parse_qs(parsed.query, keep_blank_values=True)
    if 'resizew' in qs or 'resizeh' in qs:
        return url
    width = default_width
    if qs.get('width'):
        try:
            width = max(int(qs['width'][0]), 80)
        except (TypeError, ValueError):
            pass
    qs['resizew'] = [str(width)]
    flat = {k: v[0] for k, v in qs.items() if v}
    return urlunparse(parsed._replace(query=urlencode(flat)))


def _opencv_available():
    try:
        import cv2  # noqa: F401
        import numpy  # noqa: F401
        return True
    except ImportError:
        return False


def remove_xkw_watermark(image_bytes):
    """
    Reduce semi-transparent xkw.com tiled watermarks on editorImg photos/diagrams.
    Uses alpha de-blending; best on light backgrounds. Returns None if OpenCV missing.
    """
    if not image_bytes or not _opencv_available():
        return None
    import cv2
    import numpy as np

    arr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if img is None:
        return None

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    mask = (gray > 160) & (gray < 253) & (hsv[:, :, 1] < 70)
    if not mask.any():
        return image_bytes

    out = img.astype(np.float32)
    alpha = _WATERMARK_ALPHA
    wm = float(_WATERMARK_GRAY)
    for ch in range(3):
        channel = out[:, :, ch]
        restored = (channel - alpha * wm) / (1.0 - alpha)
        out[:, :, ch] = np.where(mask, restored, channel)
    out = np.clip(out, 0, 255).astype(np.uint8)

    ok, buf = cv2.imencode('.png', out)
    if not ok:
        return image_bytes
    return buf.tobytes()


def resize_image_bytes_for_ocr(
    image_bytes,
    max_width=OCR_MAX_WIDTH,
    max_height=OCR_MAX_HEIGHT,
):
    """Downscale oversized answer images before OCR to avoid service OOM/crash."""
    if not image_bytes:
        return image_bytes
    try:
        import io

        from PIL import Image

        img = Image.open(io.BytesIO(image_bytes)).convert('RGB')
        w, h = img.size
        if w <= max_width and h <= max_height:
            return image_bytes
        scale = min(max_width / max(w, 1), max_height / max(h, 1), 1.0)
        new_w = max(1, int(w * scale))
        new_h = max(1, int(h * scale))
        img = img.resize((new_w, new_h), Image.LANCZOS)
        buf = io.BytesIO()
        img.save(buf, format='JPEG', quality=92, optimize=True)
        return buf.getvalue()
    except Exception:
        return image_bytes


def process_downloaded_image(url, image_bytes, options=None):
    """Apply optional watermark cleanup after download."""
    options = options or {}
    if not image_bytes:
        return image_bytes

    force = bool(options.get('remove_watermark'))
    auto_full = bool(options.get('auto_clean_fullsize', True))
    url_l = (url or '').lower()

    if is_xkw_editor_image(url):
        if force or (auto_full and is_fullsize_xkw_image(url)):
            cleaned = remove_xkw_watermark(image_bytes)
            if cleaned:
                return cleaned
        return image_bytes

    if force and is_xkw_like_image(url):
        cleaned = remove_xkw_watermark(image_bytes)
        if cleaned:
            return cleaned
    return image_bytes
