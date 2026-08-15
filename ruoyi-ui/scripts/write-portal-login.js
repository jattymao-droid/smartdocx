const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/views/portal/Login.vue')
const content = `<template>
  <div class="portal-login-page">
    <div class="login-left">
      <div class="brand-block">
        <router-link to="/portal/home" class="brand-logo">
          <span class="logo-icon">\u5377</span>
          <span class="logo-text">\u667a\u6167\u9898\u5e93</span>
        </router-link>
        <h1>\u6559\u5e08\u9009\u9898\u7ec4\u5377\u5e73\u53f0</h1>
        <p class="brand-desc">\u7ae0\u8282\u9009\u9898 \u00b7 \u77e5\u8bc6\u70b9\u7ec4\u5377 \u00b7 \u667a\u80fd\u51fa\u5377</p>
        <ul class="brand-features">
          <li><i class="el-icon-check" /> \u6309\u6559\u6750\u7ae0\u8282\u7cbe\u51c6\u7b5b\u9009\u8bd5\u9898</li>
          <li><i class="el-icon-check" /> \u8bd5\u9898\u7bee\u4e00\u952e\u7ec4\u5377\u4e0e\u5bfc\u51fa</li>
          <li><i class="el-icon-check" /> \u6821\u672c\u9898\u5e93\u5171\u5efa\u4e0e\u5171\u4eab</li>
        </ul>
      </div>
    </div>

    <div class="login-right">
      <div class="login-card">
        <h2 class="card-title">\u7528\u6237\u767b\u5f55</h2>
        <p class="card-subtitle">\u767b\u5f55\u540e\u53ef\u9009\u9898\u3001\u52a0\u5165\u8bd5\u9898\u7bee\u4e0e\u7ec4\u5377</p>

        <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="\u8bf7\u8f93\u5165\u8d26\u53f7"
              prefix-icon="el-icon-user"
              auto-complete="off"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="\u8bf7\u8f93\u5165\u5bc6\u7801"
              prefix-icon="el-icon-lock"
              auto-complete="off"
              @keyup.enter.native="handleLogin"
            />
          </el-form-item>
          <el-form-item v-if="captchaEnabled" prop="code">
            <div class="code-row">
              <el-input
                v-model="loginForm.code"
                placeholder="\u9a8c\u8bc1\u7801"
                prefix-icon="el-icon-key"
                auto-complete="off"
                @keyup.enter.native="handleLogin"
              />
              <img :src="codeUrl" class="code-img" alt="captcha" @click="getCode" />
            </div>
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="loginForm.rememberMe">\u8bb0\u4f4f\u5bc6\u7801</el-checkbox>
          </div>

          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click.native.prevent="handleLogin"
          >
            {{ loading ? '\u767b\u5f55\u4e2d...' : '\u767b\u5f55' }}
          </el-button>
        </el-form>

        <div class="login-links">
          <router-link to="/portal/home">\u2190 \u8fd4\u56de\u9996\u9875</router-link>
          <router-link to="/login" class="admin-link">\u7ba1\u7406\u5458\u767b\u5f55</router-link>
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
        username: [{ required: true, trigger: 'blur', message: '\u8bf7\u8f93\u5165\u8d26\u53f7' }],
        password: [{ required: true, trigger: 'blur', message: '\u8bf7\u8f93\u5165\u5bc6\u7801' }],
        code: [{ required: true, trigger: 'change', message: '\u8bf7\u8f93\u5165\u9a8c\u8bc1\u7801' }]
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
          const target = this.redirect || '/portal/home'
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
.portal-login-page {
  display: flex;
  min-height: 100vh;
  background: #f4f6f9;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #1a5fd4 0%, #2877ff 45%, #5ab0ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  color: #fff;
}

.brand-block {
  max-width: 420px;
}

.brand-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: #fff;
  margin-bottom: 32px;
}

.logo-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
}

.logo-text {
  font-size: 26px;
  font-weight: 700;
}

.brand-block h1 {
  margin: 0 0 12px;
  font-size: 28px;
  font-weight: 600;
  line-height: 1.35;
}

.brand-desc {
  margin: 0 0 28px;
  font-size: 15px;
  opacity: 0.9;
}

.brand-features {
  list-style: none;
  padding: 0;
  margin: 0;
  li {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 0;
    font-size: 14px;
    opacity: 0.92;
    i { font-size: 16px; }
  }
}

.login-right {
  width: 480px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background: #fff;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.06);
}

.login-card {
  width: 100%;
  max-width: 360px;
}

.card-title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.card-subtitle {
  margin: 0 0 28px;
  font-size: 13px;
  color: #909399;
}

.login-form {
  .el-input {
    ::v-deep input {
      height: 42px;
    }
  }
}

.code-row {
  display: flex;
  gap: 10px;
  .el-input { flex: 1; }
}

.code-img {
  height: 42px;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #dcdfe6;
}

.form-options {
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 42px;
  font-size: 15px;
  background: #2877ff;
  border-color: #2877ff;
  &:hover, &:focus {
    background: #1a6fe8;
    border-color: #1a6fe8;
  }
}

.login-links {
  display: flex;
  justify-content: space-between;
  margin-top: 24px;
  font-size: 13px;
  a {
    color: #2877ff;
    text-decoration: none;
    &:hover { text-decoration: underline; }
  }
  .admin-link { color: #909399; }
}

@media (max-width: 900px) {
  .portal-login-page { flex-direction: column; }
  .login-left { padding: 32px 24px; min-height: auto; }
  .login-right {
    width: 100%;
    box-shadow: none;
    padding: 24px;
  }
  .brand-block h1 { font-size: 22px; }
}
</style>
`

fs.writeFileSync(file, content, 'utf8')
console.log('wrote Portal Login.vue')
