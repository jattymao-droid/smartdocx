import request from '@/utils/request'

export function getVipStatus() {
  return request({
    url: '/system/education/library/vip/status',
    method: 'get',
    headers: { skipNotify: true }
  })
}

export function getVipRecentOrders(limit = 8) {
  return request({
    url: '/system/education/library/vip/recent',
    method: 'get',
    params: { limit },
    headers: { skipNotify: true }
  })
}

export function getVipAdminConfig() {
  return request({
    url: '/system/education/library/vip/admin/config',
    method: 'get'
  })
}

export function updateVipAdminConfig(data) {
  return request({
    url: '/system/education/library/vip/admin/config',
    method: 'put',
    data
  })
}

export function listVipMembers(query) {
  return request({
    url: '/system/education/library/vip/admin/list',
    method: 'get',
    params: query
  })
}

export function grantVipMember(data) {
  return request({
    url: '/system/education/library/vip/admin/grant',
    method: 'post',
    data
  })
}

export function extendVipMember(data) {
  return request({
    url: '/system/education/library/vip/admin/extend',
    method: 'put',
    data
  })
}

export function disableVipMember(username) {
  return request({
    url: '/system/education/library/vip/admin/' + encodeURIComponent(username),
    method: 'delete'
  })
}
