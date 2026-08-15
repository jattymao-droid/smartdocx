<template>
  <div class="portal-layout" :class="{ 'layout--home': isHome }">
    <portal-header />
    <main class="portal-main">
      <keep-alive :include="portalKeepAlive">
        <router-view :key="$route.path === '/library' ? 'library' : $route.fullPath" />
      </keep-alive>
    </main>
    <portal-floating-bar />
    <portal-auth-dialog />
    <footer class="portal-footer">
      <div class="portal-container footer-inner">
        <div class="footer-brand">
          <span class="footer-logo">东陆智能教学库</span>
          <p>智能教学资源与组卷平台</p>
        </div>
        <div class="footer-links">
          <router-link to="/chapter">章节选题</router-link>
          <router-link to="/knowledge">知识点选题</router-link>
          <router-link to="/paper">智能组卷</router-link>
          <router-link to="/profile">个人中心</router-link>
          <router-link to="/admin" class="admin">管理后台</router-link>
        </div>
        <div class="footer-copy">
          <span>&copy; {{ year }} 东陆智能教学库</span>
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
import { PORTAL_HOME } from '@/constants/routes'

export default {
  name: 'PortalLayout',
  components: { PortalHeader, PortalFloatingBar, PortalAuthDialog },
  computed: {
    year() { return new Date().getFullYear() },
    isHome() { return this.$route.path === '/' },
    portalKeepAlive() {
      return ['PortalHome', 'PortalLibrary', 'PortalBrowse']
    }
  },
  watch: {
    '$route.path': {
      immediate: true,
      handler(path) {
        if (path === PORTAL_LOGIN_PATH) {
          const redirect = this.$route.query.redirect
          openPortalAuth({ redirect, tab: 'login' })
          this.$router.replace(redirect || PORTAL_HOME).catch(() => {})
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

  &.layout--home {
    background: #F1F5F9;

    .portal-main {
      margin-top: 0;
    }

    .portal-footer {
      margin-top: 0;
    }
  }
}

.portal-main { flex: 1; }

.portal-footer {
  background: linear-gradient(180deg, #1a4480 0%, #1e4d8c 100%);
  border-top: none;
  padding: 32px 0 24px;
  margin-top: 32px;
  color: rgba(255, 255, 255, 0.72);
}

.footer-inner {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 24px;
}

.footer-brand {
  .footer-logo {
    font-size: 17px;
    font-weight: 800;
    color: #fff;
    letter-spacing: 0.02em;
  }

  p {
    margin: 6px 0 0;
    font-size: 12px;
    color: rgba(255, 255, 255, 0.72);
  }
}

.footer-links {
  display: flex;
  gap: 24px;
  justify-content: center;
  flex-wrap: wrap;

  a {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.88);
    text-decoration: none;
    transition: color 0.15s;

    &:hover { color: #fff; }
    &.admin { color: #fde68a; }
    &.admin:hover { color: #fef3c7; }
  }
}

.footer-copy {
  text-align: right;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
}

@media (max-width: 768px) {
  .footer-inner { grid-template-columns: 1fr; text-align: center; }
  .footer-links { justify-content: center; }
  .footer-copy { text-align: center; }
}
</style>
