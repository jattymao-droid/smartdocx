import katex from 'katex'
import 'katex/dist/katex.min.css'
import 'katex/dist/contrib/mhchem.js'

const DELIMITER_PATTERN = /(\$\$[\s\S]+?\$\$|\$[^$\n]+?\$|\\\[[\s\S]+?\\\]|\\\([\s\S]+?\\\))/g

const LATEX_CMD =
  /\\(?:ce|frac|sqrt|sum|int|lim|prod|vec|overrightarrow|overleftarrow|mathcal|mathrm|mathbf|mathit|mathbb|overline|underline|left|right|text|hat|bar|tilde|dot|ddot|sin|cos|tan|cot|sec|csc|log|ln|exp|min|max|sup|inf|det|dim|gcd|alpha|beta|gamma|delta|epsilon|varepsilon|zeta|eta|theta|vartheta|iota|kappa|lambda|mu|nu|xi|pi|rho|sigma|tau|upsilon|phi|varphi|chi|psi|omega|Gamma|Delta|Theta|Lambda|Xi|Pi|Sigma|Phi|Psi|Omega|partial|nabla|forall|exists|in|notin|subset|supset|cup|cap|emptyset|infty|dots|cdots|vdots|ddots|ge|le|ne|leq|geq|neq|gg|ll|approx|equiv|sim|simeq|propto|perp|parallel|angle|triangle|circ|quad|qquad|displaystyle|begin|end|not|pm|mp|cdot|times|div|to|rightarrow|leftarrow|leftrightarrow|Rightarrow|Leftarrow|Leftrightarrow|uparrow|downarrow|updownarrow|Big|big|bigg|Bigg)\b(?:\*?(?:\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\})*(?:_\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}|\^\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}|_[A-Za-z0-9]|\^[A-Za-z0-9])*)*/g

const UNICODE_TO_LATEX = [
  [/\u2192/g, '\\rightarrow '],
  [/\u2190/g, '\\leftarrow '],
  [/\u2194/g, '\\leftrightarrow '],
  [/\u21d2/g, '\\Rightarrow '],
  [/\u2260/g, '\\ne '],
  [/\u2264/g, '\\le '],
  [/\u2265/g, '\\ge '],
  [/\u226b/g, '\\gg '],
  [/\u226a/g, '\\ll '],
  [/\u00b7/g, '\\cdot '],
  [/\u2211/g, '\\sum '],
  [/\u222b/g, '\\int '],
  [/\u221e/g, '\\infty '],
  [/\u03bb/g, '\\lambda '],
  [/\u03bc/g, '\\mu '],
  [/\u03b8/g, '\\theta '],
  [/\u03a6/g, '\\Phi '],
  [/\u0394/g, '\\Delta '],
  [/\u221a/g, '\\sqrt '],
  [/\u00b1/g, '\\pm '],
  [/\u00d7/g, '\\times '],
  [/\u00f7/g, '\\div ']
]

const COMPARISON_CHARS = /[=<>\u2264\u2265\u2260\u226b\u226a]/
const MATH_SYMBOL_CHARS = /[A-Za-z0-9=+\-*/^_{}\\().\[\]|\\,\s\u2192\u2190\u2194\u2260\u2264\u2265\u221e\u2211\u222b\u221a\u03bb\u03bc\u03b8\u03a6\u0394\u2207\u2202\u2208\u2200\u2203\u00b7\u00d7\u00f7\u00b1]/

function escapeHtml(text) {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function countChinese(text) {
  return (String(text).match(/[\u4e00-\u9fff]/g) || []).length
}

function normalizeUnicodeMath(text) {
  let result = String(text)
  UNICODE_TO_LATEX.forEach(([pattern, replacement]) => {
    result = result.replace(pattern, replacement)
  })
  return result
}

/** Repair broken OCR formula fragments before KaTeX rendering. */
function repairOcrFormulaFragments(text) {
  let s = String(text || '')
  s = s.replace(
    /[A-Za-z]+\{\d+__\\frac\{([A-Za-z]*)(\d+)\}\{([^}]+)\}/g,
    (_, a, b, c) => `\\frac{${a}^{${b}}}{${c}}`
  )
  s = s.replace(/^[A-Za-z]+\{\d+__\s*/gm, '')
  s = s.replace(/\\frac\{([A-Za-z])_(\d+)\}\{\((\d+)\)\}/g, (_, a, b, c) => `\\frac{${a}_${b}}{${a}_${c}}`)
  s = s.replace(/\\frac\{t(\d+)\}\{\((\d+)\)\}/g, (_, a, b) => `\\frac{t_${a}}{t_${b}}`)
  s = s.replace(/\\frac\{([A-Za-z]+)(\d+)\}\{\((\d+)\)\}/g, (_, a, b, c) => `\\frac{${a}_${b}}{${a}_${c}}`)
  s = s.replace(/\\frac\{([^}]+)\}\{([^}]+)\}___+/g, (_, a, b) => `\\frac{${a}}{${b}}`)
  s = s.replace(/\\frac\{([^}]*?)___([^}]*)\}/g, (_, a, b) => `\\frac{${a}${b}}`)
  s = s.replace(/\\frac\{([A-Za-z])(\d+)\}\{([^}]+)\}/g, (_, a, b, c) => `\\frac{${a}_${b}}{${c}}`)
  s = s.replace(/\\frac\{([A-Za-z]{2,})(\d+)\}\{([^}]+)\}/g, (match, a, b, c) => (
    /[\^_]/.test(a) ? match : `\\frac{${a}^{${b}}}{${c}}`
  ))
  s = s.replace(/([A-Za-z])\{(\d+)__/g, '$1^{$2}')
  return s
}

