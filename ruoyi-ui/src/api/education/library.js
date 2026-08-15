import request from '@/utils/request'

export function generateLibraryCover(data) {
  return request({
    url: '/system/education/library/document/cover/generate',
    method: 'post',
    data,
    timeout: 180000,
    headers: { skipNotify: true }
  })
}

export function listHotLibraryDownloads(query) {
  return request({
    url: '/system/education/library/document/list',
    method: 'get',
    params: {
      portal: true,
      orderBy: 'download',
      pageNum: 1,
      pageSize: 5,
      ...query
    },
    headers: { skipNotify: true }
  })
}

export function listLibraryDocuments(query) {
  return request({
    url: '/system/education/library/document/list',
    method: 'get',
    params: query
  })
}

export function listMyLibraryDocuments(query) {
  return request({
    url: '/system/education/library/document/mine',
    method: 'get',
    params: query
  })
}

export function listLibraryFavorites(query) {
  return request({
    url: '/system/education/library/favorite/list',
    method: 'get',
    params: query
  })
}

export function changeLibraryDocumentStatus(data) {
  return request({
    url: '/system/education/library/document/status',
    method: 'post',
    data
  })
}

export function getLibraryDocument(documentId) {
  return request({
    url: '/system/education/library/document/' + documentId,
    method: 'get'
  })
}

export function getLibraryPreview(documentId) {
  return request({
    url: '/system/education/library/document/' + documentId + '/preview',
    method: 'get'
  })
}

export function recordLibraryView(documentId) {
  return request({
    url: '/system/education/library/document/' + documentId + '/view',
    method: 'post'
  })
}

export function addLibraryDocument(data) {
  return request({
    url: '/system/education/library/document',
    method: 'post',
    data
  })
}

export function addPortalLibraryDocument(data) {
  return request({
    url: '/system/education/library/document/portal',
    method: 'post',
    data
  })
}

export function updateLibraryDocument(data) {
  return request({
    url: '/system/education/library/document',
    method: 'put',
    data
  })
}

export function delLibraryDocument(documentIds) {
  return request({
    url: '/system/education/library/document/' + documentIds,
    method: 'delete'
  })
}

export function auditLibraryDocuments(data) {
  return request({
    url: '/system/education/library/document/audit',
    method: 'post',
    data
  })
}

export function favoriteLibraryDocument(documentId) {
  return request({
    url: '/system/education/library/favorite/' + documentId,
    method: 'post'
  })
}

export function unfavoriteLibraryDocument(documentId) {
  return request({
    url: '/system/education/library/favorite/' + documentId,
    method: 'delete'
  })
}

export function downloadLibraryDocument(documentId, options = {}) {
  const headers = {}
  if (options.skipNotify) headers.skipNotify = true
  return request({
    url: '/system/education/library/document/' + documentId + '/download',
    method: 'get',
    headers
  })
}

export function updatePortalLibraryDocument(data) {
  return request({
    url: '/system/education/library/document/portal',
    method: 'put',
    data
  })
}

export function recommendLibraryDocuments(data) {
  return request({
    url: '/system/education/library/document/recommend',
    method: 'post',
    data
  })
}

export function reconvertLibraryDocument(documentId) {
  return request({
    url: '/system/education/library/document/' + documentId + '/reconvert',
    method: 'post'
  })
}

export function listLibraryCategories() {
  return request({
    url: '/system/education/library/category/list',
    method: 'get',
    headers: { isToken: false }
  })
}

export function listAdminLibraryCategories(query) {
  return request({
    url: '/system/education/library/category/admin/list',
    method: 'get',
    params: query
  })
}

export function getLibraryCategory(categoryId) {
  return request({
    url: '/system/education/library/category/' + categoryId,
    method: 'get'
  })
}

export function addLibraryCategory(data) {
  return request({
    url: '/system/education/library/category',
    method: 'post',
    data
  })
}

export function updateLibraryCategory(data) {
  return request({
    url: '/system/education/library/category',
    method: 'put',
    data
  })
}

export function delLibraryCategory(categoryIds) {
  return request({
    url: '/system/education/library/category/' + categoryIds,
    method: 'delete'
  })
}

export function getLibraryAdminHealth() {
  return request({
    url: '/system/education/library/admin/health',
    method: 'get'
  })
}

export function getLibraryAdminSettings() {
  return request({
    url: '/system/education/library/admin/settings',
    method: 'get'
  })
}

export function updateLibraryAdminSettings(data) {
  return request({
    url: '/system/education/library/admin/settings',
    method: 'put',
    data
  })
}

export function listRelatedLibraryDocuments(documentId) {
  return request({
    url: '/system/education/library/document/' + documentId + '/related',
    method: 'get'
  })
}

export function listContinueReading(limit = 5) {
  return request({
    url: '/system/education/library/reading/continue',
    method: 'get',
    params: { limit },
    headers: { skipRelogin: true, skipNotify: true }
  })
}

export function saveLibraryReadProgress(documentId, readProgress) {
  return request({
    url: '/system/education/library/document/' + documentId + '/progress',
    method: 'post',
    data: { readProgress },
    headers: { repeatSubmit: false, skipNotify: true }
  })
}

export function listLibraryTopics(query) {
  return request({
    url: '/system/education/library/topic/list',
    method: 'get',
    params: query,
    headers: { skipNotify: true }
  })
}

export function listAdminLibraryTopics(query) {
  return request({
    url: '/system/education/library/topic/admin/list',
    method: 'get',
    params: query
  })
}

export function getLibraryTopic(topicId, options = {}) {
  const params = {}
  if (options.portal) params.portal = true
  const headers = options.portal ? { skipNotify: true } : {}
  return request({
    url: '/system/education/library/topic/' + topicId,
    method: 'get',
    params,
    headers
  })
}

export function getAdminLibraryTopic(topicId) {
  return request({
    url: '/system/education/library/topic/admin/' + topicId,
    method: 'get'
  })
}

export function addLibraryTopic(data) {
  return request({
    url: '/system/education/library/topic',
    method: 'post',
    data
  })
}

export function updateLibraryTopic(data) {
  return request({
    url: '/system/education/library/topic',
    method: 'put',
    data
  })
}

export function delLibraryTopic(topicIds) {
  return request({
    url: '/system/education/library/topic/' + topicIds,
    method: 'delete'
  })
}
