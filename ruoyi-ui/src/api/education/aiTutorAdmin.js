import request from '@/utils/request'

export function getAiTutorAdminConfig() {
  return request({
    url: '/system/education/ai-tutor/config',
    method: 'get'
  })
}

export function updateAiTutorAdminConfig(data) {
  return request({
    url: '/system/education/ai-tutor/config',
    method: 'put',
    data
  })
}
