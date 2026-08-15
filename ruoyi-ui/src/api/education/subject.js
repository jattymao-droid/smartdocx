import request from '@/utils/request'

export function listSubject(query) {
  return request({
    url: '/system/education/subject/list',
    method: 'get',
    params: query
  })
}

export function getSubject(subjectId) {
  return request({
    url: '/system/education/subject/' + subjectId,
    method: 'get'
  })
}

export function addSubject(data) {
  return request({
    url: '/system/education/subject',
    method: 'post',
    data
  })
}

export function updateSubject(data) {
  return request({
    url: '/system/education/subject',
    method: 'put',
    data
  })
}

export function delSubject(subjectId) {
  return request({
    url: '/system/education/subject/' + subjectId,
    method: 'delete'
  })
}

export function subjectOptions() {
  return request({
    url: '/system/education/subject/options',
    method: 'get',
    headers: { isToken: false }
  }).then(res => {
    const data = (res.data || [])
      .filter(item => item && item.subjectId != null && item.subjectName !== '\u603b\u5206')
    return { ...res, data }
  })
}

