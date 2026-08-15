export const PAPER_DRAFT_KEY = 'edu_qb_paper_draft'
export const AUTO_PAPER_KEY = 'edu_qb_basket_auto_paper'
export const LAST_SUBJECT_KEY = 'edu_qb_last_subject'
export const PAPER_SHARE_PREFIX = 'edu_qb_paper_share_'

export function saveLastSubject(subject) {
  if (!subject || !subject.subjectName) return
  try {
    localStorage.setItem(LAST_SUBJECT_KEY, JSON.stringify({
      subjectId: subject.subjectId,
      subjectName: subject.subjectName
    }))
  } catch (e) { /* ignore */ }
}

export function loadLastSubjectName() {
  try {
    const raw = localStorage.getItem(LAST_SUBJECT_KEY)
    if (!raw) return ''
    const data = JSON.parse(raw)
    return String(data?.subjectName || '').trim()
  } catch (e) {
    return ''
  }
}

export function loadPaperDraft() {
  try {
    const raw = localStorage.getItem(PAPER_DRAFT_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

export function savePaperDraft(draft) {
  try {
    localStorage.setItem(PAPER_DRAFT_KEY, JSON.stringify(draft))
  } catch (e) { /* ignore */ }
}

export function clearPaperDraft() {
  try {
    localStorage.removeItem(PAPER_DRAFT_KEY)
  } catch (e) { /* ignore */ }
}

export function generateShareId() {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 8)
}

export function savePaperShare(id, snapshot) {
  if (!id || !snapshot) return
  try {
    localStorage.setItem(PAPER_SHARE_PREFIX + id, JSON.stringify(snapshot))
  } catch (e) { /* ignore */ }
}

export function loadPaperShare(id) {
  if (!id) return null
  try {
    const raw = localStorage.getItem(PAPER_SHARE_PREFIX + id)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}
