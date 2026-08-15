import { isArchiveExt } from '@/utils/libraryFileExt'

/**
 * Portal route for a library document card.
 * Archive documents linked to a hot topic open the topic bundle page.
 */
export function resolveLibraryDocumentPath(doc) {
  if (!doc) return '/library'
  const topicId = Number(doc.topicId)
  if (topicId > 0 && isArchiveExt(doc.fileExt)) {
    return `/library/topic/${topicId}`
  }
  if (doc.documentId != null && doc.documentId !== '') {
    return `/library/${doc.documentId}`
  }
  return '/library'
}

export function openLibraryDocument(router, doc) {
  if (!router || !doc) return
  router.push(resolveLibraryDocumentPath(doc))
}
