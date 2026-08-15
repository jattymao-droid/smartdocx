/**
 * Normalize and format choice question answers (especially multi-select).
 */

const OPTION_LETTER = /^[A-H]$/

function coerceAnswerValue(raw) {
  if (raw == null || raw === '') return null
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw)
    } catch (e) {
      return raw
    }
  }
  return raw
}

function pushLetter(letters, letter) {
  const l = String(letter || '').trim().toUpperCase()
  if (OPTION_LETTER.test(l) && !letters.includes(l)) letters.push(l)
}

function expandAnswerToken(token, letters) {
  const s = String(token || '').trim().toUpperCase()
  if (!s) return
  if (OPTION_LETTER.test(s)) {
    pushLetter(letters, s)
    return
  }
  const splitParts = s.split(/[,\uFF0C\u3001\s]+/).filter(Boolean)
  if (splitParts.length > 1) {
    splitParts.forEach(part => expandAnswerToken(part, letters))
    return
  }
  const chars = s.replace(/[^A-H]/gi, '').toUpperCase().split('')
  if (chars.length && chars.every(c => OPTION_LETTER.test(c))) {
    chars.forEach(c => pushLetter(letters, c))
    return
  }
  pushLetter(letters, s)
}

export function parseMultiAnswerLetters(raw) {
  const val = coerceAnswerValue(raw)
  const letters = []
  if (val == null || val === '') return letters
  const items = Array.isArray(val) ? val : [val]
  items.forEach(item => expandAnswerToken(item, letters))
  return letters.sort()
}

export function formatMultiAnswerDisplay(raw, separator) {
  const sep = separator == null ? '\u3001' : separator
  return parseMultiAnswerLetters(raw).join(sep)
}

export function buildMultiAnswerJson(raw) {
  return JSON.stringify(parseMultiAnswerLetters(raw))
}

export function formatChoiceAnswer(questionType, raw, options) {
  const opts = options || {}
  const wrong = opts.wrongLabel == null ? '\u9519\u8bef' : opts.wrongLabel
  const right = opts.rightLabel == null ? '\u6b63\u786e' : opts.rightLabel
  const multiSep = opts.multiSeparator == null ? '\u3001' : opts.multiSeparator
  if (raw == null || raw === '') return ''
  const val = coerceAnswerValue(raw)
  if (questionType === 'judge') return String(val) === 'false' ? wrong : right
  if (questionType === 'multi') return formatMultiAnswerDisplay(val, multiSep)
  if (questionType === 'fill' && Array.isArray(val)) return val.join(' | ')
  return String(val).replace(/^"|"$/g, '')
}
