import { buildPortalPdfFetchUrl } from '@/utils/portalBanner'

const OFFICE_EXTS = new Set([
  'doc', 'docx', 'wps', 'docm', 'xls', 'xlsx', 'csv', 'xlsm', 'ppt', 'pptx',
  'vsd', 'rtf', 'odt', 'dps', 'et', 'ods', 'odp', 'xlt', 'xltm', 'ett'
])

const TXT_EXTS = new Set(['txt', 'md', 'log', 'json', 'xml', 'html', 'htm'])

export function parseKkfileviewBase(previewUrl) {
  if (!previewUrl) return ''
  try {
    const u = new URL(previewUrl, window.location.origin)
    return u.origin + (u.pathname.startsWith('/') ? u.pathname.replace(/\/onlinePreview.*$/, '/') : '/')
  } catch (e) {
    return ''
  }
}

function base64Encode(text) {
  return btoa(unescape(encodeURIComponent(text)))
}

function encodeKkfileServePath(innerPath) {
  const normalized = String(innerPath || '').replace(/\\/g, '/').replace(/^\/+/, '')
  if (!normalized) return ''
  return normalized.split('/').map(seg => encodeURIComponent(seg)).join('/')
}

export function buildKkfileInnerPathUrl(kkBase, innerPath) {
  const base = String(kkBase || '').replace(/\/$/, '')
  const encodedPath = encodeKkfileServePath(innerPath)
  return encodedPath ? `${base}/${encodedPath}` : base
}

export function buildKkfileCorsUrl(kkBase, fileUrl) {
  const base = String(kkBase || '').replace(/\/$/, '')
  return `${base}/getCorsFile?urlPath=${encodeURIComponent(base64Encode(fileUrl))}`
}

/**
 * Archive inner Office files keep the extracted folder layout; kkFileView stores
 * converted PDF beside the source file (same inner path with .pdf extension).
 */
export function buildArchivePdfServePath(innerPath, ext) {
  const path = String(innerPath || '').replace(/\\/g, '/')
  const key = String(ext || '').toLowerCase()
  if (key === 'pdf') {
    return path
  }
  const dot = path.lastIndexOf('.')
  if (dot <= 0) return path
  return `${path.substring(0, dot)}.pdf`
}

export function buildArchivePdfPath(innerPath, ext) {
  return buildArchivePdfServePath(innerPath, ext)
}

function buildKkfileFileUrl(kkBase, servePath) {
  return buildKkfileInnerPathUrl(kkBase, servePath)
}

export function buildArchiveInnerFileUrl(kkBase, innerPath, ext) {
  const key = String(ext || '').toLowerCase()
  if (TXT_EXTS.has(key)) {
    return buildKkfileInnerPathUrl(kkBase, innerPath)
  }
  const servePath = buildArchivePdfServePath(innerPath, ext)
  return buildKkfileFileUrl(kkBase, servePath)
}

/** @deprecated use buildArchiveInnerFileUrl */
export function resolveArchiveInnerMediaUrl(kkBase, innerPath, ext) {
  return buildArchiveInnerFileUrl(kkBase, innerPath, ext)
}

export function buildArchiveInnerFetchUrl(kkBase, innerPath, ext) {
  return buildPortalPdfFetchUrl(buildArchiveInnerFileUrl(kkBase, innerPath, ext))
}

export function buildArchiveWarmupUrl(kkBase, innerPath, fileKey) {
  const base = String(kkBase || '').replace(/\/$/, '')
  const rawPath = String(innerPath || '').replace(/\\/g, '/').replace(/^\/+/, '')
  const innerUrl = `${base}/${rawPath}?fileKey=${encodeURIComponent(fileKey || '')}`
  return `${base}/onlinePreview?url=${encodeURIComponent(base64Encode(innerUrl))}&officePreviewType=pdf`
}

function resolvePortalWarmupUrl(kkPreviewUrl, apiBase = process.env.VUE_APP_BASE_API) {
  const base = String(apiBase || '').replace(/\/$/, '')
  return `${base}/system/portal/banner/archive-warmup?url=${encodeURIComponent(kkPreviewUrl)}`
}

export function triggerArchiveWarmup(kkBase, innerPath, fileKey) {
  const url = buildArchiveWarmupUrl(kkBase, innerPath, fileKey)
  const proxyUrl = resolvePortalWarmupUrl(url)
  return fetch(proxyUrl, { credentials: 'include' }).catch(() => null)
}

export function resolveArchiveInnerPreview(ext) {
  const key = String(ext || '').toLowerCase()
  if (key === 'pdf') {
    return { mode: 'pdf', needsWarmup: false }
  }
  if (TXT_EXTS.has(key)) {
    return { mode: 'txt', needsWarmup: false }
  }
  if (OFFICE_EXTS.has(key)) {
    return { mode: 'pdf', needsWarmup: true }
  }
  return { mode: '', needsWarmup: false }
}

export function isTrustedArchiveMessage(origin) {
  if (!origin) return false
  if (/^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/i.test(origin)) return true
  try {
    const incoming = new URL(origin)
    const current = new URL(window.location.href)
    return incoming.hostname === current.hostname
  } catch (e) {
    return false
  }
}

export function normalizeArchiveNodes(nodes) {
  if (!nodes) return []
  if (Array.isArray(nodes)) return nodes
  if (typeof nodes === 'object') return [nodes]
  return []
}

export function flattenArchiveNodes(nodes, bucket = []) {
  normalizeArchiveNodes(nodes).forEach(node => {
    if (!node) return
    if (node.folder && Array.isArray(node.children) && node.children.length) {
      flattenArchiveNodes(node.children, bucket)
    } else if (!node.folder) {
      bucket.push(node)
    }
  })
  return bucket
}

export function fetchArchiveManifest(previewUrl) {
  const base = String(process.env.VUE_APP_BASE_API || '').replace(/\/$/, '')
  const url = `${base}/system/portal/banner/archive-manifest?previewUrl=${encodeURIComponent(previewUrl)}`
  return fetch(url, { credentials: 'include' })
    .then(res => res.json())
    .then(body => {
      if (!body || body.code !== 200 || !body.data) {
        throw new Error((body && body.msg) || 'archive manifest failed')
      }
      return body.data
    })
}

export function archiveBadgeClass(ext) {
  const key = String(ext || '').toLowerCase()
  if (['doc', 'docx', 'wps', 'odt'].includes(key)) return 'doc'
  if (['xls', 'xlsx', 'csv', 'et'].includes(key)) return 'xls'
  if (['ppt', 'pptx', 'dps'].includes(key)) return 'ppt'
  if (key === 'pdf') return 'pdf'
  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(key)) return 'img'
  if (['zip', 'rar', '7z'].includes(key)) return 'archive'
  if (['mp3', 'mp4', 'wav', 'flv'].includes(key)) return 'media'
  return 'file'
}
