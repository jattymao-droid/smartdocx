const getters = {
  sidebar: state => state.app.sidebar,
  size: state => state.app.size,
  device: state => state.app.device,
  dict: state => state.dict.dict,
  isLock: state => state.lock.isLock,
  lockPath: state => state.lock.lockPath,
  visitedViews: state => state.tagsView.visitedViews,
  cachedViews: state => state.tagsView.cachedViews,
  token: state => state.user.token,
  avatar: state => state.user.avatar,
  id: state => state.user.id,
  name: state => state.user.name,
  nickName: state => state.user.nickName,
  introduction: state => state.user.introduction,
  roles: state => state.user.roles,
  permissions: state => state.user.permissions,
  permission_routes: state => state.permission.routes,
  topbarRouters: state => state.permission.topbarRouters,
  defaultRoutes: state => state.permission.defaultRoutes,
  sidebarRouters: state => state.permission.sidebarRouters,
  questionBasketCount: state => state.questionBasket.items.length,
  questionBasketItems: state => state.questionBasket.items,
  questionBasketTotalScore: state => state.questionBasket.items.reduce(
    (sum, i) => sum + (Number(i.scoreValue) || 0),
    0
  )
}
export default getters
