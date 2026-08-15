const fs = require('fs')
const path = require('path')

const src = path.join(__dirname, '../src')

function write(rel, content) {
  const file = path.join(src, rel)
  fs.mkdirSync(path.dirname(file), { recursive: true })
  fs.writeFileSync(file, content, 'utf8')
  console.log('wrote', rel)
}

const T = {
  portalName: '\u667a\u6167\u9898\u5e93',
  logoIcon: '\u5377',
  searchPh: '\u8bf7\u8f93\u5165\u9898\u5e72\u3001\u77e5\u8bc6\u70b9\u6216\u8bd5\u5377\u540d\u79f0',
  search: '\u641c\u7d22',
  hot: '\u70ed\u641c\uff1a',
  admin: '\u7ba1\u7406\u540e\u53f0',
  logout: '\u9000\u51fa',
  login: '\u767b\u5f55',
  register: '\u6ce8\u518c',
  highSchool: '\u9ad8\u4e2d',
  footer: '\u667a\u6167\u9898\u5e93 \u00b7 \u6559\u5e08\u9009\u9898\u7ec4\u5377\u5e73\u53f0',
  footerAdmin: '\u8fdb\u5165\u7ba1\u7406\u540e\u53f0',
  basket: '\u8bd5\u9898\u7bee',
  top: '\u9876\u90e8',
  home: '\u9996\u9875',
  chapterPick: '\u7ae0\u8282\u9009\u9898',
  knowledgePick: '\u77e5\u8bc6\u70b9\u9009\u9898',
  paperPick: '\u8bd5\u5377\u9009\u9898',
  smartPaper: '\u667a\u80fd\u7ec4\u5377',
  pickSubject: '\u9009\u62e9\u5b66\u79d1',
  primary: '\u5c0f\u5b66',
  junior: '\u521d\u4e2d',
  vocational: '\u4e2d\u804c',
  physics: '\u7269\u7406',
  addedBasket: '\u5df2\u52a0\u5165\u8bd5\u9898\u7bee',
  basketFull: '\u8bd5\u9898\u7bee\u5df2\u6ee1',
  loginTip: '\u9009\u9898\u4e0e\u7ec4\u5377\u9700\u5148\u767b\u5f55\uff0c\u662f\u5426\u524d\u5f80\u767b\u5f55\uff1f',
  tip: '\u63d0\u793a',
  goLogin: '\u53bb\u767b\u5f55',
  cancel: '\u53d6\u6d88',
  browseLogin: '\u6d4f\u89c8\u8bd5\u9898\u9700\u5148\u767b\u5f55\u3002\u767b\u5f55\u540e\u53ef\u7b5b\u9009\u3001\u52a0\u7bee\u3001\u7ec4\u5377\u3002',
  loginNow: '\u7acb\u5373\u767b\u5f55',
  knowledge: '\u77e5\u8bc6\u70b9',
  searchKnowledge: '\u641c\u7d22\u77e5\u8bc6\u70b9',
  all: '\u5168\u90e8',
  noKnowledge: '\u6682\u65e0\u77e5\u8bc6\u70b9',
  questions: '\u8bd5\u9898',
  syncPaper: '\u540c\u6b65\u8bd5\u5377',
  sortDefault: '\u7efc\u5408',
  sortLatest: '\u6700\u65b0',
  searchInResult: '\u5728\u7ed3\u679c\u4e2d\u641c\u7d22\u9898\u5e72',
  totalQuestions: '\u9053\u8bd5\u9898',
  totalPrefix: '\u5171\u8ba1',
  oneClickPaper: '\u4e00\u952e\u7ec4\u5377',
  noQuestions: '\u6682\u65e0\u8bd5\u9898',
  loginToView: '\u8bf7\u767b\u5f55\u540e\u67e5\u770b\u8bd5\u9898',
  detail: '\u8bd5\u9898\u8be6\u60c5',
  difficulty: '\u96be\u5ea6',
  addBasket: '\u52a0\u5165\u8bd5\u9898\u7bee',
  back: '\u8fd4\u56de',
  loginForDetail: '\u8bf7\u767b\u5f55\u540e\u67e5\u770b\u8bd5\u9898\u8be6\u60c5',
  paperDesc: '\u4ece\u8bd5\u9898\u7bee\u9009\u62e9\u8bd5\u9898\u540e\uff0c\u53ef\u5728\u6b64\u9884\u89c8\u5e76\u5bfc\u51fa\u8bd5\u5377\u3002',
  goPreview: '\u8fdb\u5165\u7ec4\u5377\u9884\u89c8',
  continuePick: '\u7ee7\u7eed\u9009\u9898',
  basketCount: '\u5f53\u524d\u8bd5\u9898\u7bee\uff1a',
  ti: '\u9898',
  startPick: '\u7acb\u5373\u9009\u9898',
  platformData: '\u5e73\u53f0\u6570\u636e',
  totalCount: '\u8bd5\u9898\u603b\u91cf',
  recent7: '\u8fd17\u65e5\u65b0\u589e',
  loginRegister: '\u767b\u5f55 / \u6ce8\u518c',
  hotTopics: '\u70ed\u95e8\u4e13\u9898',
  examZone: '\u5907\u8003\u4e13\u533a',
  enter: '\u8fdb\u5165 \u2192'
}

