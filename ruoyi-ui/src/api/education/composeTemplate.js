import request from '@/utils/request'

export function listComposeTemplates(subjectId) {
  return request({
    url: '/system/education/paper/compose-template/list',
    method: 'get',
    params: { subjectId }
  })
}

export function getComposeTemplate(templateId) {
  return request({
    url: '/system/education/paper/compose-template/' + templateId,
    method: 'get'
  })
}

export function saveComposeTemplate(data) {
  return request({
    url: '/system/education/paper/compose-template/save',
    method: 'post',
    data
  })
}

export function deleteComposeTemplate(templateId) {
  return request({
    url: '/system/education/paper/compose-template/' + templateId,
    method: 'delete'
  })
}
