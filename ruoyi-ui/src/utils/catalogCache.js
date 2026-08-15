const MAX_ENTRIES = 24

function trimCache(cache) {
  while (cache.size > MAX_ENTRIES) {
    const first = cache.keys().next().value
    cache.delete(first)
  }
}

const chapterTreeCache = new Map()
const versionCache = new Map()
const textbookCache = new Map()
let subjectCache = null

export function getChapterTreeCache(textbookId, subjectId) {
  return chapterTreeCache.get(`${textbookId}:${subjectId || ''}`) || null
}

export function setChapterTreeCache(textbookId, subjectId, data) {
  chapterTreeCache.set(`${textbookId}:${subjectId || ''}`, data)
  trimCache(chapterTreeCache)
}

export function getVersionCache(subjectId, schoolStage) {
  return versionCache.get(`${subjectId}:${schoolStage || ''}`) || null
}

export function setVersionCache(subjectId, schoolStage, data) {
  versionCache.set(`${subjectId}:${schoolStage || ''}`, data)
  trimCache(versionCache)
}

export function getTextbookCache(versionId) {
  return textbookCache.get(String(versionId)) || null
}

export function setTextbookCache(versionId, data) {
  textbookCache.set(String(versionId), data)
  trimCache(textbookCache)
}

export function getSubjectCache() {
  return subjectCache
}

export function setSubjectCache(data) {
  subjectCache = data
}
