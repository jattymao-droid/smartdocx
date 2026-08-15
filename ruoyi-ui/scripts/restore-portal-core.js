/**
 * Restore portal utils + label modules (teacher portal, no student practice pages).
 * Run: node scripts/restore-portal-core.js
 */
const fs = require('fs')
const path = require('path')

const src = path.join(__dirname, '../src')

function write(rel, content) {
  const file = path.join(src, rel)
  fs.mkdirSync(path.dirname(file), { recursive: true })
  fs.writeFileSync(file, content, 'utf8')
  console.log('wrote', rel)
}

write('utils/portalAuth.js', `import Vue from 'vue'

export const portalAuthBus = new Vue()

export function openPortalAuth(options = {}) {
  portalAuthBus.$emit('open', options || {})
}
`)

write('utils/portalLogin.js', `import { openPortalAuth } from '@/utils/portalAuth'

export const PORTAL_LOGIN_PATH = '/portal/login'

export function isPortalPath(path) {
  return path && String(path).indexOf('/portal/') === 0
}

export function goPortalLogin(router, redirect, tab = 'login') {
  const target = redirect || '/portal/home'
  if (router && router.currentRoute && isPortalPath(router.currentRoute.path)) {
    openPortalAuth({ redirect: target, tab })
    return
  }
  router.push({ path: PORTAL_LOGIN_PATH, query: { redirect: target, tab } })
}
`)

write('utils/portalBanner.js', `import request from '@/utils/request'

export const DEFAULT_PORTAL_BANNER = {
  mode: 'none',
  imageUrl: '',
  videoUrl: '',
  videoPoster: '',
  overlay: 0.42,
  objectFit: 'cover'
}

export function bannerHasMedia(banner) {
  if (!banner) return false
  if (banner.mode === 'image') return !!banner.imageUrl
  if (banner.mode === 'video') return !!banner.videoUrl
  return false
}

export function resolvePortalMediaUrl(url, apiBase = process.env.VUE_APP_BASE_API) {
  if (!url) return ''
  const src = String(url).trim()
  if (/^https?:\\/\\//i.test(src)) {
    const base = (apiBase || '').replace(/\\/$/, '')
    return base + '/system/portal/banner/media?url=' + encodeURIComponent(src)
  }
  const path = src.startsWith('/') ? src : '/' + src
  return (apiBase || '').replace(/\\/$/, '') + path
}

export function loadPortalBannerConfig() {
  return request({
    url: '/system/portal/banner',
    method: 'get',
    headers: { isToken: false }
  }).then(res => {
    const data = (res && res.data) || {}
    return {
      mode: data.mode || DEFAULT_PORTAL_BANNER.mode,
      imageUrl: data.imageUrl || '',
      videoUrl: data.videoUrl || '',
      videoPoster: data.videoPoster || '',
      overlay: typeof data.overlay === 'number' ? data.overlay : DEFAULT_PORTAL_BANNER.overlay,
      objectFit: data.objectFit || DEFAULT_PORTAL_BANNER.objectFit
    }
  }).catch(() => ({ ...DEFAULT_PORTAL_BANNER }))
}
`)

write('api/education/portalBanner.js', `import request from '@/utils/request'

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
`)

write('views/portal/portal-home-labels.js', `export const homeLabels = {
  smartTips: [
    '\\u6309\\u7ae0\\u8282\\u96be\\u5ea6\\u667a\\u80fd\\u63a8\\u8350\\u8bd5\\u9898\\uff0c\\u5feb\\u901f\\u586b\\u6ee1\\u8bd5\\u9898\\u7bee',
    '\\u77e5\\u8bc6\\u70b9\\u6811\\u4e00\\u952e\\u5b9a\\u4f4d\\u8584\\u5f31\\u8003\\u70b9\\u76f8\\u5173\\u9898\\u76ee',
    '\\u7ec4\\u5377\\u9884\\u89c8\\u652f\\u6301 Word / PDF \\u5bfc\\u51fa\\u4e0e\\u6253\\u5370'
  ]
}
`)

