/** Inline formula ~12pt body; figures capped for Word export. */
const BODY_LINE_HEIGHT_PX = 18
const MAX_FIGURE_WIDTH = 280
const MAX_FIGURE_HEIGHT = 200

const FORMULA_URL_RE = /(?:\/formula\/|quesimg|getformula|dksih|mathimg|lateximg)/i

function parseCssPx(value, fallback) {
  const raw = String(value || '').trim().toLowerCase()
  if (!raw) return fallback
  const num = parseFloat(raw)
  if (Number.isNaN(num)) return fallback
  if (raw.endsWith('pt')) return Math.round(num * 96 / 72)
  if (raw.endsWith('em')) return Math.round(num * 12)
  if (raw.endsWith('px') || /^\d+(\.\d+)?$/.test(raw)) return Math.round(num)
  return fallback
}

function readStyleMap(node) {
  const style = (node && node.getAttribute && node.getAttribute('style')) || ''
  const map = {}
  style.split(';').forEach(chunk => {
    const piece = chunk.trim()
    if (!piece || !piece.includes(':')) return
    const [key, val] = piece.split(':')
    map[key.trim().toLowerCase()] = val.trim()
  })
  return map
}

export function isFormulaImageNode(node, src, naturalSize) {
  const url = String(src || '').toLowerCase()
  const cls = String((node && node.getAttribute && node.getAttribute('class')) || '').toLowerCase()
  if (cls.includes('formula')) return true
  if (FORMULA_URL_RE.test(url)) return true
  const w = naturalSize && naturalSize.width
  const h = naturalSize && naturalSize.height
  if (w > 0 && h > 0 && h <= 44 && w <= 280) return true
  return false
}

export async function readImageNaturalSize(data) {
  if (!data || !data.length) return null
  try {
    const blob = new Blob([data])
    const url = URL.createObjectURL(blob)
    try {
      const img = new Image()
      await new Promise((resolve, reject) => {
        img.onload = resolve
        img.onerror = reject
        img.src = url
      })
      const width = img.naturalWidth || img.width
      const height = img.naturalHeight || img.height
      if (!width || !height) return null
      return { width, height }
    } finally {
      URL.revokeObjectURL(url)
    }
  } catch (e) {
    return readImageNaturalSizeFromHeader(data)
  }
}

function readImageNaturalSizeFromHeader(bytes) {
  const data = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes)
  if (data.length >= 24 && data[0] === 0x89 && data[1] === 0x50) {
    const width = (data[16] << 24) | (data[17] << 16) | (data[18] << 8) | data[19]
    const height = (data[20] << 24) | (data[21] << 16) | (data[22] << 8) | data[23]
    if (width > 0 && height > 0) return { width, height }
  }
  if (data.length >= 4 && data[0] === 0xff && data[1] === 0xd8) {
    let offset = 2
    while (offset + 9 < data.length) {
      if (data[offset] !== 0xff) break
      const marker = data[offset + 1]
      const size = (data[offset + 2] << 8) | data[offset + 3]
      if (size < 2) break
      if ([0xc0, 0xc1, 0xc2, 0xc3, 0xc5, 0xc6, 0xc7, 0xc9, 0xca, 0xcb, 0xcd, 0xce, 0xcf].includes(marker)) {
        const height = (data[offset + 5] << 8) | data[offset + 6]
        const width = (data[offset + 7] << 8) | data[offset + 8]
        if (width > 0 && height > 0) return { width, height }
        break
      }
      offset += size + 2
    }
  }
  return null
}

function scaleToBox(width, height, maxW, maxH) {
  const scale = Math.min(maxW / width, maxH / height, 1)
  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale))
  }
}

export function computeDocxImageTransform(node, src, naturalSize, opts = {}) {
  const style = readStyleMap(node)
  const naturalW = (naturalSize && naturalSize.width) || parseCssPx(node && node.getAttribute('width'), 0)
  const naturalH = (naturalSize && naturalSize.height) || parseCssPx(node && node.getAttribute('height'), 0)
  const formula = opts.formula != null
    ? opts.formula
    : isFormulaImageNode(node, src, naturalSize || (naturalW && naturalH ? { width: naturalW, height: naturalH } : null))

  if (formula) {
    const cssHeight = parseCssPx(style.height, 0)
    const targetHeight = cssHeight > 0 ? cssHeight : BODY_LINE_HEIGHT_PX
    if (naturalW > 0 && naturalH > 0) {
      if (naturalH <= targetHeight * 1.15) {
        return { width: naturalW, height: naturalH }
      }
      const scale = targetHeight / naturalH
      return {
        width: Math.max(1, Math.round(naturalW * scale)),
        height: Math.max(1, Math.round(naturalH * scale))
      }
    }
    return { width: Math.round(targetHeight * 2.2), height: targetHeight }
  }

  if (naturalW > 0 && naturalH > 0) {
    const cssW = parseCssPx(style.width, 0)
    const cssH = parseCssPx(style.height, 0)
    if (cssW > 0 || cssH > 0) {
      if (cssW > 0 && cssH > 0) return { width: cssW, height: cssH }
      if (cssW > 0) {
        return { width: cssW, height: Math.max(1, Math.round(naturalH * (cssW / naturalW))) }
      }
      return { width: Math.max(1, Math.round(naturalW * (cssH / naturalH))), height: cssH }
    }
    return scaleToBox(naturalW, naturalH, MAX_FIGURE_WIDTH, MAX_FIGURE_HEIGHT)
  }

  return { width: MAX_FIGURE_WIDTH, height: MAX_FIGURE_HEIGHT }
}

export function isInlineImageParagraph(node) {
  if (!node || node.nodeType !== Node.ELEMENT_NODE) return false
  const tag = node.tagName.toLowerCase()
  if (tag !== 'p' && tag !== 'div') return false
  const elements = Array.from(node.childNodes).filter(n => {
    if (n.nodeType === Node.TEXT_NODE) return (n.textContent || '').trim().length > 0
    return n.nodeType === Node.ELEMENT_NODE
  })
  return elements.length === 1
    && elements[0].nodeType === Node.ELEMENT_NODE
    && elements[0].tagName.toLowerCase() === 'img'
}