write('layout-portal/components/PortalHeader.vue', `<template>
  <div class="portal-header">
    <div class="portal-topbar">
      <div class="portal-container topbar-inner">
        <div class="topbar-left">
          <router-link to="/portal/home" class="portal-logo">
            <span class="logo-icon">${T.logoIcon}</span>
            <span class="logo-text">${T.portalName}</span>
          </router-link>
        </div>
        <div class="topbar-search">
          <el-input
            v-model="keyword"
            placeholder="${T.searchPh}"
            clearable
            @keyup.enter.native="onSearch"
          >
            <el-button slot="append" type="primary" icon="el-icon-search" @click="onSearch">${T.search}</el-button>
          </el-input>
          <div v-if="hotWords.length" class="hot-words">
            ${T.hot}
            <span
              v-for="w in hotWords"
              :key="w"
              class="hot-word"
              @click="searchWord(w)"
            >{{ w }}</span>
          </div>
        </div>
        <div class="topbar-right">
          <template v-if="token">
            <span class="user-name">{{ nickName || name }}</span>
            <el-button type="text" @click="goAdmin">${T.admin}</el-button>
            <el-button type="text" @click="logout">${T.logout}</el-button>
          </template>
          <template v-else>
            <el-button type="text" @click="goLogin">${T.login}</el-button>
            <el-button type="primary" size="small" @click="goLogin">${T.register}</el-button>
          </template>
        </div>
      </div>
    </div>
    <portal-nav :subject-id.sync="subjectId" :school-stage.sync="schoolStage" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import PortalNav from './PortalNav'

export default {
  name: 'PortalHeader',
  components: { PortalNav },
  data() {
    return {
      keyword: '',
      hotWords: ['\u725b\u987f\u7b2c\u4e00\u5b9a\u5f8b', '\u5300\u53d8\u901f\u76f4\u7ebf\u8fd0\u52a8', '\u53d7\u529b\u5206\u6790', '\u52a8\u91cf\u5b88\u6052'],
      subjectId: null,
      schoolStage: '${T.highSchool}'
    }
  },
  computed: {
    ...mapGetters(['token', 'name', 'nickName'])
  },
  watch: {
    '$route.query.subjectId': {
      immediate: true,
      handler(val) {
        if (val != null && val !== '') this.subjectId = Number(val)
      }
    }
  },
  methods: {
    onSearch() {
      const kw = (this.keyword || '').trim()
      this.$router.push({ path: '/portal/chapter', query: kw ? { keyword: kw } : {} })
    },
    searchWord(w) {
      this.keyword = w
      this.onSearch()
    },
    goLogin() {
      this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
    },
    goAdmin() {
      this.$router.push('/')
    },
    logout() {
      this.$store.dispatch('LogOut').then(() => {
        this.$router.push('/portal/home')
      })
    }
  }
}
</script>

<style scoped lang="scss">
.portal-topbar {
  background: #fff;
  border-bottom: 1px solid #e8edf3;
  padding: 14px 0 10px;
}

.topbar-inner {
  display: flex;
  align-items: flex-start;
  gap: 24px;
}

.portal-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: #2877ff;
  font-weight: 700;
  font-size: 22px;
  white-space: nowrap;
}

.logo-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #2877ff, #5aa0ff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.topbar-search {
  flex: 1;
  min-width: 0;
}

.hot-words {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.hot-word {
  margin-right: 12px;
  color: #f56c6c;
  cursor: pointer;
  &:hover { text-decoration: underline; }
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  padding-top: 4px;
}

.user-name {
  color: #606266;
  font-size: 13px;
  margin-right: 4px;
}
</style>
`)

