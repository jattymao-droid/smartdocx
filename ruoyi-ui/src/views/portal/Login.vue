<template>
  <div class="portal-login-page">
    <div class="login-left">
      <div class="brand-block">
        <router-link to="/" class="brand-logo">
          <span class="logo-icon">卷</span>
          <span class="logo-text">东陆智能教学库</span>
        </router-link>
        <h1>东陆智能教学库</h1>
        <p class="brand-desc">章节选题 · 知识点组卷 · 智能出卷</p>
        <ul class="brand-features">
          <li><i class="el-icon-check" /> 按教材章节精准筛选试题</li>
          <li><i class="el-icon-check" /> 试题篮一键组卷与导出</li>
          <li><i class="el-icon-check" /> 校本题库共建与共享</li>
        </ul>
      </div>
    </div>

    <div class="login-right">
      <div class="login-card">
        <h2 class="card-title">用户登录</h2>
        <p class="card-subtitle">登录后可选题、加入试题篮与组卷</p>

        <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入账号"
              prefix-icon="el-icon-user"
              auto-complete="off"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="el-icon-lock"
              auto-complete="off"
              @keyup.enter.native="handleLogin"
            />
          </el-form-item>
          <el-form-item v-if="captchaEnabled" prop="code">
            <div class="code-row">
              <el-input
                v-model="loginForm.code"
                placeholder="验证码"
                prefix-icon="el-icon-key"
                auto-complete="off"
                @keyup.enter.native="handleLogin"
              />
              <img :src="codeUrl" class="code-img" alt="captcha" @click="getCode" />
            </div>
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
          </div>

          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click.native.prevent="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form>

        <div class="login-links">
          <router-link to="/">← 返回首页</router-link>
          <router-link to="/admin/login" class="admin-link">管理员登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getCodeImg } from '@/api/login'
import Cookies from 'js-cookie'
import { encrypt, decrypt } from '@/utils/jsencrypt'

const COOKIE_USER = 'portal_username'
const COOKIE_PASS = 'portal_password'
const COOKIE_REMEMBER = 'portal_rememberMe'

export default {
  name: 'PortalLogin',
  data() {
    return {
      codeUrl: '',
      loginForm: {
        username: '',
        password: '',
        rememberMe: false,
        code: '',
        uuid: ''
      },
      loginRules: {
        username: [{ required: true, trigger: 'blur', message: '请输入账号' }],
        password: [{ required: true, trigger: 'blur', message: '请输入密码' }],
        code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
      },
      loading: false,
      captchaEnabled: true,
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  created() {
    this.getCode()
    this.getCookie()
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = 'data:image/gif;base64,' + res.img
          this.loginForm.uuid = res.uuid
        }
      })
    },
    getCookie() {
      const username = Cookies.get(COOKIE_USER)
      const password = Cookies.get(COOKIE_PASS)
      const rememberMe = Cookies.get(COOKIE_REMEMBER)
      this.loginForm.username = username || this.loginForm.username
      this.loginForm.password = password ? decrypt(password) : this.loginForm.password
      this.loginForm.rememberMe = rememberMe ? rememberMe === 'true' : false
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (!valid) return
        this.loading = true
        if (this.loginForm.rememberMe) {
          Cookies.set(COOKIE_USER, this.loginForm.username, { expires: 30 })
          Cookies.set(COOKIE_PASS, encrypt(this.loginForm.password), { expires: 30 })
          Cookies.set(COOKIE_REMEMBER, 'true', { expires: 30 })
        } else {
          Cookies.remove(COOKIE_USER)
          Cookies.remove(COOKIE_PASS)
          Cookies.remove(COOKIE_REMEMBER)
        }
        this.$store.dispatch('Login', this.loginForm).then(() => {
          const target = this.redirect || '/'
          this.$router.push({ path: target }).catch(() => {})
        }).catch(() => {
          this.loading = false
          if (this.captchaEnabled) this.getCode()
        })
      })
    }
  }
}
</script>

<style scoped lang="scss">

