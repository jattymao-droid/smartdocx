import katex from 'katex'

const CJK_RE = /[\u4e00-\u9fff]/

function escapeHtml(text) {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

/** Wrap Chinese subscripts/superscripts for KaTeX, e.g. t_{上} -> t_{\text{上}} */
export function preprocessAiLatex(latex) {
  let s = String(latex || '').trim()
  if (!s) return ''

  s = s.replace(/(_|\^)\{([^{}]*)\}/g, (match, op, inner) => {
    if (!CJK_RE.test(inner) || /\\text\{/.test(inner) || /\\mathrm\{/.test(inner)) {
      return match
    }
    return `${op}{\\text{${inner}}}`
  })

  return s
}

export function renderAiKatex(latex, displayMode = false) {
  const prepared = preprocessAiLatex(latex)
  if (!prepared) return ''
  try {
    return katex.renderToString(prepared, {
      displayMode,
      throwOnError: false,
      strict: 'ignore',
      trust: true,
      output: 'html'
    })
  } catch (e) {
    return escapeHtml(latex)
  }
}

function findAiDollarSegments(input) {
  const text = String(input || '')
  const segments = []
  let i = 0

  while (i < text.length) {
    if (text.startsWith('$$', i)) {
      const close = text.indexOf('$$', i + 2)
      if (close === -1) break
      const inner = text.slice(i + 2, close).trim()
      if (inner) {
        segments.push({ start: i, end: close + 2, inner, display: true })
      }
      i = close + 2
      continue
    }
    if (text[i] === '$') {
      const close = text.indexOf('$', i + 1)
      if (close === -1) break
      const inner = text.slice(i + 1, close).trim()
      if (inner) {
        segments.push({ start: i, end: close + 1, inner, display: false })
      }
      i = close + 1
      continue
    }
    i += 1
  }

  return segments
}

function renderBoldSegments(text) {
  const input = String(text || '')
  const parts = input.split(/(\*\*[^*\n]+\*\*)/g)
  return parts.map(part => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return `<strong>${renderAiFormulaInline(part.slice(2, -2))}</strong>`
    }
    return renderAiFormulaInline(part)
  }).join('')
}

/** Render inline / mixed text with $...$ and $$...$$ (AI tutor, allows Chinese in math). */
export function renderAiFormulaInline(text) {
  const input = String(text || '')
  if (!input) return ''

  const segments = findAiDollarSegments(input)
  if (!segments.length) {
    return escapeHtml(input)
  }

  const parts = []
  let last = 0
  segments.forEach(seg => {
    if (seg.start > last) {
      parts.push(escapeHtml(input.slice(last, seg.start)))
    }
    if (seg.display) {
      parts.push(`<span class="ai-formula-inline-block">${renderAiKatex(seg.inner, true)}</span>`)
    } else {
      parts.push(renderAiKatex(seg.inner, false))
    }
    last = seg.end
  })
  if (last < input.length) {
    parts.push(escapeHtml(input.slice(last)))
  }
  return parts.join('')
}

/** Full-line display formula: $$ ... $$ */
export function renderAiFormulaBlock(line) {
  const trimmed = String(line || '').trim()
  const match = trimmed.match(/^\$\$([\s\S]+)\$\$$/)
  if (!match) return ''
  return `<div class="ai-formula-block">${renderAiKatex(match[1], true)}</div>`
}

export function isAiFormulaBlockLine(line) {
  return /^\$\$[\s\S]+\$\$$/.test(String(line || '').trim())
}

export { renderBoldSegments }