write('layout-portal/components/PortalNav.vue', `<template>
  <div class="portal-nav">
    <div class="portal-container nav-inner">
      <div class="subject-dropdown" @mouseenter="panelOpen = true" @mouseleave="panelOpen = false">
        <button type="button" class="subject-btn">
          {{ currentSubjectLabel }}
          <i class="el-icon-arrow-down" />
        </button>
        <div v-show="panelOpen" class="subject-panel portal-card">
          <div class="stage-tabs">
            <span
              v-for="s in stages"
              :key="s"
              :class="['stage-tab', { active: schoolStage === s }]"
              @click="pickStage(s)"
            >{{ s }}</span>
          </div>
          <div class="subject-grid">
            <span
              v-for="item in subjectOptions"
              :key="item.subjectId"
              :class="['subject-item', { active: subjectId === item.subjectId }]"
              @click="pickSubject(item)"
            >{{ item.subjectName }}</span>
          </div>
        </div>
      </div>
      <nav class="nav-links">
        <router-link
          v-for="item in navItems"
          :key="item.path + item.label"
          :to="item.path"
          class="nav-link"
          active-class="active"
          exact
        >{{ item.label }}</router-link>
      </nav>
    </div>
  </div>
</template>

<script>
import { listSubject } from '@/api/education/subject'

export default {
  name: 'PortalNav',
  props: {
    subjectId: { type: [Number, String], default: null },
    schoolStage: { type: String, default: '${T.highSchool}' }
  },
  data() {
    return {
      panelOpen: false,
      subjectOptions: [],
      stages: ['${T.primary}', '${T.junior}', '${T.highSchool}', '${T.vocational}'],
      navItems: [
        { label: '${T.home}', path: '/portal/home' },
        { label: '${T.chapterPick}', path: '/portal/chapter' },
        { label: '${T.knowledgePick}', path: '/portal/knowledge' },
        { label: '${T.paperPick}', path: '/portal/chapter' },
        { label: '${T.smartPaper}', path: '/portal/paper' }
      ]
    }
  },
  computed: {
    currentSubjectLabel() {
      const hit = this.subjectOptions.find(s => s.subjectId === this.subjectId)
      return hit ? hit.subjectName : '${T.pickSubject}'
    }
  },
  created() {
    this.loadSubjects()
  },
  methods: {
    loadSubjects() {
      listSubject({ pageNum: 1, pageSize: 100, status: '0' }).then(res => {
        this.subjectOptions = res.rows || []
        if (!this.subjectId && this.subjectOptions.length) {
          const physics = this.subjectOptions.find(s => (s.subjectName || '').includes('${T.physics}'))
          this.$emit('update:subjectId', (physics || this.subjectOptions[0]).subjectId)
        }
      })
    },
    pickStage(stage) {
      this.$emit('update:schoolStage', stage)
    },
    pickSubject(item) {
      this.$emit('update:subjectId', item.subjectId)
      this.panelOpen = false
      const path = this.$route.path
      const target = path === '/portal/knowledge' ? '/portal/knowledge' : '/portal/chapter'
      this.$router.push({
        path: target,
        query: { ...this.$route.query, subjectId: item.subjectId }
      })
    }
  }
}
</script>

<style scoped lang="scss">
.portal-nav {
  background: #fff;
  border-bottom: 2px solid #2877ff;
}

.nav-inner {
  display: flex;
  align-items: stretch;
  min-height: 48px;
}

.subject-dropdown {
  position: relative;
  flex-shrink: 0;
}

.subject-btn {
  height: 48px;
  min-width: 130px;
  padding: 0 16px;
  border: none;
  background: #2877ff;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.subject-panel {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 2000;
  width: 520px;
  padding: 12px 16px 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.stage-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  border-bottom: 1px solid #eee;
  padding-bottom: 8px;
}

.stage-tab {
  padding: 4px 12px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  border-radius: 2px;
  &.active {
    color: #2877ff;
    background: #ecf3ff;
    font-weight: 600;
  }
}

.subject-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.subject-item {
  padding: 6px 8px;
  font-size: 13px;
  text-align: center;
  border-radius: 2px;
  cursor: pointer;
  color: #303133;
  &:hover { background: #f5f7fa; color: #2877ff; }
  &.active { background: #2877ff; color: #fff; }
}

.nav-links {
  display: flex;
  align-items: stretch;
  flex: 1;
  overflow-x: auto;
}

.nav-link {
  display: flex;
  align-items: center;
  padding: 0 18px;
  color: #303133;
  font-size: 15px;
  text-decoration: none;
  white-space: nowrap;
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  &:hover { color: #2877ff; }
  &.active {
    color: #2877ff;
    font-weight: 600;
    border-bottom-color: #2877ff;
  }
}
</style>
`)