write('views/portal/portal-labels.js', `export const browseLabels = {
  loginHint: '\\u767b\\u5f55\\u540e\\u53ef\\u6d4f\\u89c8\\u9898\\u5e93\\u5e76\\u52a0\\u5165\\u8bd5\\u9898\\u7bee',
  loginBtn: '\\u53bb\\u767b\\u5f55',
  smartAction: '\\u5df2\\u5e94\\u7528\\u667a\\u80fd\\u96be\\u5ea6\\u7b5b\\u9009',
  catalogCollapse: '\\u6536\\u8d77\\u76ee\\u5f55',
  catalogExpand: '\\u5c55\\u5f00\\u76ee\\u5f55',
  knowledge: '\\u77e5\\u8bc6\\u70b9',
  searchKnowledgePh: '\\u641c\\u7d22\\u77e5\\u8bc6\\u70b9',
  all: '\\u5168\\u90e8',
  noKnowledge: '\\u6682\\u65e0\\u77e5\\u8bc6\\u70b9',
  examSideTitle: '\\u8bd5\\u5377\\u5206\\u7c7b',
  sortDefault: '\\u9ed8\\u8ba4\\u6392\\u5e8f',
  sortLatest: '\\u6700\\u65b0\\u5165\\u5e93',
  resultPrefix: '\\u5171',
  resultSuffix: '\\u9898',
  oneKeyPaper: '\\u4e00\\u952e\\u7ec4\\u5377',
  goPickFirst: '\\u53bb\\u9009\\u9898',
  noQuestion: '\\u6682\\u65e0\\u8bd5\\u9898',
  loginToView: '\\u8bf7\\u767b\\u5f55\\u540e\\u67e5\\u770b\\u8bd5\\u9898',
  chapterPick: '\\u7ae0\\u8282\\u9009\\u9898',
  knowledgePick: '\\u77e5\\u8bc6\\u70b9\\u9009\\u9898',
  examPick: '\\u8bd5\\u5377\\u9009\\u9898',
  chapterDesc: '\\u6309\\u6559\\u6750\\u7ae0\\u8282\\u6d4f\\u89c8\\u5df2\\u5ba1\\u6838\\u8bd5\\u9898\\uff0c\\u52a0\\u5165\\u8bd5\\u9898\\u7bee\\u540e\\u7ec4\\u5377\\u5bfc\\u51fa',
  knowledgeDesc: '\\u6309\\u77e5\\u8bc6\\u70b9\\u6807\\u7b7e\\u7b5b\\u9009\\u8bd5\\u9898\\uff0c\\u9002\\u5408\\u4e13\\u9898\\u8bad\\u7ec3\\u5377',
  examDesc: '\\u6309\\u8bd5\\u5377\\u7c7b\\u578b\\u5173\\u952e\\u8bcd\\u7b5b\\u9009\\u76f8\\u5173\\u8bd5\\u9898'
}
`)

write('views/portal/portal-paper-labels.js', `export const paperLabels = {
  home: '\\u9996\\u9875',
  breadcrumb: '\\u667a\\u80fd\\u7ec4\\u5377',
  title: '\\u667a\\u80fd\\u7ec4\\u5377',
  desc: '\\u4ece\\u8bd5\\u9898\\u7bee\\u9009\\u62e9\\u8bd5\\u9898\\u540e\\uff0c\\u53ef\\u5728\\u6b64\\u9884\\u89c8\\u5e76\\u5bfc\\u51fa\\u8bd5\\u5377\\u3002',
  preview: '\\u8fdb\\u5165\\u7ec4\\u5377\\u9884\\u89c8',
  continuePick: '\\u7ee7\\u7eed\\u9009\\u9898',
  basketUnit: '\\u9898',
  basketPrefix: '\\u5f53\\u524d\\u8bd5\\u9898\\u7bee\\uff1a',
  emptyBasket: '\\u8bd5\\u9898\\u7bee\\u4e3a\\u7a7a\\uff0c\\u8bf7\\u5148\\u9009\\u9898',
  goPick: '\\u53bb\\u9009\\u9898',
  smartTitle: '\\u667a\\u80fd\\u7ec4\\u5377\\u52a9\\u624b',
  smartDesc: '\\u6309\\u9898\\u578b\\u4e0e\\u96be\\u5ea6\\u81ea\\u52a8\\u7edf\\u8ba1\\u5206\\u503c\\uff0c\\u652f\\u6301\\u4e00\\u952e\\u5bfc\\u51fa\\u8bd5\\u5377\\u3002',
  stepsTitle: '\\u7ec4\\u5377\\u6d41\\u7a0b'
}
`)