/** Clean Pix2Text / OCR LaTeX noise before rendering or storing. */
export function normalizeOcrLatex(text) {
  if (text == null || text === '') return ''
  let s = repairOcrFormulaFragments(String(text))
  s = s.replace(/\\!/g, '')
  s = s.replace(/\\operatorname\{([^}]*)\}/g, (_, inner) => {
    const compact = inner.replace(/\s+/g, '').toLowerCase()
    if (compact === 'cos') return '\\cos'
    if (compact === 'sin') return '\\sin'
    if (compact === 'tan') return '\\tan'
    if (compact === 'cot') return '\\cot'
    if (compact === 'log') return '\\log'
    if (compact === 'ln') return '\\ln'
    return `\\operatorname{${inner.replace(/\s+/g, '')}}`
  })
  s = s.replace(/\\textrm\{\.\}/g, '\u3001')
  s = s.replace(/\\text\{\.\}/g, '\u3001')
  s = s.replace(/\\textrm\{,\}/g, '\uFF0C')
  s = s.replace(/\\text\{,\}/g, '\uFF0C')
  s = s.replace(/_\{\\mathrm\{([A-Za-z0-9])\}\}/g, '_{$1}')
  s = s.replace(/\^\{\\mathrm\{([A-Za-z0-9])\}\}/g, '^{$1}')
  s = s.replace(/\\mathrm\{\s*\}/g, '')
  s = s.replace(/\\frac\{(\d)\}\{(\d)\}(?=\s*____|\s*$)/g, '____')
  s = s.replace(/摆长[1I|]=/g, '摆长l=')
  s = s.replace(/\\sin\s*\\theta/g, '\\sin\\theta')
  s = s.replace(/\\cos\s*\\theta/g, '\\cos\\theta')
  s = s.replace(/\\sim\\cdots\s*/g, '')
  s = s.replace(/\+\s*\\sim\\cdots/g, '')
  s = s.replace(/\s*\u56fe\s*\d+-\d+-\d+\s*/g, '')
  s = s.replace(/\\m_(\d)/g, 'm_$1')
  s = s.replace(/\uFF04/g, '$')
  s = s.replace(/=\s*=/g, '=')
  s = s.replace(/(_\{[^{}]+\}|_[A-Za-z0-9])\s+([a-zA-Z])/g, '$1$2')
  s = s.replace(/\s{2,}/g, ' ')
  return s.trim()
}

const OPTION_PREFIX_RE = /^([A-Da-d][\.\uFF0E\u3001\)\uFF09:]\s*)/

export function parseOcrLine(line) {
  const raw = normalizeOcrLatex(line || '')
  const m = raw.match(OPTION_PREFIX_RE)
  if (m) {
    return { prefix: m[1], body: raw.slice(m[1].length).trim() }
  }
  return { prefix: '', body: raw }
}

export function mergeOcrLine(prefix, mathLatex) {
  return normalizeOcrLatex((prefix || '') + (mathLatex || ''))
}

/** Wrap Chinese segments as \\text{} for MathLive visual editor. */
export function toMathLiveLatex(text) {
  const s = normalizeOcrLatex(text || '')
  if (!s) return ''
  if (!/[\u4e00-\u9fff\u3001\uFF0C]/.test(s)) return s
  const parts = []
  let buf = ''
  for (let i = 0; i < s.length; i++) {
    const ch = s[i]
    if (/[\u4e00-\u9fff\u3001\uFF0C]/.test(ch)) {
      if (buf) {
        parts.push(buf)
        buf = ''
      }
      let cn = ch
      while (i + 1 < s.length && /[\u4e00-\u9fff\u3001\uFF0C]/.test(s[i + 1])) {
        i += 1
        cn += s[i]
      }
      parts.push(`\\text{${cn}}`)
    } else {
      buf += ch
    }
  }
  if (buf) parts.push(buf)
  return parts.join('')
}

