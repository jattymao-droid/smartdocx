/**
 * Parse choice-option text and strip leading labels (A./B./A、 etc.)
 * so UI can render `{{ label }}.` without duplication.
 */

import { hasOptionsDisplayType } from '@/utils/questionTypes'
import { isQuestionHtml } from '@/utils/questionContent'

export const OPTION_LABELS = 'ABCDEFGH'.split('')

function optionPlainText(raw) {
  return String(raw ?? '')
    .replace(/<[^>]+>/g, '')
    .replace(/[\uFEFF\u200B]/g, '')
    .trim()
}

function stripLabelPrefix(plain) {
  const m = plain.match(/^([A-Ha-h])[\.\uFF0E\u3001\u3002\)\uFF09:：]\s*(.*)$/s)
  if (!m) return null
  return { label: m[1].toUpperCase(), text: (m[2] || '').trim() }
}

export function parseQuestionOption(raw, index = 0) {
  const original = String(raw ?? '').trim()
  if (!original) {
    return { label: OPTION_LABELS[index] || 'A', text: '' }
  }

  if (isQuestionHtml(original)) {
    const htmlLabel = original.match(/^([A-Ha-h])[\.\uFF0E\u3001\u3002\)\uFF09:：]\s*/i)
    if (htmlLabel) {
      return {
        label: htmlLabel[1].toUpperCase(),
        text: original.slice(htmlLabel[0].length).trim()
      }
    }
    return { label: OPTION_LABELS[index] || 'A', text: original }
  }

  const plain = optionPlainText(original)
  const first = stripLabelPrefix(plain)
  if (first) {
    const dup = stripLabelPrefix(first.text)
    if (dup && dup.label === first.label) {
      return { label: first.label, text: dup.text }
    }
    return first
  }

  const withSpace = plain.match(/^([A-Ha-h])\s+(.+)$/s)
  if (withSpace) {
    return {
      label: withSpace[1].toUpperCase(),
      text: withSpace[2].trim()
    }
  }

  return { label: OPTION_LABELS[index] || 'A', text: original }
}

const COMPOSITE_OPTION_TYPES = new Set(['comprehensive', 'experiment', 'reading', 'answer'])

export function shouldShowQuestionOptions(questionType, rawOptions) {
  if (questionType && hasOptionsDisplayType(questionType)) return true
  if (!questionType || !COMPOSITE_OPTION_TYPES.has(questionType)) return false
  return parseOptionsArray(rawOptions).length > 0
}

export function parseQuestionOptions(raw, questionType) {
  const arr = parseOptionsArray(raw)
  if (!arr.length) return []
  if (questionType && !shouldShowQuestionOptions(questionType, arr)) return []
  return arr.map((text, i) => parseQuestionOption(text, i))
}

function parseOptionsArray(raw) {
  if (!raw) return []
  if (Array.isArray(raw)) return raw
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

export function formatQuestionOption(label, text, index = 0) {
  const parsed = parseQuestionOption(text, index)
  const finalLabel = (label || parsed.label || OPTION_LABELS[index] || 'A').toUpperCase()
  const body = parsed.text.trim()
  return body ? `${finalLabel}.${body}` : `${finalLabel}.`
}