write('views/portal/portal-profile-labels.js', `export const profileLabels = {
  home: '\\u9996\\u9875',
  breadcrumb: '\\u4e2a\\u4eba\\u4e2d\\u5fc3',
  loginRequired: '\\u8bf7\\u767b\\u5f55\\u540e\\u67e5\\u770b\\u4e2a\\u4eba\\u4e2d\\u5fc3',
  goLogin: '\\u53bb\\u767b\\u5f55',
  title: '\\u4e2a\\u4eba\\u4e2d\\u5fc3',
  subtitle: '\\u7ba1\\u7406\\u8d26\\u53f7\\u4fe1\\u606f\\u4e0e\\u5feb\\u6377\\u5165\\u53e3',
  goPick: '\\u53bb\\u9009\\u9898',
  goPaper: '\\u667a\\u80fd\\u7ec4\\u5377',
  cardProfile: '\\u8d26\\u53f7\\u4fe1\\u606f',
  phone: '\\u624b\\u673a',
  email: '\\u90ae\\u7bb1',
  dept: '\\u90e8\\u95e8',
  role: '\\u89d2\\u8272',
  createTime: '\\u6ce8\\u518c\\u65f6\\u95f4',
  basketStat: '\\u8bd5\\u9898\\u7bee',
  basketUnit: '\\u9898',
  cardEdit: '\\u8d44\\u6599\\u4fee\\u6539',
  tabInfo: '\\u57fa\\u672c\\u8d44\\u6599',
  tabPwd: '\\u4fee\\u6539\\u5bc6\\u7801',
  nickName: '\\u6635\\u79f0',
  sex: '\\u6027\\u522b',
  male: '\\u7537',
  female: '\\u5973',
  save: '\\u4fdd\\u5b58',
  reset: '\\u91cd\\u7f6e',
  oldPwd: '\\u65e7\\u5bc6\\u7801',
  newPwd: '\\u65b0\\u5bc6\\u7801',
  confirmPwd: '\\u786e\\u8ba4\\u5bc6\\u7801',
  oldPwdPh: '\\u8bf7\\u8f93\\u5165\\u65e7\\u5bc6\\u7801',
  newPwdPh: '\\u8bf7\\u8f93\\u5165\\u65b0\\u5bc6\\u7801',
  confirmPwdPh: '\\u8bf7\\u518d\\u6b21\\u8f93\\u5165\\u65b0\\u5bc6\\u7801',
  nickRequired: '\\u8bf7\\u8f93\\u5165\\u6635\\u79f0',
  emailRequired: '\\u8bf7\\u8f93\\u5165\\u90ae\\u7bb1',
  emailInvalid: '\\u90ae\\u7bb1\\u683c\\u5f0f\\u4e0d\\u6b63\\u786e',
  phoneRequired: '\\u8bf7\\u8f93\\u5165\\u624b\\u673a\\u53f7',
  phoneInvalid: '\\u624b\\u673a\\u53f7\\u683c\\u5f0f\\u4e0d\\u6b63\\u786e',
  oldPwdRequired: '\\u8bf7\\u8f93\\u5165\\u65e7\\u5bc6\\u7801',
  confirmRequired: '\\u8bf7\\u786e\\u8ba4\\u5bc6\\u7801',
  pwdMismatch: '\\u4e24\\u6b21\\u5bc6\\u7801\\u4e0d\\u4e00\\u81f4',
  loadFail: '\\u52a0\\u8f7d\\u8d44\\u6599\\u5931\\u8d25',
  saveOk: '\\u4fdd\\u5b58\\u6210\\u529f',
  pwdOk: '\\u5bc6\\u7801\\u4fee\\u6539\\u6210\\u529f'
}
`)

