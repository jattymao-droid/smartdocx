import katex from 'katex'
import 'katex/dist/katex.min.css'
import 'katex/dist/contrib/mhchem.js'
import Quill from 'quill'
import Delta from 'quill-delta'

const INLINE_FORMULA_RE = /\$([^$\n]+?)\$/g

/** Quill built-in formula blot requires window.katex. */
export function ensureKatexForQuill() {
  if (!window.katex) {
    window.katex = katex
  }
}

/** Parse storage text ($...$) into a Quill delta with rendered formula embeds. */
export function textToQuillDelta(text) {
  const delta = new Delta()
  const input = text == null ? '' : String(text)
  if (!input) return delta

  let lastIndex = 0
  let match
  INLINE_FORMULA_RE.lastIndex = 0
  while ((match = INLINE_FORMULA_RE.exec(input)) !== null) {
    if (match.index > lastIndex) {
      delta.insert(input.slice(lastIndex, match.index))
    }
    const latex = (match[1] || '').trim()
    if (latex) {
      delta.insert({ formula: latex })
    } else if (match[0]) {
      delta.insert(match[0])
    }
    lastIndex = match.index + match[0].length
  }
  if (lastIndex < input.length) {
    delta.insert(input.slice(lastIndex))
  }
  return delta
}

/** Serialize Quill contents back to storage text with $...$ wrappers. */
export function quillContentsToText(contents) {
  if (!contents || !Array.isArray(contents.ops)) return ''
  let result = ''
  contents.ops.forEach(op => {
    if (typeof op.insert === 'string') {
      result += op.insert
      return
    }
    if (op.insert && op.insert.formula != null) {
      result += `$${op.insert.formula}$`
    }
  })
  return result.replace(/\n$/, '')
}

export { Delta }
