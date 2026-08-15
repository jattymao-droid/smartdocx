/**
 * Rewrite portal layout shell (header + layout index) with ASCII-only \\u escapes.
 * Must run LAST in fix-portal-all.js so later patches cannot corrupt Chinese text.
 */
const fs = require('fs')
const path = require('path')

const src = path.join(__dirname, '../src')

function write(rel, content) {
  const f = path.join(src, rel)
  fs.mkdirSync(path.dirname(f), { recursive: true })
  fs.writeFileSync(f, content, 'utf8')
  console.log('wrote', rel)
}

const T = {
  portalName: '\u4e1c\u9646\u667a\u80fd\u6559\u5b66\u5e93',
  logoSlogan: '\u6559\u5e08\u9009\u9898\u7ec4\u5377',
  searchPh: '\u8bf7\u8f93\u5165\u9898\u5e72\u3001\u77e5\u8bc6\u70b9\u6216\u8bd5\u5377\u540d\u79f0',
  search: '\u641c\u7d22',
  hot: '\u70ed\u641c\uff1a',
  admin: '\u7ba1\u7406\u540e\u53f0',
  logout: '\u9000\u51fa',
  login: '\u767b\u5f55',
  register: '\u6ce8\u518c',
  highSchool: '\u9ad8\u4e2d',
  footerTagline: '\u6821\u5185\u6559\u5e08\u9009\u9898\u7ec4\u5377\u5e73\u53f0',
  chapterPick: '\u7ae0\u8282\u9009\u9898',
  knowledgePick: '\u77e5\u8bc6\u70b9\u9009\u9898',
  smartPaper: '\u667a\u80fd\u7ec4\u5377',
  menuProfile: '\u4e2a\u4eba\u4e2d\u5fc3',
  menuMyPapers: '\u6211\u7684\u8bd5\u5377'
}

