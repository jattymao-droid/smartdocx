import { openPortalAuth } from '@/utils/portalAuth'
import { PORTAL_HOME, PORTAL_LOGIN, isPortalPath } from '@/constants/routes'

export { PORTAL_LOGIN as PORTAL_LOGIN_PATH }

export { isPortalPath }

export function goPortalLogin(router, redirect, tab = 'login') {
  const target = redirect || PORTAL_HOME
  if (router && router.currentRoute && isPortalPath(router.currentRoute.path)) {
    openPortalAuth({ redirect: target, tab })
    return
  }
  router.push({ path: PORTAL_LOGIN, query: { redirect: target, tab } })
}
