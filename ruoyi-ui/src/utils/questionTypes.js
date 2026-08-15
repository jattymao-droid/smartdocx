/** Canonical question types for question bank (aligned with EduQbConstants). */

import { questionTypeOptions as fetchQuestionTypeOptions } from '@/api/education/questionType'

export const QUESTION_TYPE_OPTIONS = [
  { label: '\u5355\u9009', value: 'single', answerMode: 'choice' },
  { label: '\u591a\u9009', value: 'multi', answerMode: 'multi' },
  { label: '\u586b\u7a7a', value: 'fill', answerMode: 'fill' },
  { label: '\u5b9e\u9a8c', value: 'experiment', answerMode: 'subjective' },
  { label: '\u89e3\u7b54', value: 'answer', answerMode: 'subjective' },
  { label: '\u7efc\u5408', value: 'comprehensive', answerMode: 'subjective' },
  { label: '\u9605\u8bfb', value: 'reading', answerMode: 'subjective', contentMaxLen: 10000 },
  { label: '\u5224\u65ad', value: 'judge', answerMode: 'judge' },
  { label: '\u4f5c\u56fe', value: 'drawing', answerMode: 'subjective' },
  { label: '\u77e5\u8bc6\u586b\u7a7a', value: 'knowledge_fill', answerMode: 'fill' },
  { label: '\u7b80\u7b54', value: 'short', answerMode: 'subjective' }
]

export const QUESTION_TYPE_MAP = QUESTION_TYPE_OPTIONS.reduce((map, item) => {
  map[item.value] = item.label
  return map
}, {})

export const CHOICE_QUESTION_TYPES = ['single', 'multi']

export const ANSWER_MODE_OPTIONS = [
  { label: '\u5355\u9009', value: 'choice' },
  { label: '\u591a\u9009', value: 'multi' },
  { label: '\u5224\u65ad', value: 'judge' },
  { label: '\u586b\u7a7a', value: 'fill' },
  { label: '\u4e3b\u89c2\u9898', value: 'subjective' }
]

export const ANSWER_MODE_LABELS = ANSWER_MODE_OPTIONS.reduce((map, item) => {
  map[item.value] = item.label
  return map
}, {})

let cachedOptions = null
let cachedMap = null
let loadPromise = null

function mapApiRow(row) {
  return {
    typeId: row.typeId,
    label: row.typeName,
    value: row.typeCode,
    answerMode: row.answerMode,
    contentMaxLen: row.contentMaxLen,
    builtin: row.builtin === '1',
    orderNum: row.orderNum,
    status: row.status
  }
}

function rebuildMap(options) {
  cachedMap = options.reduce((map, item) => {
    map[item.value] = item.label
    return map
  }, {})
}

function resolveFallbackOptions() {
  return QUESTION_TYPE_OPTIONS.slice()
}

export async function loadQuestionTypeOptions(force = false) {
  if (!force && cachedOptions) {
    return cachedOptions
  }
  if (!force && loadPromise) {
    return loadPromise
  }
  loadPromise = (async () => {
    try {
      const res = await fetchQuestionTypeOptions()
      const rows = res.data || []
      if (rows.length) {
        cachedOptions = rows.map(mapApiRow)
        rebuildMap(cachedOptions)
        return cachedOptions
      }
    } catch (e) {
      // fallback when API unavailable
    }
    cachedOptions = resolveFallbackOptions()
    rebuildMap(cachedOptions)
    return cachedOptions
  })()
  try {
    return await loadPromise
  } finally {
    loadPromise = null
  }
}

export function clearQuestionTypeCache() {
  cachedOptions = null
  cachedMap = null
  loadPromise = null
}

export function getCachedQuestionTypeOptions() {
  return cachedOptions || resolveFallbackOptions()
}

export function getQuestionTypeOrder() {
  return getCachedQuestionTypeOptions().map(item => item.value)
}

export function getDefaultQuestionType() {
  const options = getCachedQuestionTypeOptions()
  return options.length ? options[0].value : 'single'
}

export function getQuestionTypeLabel(value) {
  return (cachedMap && cachedMap[value]) || QUESTION_TYPE_MAP[value] || value || '-'
}

export function getAnswerModeForType(type) {
  if (cachedOptions) {
    const item = cachedOptions.find(t => t.value === type)
    if (item && item.answerMode) {
      return item.answerMode
    }
  }
  const fallback = QUESTION_TYPE_OPTIONS.find(t => t.value === type)
  return fallback ? fallback.answerMode : null
}

export function isChoiceQuestionType(type) {
  const mode = getAnswerModeForType(type)
  if (mode) {
    return mode === 'choice' || mode === 'multi'
  }
  return CHOICE_QUESTION_TYPES.includes(type)
}

export function isJudgeQuestionType(type) {
  return getAnswerModeForType(type) === 'judge'
}

export function isFillQuestionType(type) {
  return getAnswerModeForType(type) === 'fill'
}

export function isSubjectiveQuestionType(type) {
  return getAnswerModeForType(type) === 'subjective'
}

export function hasOptionsDisplayType(type) {
  const mode = getAnswerModeForType(type)
  return mode === 'choice' || mode === 'multi' || mode === 'judge'
}

export function isPaperChoiceVolumeType(type) {
  return hasOptionsDisplayType(type)
}

export const CONTENT_MAX_LENGTH = 2000
export const CONTENT_MAX_LENGTH_READING = 20000
export const CONTENT_MAX_LENGTH_HTML = 20000

export function getContentMaxLength(questionType, content) {
  if (content && /<(table|img|p|div|span|tbody|tr|td|sub|sup|br)\b/i.test(String(content))) {
    return CONTENT_MAX_LENGTH_HTML
  }
  if (cachedOptions) {
    const item = cachedOptions.find(t => t.value === questionType)
    if (item && item.contentMaxLen) {
      return item.contentMaxLen
    }
  }
  const fallback = QUESTION_TYPE_OPTIONS.find(t => t.value === questionType)
  if (fallback && fallback.contentMaxLen) {
    return fallback.contentMaxLen
  }
  return questionType === 'reading' ? CONTENT_MAX_LENGTH_READING : CONTENT_MAX_LENGTH
}

export function sortTypeCodes(codes) {
  const order = getQuestionTypeOrder()
  const rank = {}
  order.forEach((code, index) => {
    rank[code] = index
  })
  return [...codes].sort((a, b) => {
    const ra = rank[a] != null ? rank[a] : 9999
    const rb = rank[b] != null ? rank[b] : 9999
    return ra - rb
  })
}

export function groupItemsByQuestionType(items, getType = item => item.questionType) {
  const order = getQuestionTypeOrder()
  const map = {}
  items.forEach(item => {
    const type = getType(item) || getDefaultQuestionType()
    if (!map[type]) {
      map[type] = {
        type,
        label: getQuestionTypeLabel(type),
        items: []
      }
    }
    map[type].items.push(item)
  })
  const known = order.filter(type => map[type]).map(type => map[type])
  const extra = Object.keys(map)
    .filter(type => !order.includes(type))
    .sort()
    .map(type => map[type])
  return [...known, ...extra]
}
