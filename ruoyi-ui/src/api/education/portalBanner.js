import request from '@/utils/request'

export function getPortalBannerAdminConfig() {
  return request({
    url: '/system/portal/banner/admin',
    method: 'get'
  })
}

export function updatePortalBannerAdminConfig(data) {
  return request({
    url: '/system/portal/banner/admin',
    method: 'put',
    data
  })
}
