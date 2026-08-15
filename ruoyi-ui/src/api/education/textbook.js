import request from '@/utils/request'

export function listTextbookVersions(subjectId, schoolStage) {
  return request({
    url: '/system/education/textbook/versions',
    method: 'get',
    timeout: 30000,
    params: { subjectId, schoolStage }
  })
}

export function listTextbooks(versionId) {
  return request({
    url: '/system/education/textbook/list',
    method: 'get',
    timeout: 30000,
    params: { versionId }
  })
}

export function getTextbookChapterTree(textbookId, subjectId) {
  return request({
    url: '/system/education/textbook/chapter/tree',
    method: 'get',
    timeout: 30000,
    params: { textbookId, subjectId }
  })
}

export function listTextbookVersionsAdmin(subjectId, schoolStage) {
  return request({
    url: '/system/education/textbook/versions/admin',
    method: 'get',
    params: { subjectId, schoolStage }
  })
}

export function getTextbookVersion(versionId) {
  return request({
    url: '/system/education/textbook/version/' + versionId,
    method: 'get'
  })
}

export function addTextbookVersion(data) {
  return request({
    url: '/system/education/textbook/version',
    method: 'post',
    data
  })
}

export function updateTextbookVersion(data) {
  return request({
    url: '/system/education/textbook/version',
    method: 'put',
    data
  })
}

export function delTextbookVersion(versionIds) {
  return request({
    url: '/system/education/textbook/version/' + versionIds,
    method: 'delete'
  })
}

export function listTextbooksAdmin(versionId) {
  return request({
    url: '/system/education/textbook/list/admin',
    method: 'get',
    params: { versionId }
  })
}

export function getTextbook(textbookId) {
  return request({
    url: '/system/education/textbook/' + textbookId,
    method: 'get'
  })
}

export function addTextbook(data) {
  return request({
    url: '/system/education/textbook',
    method: 'post',
    data
  })
}

export function updateTextbook(data) {
  return request({
    url: '/system/education/textbook',
    method: 'put',
    data
  })
}

export function delTextbook(textbookIds) {
  return request({
    url: '/system/education/textbook/' + textbookIds,
    method: 'delete'
  })
}

export function listTextbookChapters(textbookId) {
  return request({
    url: '/system/education/textbook/chapter/list',
    method: 'get',
    params: { textbookId }
  })
}

export function getTextbookChapter(chapterId) {
  return request({
    url: '/system/education/textbook/chapter/' + chapterId,
    method: 'get'
  })
}

export function addTextbookChapter(data) {
  return request({
    url: '/system/education/textbook/chapter',
    method: 'post',
    data
  })
}

export function updateTextbookChapter(data) {
  return request({
    url: '/system/education/textbook/chapter',
    method: 'put',
    data
  })
}

export function delTextbookChapter(chapterIds) {
  return request({
    url: '/system/education/textbook/chapter/' + chapterIds,
    method: 'delete'
  })
}
