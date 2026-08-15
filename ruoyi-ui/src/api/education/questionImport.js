import request from '@/utils/request'

export function uploadDocx(data) {
  return request({
    url: '/system/education/question/import/docx',
    method: 'post',
    data: data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function listImportTasks(query) {
  return request({
    url: '/system/education/question/import/tasks',
    method: 'get',
    params: query
  })
}

export function getImportTask(taskId) {
  return request({
    url: '/system/education/question/import/task/' + taskId,
    method: 'get'
  })
}

export function uploadImportImage(formData) {
  return request({
    url: '/system/common/upload',
    method: 'post',
    data: formData,
    timeout: 60000,
    headers: { repeatSubmit: false, 'Content-Type': 'multipart/form-data' }
  })
}

export function commitImport(data) {
  return request({
    url: '/system/education/question/import/commit',
    method: 'post',
    data: data
  })
}

export function matchImportChapters(data) {
  return request({
    url: '/system/education/question/import/match-chapters',
    method: 'post',
    data
  })
}
