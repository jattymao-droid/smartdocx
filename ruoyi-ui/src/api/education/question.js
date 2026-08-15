import request from '@/utils/request'

export function listQuestion(query) {
  return request({
    url: '/system/education/question/list',
    method: 'get',
    params: query
  })
}

export function getQuestion(questionId) {
  return request({
    url: '/system/education/question/' + questionId,
    method: 'get'
  })
}

export function addQuestion(data) {
  return request({
    url: '/system/education/question',
    method: 'post',
    data: data
  })
}

export function updateQuestion(data) {
  return request({
    url: '/system/education/question',
    method: 'put',
    data: data
  })
}

export function delQuestion(questionIds) {
  return request({
    url: '/system/education/question/' + questionIds,
    method: 'delete'
  })
}

export function getChapterTree(query) {
  // Deprecated: prefer getTextbookChapterTree from @/api/education/textbook
  return request({
    url: '/system/education/question/chapter/tree',
    method: 'get',
    params: query
  })
}

export function listKnowledgeTags(query) {
  return request({
    url: '/system/education/question/knowledge/tags',
    method: 'get',
    params: query
  })
}

export function getKnowledgeTree(query) {
  return request({
    url: '/system/education/question/knowledge/tree',
    method: 'get',
    params: query
  })
}

export function auditQuestions(data) {
  return request({
    url: '/system/education/question/audit',
    method: 'post',
    data: data
  })
}

export function getPendingAuditCount() {
  return request({
    url: '/system/education/question/audit/pending-count',
    method: 'get'
  })
}

export function checkDuplicates(data) {
  return request({
    url: '/system/education/question/duplicate/check',
    method: 'post',
    data: data
  })
}

export function getQuestionDuplicates(questionId) {
  return request({
    url: '/system/education/question/' + questionId + '/duplicates',
    method: 'get'
  })
}

export function submitQuestionFeedback(data) {
  return request({
    url: '/system/education/question/feedback',
    method: 'post',
    data: data
  })
}