function extractOptionPrefix(line) {
  const parsed = parseOcrLine(line)
  return { prefix: parsed.prefix, body: parsed.body }
}

function isMathHeavyLine(text) {
  const t = String(text).trim()
  if (!t) return false
  if (/\\(?:sin|cos|tan|frac|sqrt|theta|mathrm)|[_^]\{|=/.test(t)) return true
  return (t.match(/[A-Za-z_{}\\^=+\-*/]/g) || []).length / t.length > 0.45
}

function containsOcrMath(text) {
  return /\\(?:frac|sqrt|sin|cos|tan|theta|mathrm|operatorname)|[_^]\{|=\s*[A-Za-z]/.test(String(text))
}

function renderLineAsKatexWithText(line) {
  const parts = []
  let buf = ''
  const input = String(line)
  for (let i = 0; i < input.length; i++) {
    const ch = input[i]
    if (/[\u4e00-\u9fff]/.test(ch)) {
      if (buf) {
        parts.push(buf)
        buf = ''
      }
      let cn = ch
      while (i + 1 < input.length && /[\u4e00-\u9fff]/.test(input[i + 1])) {
        i += 1
        cn += input[i]
      }
      parts.push(`\\text{${cn}}`)
    } else {
      buf += ch
    }
  }
  if (buf) parts.push(buf)
  return renderMath(parts.join(''), false)
}

function stripDelimiters(raw) {
  let latex = raw.trim()
  if (latex.startsWith('$$') && latex.endsWith('$$')) {
    return { latex: latex.slice(2, -2).trim(), displayMode: true }
  }
  if (latex.startsWith('$') && latex.endsWith('$')) {
    return { latex: latex.slice(1, -1).trim(), displayMode: false }
  }
  if (latex.startsWith('\\[') && latex.endsWith('\\]')) {
    return { latex: latex.slice(2, -2).trim(), displayMode: true }
  }
  if (latex.startsWith('\\(') && latex.endsWith('\\)')) {
    return { latex: latex.slice(2, -2).trim(), displayMode: false }
  }
  return { latex, displayMode: false }
}

function isChemicalEquation(text) {
  const t = String(text).trim()
  if (!t || /[\u4e00-\u9fff]/.test(t)) return false
  if (/\\(?:frac|sqrt|sum|int|lim|vec|mathcal|mathrm|mathbf|sin|cos|tan|log|ln|alpha|beta|gamma|theta|lambda|mu|pi|sigma|phi|omega)\b/.test(t)) {
    return false
  }
  const hasElement = /\d*[A-Z][a-z]?\d*/.test(t)
  const hasReaction = /(?:\+|->|\\rightarrow|\u2192|=\s*\d*[A-Z])/.test(t)
  return hasElement && hasReaction
}

function prepareLatex(text) {
  let raw = stripDelimiters(normalizeOcrLatex(text)).latex
  if (!raw) return ''
  if (!/\\[a-zA-Z]{2,}/.test(raw) && /[=+\-*/^_0-9A-Za-z]/.test(raw) && countChinese(raw) === 0) {
    raw = normalizePlainMathText(raw)
  } else {
    raw = normalizeUnicodeMath(raw)
  }
  if (!raw) return ''
  if (!/\\ce\{/.test(raw) && isChemicalEquation(raw)) {
    const ceBody = raw
      .replace(/\\rightarrow/g, '->')
      .replace(/\u2192/g, '->')
    return `\\ce{${ceBody}}`
  }
  return raw
}

function renderMath(latex, displayMode) {
  if (!latex) return ''
  try {
    return katex.renderToString(latex, {
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

/** Render a compact KaTeX icon for formula template buttons. */
export function renderFormulaPreview(latex) {
  return renderMath(latex, false)
}

function isDisplayFormulaLine(line) {
  const t = line.trim()
  if (!t) return false

  const chinese = countChinese(t)
  const ratio = chinese / t.length

  // Short equations (e.g. MC options) render inline, not centered display blocks
  if (t.length <= 120 && !/\\(?:frac|sqrt|sum|int|lim|prod|begin|left|right|ce\{)/.test(t)) {
    return false
  }

  if (/\\(?:frac|sqrt|sum|int|lim|prod|vec|ce|left|right|mathrm|mathbf|mathcal|overline|underline|begin|end|quad|qquad|displaystyle|dots|cdots|vdots|ddots|ge|le|ne|leq|geq|neq|infty|sin|cos|tan|log|ln|exp|pm|mp|cdot|times|div|alpha|beta|gamma|delta|theta|lambda|mu|pi|sigma|phi|omega|partial|nabla|forall|exists|to|rightarrow|leftarrow|approx|equiv|sim|propto|perp|parallel|angle|triangle|circ|hat|bar|tilde|dot|ddot)\b/.test(t)) {
    return ratio < 0.2
  }

  if (chinese === 0) {
    if (/\d*[A-Z][a-z]?\d*/.test(t) && /(?:\+|->|\\rightarrow|\u2192)/.test(t)) {
      return true
    }
    if (COMPARISON_CHARS.test(t) && /[A-Za-z^_{}\\]/.test(t)) {
      return true
    }
    const mathChars = (t.match(MATH_SYMBOL_CHARS) || []).length
    if (mathChars / t.length > 0.82 && t.length >= 3) {
      return true
    }
  }

  return false
}

function isValidInlineDelimitedFormula(inner) {
  const body = String(inner || '').trim()
  if (!body) return false
  if (countChinese(body) > 0) return false
  if (body.length > 100) return false
  if (/[??????????????]/.test(body)) return false
  return /[A-Za-z0-9\\^_{}+\-*/().=<>]/.test(body)
}

const DOCX_SUBSCRIPT_PATTERN = /[A-Za-z](?:_\{[^{}]+\}|_[A-Za-z0-9]|\^\{[^{}]+\}|\^[A-Za-z0-9])+/

function findClosingBrace(text, openBraceIndex) {
  let depth = 0
  for (let i = openBraceIndex; i < text.length; i += 1) {
    if (text[i] === '{') depth += 1
    else if (text[i] === '}') {
      depth -= 1
      if (depth === 0) return i
    }
  }
  return -1
}

function findDocxDelimitedSegments(line) {
  const segments = findDollarDelimitedSegments(line)
  const input = String(line)
  DELIMITER_PATTERN.lastIndex = 0
  let match
  while ((match = DELIMITER_PATTERN.exec(input)) !== null) {
    const raw = match[0]
    if (raw.startsWith('$')) continue
    const parsed = stripDelimiters(raw)
    if (!isValidInlineDelimitedFormula(parsed.latex)) continue
    segments.push({ start: match.index, end: match.index + raw.length, raw, kind: 'delimited' })
  }
  segments.sort((a, b) => a.start - b.start || b.end - a.end - (a.end - a.start))
  const merged = []
  segments.forEach(item => {
    if (merged.some(prev => item.start >= prev.start && item.end <= prev.end)) return
    merged.push(item)
  })
  return merged
}

function mergeDocxTextParts(parts) {
  const merged = []
  parts.forEach(part => {
    if (part.type === 'text') {
      const last = merged[merged.length - 1]
      if (last && last.type === 'text') last.content += part.content
      else merged.push({ type: 'text', content: part.content })
    } else {
      merged.push(part)
    }
  })
  return merged
}

function isExportableDocxLatex(latex) {
  const body = String(latex || '').trim()
  if (!body) return false
  if (countChinese(body) > 0) return false
  if (/\\text\{|\\mathrm\{/.test(body)) return false
  if (body.length > 120) return false
  return true
}

function pushDocxLatexPart(parts, latex) {
  const prepared = prepareLatex(latex)
  if (!prepared || !isExportableDocxLatex(prepared)) return
  parts.push({ type: 'latex', content: prepared })
}

/** Chinese + $...$: only explicit delimiters become Word math (mirror server export). */
function splitDocxDelimitedParts(line) {
  const segments = findDocxDelimitedSegments(line)
  if (!segments.length) return [{ type: 'text', content: line }]
  const parts = []
  let lastIndex = 0
  segments.forEach(segment => {
    if (segment.start > lastIndex) {
      parts.push({ type: 'text', content: line.slice(lastIndex, segment.start) })
    }
    pushDocxLatexPart(parts, stripDelimiters(segment.raw).latex)
    lastIndex = segment.end
  })
  if (lastIndex < line.length) {
    parts.push({ type: 'text', content: line.slice(lastIndex) })
  }
  return mergeDocxTextParts(parts)
}

function findNextTextCommandIndex(line, start) {
  const textIdx = line.indexOf('\\text{', start)
  const mathrmIdx = line.indexOf('\\mathrm{', start)
  const indices = [textIdx, mathrmIdx].filter(i => i >= 0)
  return indices.length ? Math.min(...indices) : -1
}

function findNextDocxSpecialIndex(input, start) {
  let next = input.length
  const textIdx = findNextTextCommandIndex(input, start)
  if (textIdx >= 0) next = Math.min(next, textIdx)

  const dollarIdx = input.indexOf('$', start)
  if (dollarIdx >= 0) next = Math.min(next, dollarIdx)

  const slice = input.slice(start)
  DELIMITER_PATTERN.lastIndex = 0
  const delim = DELIMITER_PATTERN.exec(slice)
  if (delim) next = Math.min(next, start + delim.index)

  LATEX_CMD.lastIndex = 0
  const cmd = LATEX_CMD.exec(slice)
  if (cmd) next = Math.min(next, start + cmd.index)

  DOCX_SUBSCRIPT_PATTERN.lastIndex = 0
  const sub = DOCX_SUBSCRIPT_PATTERN.exec(slice)
  if (sub) next = Math.min(next, start + sub.index)

  return next
}

function tryDocxTextCommand(input, index, parts) {
  for (const cmd of ['\\text{', '\\mathrm{']) {
    if (!input.startsWith(cmd, index)) continue
    const open = index + cmd.length - 1
    const close = findClosingBrace(input, open)
    if (close > open) {
      parts.push({ type: 'text', content: input.slice(open + 1, close) })
      return close + 1
    }
  }
  return -1
}

function tryDocxDollarAt(input, index, parts) {
  if (input[index] !== '$') return -1
  const local = findDollarDelimitedSegments(input.slice(index))
  if (!local.length || local[0].start !== 0) return -1
  const seg = local[0]
  pushDocxLatexPart(parts, stripDelimiters(seg.raw).latex)
  return index + seg.end
}

function tryDocxPatternAt(input, index, parts, pattern, allowChinese) {
  const slice = input.slice(index)
  pattern.lastIndex = 0
  const match = pattern.exec(slice)
  if (!match || match.index !== 0) return -1
  if (!allowChinese && countChinese(match[0]) > 0) return -1
  pushDocxLatexPart(parts, match[0])
  return index + match[0].length
}

function tryDocxBracketDelimiterAt(input, index, parts) {
  const slice = input.slice(index)
  DELIMITER_PATTERN.lastIndex = 0
  const match = DELIMITER_PATTERN.exec(slice)
  if (!match || match.index !== 0) return -1
  const raw = match[0]
  if (raw.startsWith('$')) return -1
  const parsed = stripDelimiters(raw)
  if (!isValidInlineDelimitedFormula(parsed.latex)) return -1
  pushDocxLatexPart(parts, parsed.latex)
  return index + raw.length
}

/** Walk mixed Chinese+math line: text stays text, fragments like m_1 / \\theta become Word math. */
function splitDocxWalkParts(line) {
  const parts = []
  const input = String(line)
  let index = 0

  while (index < input.length) {
    let nextIndex = tryDocxTextCommand(input, index, parts)
    if (nextIndex >= 0) {
      index = nextIndex
      continue
    }

    nextIndex = tryDocxDollarAt(input, index, parts)
    if (nextIndex >= 0) {
      index = nextIndex
      continue
    }

    nextIndex = tryDocxBracketDelimiterAt(input, index, parts)
    if (nextIndex >= 0) {
      index = nextIndex
      continue
    }

    nextIndex = tryDocxPatternAt(input, index, parts, LATEX_CMD, false)
    if (nextIndex >= 0) {
      index = nextIndex
      continue
    }

    nextIndex = tryDocxPatternAt(input, index, parts, DOCX_SUBSCRIPT_PATTERN, false)
    if (nextIndex >= 0) {
      index = nextIndex
      continue
    }

    const next = findNextDocxSpecialIndex(input, index)
    if (next > index) {
      parts.push({ type: 'text', content: input.slice(index, next) })
      index = next
    } else {
      parts.push({ type: 'text', content: input[index] })
      index += 1
    }
  }

  return mergeDocxTextParts(parts)
}

function hasDocxDollarMarkup(text) {
  return String(text).includes('$') || /\\\(|\\\[/.test(String(text))
}

function splitDocxLineParts(line) {
  const normalized = normalizeUnicodeMath(normalizeOcrLatex(line))
  if (countChinese(normalized) > 0) {
    if (hasDocxDollarMarkup(normalized)) return splitDocxDelimitedParts(normalized)
    return splitDocxWalkParts(normalized)
  }
  return splitAutoLineParts(normalized)
}

/** Pair $...$ left-to-right; reject spans that contain Chinese (malformed delimiters). */
function findDollarDelimitedSegments(input) {
  const segments = []
  const text = String(input)
  let i = 0
  while (i < text.length) {
    if (text[i] !== '$') {
      i += 1
      continue
    }
    if (text[i + 1] === '$') {
      const start = i
      const close = text.indexOf('$$', i + 2)
      if (close === -1) break
      const raw = text.slice(start, close + 2)
      const inner = text.slice(start + 2, close)
      if (isValidInlineDelimitedFormula(inner)) {
        segments.push({ start, end: close + 2, raw, kind: 'delimited' })
      }
      i = close + 2
      continue
    }
    const start = i
    let close = -1
    for (let j = i + 1; j < text.length; j += 1) {
      if (text[j] === '$') {
        close = j
        break
      }
    }
    if (close === -1) break
    const raw = text.slice(start, close + 1)
    const inner = text.slice(start + 1, close)
    if (isValidInlineDelimitedFormula(inner)) {
      segments.push({ start, end: close + 1, raw, kind: 'delimited' })
      i = close + 1
    } else {
      i += 1
    }
  }
  return segments
}

function findInlineFormulaSegments(line) {
  const segments = findDollarDelimitedSegments(line)
  const input = String(line)

  DELIMITER_PATTERN.lastIndex = 0
  let match
  while ((match = DELIMITER_PATTERN.exec(input)) !== null) {
    const raw = match[0]
    if (raw.startsWith('$')) continue
    const parsed = stripDelimiters(raw)
    if (!isValidInlineDelimitedFormula(parsed.latex)) continue
    segments.push({ start: match.index, end: match.index + raw.length, raw, kind: 'delimited' })
  }

  if (segments.length && /[\u4e00-\u9fff]/.test(input)) {
    segments.sort((a, b) => a.start - b.start || b.end - a.end - (a.end - a.start))
    const merged = []
    segments.forEach(item => {
      if (merged.some(prev => item.start >= prev.start && item.end <= prev.end)) return
      merged.push(item)
    })
    return merged
  }

  LATEX_CMD.lastIndex = 0
  while ((match = LATEX_CMD.exec(input)) !== null) {
    const start = match.index
    const end = start + match[0].length
    if (segments.some(item => start >= item.start && end <= item.end)) continue
    segments.push({ start, end, raw: match[0], kind: 'latex' })
  }

  const cePattern = /\\ce\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}/g
  while ((match = cePattern.exec(input)) !== null) {
    const start = match.index
    const end = start + match[0].length
    if (segments.some(item => start >= item.start && end <= item.end)) continue
    segments.push({ start, end, raw: match[0], kind: 'latex' })
  }

  const fracPattern = /\\frac\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}/g
  while ((match = fracPattern.exec(input)) !== null) {
    const start = match.index
    const end = start + match[0].length
    if (segments.some(item => !(end <= item.start || start >= item.end))) continue
    segments.push({ start, end, raw: match[0], kind: 'latex' })
  }

  const subSupPattern = /[A-Za-z](?:_\{[^{}]+\}|\^\{[^{}]+\}|_[A-Za-z0-9]|\^[A-Za-z0-9])+/g
  while ((match = subSupPattern.exec(input)) !== null) {
    const start = match.index
    const end = start + match[0].length
    if (segments.some(item => !(end <= item.start || start >= item.end))) continue
    segments.push({ start, end, raw: match[0], kind: 'math' })
  }

  const chemPattern = /\d*[A-Z][a-z]?\d*(?:\s*[+\-]\s*\d*[A-Z][a-z]?\d*)+\s*(?:->|\\rightarrow|\u2192)\s*\d*[A-Z][a-z]?\d*(?:\s*[+\-]\s*\d*[A-Z][a-z]?\d*)*/g
  while ((match = chemPattern.exec(input)) !== null) {
    const start = match.index
    const end = start + match[0].length
    if (segments.some(item => !(end <= item.start || start >= item.end))) continue
    segments.push({ start, end, raw: match[0], kind: 'chem' })
  }

  const inlineMathPattern = /[A-Za-z][A-Za-z0-9]*(?:_\{[^{}]+\}|_\w|\^\{[^{}]+\}|\^\w|\{[^{}]+\})+(?:\s*[=+\-]\s*[A-Za-z0-9\\^_{}+\-*/().\[\]|\\,\s]+)?/g
  while ((match = inlineMathPattern.exec(input)) !== null) {
    const start = match.index
    const end = start + match[0].length
    if (segments.some(item => !(end <= item.start || start >= item.end))) continue
    if (/[\u4e00-\u9fff]/.test(match[0])) continue
    if (!/[\^_\\=]/.test(match[0])) continue
    segments.push({ start, end, raw: match[0], kind: 'math' })
  }

  segments.sort((a, b) => a.start - b.start || b.end - a.end - (a.end - a.start))

  const merged = []
  segments.forEach(item => {
    if (merged.some(prev => item.start >= prev.start && item.end <= prev.end)) return
    merged.push(item)
  })

  return merged
}

function renderMixedLine(line) {
  const segments = findInlineFormulaSegments(line)
  if (!segments.length) {
    return escapeHtml(line)
  }

  const parts = []
  let lastIndex = 0
  segments.forEach(segment => {
    if (segment.start > lastIndex) {
      parts.push(escapeHtml(line.slice(lastIndex, segment.start)))
    }
    let latex = segment.raw
    if (segment.kind === 'delimited') {
      latex = stripDelimiters(segment.raw).latex
    } else if (segment.kind === 'chem') {
      latex = prepareLatex(segment.raw)
    } else {
      latex = prepareLatex(segment.raw)
    }
    parts.push(renderMath(latex, false))
    lastIndex = segment.end
  })

  if (lastIndex < line.length) {
    parts.push(escapeHtml(line.slice(lastIndex)))
  }

  return parts.join('')
}

/** Normalize OCR/plain physics math (e.g. FN =m1g-Fsin??) to LaTeX for export. */
export function normalizePlainMathText(text) {
  let s = String(text || '').trim()
  if (!s) return ''
  s = normalizeUnicodeMath(normalizeOcrLatex(s))
  s = s.replace(/\bFN\b/g, 'F_N')
  s = s.replace(/\bFf\b/g, 'F_f')
  s = s.replace(/\bm(\d+)g\b/g, 'm_$1 g')
  s = s.replace(/\bm(\d+)\s+g\b/g, 'm_$1 g')
  s = s.replace(/\bm(\d+)\b/g, 'm_$1')
  s = s.replace(/([A-Za-z0-9}])(sin|cos|tan|cot)(\u03b8|\\theta)?/gi, (_, base, fn, theta) => {
    const t = theta ? (String(theta).startsWith('\\') ? theta : '\\theta') : '\\theta'
    return `${base}\\${fn.toLowerCase()}${t}`
  })
  s = s.replace(/([A-Za-z0-9}])([+\-=])/g, '$1 $2')
  s = s.replace(/([+\-=])([A-Za-z0-9\\])/g, '$1 $2')
  return s.replace(/\s{2,}/g, ' ').trim()
}

function pushLatexPart(parts, latex) {
  const prepared = prepareLatex(latex)
  if (prepared) parts.push({ type: 'latex', content: prepared })
}

function splitMixedLineParts(line) {
  const segments = findInlineFormulaSegments(line)
  if (!segments.length) return [{ type: 'text', content: line }]
  const parts = []
  let lastIndex = 0
  segments.forEach(segment => {
    if (segment.start > lastIndex) {
      parts.push({ type: 'text', content: line.slice(lastIndex, segment.start) })
    }
    const latex = segment.kind === 'delimited' ? stripDelimiters(segment.raw).latex : segment.raw
    pushLatexPart(parts, latex)
    lastIndex = segment.end
  })
  if (lastIndex < line.length) {
    parts.push({ type: 'text', content: line.slice(lastIndex) })
  }
  return parts
}

function splitAutoLineParts(line) {
  const normalized = normalizeOcrLatex(line)
  const trimmed = normalized.trim()
  if (!trimmed) return []

  const { prefix, body } = extractOptionPrefix(trimmed)
  if (prefix && isMathHeavyLine(body)) {
    const prepared = prepareLatex(body)
    return prepared
      ? [{ type: 'text', content: prefix }, { type: 'latex', content: prepared }]
      : [{ type: 'text', content: line }]
  }

  if (containsOcrMath(trimmed)) {
    if (countChinese(trimmed) === 0) {
      const prepared = prepareLatex(trimmed)
      return prepared ? [{ type: 'latex', content: prepared }] : [{ type: 'text', content: line }]
    }
    return splitMixedLineParts(trimmed)
  }

  if (isDisplayFormulaLine(trimmed)) {
    const prepared = prepareLatex(trimmed)
    return prepared ? [{ type: 'latex', content: prepared }] : [{ type: 'text', content: line }]
  }

  return splitMixedLineParts(trimmed)
}

/** Split question text into plain text and LaTeX parts for Word native math export. */
export function splitTextIntoDocxParts(text) {
  if (text == null || text === '') return []
  const input = normalizeOcrLatex(String(text))
  const parts = []
  input.split('\n').forEach((line, idx) => {
    if (idx > 0) parts.push({ type: 'text', content: '\n' })
    if (!(line || '').trim()) return
    parts.push(...splitDocxLineParts(line))
  })
  return parts.filter(part => (part.type === 'latex' ? part.content : (part.content || '').length > 0))
}

export { isExportableDocxLatex }

function renderAutoLine(line) {
  const normalized = normalizeOcrLatex(line)
  const trimmed = normalized.trim()
  if (!trimmed) return ''

  const { prefix, body } = extractOptionPrefix(trimmed)
  if (prefix && isMathHeavyLine(body)) {
    return escapeHtml(prefix) + renderMath(prepareLatex(body), false)
  }

  if (containsOcrMath(trimmed)) {
    if (countChinese(trimmed) === 0) {
      if (isDisplayFormulaLine(trimmed)) {
        return renderMath(prepareLatex(trimmed), true)
      }
      return renderMath(prepareLatex(trimmed), false)
    }
    return renderMixedLine(trimmed)
  }

  if (isDisplayFormulaLine(trimmed)) {
    return renderMath(prepareLatex(trimmed), true)
  }

  return renderMixedLine(trimmed)
}

function renderTextPart(textPart) {
  if (!textPart) return ''
  return textPart.split('\n').map(line => {
    if (/[\u4e00-\u9fff]/.test(line) || /\$[^$\n]+?\$/.test(line)) {
      return renderMixedLine(line)
    }
    return renderAutoLine(line)
  }).join('<br>')
}

function renderWithDelimiters(input) {
  DELIMITER_PATTERN.lastIndex = 0
  const parts = []
  let lastIndex = 0
  let match

  while ((match = DELIMITER_PATTERN.exec(input)) !== null) {
    if (match.index > lastIndex) {
      parts.push(renderTextPart(input.slice(lastIndex, match.index)))
    }
    const parsed = stripDelimiters(match[0])
    parts.push(renderMath(prepareLatex(parsed.latex), parsed.displayMode))
    lastIndex = DELIMITER_PATTERN.lastIndex
  }

  if (lastIndex < input.length) {
    parts.push(renderTextPart(input.slice(lastIndex)))
  }

  return parts.join('')
}

function hasExplicitDelimiters(text) {
  DELIMITER_PATTERN.lastIndex = 0
  const found = DELIMITER_PATTERN.test(String(text))
  DELIMITER_PATTERN.lastIndex = 0
  return found
}

/**
 * Render question text with auto-detected formulas (no $ delimiters required).
 * Also supports legacy $...$, $$...$$, \(...\), \[...\], and \ce{...} (mhchem).
 */
export function renderFormulaText(text) {
  if (text == null || text === '') return ''
  const input = normalizeOcrLatex(String(text).replace(/<br\s*\/?>/gi, '\n'))

  const renderLine = line => {
    const trimmed = (line || '').trim()
    if (!trimmed) return ''
    if (/[\u4e00-\u9fff]/.test(trimmed) || /\$[^$\n]+?\$/.test(trimmed)) {
      return renderMixedLine(line)
    }
    return renderAutoLine(line)
  }

  if (hasExplicitDelimiters(input) && !/[\u4e00-\u9fff]/.test(input)) {
    return renderWithDelimiters(input)
  }

  return input.split('\n').map(renderLine).join('<br>')
}

export function hasFormula(text) {
  if (!text) return false
  const input = String(text)
  if (hasExplicitDelimiters(input)) return true
  if (/\\(?:ce|frac|sqrt|sum|int|lim|vec|mathcal|mathrm|mathbf|alpha|beta|gamma|theta|lambda|mu|pi|sigma|phi|omega|circ|cdot|times|div|ge|le|ne|infty)\b/.test(input)) {
    return true
  }
  if (/[A-Za-z](?:_\{[^{}]+\}|\^\{[^{}]+\}|_[A-Za-z0-9]|\^[A-Za-z0-9])/.test(input)) {
    return true
  }
  if (isChemicalEquation(input)) return true
  return input.split('\n').some(isDisplayFormulaLine)
}

const INLINE_IMAGE_RE = /!\[图(\d+)\]/g

/** Indices (0-based) of images referenced via ![图N] in question content. */
export function getReferencedImageIndices(text) {
  const refs = new Set()
  const input = String(text || '')
  let match
  INLINE_IMAGE_RE.lastIndex = 0
  while ((match = INLINE_IMAGE_RE.exec(input)) !== null) {
    const idx = parseInt(match[1], 10) - 1
    if (idx >= 0) refs.add(idx)
  }
  return refs
}

/** Split content into alternating text / image parts for inline rendering. */
export function splitContentImageParts(text, imageUrls) {
  const input = String(text || '')
  const urls = Array.isArray(imageUrls) ? imageUrls : []
  if (!input || !INLINE_IMAGE_RE.test(input)) {
    return [{ type: 'text', content: input }]
  }
  INLINE_IMAGE_RE.lastIndex = 0
  const parts = []
  let lastIndex = 0
  let match
  while ((match = INLINE_IMAGE_RE.exec(input)) !== null) {
    if (match.index > lastIndex) {
      parts.push({ type: 'text', content: input.slice(lastIndex, match.index) })
    }
    const idx = parseInt(match[1], 10) - 1
    const url = urls[idx]
    if (url) parts.push({ type: 'image', url, index: idx })
    lastIndex = match.index + match[0].length
  }
  if (lastIndex < input.length) {
    parts.push({ type: 'text', content: input.slice(lastIndex) })
  }
  return parts.length ? parts : [{ type: 'text', content: input }]
}
