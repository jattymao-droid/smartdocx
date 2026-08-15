import { listLibraryCategories, listHotLibraryDownloads, listLibraryTopics } from '@/api/education/library'
import { subjectOptions } from '@/api/education/subject'
import { getSubjectCache, setSubjectCache } from '@/utils/catalogCache'

let categoriesPromise = null
let categoriesData = null
const hotDownloadsCache = new Map()
const HOT_DOWNLOADS_TTL = 2 * 60 * 1000
let topicsPromise = null
let topicsCache = null
const TOPICS_TTL = 2 * 60 * 1000
let topicsCacheAt = 0

export function fetchSubjectOptionsCached() {
  const cached = getSubjectCache()
  if (cached) {
    return Promise.resolve({ data: cached })
  }
  return subjectOptions().then(res => {
    const data = (res && res.data) || []
    setSubjectCache(data)
    return { data }
  })
}

export function fetchLibraryCategoriesCached() {
  if (categoriesData) {
    return Promise.resolve({ data: categoriesData })
  }
  if (!categoriesPromise) {
    categoriesPromise = listLibraryCategories()
      .then(res => {
        categoriesData = (res && (res.data || res.rows)) || []
        return { data: categoriesData }
      })
      .catch(err => {
        categoriesPromise = null
        throw err
      })
  }
  return categoriesPromise
}

export function fetchHotDownloadsCached(pageSize = 6) {
  const key = String(pageSize)
  const cached = hotDownloadsCache.get(key)
  if (cached && Date.now() - cached.at < HOT_DOWNLOADS_TTL) {
    return Promise.resolve(cached.data)
  }
  return listHotLibraryDownloads({ pageSize }).then(res => {
    hotDownloadsCache.set(key, { at: Date.now(), data: res })
    return res
  }).catch(err => {
    hotDownloadsCache.delete(key)
    throw err
  })
}

export function fetchLibraryTopicsCached(pageSize = 12) {
  const now = Date.now()
  if (topicsCache && now - topicsCacheAt < TOPICS_TTL) {
    return Promise.resolve(topicsCache.slice(0, pageSize))
  }
  if (!topicsPromise) {
    topicsPromise = listLibraryTopics({ portal: true, pageNum: 1, pageSize: 24 })
      .then(res => {
        topicsCache = (res && res.rows) || []
        topicsCacheAt = Date.now()
        return topicsCache
      })
      .catch(err => {
        topicsPromise = null
        throw err
      })
  }
  return topicsPromise.then(rows => rows.slice(0, pageSize))
}

export function clearMetaCache() {
  categoriesPromise = null
  categoriesData = null
  hotDownloadsCache.clear()
  topicsPromise = null
  topicsCache = null
  topicsCacheAt = 0
}
