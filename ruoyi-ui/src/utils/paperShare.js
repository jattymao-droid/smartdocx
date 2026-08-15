import { createPaperShare } from '@/api/education/paper'
import { savePaperShare, generateShareId } from '@/utils/questionBasketPrefs'

/** Server share ids are 32-char hex (UUID without dashes). */
const SERVER_SHARE_ID_RE = /^[a-f0-9]{32}$/i

export function isServerShareId(id) {
  return SERVER_SHARE_ID_RE.test(String(id || ''))
}

export function buildPaperShareUrl(path, shareId) {
  const base = window.location.origin
  return `${base}${path}?share=${encodeURIComponent(shareId)}`
}

export function cachePaperShareLocally(id, snapshot) {
  if (id && snapshot) {
    savePaperShare(id, snapshot)
  }
}

export function createLocalPaperShare(snapshot) {
  const id = generateShareId()
  cachePaperShareLocally(id, snapshot)
  return id
}

/**
 * Create a server-side share snapshot (cross-device).
 */
export function createServerPaperShare(snapshot) {
  return createPaperShare(snapshot).then(res => {
    const id = res && res.data
    if (!id) {
      return Promise.reject(new Error('empty share id'))
    }
    cachePaperShareLocally(id, snapshot)
    return id
  })
}
