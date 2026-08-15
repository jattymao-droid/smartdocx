<template>
  <div class="portal-header" :class="{ 'is-scrolled': scrolled, 'header--home': true }">
    <div class="portal-header-front">
    <div class="portal-topbar">
      <div class="portal-container topbar-inner">
        <div class="topbar-left">
          <router-link to="/" class="portal-logo">
            <span class="logo-icon"><i class="el-icon-notebook-2" /></span>
            <span class="logo-text-wrap">
              <span class="logo-text">东陆智能教学库</span>
              <span class="logo-slogan">教师选题组卷</span>
            </span>
          </router-link>
        </div>
        <portal-nav home-theme embedded />
        <div class="topbar-right">
          <button type="button" class="upload-doc-link" @click="goUploadDoc">
            <i class="el-icon-upload2" />
            <span>上传文档</span>
          </button>
          <router-link to="/library/vip" class="vip-entry">
            <i class="el-icon-medal" />
            <span>加入VIP</span>
          </router-link>
          <template v-if="token">
            <el-dropdown trigger="click" @command="onUserCommand">
              <span class="user-entry">
                <img v-if="avatar" :src="avatar" class="user-avatar-mini" alt="">
                <span v-else class="user-avatar-fallback"><i class="el-icon-user-solid" /></span>
                <span class="user-name">{{ nickName || name }}</span>
                <i class="el-icon-arrow-down user-arrow" />
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="myPapers">我的试卷</el-dropdown-item>
                <el-dropdown-item command="myPurchases">我的购买记录</el-dropdown-item>
                <el-dropdown-item command="admin">管理后台</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </template>
          <template v-else>
            <button type="button" class="auth-login auth-login--home" @click="goLogin">
              <i class="el-icon-user" />
              <span>登录</span>
            </button>
          </template>
        </div>
      </div>
    </div>
    </div>
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
      scrolled: false
    }
  },
  computed: {
    ...mapGetters(['token', 'name', 'nickName', 'avatar'])
  },
  mounted() {
    this.onScroll = () => { this.scrolled = window.scrollY > 8 }
    window.addEventListener('scroll', this.onScroll, { passive: true })
    this.onScroll()
  },
  beforeDestroy() {
    window.removeEventListener('scroll', this.onScroll)
  },
  methods: {
    goLogin() {
      goPortalLogin(this.$router, this.$route.fullPath, 'login')
    },
    goUploadDoc() {
      if (!this.token) {
        goPortalLogin(this.$router, '/library/upload', 'login')
        return
      }
      this.$router.push('/library/upload')
    },
    goAdmin() {
      this.$router.push('/')
    },
    onUserCommand(cmd) {
      if (cmd === 'profile') {
        this.$router.push('/profile')
        return
      }
      if (cmd === 'myPapers') {
        this.$router.push('/my-papers')
        return
      }
      if (cmd === 'myPurchases') {
        this.$router.push('/my-purchases')
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
        this.$router.push('/')
      })
    }
  }
}
</script>

<style scoped lang="scss">
$primary: #0F766E;
$primary-light: #14B8A6;
$violet: #0E7490;

.portal-header {
  position: static;
  z-index: 1000;
  transition: box-shadow 0.25s ease;
  isolation: isolate;

  &.header--home {
    background: linear-gradient(180deg, #1a4480 0%, #1e4d8c 100%);

    .portal-topbar {
      padding: 0;
      background: transparent;
      border-bottom: none;
      backdrop-filter: none;
      -webkit-backdrop-filter: none;

      &::after { display: none; }
    }

    ::v-deep .portal-nav {
      background: transparent;
      border-bottom: none;
    }

    .logo-text {
      color: #fff;
      background: none;
      -webkit-text-fill-color: #fff;
    }

    .logo-slogan {
      color: rgba(255, 255, 255, 0.72);
    }

    .logo-icon {
      background: rgba(255, 255, 255, 0.16);
      box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.18);
    }

    .user-name { color: rgba(255, 255, 255, 0.92); }
    .user-entry {
      &:hover { background: rgba(255, 255, 255, 0.12); border-color: rgba(255, 255, 255, 0.18); }
    }
    .user-arrow { color: rgba(255, 255, 255, 0.72); }
    .user-avatar-fallback {
      border-color: rgba(255, 255, 255, 0.35);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
    }
    .user-avatar-mini { border-color: rgba(255, 255, 255, 0.35); }
  }

  &.header--home.is-scrolled {
    box-shadow: 0 8px 24px rgba(15, 23, 42, 0.18);
  }
}

