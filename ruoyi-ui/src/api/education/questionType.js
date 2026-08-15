import request from '@/utils/request'

export function listQuestionType(query) {
  return request({
    url: '/system/education/question/type/list',
    method: 'get',
    params: query
  })
}

export function getQuestionType(typeId) {
  return request({
    url: '/system/education/question/type/' + typeId,
    method: 'get'
  })
}

export function addQuestionType(data) {
  return request({
    url: '/system/education/question/type',
    method: 'post',
    data
  })
}

export function updateQuestionType(data) {
  return request({
    url: '/system/education/question/type',
    method: 'put',
    data
  })
}

export function delQuestionType(typeIds) {
  return request({
    url: '/system/education/question/type/' + typeIds,
    method: 'delete'
  })
}

export function questionTypeOptions() {
  return request({
    url: '/system/education/question/type/options',
    method: 'get'
  })
}
