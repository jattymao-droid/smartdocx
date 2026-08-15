import request from '@/utils/request'

export function smartComposePaper(data) {
  return request({
    url: '/system/education/paper/smart-compose',
    method: 'post',
    data
  })
}
