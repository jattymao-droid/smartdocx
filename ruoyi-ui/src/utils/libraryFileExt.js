export const LIBRARY_UPLOAD_EXTS = [
  'pdf', 'txt', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'zip', 'rar', '7z'
]

export const LIBRARY_ARCHIVE_EXTS = ['zip', 'rar', '7z']

export function isArchiveExt(ext) {
  return LIBRARY_ARCHIVE_EXTS.includes(normalizeFileExt(ext))
}

/** Normalize file extension for badge styling */
export function normalizeFileExt(ext) {
  return String(ext || 'file').toLowerCase().replace(/^\./, '').trim() || 'file'
}

export function formatFileExtLabel(ext) {
  return normalizeFileExt(ext).toUpperCase()
}

/** CSS class suffix for ext badge, e.g. ext-badge--pdf */
export function getFileExtBadgeClass(ext) {
  const key = normalizeFileExt(ext)
  if (key === 'pdf') return 'ext-badge--pdf'
  if (key === 'doc' || key === 'docx') return 'ext-badge--word'
  if (key === 'ppt' || key === 'pptx') return 'ext-badge--ppt'
  if (key === 'xls' || key === 'xlsx') return 'ext-badge--excel'
  if (key === 'txt') return 'ext-badge--txt'
  if (key === 'zip' || key === 'rar' || key === '7z') return 'ext-badge--archive'
  return 'ext-badge--file'
}

/** Cover/thumbnail fallback gradient class used on cards */
export function getFileExtCoverClass(ext) {
  return 'ext-' + normalizeFileExt(ext)
}

function isValidUploadedCoverUrl(coverUrl) {
  const url = String(coverUrl || '').trim()
  if (!url) return false
  const lower = url.toLowerCase()
  if (!(lower.includes('/statics/') || lower.includes('/profile/'))) return false
  return /\.(jpe?g|png|gif|webp|bmp)(\?|$)/i.test(lower)
}

/** Archives without a user-uploaded image cover should use the built-in CSS cover block. */
export function shouldUseArchiveDefaultCover(coverUrl, fileExt) {
  if (!isArchiveExt(fileExt)) return false
  const trimmed = String(coverUrl || '').trim()
  if (!trimmed) return true
  return !isValidUploadedCoverUrl(trimmed)
}

/** Resolve uploaded document cover URL (not for archive default cover). */
export function resolveLibraryDocumentCover(coverUrl, fileExt, resolveMediaUrl) {
  if (shouldUseArchiveDefaultCover(coverUrl, fileExt)) {
    return ''
  }
  const resolver = typeof resolveMediaUrl === 'function' ? resolveMediaUrl : (url) => url
  const trimmed = String(coverUrl || '').trim()
  if (!trimmed) {
    return ''
  }
  return resolver(trimmed) || ''
}