write('layout-portal/components/PortalHeader.vue', `<template>
  <div class="portal-header">
    <div class="portal-topbar">
      <div class="portal-container topbar-inner">
        <div class="topbar-left">
          <router-link to="/portal/home" class="portal-logo">
            <span class="logo-icon"><i class="el-icon-notebook-2" /></span>
            <span class="logo-text-wrap">
              <span class="logo-text">${T.portalName}</span>
              <span class="logo-slogan">${T.logoSlogan}</span>
            </span>
          </router-link>
        </div>
        <div class="topbar-search">
          <div class="search-bar">
            <el-input
              v-model="keyword"
              class="search-input"
              placeholder="${T.searchPh}"
              clearable
              prefix-icon="el-icon-search"
              @keyup.enter.native="onSearch"
            />
            <button type="button" class="search-btn" @click="onSearch">
              <i class="el-icon-search" aria-hidden="true" />
              <span class="search-btn-text">${T.search}</span>
            </button>
          </div>
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
            <el-dropdown trigger="click" @command="onUserCommand">
              <span class="user-entry">
                <img v-if="avatar" :src="avatar" class="user-avatar-mini" alt="">
                <span v-else class="user-avatar-fallback"><i class="el-icon-user-solid" /></span>
                <span class="user-name">{{ nickName || name }}</span>
                <i class="el-icon-arrow-down user-arrow" />
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="profile">${T.menuProfile}</el-dropdown-item>
                <el-dropdown-item command="myPapers">${T.menuMyPapers}</el-dropdown-item>
                <el-dropdown-item command="admin">${T.admin}</el-dropdown-item>
                <el-dropdown-item divided command="logout">${T.logout}</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="text" @click="goLogin">${T.login}</el-button>
            <el-button type="primary" size="small" @click="goRegister">${T.register}</el-button>
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
import { goPortalLogin } from '@/utils/portalLogin'

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
    ...mapGetters(['token', 'name', 'nickName', 'avatar'])
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
      goPortalLogin(this.$router, this.$route.fullPath, 'login')
    },
    goRegister() {
      goPortalLogin(this.$router, this.$route.fullPath, 'register')
    },
    goAdmin() {
      this.$router.push('/')
    },
    onUserCommand(cmd) {
      if (cmd === 'profile') {
        this.$router.push('/portal/profile')
        return
      }
      if (cmd === 'myPapers') {
        this.$router.push('/portal/my-papers')
        return
      }
      if (cmd === 'admin') {
        this.goAdmin()
        return
      }
      if (cmd === 'logout') {
        this.logout()
      }
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
$primary: #2563EB;
$primary-light: #3B82F6;

.portal-topbar {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid #E2E8F0;
  padding: 14px 0;
  position: sticky;
  top: 0;
  z-index: 1000;
  box-shadow: 0 4px 24px rgba(37, 99, 235, 0.08);

  &::after {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(37, 99, 235, 0.12), transparent);
  }
}

.topbar-inner {
  display: flex;
  align-items: center;
  gap: 28px;
}

.topbar-left { flex-shrink: 0; }

.portal-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  color: #1E293B;
}

.logo-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, $primary, $primary-light);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.2);
  flex-shrink: 0;
}

.logo-text-wrap { display: flex; flex-direction: column; }

.logo-text {
  font-weight: 800;
  font-size: 18px;
  line-height: 1.2;
}

.logo-slogan {
  font-size: 11px;
  font-weight: 500;
  color: #94a3b8;
  letter-spacing: 0.5px;
}

.topbar-search {
  flex: 1;
  min-width: 0;
  max-width: 640px;
  margin: 0 auto;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 6px 5px 4px;
  background: #fff;
  border: 1.5px solid #E2E8F0;
  border-radius: 26px;
  box-shadow: 0 2px 14px rgba(37, 99, 235, 0.1);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:focus-within {
    border-color: $primary-light;
    box-shadow: 0 4px 22px rgba(37, 99, 235, 0.2);
  }
}

.search-input {
  flex: 1;
  min-width: 0;

  ::v-deep .el-input__inner {
    height: 38px;
    line-height: 38px;
    border: none;
    background: transparent;
    padding-left: 38px;
    padding-right: 8px;
    font-size: 14px;
    color: #1E293B;
    box-shadow: none;

    &::placeholder { color: #94a3b8; }
    &:focus { box-shadow: none; }
  }

  ::v-deep .el-input__prefix {
    left: 10px;
    color: #94a3b8;
    font-size: 16px;
    transition: color 0.2s;
  }

  ::v-deep .el-input__suffix .el-input__clear {
    color: #cbd5e1;
    &:hover { color: $primary; }
  }
}

.search-bar:focus-within .search-input ::v-deep .el-input__prefix {
  color: $primary;
}

.search-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 38px;
  padding: 0 22px;
  border: none;
  border-radius: 20px;
  background: linear-gradient(135deg, $primary-light 0%, $primary 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.04em;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.38);
  transition: transform 0.15s ease, box-shadow 0.15s ease, filter 0.15s ease;

  i { font-size: 15px; font-weight: bold; }

  &:hover {
    filter: brightness(1.06);
    box-shadow: 0 6px 20px rgba(37, 99, 235, 0.48);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
    box-shadow: 0 2px 10px rgba(37, 99, 235, 0.32);
    filter: brightness(0.98);
  }
}

.hot-words { margin-top: 10px; padding-left: 12px; font-size: 12px; color: #94a3b8; }
.hot-word {
  margin-right: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  color: #64748B;
  cursor: pointer;
  &:hover { background: #EFF6FF; color: $primary; }
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  flex-shrink: 0;
}

.user-name { color: #475569; font-size: 13px; max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.user-entry {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 20px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: background 0.15s, border-color 0.15s;
  &:hover { background: #EFF6FF; border-color: #DBEAFE; }
}

.user-avatar-mini {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #E2E8F0;
}

.user-avatar-fallback {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #EFF6FF;
  color: $primary;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.user-arrow { font-size: 12px; color: #94a3b8; }

@media (max-width: 900px) {
  .topbar-inner { flex-wrap: wrap; }
  .topbar-search { order: 3; flex: 1 1 100%; max-width: none; margin: 8px 0 0; }
  .logo-slogan { display: none; }
}

@media (max-width: 520px) {
  .search-btn-text { display: none; }
  .search-btn { padding: 0 14px; }
  .search-input ::v-deep .el-input__inner { padding-left: 34px; }
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
    <portal-auth-dialog />
    <footer class="portal-footer">
      <div class="portal-container footer-inner">
        <div class="footer-brand">
          <span class="footer-logo">${T.portalName}</span>
          <p>${T.footerTagline}</p>
        </div>
        <div class="footer-links">
          <router-link to="/portal/chapter">${T.chapterPick}</router-link>
          <router-link to="/portal/knowledge">${T.knowledgePick}</router-link>
          <router-link to="/portal/paper">${T.smartPaper}</router-link>
          <router-link to="/portal/profile">${T.menuProfile}</router-link>
          <router-link to="/" class="admin">${T.admin}</router-link>
        </div>
        <div class="footer-copy">
          <span>&copy; {{ year }} ${T.portalName}</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script>
import PortalHeader from './components/PortalHeader'
import PortalFloatingBar from './components/PortalFloatingBar'
import PortalAuthDialog from './components/PortalAuthDialog'
import { openPortalAuth } from '@/utils/portalAuth'
import { PORTAL_LOGIN_PATH } from '@/utils/portalLogin'

export default {
  name: 'PortalLayout',
  components: { PortalHeader, PortalFloatingBar, PortalAuthDialog },
  computed: {
    year() { return new Date().getFullYear() }
  },
  watch: {
    '$route.path': {
      immediate: true,
      handler(path) {
        if (path === PORTAL_LOGIN_PATH) {
          const redirect = this.$route.query.redirect
          openPortalAuth({ redirect, tab: 'login' })
          this.$router.replace(redirect || '/portal/home').catch(() => {})
        }
      }
    }
  }
}
</script>

<style scoped lang="scss">
.portal-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #F8FAFC;
}

.portal-main { flex: 1; }

.portal-footer {
  background: #fff;
  border-top: 1px solid #E2E8F0;
  padding: 32px 0 24px;
  margin-top: 24px;
}

.footer-inner {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 20px;
}

.footer-brand {
  .footer-logo { font-size: 16px; font-weight: 800; color: #1E293B; }
  p { margin: 6px 0 0; font-size: 12px; color: #94a3b8; }
}

.footer-links {
  display: flex;
  gap: 20px;
  justify-content: center;
  flex-wrap: wrap;
  a {
    font-size: 13px;
    color: #64748b;
    text-decoration: none;
    &:hover { color: #2563EB; }
    &.admin { color: #2563EB; }
  }
}

.footer-copy {
  text-align: right;
  font-size: 12px;
  color: #cbd5e1;
}

@media (max-width: 768px) {
  .footer-inner { grid-template-columns: 1fr; text-align: center; }
  .footer-links { justify-content: center; }
  .footer-copy { text-align: center; }
}
</style>
`)

console.log('portal shell (header + layout) written')