write('views/portal/portal-auth-labels.js', `export const authLabels = {
  login: '\\u767b\\u5f55',
  register: '\\u6ce8\\u518c',
  loginTitle: '\\u7528\\u6237\\u767b\\u5f55',
  registerTitle: '\\u7528\\u6237\\u6ce8\\u518c',
  loginSubtitle: '\\u767b\\u5f55\\u540e\\u53ef\\u9009\\u9898\\u3001\\u7ec4\\u5377\\u4e0e\\u5bfc\\u51fa\\u8bd5\\u5377',
  registerSubtitle: '\\u6ce8\\u518c\\u6559\\u5e08\\u8d26\\u53f7\\u540e\\u5373\\u53ef\\u4f7f\\u7528\\u9898\\u5e93',
  usernamePh: '\\u8bf7\\u8f93\\u5165\\u8d26\\u53f7',
  passwordPh: '\\u8bf7\\u8f93\\u5165\\u5bc6\\u7801',
  confirmPasswordPh: '\\u8bf7\\u518d\\u6b21\\u8f93\\u5165\\u5bc6\\u7801',
  codePh: '\\u9a8c\\u8bc1\\u7801',
  rememberMe: '\\u8bb0\\u4f4f\\u5bc6\\u7801',
  loginBtn: '\\u767b \\u5f55',
  loginLoading: '\\u767b\\u5f55\\u4e2d...',
  registerBtn: '\\u6ce8 \\u518c',
  registerLoading: '\\u6ce8\\u518c\\u4e2d...',
  noAccount: '\\u6ca1\\u6709\\u8d26\\u53f7\\uff1f\\u7acb\\u5373\\u6ce8\\u518c',
  hasAccount: '\\u5df2\\u6709\\u8d26\\u53f7\\uff1f\\u53bb\\u767b\\u5f55',
  adminLogin: '\\u7ba1\\u7406\\u5458\\u767b\\u5f55',
  usernameRequired: '\\u8bf7\\u8f93\\u5165\\u8d26\\u53f7',
  passwordRequired: '\\u8bf7\\u8f93\\u5165\\u5bc6\\u7801',
  codeRequired: '\\u8bf7\\u8f93\\u5165\\u9a8c\\u8bc1\\u7801',
  loginSuccess: '\\u767b\\u5f55\\u6210\\u529f',
  registerSuccess: '\\u6ce8\\u518c\\u6210\\u529f'
}
`)

write('views/portal/portal-mypapers-labels.js', `export const myPaperLabels = {
  home: '\\u9996\\u9875',
  breadcrumb: '\\u6211\\u7684\\u8bd5\\u5377',
  loginRequired: '\\u8bf7\\u767b\\u5f55\\u540e\\u67e5\\u770b\\u6211\\u7684\\u8bd5\\u5377',
  goLogin: '\\u53bb\\u767b\\u5f55',
  title: '\\u6211\\u7684\\u8bd5\\u5377',
  subtitle: '\\u67e5\\u770b\\u4e0e\\u7ba1\\u7406\\u5df2\\u4fdd\\u5b58\\u7684\\u7ec4\\u5377\\u8bb0\\u5f55',
  searchPh: '\\u641c\\u7d22\\u8bd5\\u5377\\u6807\\u9898',
  search: '\\u641c\\u7d22',
  refresh: '\\u5237\\u65b0',
  colTitle: '\\u8bd5\\u5377\\u6807\\u9898',
  colCount: '\\u9898\\u91cf',
  colScore: '\\u603b\\u5206',
  colTime: '\\u4fdd\\u5b58\\u65f6\\u95f4',
  colAction: '\\u64cd\\u4f5c',
  open: '\\u6253\\u5f00',
  delete: '\\u5220\\u9664',
  empty: '\\u6682\\u65e0\\u4fdd\\u5b58\\u7684\\u8bd5\\u5377',
  goPick: '\\u53bb\\u9009\\u9898'
}
`)


console.log('Portal core utils and labels restored.')
