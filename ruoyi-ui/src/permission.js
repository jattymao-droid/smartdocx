import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'
import { goPortalLogin } from '@/utils/portalLogin'
import {
  ADMIN_HOME,
  ADMIN_LOGIN,
  ADMIN_REGISTER,
  ADMIN_LOCK,
  isAdminPath,
  isPortalPath
} from '@/constants/routes'

let adminIconsLoaded = false
function ensureAdminIcons() {
  if (!adminIconsLoaded) {
    adminIconsLoaded = true
    import('@/assets/icons')
  }
}

NProgress.configure({ showSpinner: false })

const whiteList = [ADMIN_LOGIN, ADMIN_REGISTER]

const portalPublicPaths = [
  '/',
  '/login',
  '/chapter',
  '/knowledge',
  '/exam',
  '/paper'
]

const isPortalPublic = (path) => {
  return portalPublicPaths.some(pattern => isPathMatch(pattern, path))
}

const isWhiteList = (path) => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

router.beforeEach((to, from, next) => {
  if (isAdminPath(to.path)) {
    ensureAdminIcons()
  }
  NProgress.start()
  if (getToken()) {
    to.meta.title && store.dispatch('settings/setTitle', to.meta.title)
    const isLock = store.getters.isLock
    if (to.path === ADMIN_LOGIN) {
      next({ path: ADMIN_HOME })
      NProgress.done()
    } else if (isWhiteList(to.path)) {
      if (store.getters.roles.length === 0) {
        isRelogin.show = true
        store.dispatch('GetInfo').then(() => {
          isRelogin.show = false
        }).catch(() => {
          isRelogin.show = false
        })
      }
      next()
    } else if (isLock && to.path !== ADMIN_LOCK) {
      next({ path: ADMIN_LOCK })
      NProgress.done()
    } else if (!isLock && to.path === ADMIN_LOCK) {
      next({ path: ADMIN_HOME })
      NProgress.done()
    } else if (isPortalPath(to.path)) {
      next()
    } else {
      if (store.getters.roles.length === 0) {
        isRelogin.show = true
        store.dispatch('GetInfo').then(() => {
          isRelogin.show = false
          store.dispatch('GenerateRoutes').then(accessRoutes => {
            router.addRoutes(accessRoutes)
            next({ ...to, replace: true })
          })
        }).catch(err => {
          store.dispatch('LogOut').then(() => {
            Message.error(err)
            next({ path: ADMIN_HOME })
          })
        })
      } else {
        next()
      }
    }
  } else {
    if (isWhiteList(to.path) || isPortalPublic(to.path)) {
      next()
    } else if (isPortalPath(to.path)) {
      goPortalLogin(router, to.fullPath)
      NProgress.done()
    } else if (isAdminPath(to.path)) {
      next(`${ADMIN_LOGIN}?redirect=${encodeURIComponent(to.fullPath)}`)
      NProgress.done()
    } else {
      next()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
