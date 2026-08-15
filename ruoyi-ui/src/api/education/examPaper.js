import request from '@/utils/request'

export function listExamPaper(query) {
  return request({
    url: '/system/education/exam-paper/list',
    method: 'get',
    params: query
  })
}

export function getExamPaper(paperId) {
  return request({
    url: '/system/education/exam-paper/' + paperId,
    method: 'get'
  })
}

export function uploadExamPaper(data) {
  return request({
    url: '/system/education/exam-paper/upload',
    method: 'post',
    data: data,
    timeout: 120000,
    headers: { repeatSubmit: false, 'Content-Type': 'multipart/form-data' }
  })
}

export function analyzeExamPaper(data) {
  return request({
    url: '/system/education/exam-paper/analyze',
    method: 'post',
    data: data
  })
}

export function commitExamPaper(data) {
  return request({
    url: '/system/education/exam-paper/commit',
    method: 'post',
    timeout: 120000,
    data: data
  })
}

export function publishExamPaper(paperId, status) {
  return request({
    url: '/system/education/exam-paper/' + paperId + '/publish',
    method: 'put',
    params: { status }
  })
}

export function delExamPaper(paperId) {
  return request({
    url: '/system/education/exam-paper/' + paperId,
    method: 'delete'
  })
}
