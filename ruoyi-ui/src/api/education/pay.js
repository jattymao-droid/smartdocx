import request from '@/utils/request'

export function checkPayAccess(params) {
  return request({
    url: '/system/education/pay/check',
    method: 'get',
    params
  })
}

export function createPayOrder(data) {
  return request({
    url: '/system/education/pay/order',
    method: 'post',
    data
  })
}

export function getPayOrderStatus(orderNo) {
  return request({
    url: '/system/education/pay/order/' + orderNo,
    method: 'get'
  })
}

export function listMyPayOrders(params) {
  return request({
    url: '/system/education/pay/orders/mine',
    method: 'get',
    params
  })
}

export function getPayAdminSettings() {
  return request({
    url: '/system/education/pay/admin/settings',
    method: 'get'
  })
}

export function updatePayAdminSettings(data) {
  return request({
    url: '/system/education/pay/admin/settings',
    method: 'put',
    data
  })
}

export const PAY_BIZ = {
  LIBRARY_DOCUMENT: 'library_document',
  LIBRARY_TOPIC: 'library_topic',
  PAPER_EXPORT: 'paper_export',
  LIBRARY_VIP: 'library_vip'
}

export const PAY_REQUIRED_CODE = 402