.portal-header-bg {
  display: none;
}

.portal-header-front {
  position: relative;
  z-index: 1;
}

.portal-topbar {
  position: relative;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  padding: 6px 0;
  transition: background 0.25s ease;

  &::after {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(15, 118, 110, 0.16), transparent);
  }
}

.topbar-inner {
  display: flex;
  align-items: center;
  gap: 14px;
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
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.18);
  flex-shrink: 0;
  transition: transform 0.2s ease, background 0.2s ease;
}

.portal-logo:hover .logo-icon {
  transform: scale(1.04);
  background: rgba(255, 255, 255, 0.22);
}

.logo-text-wrap { display: flex; flex-direction: column; }

.logo-text {
  font-weight: 800;
  font-size: 16px;
  line-height: 1.2;
  color: #fff;
}

.logo-slogan {
  font-size: 10px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.72);
  letter-spacing: 0.5px;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
  flex-shrink: 0;
}

.upload-doc-link,
.upload-doc-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: rgba(255, 255, 255, 0.92);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  transition: background 0.15s ease, color 0.15s ease;

  i { font-size: 15px; }

  &:hover {
    color: #fff;
    background: rgba(255, 255, 255, 0.12);
  }
}

.vip-entry {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #fde68a;
  text-decoration: none;
  transition: background 0.15s ease;

  i {
    font-size: 15px;
    color: #fbbf24;
  }

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #fef3c7;
  }
}

.upload-doc-btn {
  background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%) !important;
  border: none !important;
  color: #fff !important;
  font-weight: 700;
  letter-spacing: 0.02em;
  box-shadow: 0 4px 14px rgba(217, 119, 6, 0.38);
  transition: transform 0.15s ease, box-shadow 0.15s ease, filter 0.15s ease;

  &:hover,
  &:focus {
    background: linear-gradient(135deg, #FBBF24 0%, #EA580C 100%) !important;
    color: #fff !important;
    transform: translateY(-1px);
    box-shadow: 0 6px 18px rgba(234, 88, 12, 0.42);
    filter: brightness(1.03);
  }
}

::v-deep .upload-doc-btn.el-button--small {
  padding: 8px 14px;
}

::v-deep .upload-doc-btn.el-button--small.is-round {
  padding-left: 14px;
  padding-right: 16px;
}

.user-name { color: rgba(255, 255, 255, 0.92); font-size: 13px; max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.user-entry {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 3px 9px 3px 3px;
  border-radius: 18px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: background 0.15s, border-color 0.15s;
  &:hover { background: rgba(255, 255, 255, 0.12); border-color: rgba(255, 255, 255, 0.18); }
}

.user-avatar-mini {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #E2E8F0;
}

.user-avatar-fallback {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #CCFBF1, #CFFAFE);
  color: $primary;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(15, 118, 110, 0.15);
}

.user-arrow { font-size: 12px; color: rgba(255, 255, 255, 0.72); transition: transform 0.2s; }
.user-entry:hover .user-arrow { transform: rotate(180deg); }

.auth-login {
  color: #64748B !important;
  font-weight: 500;
  &:hover { color: $primary !important; }
}

.auth-login--home {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: rgba(255, 255, 255, 0.92) !important;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;

  i { font-size: 15px; }

  &:hover {
    background: rgba(255, 255, 255, 0.12);
    color: #fff !important;
  }
}

.auth-register {
  background: linear-gradient(135deg, $violet, $primary) !important;
  border-color: transparent !important;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(15, 118, 110, 0.28);
  &:hover { filter: brightness(1.05); }
}

::v-deep .auth-register.el-button--small {
  padding: 8px 12px;
}

::v-deep .portal-nav {
  transition: background 0.25s ease;
}

@media (max-width: 900px) {
  .topbar-inner { gap: 10px; }
  .logo-slogan { display: none; }
  .logo-text { font-size: 15px; }
  .user-name { display: none; }
}
</style>
