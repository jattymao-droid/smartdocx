export const ADMIN_PREFIX = '/admin'
export const ADMIN_HOME = '/admin'
export const ADMIN_INDEX = '/admin/index'
export const ADMIN_LOGIN = '/admin/login'
export const ADMIN_REGISTER = '/admin/register'
export const ADMIN_LOCK = '/admin/lock'
export const ADMIN_REDIRECT = '/admin/redirect'
export const ADMIN_USER_PROFILE = '/admin/user/profile'

export const PORTAL_HOME = '/'
export const PORTAL_LOGIN = '/login'

export function prefixAdminPath(path) {
  if (!path || typeof path !== 'string') return path
  if (path.startsWith(ADMIN_PREFIX)) return path
  if (path.startsWith('http')) return path
  return ADMIN_PREFIX + path
}

export function isAdminPath(path) {
  return !!(path && String(path).startsWith(ADMIN_PREFIX))
}

export function isPortalPath(path) {
  if (!path) return false
  const p = String(path)
  if (isAdminPath(p)) return false
  if (p === '/404' || p === '/401') return false
  return true
}
