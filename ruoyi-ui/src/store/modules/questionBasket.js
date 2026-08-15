import { AUTO_PAPER_KEY, loadPaperDraft, savePaperDraft } from '@/utils/questionBasketPrefs'
import { htmlToPlainText, isQuestionHtml } from '@/utils/questionContent'

const MAX_SIZE = 200
const STORAGE_KEY = 'edu_qb_basket'

function loadFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch (e) {
    return []
  }
}

function saveToStorage(items) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(items))
}

function normalizeOrder(items) {
  return items.map((item, idx) => ({ ...item, orderNum: idx + 1 }))
}

export function contentBrief(content, max = 40) {
  if (!content) return ''
  const raw = isQuestionHtml(content) ? htmlToPlainText(content) : String(content)
  const text = raw.replace(/\s+/g, ' ').trim()
  return text.length > max ? text.slice(0, max) + '...' : text
}

const state = {
  items: loadFromStorage()
}

const getters = {
  basketCount: state => state.items.length,
  basketTotalScore: state => state.items.reduce((sum, i) => sum + (Number(i.scoreValue) || 0), 0),
  basketItems: state => state.items
}

const mutations = {
  SET_ITEMS(state, items) {
    state.items = normalizeOrder(items)
    saveToStorage(state.items)
  },
  ADD_ITEMS(state, newItems) {
    const existingIds = new Set(state.items.map(i => (i.questionId != null ? Number(i.questionId) : i.questionId)))
    const merged = [...state.items]
    newItems.forEach(q => {
      const questionId = q.questionId != null ? Number(q.questionId) : q.questionId
      if (questionId == null || existingIds.has(questionId)) {
        return
      }
      merged.push({
        questionId,
        questionCode: q.questionCode,
        content: q.content || '',
        contentBrief: q.contentBrief || contentBrief(q.content),
        questionType: q.questionType,
        difficulty: q.difficulty,
        options: q.options != null ? q.options : null,
        images: q.images != null ? q.images : null,
        scoreValue: q.scoreValue != null ? Number(q.scoreValue) : 5,
        orderNum: 0
      })
      existingIds.add(q.questionId)
    })
    state.items = normalizeOrder(merged)
    saveToStorage(state.items)
  },
  REMOVE(state, questionId) {
    state.items = normalizeOrder(state.items.filter(i => i.questionId !== questionId))
    saveToStorage(state.items)
  },
  REMOVE_BATCH(state, questionIds) {
    const set = new Set(Array.isArray(questionIds) ? questionIds : [])
    state.items = normalizeOrder(state.items.filter(i => !set.has(i.questionId)))
    saveToStorage(state.items)
  },
  REMOVE_BY_TYPE(state, questionType) {
    state.items = normalizeOrder(state.items.filter(i => i.questionType !== questionType))
    saveToStorage(state.items)
  },
  REORDER(state, items) {
    state.items = normalizeOrder(items)
    saveToStorage(state.items)
  },
  SET_SCORE(state, { questionId, scoreValue }) {
    const item = state.items.find(i => i.questionId === questionId)
    if (item) {
      item.scoreValue = Number(scoreValue) || 0
      saveToStorage(state.items)
    }
  },
  BATCH_SET_SCORE(state, scoreValue) {
    state.items.forEach(i => { i.scoreValue = Number(scoreValue) || 0 })
    saveToStorage(state.items)
  },
  CLEAR(state) {
    state.items = []
    saveToStorage(state.items)
  },
  REPLACE(state, { oldQuestionId, newQuestion }) {
    const idx = state.items.findIndex(i => i.questionId === oldQuestionId)
    if (idx < 0 || !newQuestion) return
    const old = state.items[idx]
    state.items[idx] = {
      questionId: newQuestion.questionId,
      questionCode: newQuestion.questionCode,
      content: newQuestion.content || '',
      contentBrief: contentBrief(newQuestion.content),
      questionType: newQuestion.questionType,
      difficulty: newQuestion.difficulty,
      options: newQuestion.options != null ? newQuestion.options : null,
      images: newQuestion.images != null ? newQuestion.images : null,
      scoreValue: old.scoreValue != null ? old.scoreValue : 5,
      orderNum: old.orderNum
    }
    state.items = normalizeOrder(state.items)
    saveToStorage(state.items)
  },
  SET_SCORE_BY_TYPE(state, { questionType, scoreValue }) {
    const score = Number(scoreValue) || 0
    state.items.forEach(i => {
      if (i.questionType === questionType) {
        i.scoreValue = score
      }
    })
    saveToStorage(state.items)
  },
  DISTRIBUTE_SCORE(state, total) {
    const target = Math.max(0, Number(total) || 0)
    const items = state.items
    if (!items.length) return
    const base = Math.floor(target / items.length)
    let remainder = target - base * items.length
    items.forEach((item, idx) => {
      item.scoreValue = base + (idx < remainder ? 1 : 0)
    })
    saveToStorage(state.items)
  }
}

const actions = {
  addQuestions({ state, commit }, questions) {
    const list = Array.isArray(questions) ? questions : [questions]
    const normalizeId = id => (id != null ? Number(id) : id)
    const newCount = list.filter(q => !state.items.some(i => normalizeId(i.questionId) === normalizeId(q.questionId))).length
    if (state.items.length + newCount > MAX_SIZE) {
      return Promise.reject(new Error('OVER_LIMIT'))
    }
    commit('ADD_ITEMS', list)
    try {
      if (localStorage.getItem(AUTO_PAPER_KEY) === '1' && newCount > 0) {
        const draft = loadPaperDraft() || { items: [], header: {}, exportConfig: {} }
        const existingIds = new Set((draft.items || []).map(i => i.questionId))
        const merged = [...(draft.items || [])]
        list.forEach(q => {
          if (!existingIds.has(q.questionId)) {
            merged.push({
              questionId: q.questionId,
              questionCode: q.questionCode,
              content: q.content || '',
              contentBrief: q.contentBrief || contentBrief(q.content),
              questionType: q.questionType,
              difficulty: q.difficulty,
              options: q.options != null ? q.options : null,
              images: q.images != null ? q.images : null,
              scoreValue: 5,
              orderNum: merged.length + 1
            })
            existingIds.add(q.questionId)
          }
        })
        savePaperDraft({ ...draft, items: merged.map((item, idx) => ({ ...item, orderNum: idx + 1 })) })
      }
    } catch (e) { /* ignore */ }
    return Promise.resolve(newCount)
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}

export { MAX_SIZE }