write('layout-portal/components/PortalFloatingBar.vue', `<template>
  <div class="portal-float-bar">
    <div class="float-item basket" @click="onBasketClick">
      <el-badge :value="count" :hidden="!count">
        <i class="el-icon-shopping-cart-2" />
      </el-badge>
      <span>${T.basket}</span>
    </div>
    <div class="float-item" @click="scrollTop">
      <i class="el-icon-top" />
      <span>${T.top}</span>
    </div>
    <question-basket-drawer v-model="openBasket" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import QuestionBasketDrawer from '@/views/education/question-bank/components/QuestionBasketDrawer'

export default {
  name: 'PortalFloatingBar',
  components: { QuestionBasketDrawer },
  data() {
    return { openBasket: false }
  },
  computed: {
    ...mapGetters(['questionBasketCount', 'token']),
    count() {
      return this.questionBasketCount
    }
  },
  methods: {
    onBasketClick() {
      if (!this.token) {
        this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
        return
      }
      this.openBasket = true
    },
    scrollTop() {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }
}
</script>

<style scoped lang="scss">
.portal-float-bar {
  position: fixed;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  z-index: 1900;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #e8edf3;
  border-right: none;
  border-radius: 6px 0 0 6px;
  box-shadow: -2px 0 12px rgba(0, 0, 0, 0.06);
}

.float-item {
  width: 52px;
  padding: 12px 8px;
  text-align: center;
  font-size: 11px;
  color: #606266;
  cursor: pointer;
  border-bottom: 1px solid #f0f2f5;
  i { font-size: 20px; display: block; margin-bottom: 4px; }
  &:hover { color: #2877ff; background: #f5f9ff; }
  &:last-child { border-bottom: none; }
}

.float-item.basket {
  color: #e6a23c;
  i { color: #e6a23c; }
}
</style>
`)

write('layout-portal/index.vue', `<template>
  <div class="portal-layout">
    <portal-header />
    <main class="portal-main">
      <router-view />
    </main>
    <portal-floating-bar />
    <footer class="portal-footer">
      <div class="portal-container">
        <span>${T.footer}</span>
        <router-link to="/" class="footer-admin-link">${T.footerAdmin}</router-link>
      </div>
    </footer>
  </div>
</template>

<script>
import PortalHeader from './components/PortalHeader'
import PortalFloatingBar from './components/PortalFloatingBar'

export default {
  name: 'PortalLayout',
  components: { PortalHeader, PortalFloatingBar }
}
</script>

<style scoped lang="scss">
.portal-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f4f6f9;
}

.portal-main {
  flex: 1;
}

.portal-footer {
  background: #fff;
  border-top: 1px solid #e8edf3;
  padding: 16px 0;
  font-size: 13px;
  color: #909399;
  text-align: center;
}

.footer-admin-link {
  margin-left: 16px;
  color: #2877ff;
}
</style>
`)

console.log('portal encoding fix complete')
