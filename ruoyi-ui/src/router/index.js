import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

/* Layout */
const Layout = () => import('@/layout')
const PortalLayout = () => import('@/layout-portal')
import {
  ADMIN_PREFIX,
  ADMIN_INDEX,
  ADMIN_LOGIN,
  ADMIN_REGISTER,
  ADMIN_LOCK,
  ADMIN_REDIRECT,
  PORTAL_LOGIN,
  prefixAdminPath
} from '@/constants/routes'

/**
 * Note: 路由配置项
 *
 * hidden: true                     // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true                 // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                  // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                  // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                  // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect             // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'               // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * query: '{"id": 1, "name": "ry"}' // 访问路由的默认传递参数
 * roles: ['admin', 'common']       // 访问路由的角色权限
 * permissions: ['a:a:a', 'b:b:b']  // 访问路由的菜单权限
 * meta : {
    noCache: true                   // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'                  // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'                // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false               // 如果设置为false，则不会在breadcrumb面包屑中显示
    activeMenu: '/system/user'      // 当路由设置了该属性，则会高亮相对应的侧边栏。
  }
 */

// 公共路由
export const constantRoutes = [
  { path: '/portal', redirect: '/' },
  { path: '/portal/home', redirect: '/' },
  { path: '/portal/:path(.*)', redirect: to => (to.params.path === 'home' ? '/' : '/' + to.params.path) },
  { path: '/index', redirect: ADMIN_INDEX },
  {
    path: ADMIN_REDIRECT,
    component: Layout,
    hidden: true,
    children: [
      {
        path: ADMIN_REDIRECT + '/:path(.*)',
        component: () => import('@/views/redirect')
      }
    ]
  },
  {
    path: ADMIN_LOGIN,
    component: () => import('@/views/login'),
    hidden: true
  },
  {
    path: ADMIN_REGISTER,
    component: () => import('@/views/register'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/error/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401'),
    hidden: true
  },
  {
    path: PORTAL_LOGIN,
    component: () => import('@/views/portal/Login'),
    hidden: true,
    meta: { title: '用户登录' }
  },
  {
    path: '/',
    component: PortalLayout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/portal/Home'),
        name: 'PortalHome',
        meta: { title: '门户首页' }
      },
      {
        path: 'chapter',
        component: () => import('@/views/portal/PortalBrowse'),
        name: 'PortalChapter',
        meta: { title: '章节选题' },
        props: { mode: 'chapter' }
      },
      {
        path: 'knowledge',
        component: () => import('@/views/portal/PortalBrowse'),
        name: 'PortalKnowledge',
        meta: { title: '知识点选题' },
        props: { mode: 'knowledge' }
      },
      {
        path: 'exam',
        component: () => import('@/views/portal/PortalBrowse'),
        name: 'PortalExam',
        meta: { title: '试卷选题' },
        props: { mode: 'exam' }
      },
      {
        path: 'paper',
        component: () => import('@/views/portal/Paper'),
        name: 'PortalPaper',
        meta: { title: '智能组卷' }
      },
      {
        path: 'paper/preview',
        component: () => import('@/views/education/question-bank/paper/preview'),
        name: 'PortalPaperPreview',
        meta: { title: '组卷预览' },
        props: { portalMode: true }
      },
      {
        path: 'question/:questionId',
        component: () => import('@/views/portal/QuestionDetail'),
        name: 'PortalQuestionDetail',
        meta: { title: '试题详情' }
      },
      {
        path: 'profile',
        component: () => import('@/views/portal/Profile'),
        name: 'PortalProfile',
        meta: { title: '个人中心' }
      },
      {
        path: 'my-papers',
        component: () => import('@/views/portal/MyPapers'),
        name: 'PortalMyPapers',
        meta: { title: '我的试卷' }
      },
      {
        path: 'my-purchases',
        component: () => import('@/views/portal/MyPurchases'),
        name: 'PortalMyPurchases',
        meta: { title: '我的购买记录' }
      },
      {
        path: 'library/upload',
        component: () => import('@/views/portal/library/Upload'),
        name: 'PortalLibraryUpload',
        meta: { title: '上传文档' }
      },
      {
        path: 'library/vip',
        component: () => import('@/views/portal/library/Vip'),
        name: 'PortalLibraryVip',
        meta: { title: 'VIP会员' }
      },
      {
        path: 'library/topics',
        component: () => import('@/views/portal/library/TopicList'),
        name: 'PortalLibraryTopics',
        meta: { title: '热门专题' }
      },
      {
        path: 'library/topic/:topicId(\\d+)',
        component: () => import('@/views/portal/library/TopicDetail'),
        name: 'PortalLibraryTopic',
        meta: { title: '热门专题' }
      },
      {
        path: 'library',
        component: () => import('@/views/portal/library/index'),
        name: 'PortalLibrary',
        meta: { title: '文库' }
      },
      {
        path: 'library/:documentId(\\d+)',
        component: () => import('@/views/portal/library/Detail'),
        name: 'PortalLibraryDetail',
        meta: { title: '文档详情' }
      }
    ]
  },
  {
    path: ADMIN_PREFIX,
    component: Layout,
    redirect: ADMIN_INDEX,
    children: [
      {
        path: 'index',
        component: () => import('@/views/index'),
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: ADMIN_LOCK,
    component: () => import('@/views/lock'),
    hidden: true,
    meta: { title: '锁定屏幕' }
  },
  {
    path: ADMIN_PREFIX + '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile',
        component: () => import('@/views/system/user/profile/index'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  }
]

// 动态路由，基于用户权限动态去加载
export const dynamicRoutes = [
  {
    path: prefixAdminPath('/system/user-auth'),
    component: Layout,
    hidden: true,
    permissions: ['system:user:edit'],
    children: [
      {
        path: 'role/:userId(\\d+)',
        component: () => import('@/views/system/user/authRole'),
        name: 'AuthRole',
        meta: { title: '分配角色', activeMenu: prefixAdminPath('/system/user') }
      }
    ]
  },
  {
    path: prefixAdminPath('/system/role-auth'),
    component: Layout,
    hidden: true,
    permissions: ['system:role:edit'],
    children: [
      {
        path: 'user/:roleId(\\d+)',
        component: () => import('@/views/system/role/authUser'),
        name: 'AuthUser',
        meta: { title: '分配用户', activeMenu: prefixAdminPath('/system/role') }
      }
    ]
  },
  {
    path: prefixAdminPath('/system/dict-data'),
    component: Layout,
    hidden: true,
    permissions: ['system:dict:list'],
    children: [
      {
        path: 'index/:dictId(\\d+)',
        component: () => import('@/views/system/dict/data'),
        name: 'Data',
        meta: { title: '字典数据', activeMenu: prefixAdminPath('/system/dict') }
      }
    ]
  },
  {
    path: prefixAdminPath('/monitor/job-log'),
    component: Layout,
    hidden: true,
    permissions: ['monitor:job:list'],
    children: [
      {
        path: 'index/:jobId(\\d+)',
        component: () => import('@/views/monitor/job/log'),
        name: 'JobLog',
        meta: { title: '调度日志', activeMenu: prefixAdminPath('/monitor/job') }
      }
    ]
  },
  {
    path: prefixAdminPath('/tool/gen-edit'),
    component: Layout,
    hidden: true,
    permissions: ['tool:gen:edit'],
    children: [
      {
        path: 'index/:tableId(\\d+)',
        component: () => import('@/views/tool/gen/editTable'),
        name: 'GenEdit',
        meta: { title: '修改生成配置', activeMenu: prefixAdminPath('/tool/gen') }
      }
    ]
  },
  {
    path: prefixAdminPath('/question-bank-center'),
    component: Layout,
    hidden: true,
    permissions: ['education:question:list', 'education:question:add', 'education:question:import', 'education:question:audit'],
    children: [
      {
        path: 'question-create/docx',
        component: () => import('@/views/education/question-bank/import/DocxImport'),
        name: 'QuestionCreateDocxImport',
        meta: { title: 'DOCX导入', activeMenu: prefixAdminPath('/question-bank/question-create') }
      },
      {
        path: 'question-create/ocr',
        component: () => import('@/views/education/question-bank/import/OcrImport'),
        name: 'QuestionCreateOcrImport',
        meta: { title: 'OCR导入', activeMenu: prefixAdminPath('/question-bank/question-create') }
      },
      {
        path: 'question-create/history',
        component: () => import('@/views/education/question-bank/import/ImportHistory'),
        name: 'QuestionImportHistory',
        meta: { title: '导入记录', activeMenu: prefixAdminPath('/question-bank/question-create') }
      },
      {
        path: 'question-create/audit',
        component: () => import('@/views/education/question-bank/audit/QuestionAudit'),
        name: 'QuestionAudit',
        meta: { title: '题库审核', activeMenu: prefixAdminPath('/question-bank/question-create') }
      }
    ]
  },
  {
    path: prefixAdminPath('/question-bank-center'),
    component: Layout,
    hidden: true,
    permissions: ['education:paper:preview', 'education:question:list', 'education:question:add'],
    children: [
      {
        path: 'question-bank/paper/preview',
        component: () => import('@/views/education/question-bank/paper/preview'),
        name: 'PaperPreview',
        meta: { title: '组卷预览', activeMenu: prefixAdminPath('/question-bank') }
      },
      {
        path: 'question-bank/paper/practice',
        component: () => import('@/views/education/question-bank/paper/PaperPractice'),
        name: 'PaperPractice',
        meta: { title: '在线练习', activeMenu: prefixAdminPath('/question-bank') }
      }
    ]
  },
  {
    path: prefixAdminPath('/question-bank-center'),
    component: Layout,
    hidden: true,
    permissions: ['education:exam-paper:list', 'education:exam-paper:add', 'education:exam-paper:query'],
    children: [
      {
        path: 'exam-paper',
        component: () => import('@/views/education/exam-paper/index'),
        name: 'ExamPaperManage',
        meta: { title: '试卷选题管理', activeMenu: prefixAdminPath('/question-bank-center/exam-paper') }
      },
      {
        path: 'exam-paper/mark',
        component: () => import('@/views/education/exam-paper/mark'),
        name: 'ExamPaperMark',
        meta: { title: '智能标记题目', activeMenu: prefixAdminPath('/question-bank-center/exam-paper') }
      }
    ]
  }
]

// 防止连续点击多次路由报错
let routerPush = Router.prototype.push
let routerReplace = Router.prototype.replace
// push
Router.prototype.push = function push(location) {
  return routerPush.call(this, location).catch(err => err)
}
// replace
Router.prototype.replace = function push(location) {
  return routerReplace.call(this, location).catch(err => err)
}

export default new Router({
  mode: 'history', // 去掉url中的#
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})