$primary: #2563EB;
$primary-light: #3B82F6;
$ink: #0F172A;
$muted: #64748B;
$border: #E2E8F0;

.portal-login-page {
  display: flex;
  min-height: 100vh;
  background: radial-gradient(ellipse 80% 50% at 50% -20%, rgba(124, 58, 237, 0.12), transparent),
    radial-gradient(ellipse 60% 40% at 100% 50%, rgba(5, 150, 105, 0.08), transparent),
    linear-gradient(180deg, #F8FAFC 0%, #F1F5F9 100%);
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #1E3A8A 0%, #2563EB 35%, #7C3AED 68%, #059669 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  color: #ffffff;
  position: relative;
  overflow: hidden;
  &::before {
    content: '';
    position: absolute;
    width: 420px;
    height: 420px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(167, 139, 250, 0.35), transparent 70%);
    top: -120px;
    right: -80px;
  }
  &::after {
    content: '';
    position: absolute;
    width: 560px;
    height: 560px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(52, 211, 153, 0.22), transparent 65%);
    bottom: -240px;
    left: -180px;
  }
}

.brand-block { max-width: 400px; position: relative; z-index: 1; }

.brand-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: #ffffff;
  margin-bottom: 32px;
}

.logo-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(255,255,255,0.25), rgba(255,255,255,0.1));
  border: 1px solid rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.02em;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.15);
}

.logo-text { font-size: 24px; font-weight: 800; letter-spacing: 0.02em; }
.brand-block h1 { margin: 0 0 10px; font-size: 34px; font-weight: 800; letter-spacing: -0.02em; }
.brand-desc { margin: 0 0 18px; font-size: 14px; color: rgba(255, 255, 255, 0.88); }

.brand-features {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 10px;
  li {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 0;
    font-size: 14px;
    color: rgba(255, 255, 255, 0.92);
    i {
      width: 22px;
      height: 22px;
      border-radius: 999px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      color: #fff;
      flex-shrink: 0;
    }
    &:nth-child(1) i { background: linear-gradient(135deg, #60A5FA, #2563EB); }
    &:nth-child(2) i { background: linear-gradient(135deg, #A78BFA, #7C3AED); }
    &:nth-child(3) i { background: linear-gradient(135deg, #34D399, #059669); }
  }
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 28px;
  background: transparent;
}

.login-card {
  width: 420px;
  max-width: 100%;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 18px;
  padding: 28px 28px 22px;
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.04),
    0 18px 56px rgba(15, 23, 42, 0.10);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, #2563EB, #7C3AED, #059669);
  }
}
.card-title { margin: 0 0 6px; font-size: 20px; font-weight: 800; color: $ink; letter-spacing: -0.01em; }
.card-subtitle { margin: 0 0 18px; font-size: 13px; color: $muted; }

.login-form ::v-deep .el-input__inner { height: 42px; line-height: 42px; border-radius: 12px; border-color: $border; }
.login-form ::v-deep .el-input__inner:focus { border-color: $primary-light; }
.code-row { display: grid; grid-template-columns: 1fr 132px; gap: 10px; align-items: center; }
.code-img { height: 42px; width: 132px; border-radius: 12px; cursor: pointer; border: 1px solid $border; background: #fff; }
.form-options { margin: 4px 0 10px; color: $muted; }

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563EB, #7C3AED);
  border-color: #7C3AED;
  font-weight: 700;
  letter-spacing: 0.02em;
  box-shadow: 0 6px 20px rgba(124, 58, 237, 0.25);
  &:hover, &:focus { background: linear-gradient(135deg, #3B82F6, #8B5CF6); border-color: #8B5CF6; }
}

.login-links {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  font-size: 13px;
  a { color: $muted; text-decoration: none; &:hover { color: $primary; } }
  .admin-link { color: $primary; font-weight: 600; }
}

@media (max-width: 900px) {
  .portal-login-page { flex-direction: column; }
  .login-left { padding: 32px 24px; }
  .login-right { width: 100%; padding: 24px; }
  .login-card { width: 100%; }
}

</style>
