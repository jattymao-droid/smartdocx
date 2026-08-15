import { checkPayAccess, PAY_BIZ, PAY_REQUIRED_CODE } from '@/api/education/pay'
import { downloadLibraryDocument } from '@/api/education/library'
import { resolvePortalMediaUrl } from '@/utils/portalBanner'

export function formatDownloadPrice(price) {
  const n = Number(price)
  if (!Number.isFinite(n) || n <= 0) return 0
  return n
}

export function buildDownloadButtonLabel(options = {}) {
  const price = formatDownloadPrice(options.price)
  const needPay = !!options.needPay
  const purchased = !!options.purchased
  const freeLabel = options.freeLabel || '\u4e0b\u8f7d'
  const paidLabel = options.paidLabel || '\u4ed8\u8d39\u4e0b\u8f7d'
  const ownedLabel = options.ownedLabel || '\u5df2\u8d2d\u4e70 \u00b7 \u4e0b\u8f7d'

  if (!needPay) return freeLabel
  if (purchased) return ownedLabel
  return `${paidLabel} \u00a5${price.toFixed(2)}`
}

export function deriveLibraryPayStatus(options = {}) {
  const docPrice = formatDownloadPrice(options.docPrice)
  const payEnabled = options.payEnabled !== false
  const purchased = !!options.purchased
  const needPay = docPrice > 0
  return {
    enabled: payEnabled,
    needPay,
    purchased: !needPay || purchased,
    price: docPrice,
    productName: options.productName || ''
  }
}

export function loadLibraryPayStatus(documentId, options = {}) {
  const id = Number(documentId)
  const fallback = deriveLibraryPayStatus({
    docPrice: options.docPrice,
    payEnabled: options.payEnabled,
    purchased: false,
    productName: options.productName
  })
  if (!id) {
    return Promise.resolve(fallback)
  }
  return checkPayAccess({
    bizType: PAY_BIZ.LIBRARY_DOCUMENT,
    bizId: id
  }).then(res => {
    const data = res.data || {}
    const docPrice = formatDownloadPrice(data.price != null ? data.price : options.docPrice)
    const payEnabled = data.enabled === true
    const purchased = !!data.purchased
    const needPay = data.needPay === true || docPrice > 0
    return {
      enabled: payEnabled,
      needPay,
      purchased: !needPay || purchased,
      price: docPrice,
      productName: data.productName || options.productName || ''
    }
  }).catch(() => fallback)
}

export function ensureLibraryDownloadPaid(vm, options = {}) {
  const documentId = Number(options.documentId)
  const title = options.title || ''
  const onLogin = options.onLogin

  if (!vm || !vm.$refs || !vm.$refs.payDialog) {
    return Promise.reject(new Error('pay dialog missing'))
  }
  if (!vm.token) {
    if (typeof onLogin === 'function') onLogin()
    return Promise.reject(new Error('login'))
  }

  return loadLibraryPayStatus(documentId, {
    docPrice: options.docPrice,
    payEnabled: options.payEnabled,
    productName: title
  }).then(status => {
    if (options.onStatus) options.onStatus(status)
    if (!status.needPay || status.purchased) {
      return { purchased: true, skipped: true, status }
    }
    if (!status.enabled) {
      return Promise.reject(new Error('\u652f\u4ed8\u529f\u80fd\u6682\u672a\u5f00\u542f\uff0c\u8bf7\u5728\u540e\u53f0\u6587\u5e93\u5065\u5eb7\u68c0\u67e5\u4e2d\u914d\u7f6e\u5e76\u542f\u7528 ZPay'))
    }
    return vm.$refs.payDialog.open({
      bizType: PAY_BIZ.LIBRARY_DOCUMENT,
      bizId: documentId,
      title: options.dialogTitle || '\u4ed8\u8d39\u4e0b\u8f7d',
      productLabel: title || status.productName
    }).then(result => ({ ...result, status }))
  })
}

export function triggerLibraryDownload(documentId) {
  return downloadLibraryDocument(documentId, { skipNotify: true }).then(res => {
    const url = (res && res.url) || (res && res.data && res.data.url) || ''
    if (!url) {
      return Promise.reject(new Error('\u672a\u83b7\u53d6\u5230\u4e0b\u8f7d\u94fe\u63a5'))
    }
    window.open(url, '_blank')
    return { url }
  })
}

export function downloadLibraryWithPay(vm, options = {}) {
  const documentId = Number(options.documentId)

  return ensureLibraryDownloadPaid(vm, options)
    .then(() => triggerLibraryDownload(documentId))
    .catch(err => {
      const code = err && (err.code || err.response?.data?.code)
      const msg = (err && err.message) || ''
      if (code === PAY_REQUIRED_CODE || code === 402 || msg.includes('\u652f\u4ed8')) {
        return ensureLibraryDownloadPaid(vm, options)
          .then(() => triggerLibraryDownload(documentId))
      }
      throw err
    })
}

export function isPayReturnQuery(query = {}) {
  return !!(query.payOrder || query.out_trade_no || query.payReturn === '1')
}

export function resolvePayReturnOrderNo(query = {}) {
  return query.payOrder || query.out_trade_no || ''
}

export function formatListPriceLabel(downloadPrice) {
  const price = formatDownloadPrice(downloadPrice)
  return price > 0 ? `\u00a5${price.toFixed(2)}` : ''
}
