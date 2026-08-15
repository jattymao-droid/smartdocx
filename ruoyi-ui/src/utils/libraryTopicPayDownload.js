import { checkPayAccess, PAY_BIZ, PAY_REQUIRED_CODE } from '@/api/education/pay'
import { formatDownloadPrice, buildDownloadButtonLabel } from '@/utils/libraryPayDownload'
import download from '@/plugins/download'

export { formatDownloadPrice, buildDownloadButtonLabel }

export function deriveTopicPayStatus(options = {}) {
  const price = formatDownloadPrice(options.bundlePrice)
  const payEnabled = options.payEnabled !== false
  const purchased = !!options.purchased
  const needPay = price > 0
  return {
    enabled: payEnabled,
    needPay,
    purchased: !needPay || purchased,
    price,
    productName: options.productName || ''
  }
}

export function loadTopicPayStatus(topicId, options = {}) {
  const id = Number(topicId)
  const fallback = deriveTopicPayStatus({
    bundlePrice: options.bundlePrice,
    payEnabled: options.payEnabled,
    purchased: false,
    productName: options.productName
  })
  if (!id) return Promise.resolve(fallback)
  return checkPayAccess({
    bizType: PAY_BIZ.LIBRARY_TOPIC,
    bizId: id
  }).then(res => {
    const data = res.data || {}
    const price = formatDownloadPrice(data.price != null ? data.price : options.bundlePrice)
    const payEnabled = data.enabled === true
    const purchased = !!data.purchased
    const needPay = data.needPay === true || price > 0
    return {
      enabled: payEnabled,
      needPay,
      purchased: !needPay || purchased,
      price,
      productName: data.productName || options.productName || ''
    }
  }).catch(() => fallback)
}

export function ensureTopicDownloadPaid(vm, options = {}) {
  const topicId = Number(options.topicId)
  const title = options.title || ''
  const onLogin = options.onLogin

  if (!vm || !vm.$refs || !vm.$refs.payDialog) {
    return Promise.reject(new Error('pay dialog missing'))
  }
  if (!vm.token) {
    if (typeof onLogin === 'function') onLogin()
    return Promise.reject(new Error('login'))
  }

  return loadTopicPayStatus(topicId, {
    bundlePrice: options.bundlePrice,
    payEnabled: options.payEnabled,
    productName: title
  }).then(status => {
    if (options.onStatus) options.onStatus(status)
    if (!status.needPay || status.purchased) {
      return { purchased: true, skipped: true, status }
    }
    if (!status.enabled) {
      return Promise.reject(new Error('\u652f\u4ed8\u529f\u80fd\u6682\u672a\u5f00\u542f\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458'))
    }
    return vm.$refs.payDialog.open({
      bizType: PAY_BIZ.LIBRARY_TOPIC,
      bizId: topicId,
      title: options.dialogTitle || '\u4e13\u9898\u6253\u5305\u4e0b\u8f7d',
      productLabel: title || status.productName
    }).then(result => ({ ...result, status }))
  })
}

export function triggerTopicZipDownload(topicId, title) {
  const safeName = (title || 'topic').replace(/[\\/:*?"<>|]/g, '_')
  download.zip('/system/education/library/topic/' + topicId + '/download', safeName + '.zip')
  return Promise.resolve()
}

export function downloadTopicWithPay(vm, options = {}) {
  const topicId = Number(options.topicId)
  return ensureTopicDownloadPaid(vm, options)
    .then(() => triggerTopicZipDownload(topicId, options.title))
    .catch(err => {
      const code = err && (err.code || err.response?.data?.code)
      const msg = (err && err.message) || ''
      if (code === PAY_REQUIRED_CODE || code === 402 || msg.includes('\u652f\u4ed8')) {
        return ensureTopicDownloadPaid(vm, options)
          .then(() => triggerTopicZipDownload(topicId, options.title))
      }
      throw err
    })
}
