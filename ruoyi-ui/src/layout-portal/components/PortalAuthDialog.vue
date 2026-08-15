<template>
  <el-dialog
    :title="dialogTitle"
    :visible.sync="visible"
    width="440px"
    class="portal-auth-dialog"
    append-to-body
    :close-on-click-modal="false"
    @closed="onClosed"
  >
    <p class="auth-subtitle">{{ dialogSubtitle }}</p>
    <div class="auth-tabs">
      <button type="button" class="auth-tab" :class="{ active: tab === 'login' }" @click="switchTab('login')">{{ labels.login }}</button>
      <button type="button" class="auth-tab" :class="{ active: tab === 'register' }" @click="switchTab('register')">{{ labels.register }}</button>
    </div>
    <el-form v-show="tab === 'login'" ref="loginForm" :model="loginForm" :rules="loginRules" class="auth-form" @submit.native.prevent>
      <el-form-item prop="username">
        <el-input v-model="loginForm.username" :placeholder="labels.usernamePh" prefix-icon="el-icon-user" auto-complete="off" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="loginForm.password" type="password" :placeholder="labels.passwordPh" prefix-icon="el-icon-lock" auto-complete="off" @keyup.enter.native="handleLogin" />
      </el-form-item>
      <el-form-item v-if="captchaEnabled" prop="code">
        <div class="code-row">
          <el-input v-model="loginForm.code" :placeholder="labels.codePh" prefix-icon="el-icon-key" auto-complete="off" @keyup.enter.native="handleLogin" />
          <img :src="codeUrl" class="code-img" alt="captcha" @click="getCode" />
        </div>
      </el-form-item>
      <div class="form-options"><el-checkbox v-model="loginForm.rememberMe">{{ labels.rememberMe }}</el-checkbox></div>
      <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">{{ loading ? labels.loginLoading : labels.loginBtn }}</el-button>
    </el-form>
    <el-form v-show="tab === 'register'" ref="registerForm" :model="registerForm" :rules="registerRules" class="auth-form" @submit.native.prevent>
      <el-form-item prop="username">
        <el-input v-model="registerForm.username" :placeholder="labels.usernamePh" prefix-icon="el-icon-user" auto-complete="off" />
      </el-form-item>
      <el-form-item prop="password" :rules="registerPwdValidator">
        <el-input v-model="registerForm.password" type="password" :placeholder="labels.passwordPh" prefix-icon="el-icon-lock" auto-complete="off" />
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input v-model="registerForm.confirmPassword" type="password" :placeholder="labels.confirmPasswordPh" prefix-icon="el-icon-lock" auto-complete="off" @keyup.enter.native="handleRegister" />
      </el-form-item>
      <el-form-item v-if="captchaEnabled" prop="code">
        <div class="code-row">
          <el-input v-model="registerForm.code" :placeholder="labels.codePh" prefix-icon="el-icon-key" auto-complete="off" @keyup.enter.native="handleRegister" />
          <img :src="codeUrl" class="code-img" alt="captcha" @click="getCode" />
        </div>
      </el-form-item>
      <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister">{{ loading ? labels.registerLoading : labels.registerBtn }}</el-button>
    </el-form>
    <div class="auth-footer">
      <a v-if="tab === 'login'" href="javascript:;" @click="switchTab('register')">{{ labels.noAccount }}</a>
      <a v-else href="javascript:;" @click="switchTab('login')">{{ labels.hasAccount }}</a>
      <router-link to="/admin/login" class="admin-link" @click.native="visible = false">{{ labels.adminLogin }}</router-link>
    </div>
  </el-dialog>
</template>

<script>
import Cookies from 'js-cookie'
import { getCodeImg, register } from '@/api/login'
import { encrypt, decrypt } from '@/utils/jsencrypt'
import passwordRule from '@/utils/passwordRule'
import { authLabels } from '@/views/portal/portal-auth-labels'
import { portalAuthBus } from '@/utils/portalAuth'

const COOKIE_USER = 'portal_username'
const COOKIE_PASS = 'portal_password'
const COOKIE_REMEMBER = 'portal_rememberMe'
const MSG_USERNAME_LEN = '用户账号长度必须介于 2 和 20 之间'
const MSG_CONFIRM_PWD = '请再次输入密码'
const MSG_PWD_MISMATCH = '两次输入的密码不一致'

export default {
  name: 'PortalAuthDialog',
  mixins: [passwordRule],
  data() {
    return {
      visible: false,
      tab: 'login',
      labels: authLabels,
      pendingRedirect: undefined,
      codeUrl: '',
      captchaEnabled: true,
      loading: false,
      loginForm: { username: '', password: '', rememberMe: false, code: '', uuid: '' },
      registerForm: { username: '', password: '', confirmPassword: '', code: '', uuid: '' }
    }
  },
  computed: {
    dialogTitle() { return this.tab === 'login' ? authLabels.loginTitle : authLabels.registerTitle },
    dialogSubtitle() { return this.tab === 'login' ? authLabels.loginSubtitle : authLabels.registerSubtitle },
    loginRules() {
      return {
        username: [{ required: true, trigger: 'blur', message: authLabels.usernameRequired }],
        password: [{ required: true, trigger: 'blur', message: authLabels.passwordRequired }],
        code: [{ required: true, trigger: 'change', message: authLabels.codeRequired }]
      }
    },
    registerRules() {
      const vm = this
      return {
        username: [
          { required: true, trigger: 'blur', message: authLabels.usernameRequired },
          { min: 2, max: 20, message: MSG_USERNAME_LEN, trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: MSG_CONFIRM_PWD, trigger: 'blur' },
          { validator: (rule, value, callback) => {
            if (vm.registerForm.password !== value) callback(new Error(MSG_PWD_MISMATCH))
            else callback()
          }, trigger: 'blur' }
        ],
        code: [{ required: true, trigger: 'change', message: authLabels.codeRequired }]
      }
    }
  },
  created() {
    portalAuthBus.$on('open', this.onOpen)
    this.loadLoginCookie()
  },
  beforeDestroy() {
    portalAuthBus.$off('open', this.onOpen)
  },
  methods: {
    onOpen({ redirect, tab }) {
      this.pendingRedirect = redirect
      this.tab = tab || 'login'
      this.visible = true
      this.getCode()
      this.loadLoginCookie()
      this.$nextTick(() => {
        if (this.$refs.loginForm) this.$refs.loginForm.clearValidate()
        if (this.$refs.registerForm) this.$refs.registerForm.clearValidate()
      })
    },
    switchTab(name) { this.tab = name; this.getCode() },
    onClosed() { this.loading = false },
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = 'data:image/gif;base64,' + res.img
          this.loginForm.uuid = res.uuid
          this.registerForm.uuid = res.uuid
        }
      })
    },
    loadLoginCookie() {
      const username = Cookies.get(COOKIE_USER)
      const password = Cookies.get(COOKIE_PASS)
      const rememberMe = Cookies.get(COOKIE_REMEMBER)
      this.loginForm.username = username || ''
      this.loginForm.password = password ? decrypt(password) : ''
      this.loginForm.rememberMe = rememberMe ? rememberMe === 'true' : false
    },
    saveLoginCookie() {
      if (this.loginForm.rememberMe) {
        Cookies.set(COOKIE_USER, this.loginForm.username, { expires: 30 })
        Cookies.set(COOKIE_PASS, encrypt(this.loginForm.password), { expires: 30 })
        Cookies.set(COOKIE_REMEMBER, 'true', { expires: 30 })
      } else {
        Cookies.remove(COOKIE_USER)
        Cookies.remove(COOKIE_PASS)
        Cookies.remove(COOKIE_REMEMBER)
      }
    },
    afterAuthSuccess() {
      this.visible = false
      this.loading = false
      portalAuthBus.$emit('success', { redirect: this.pendingRedirect })
      const target = this.pendingRedirect
      if (target && this.$route.fullPath !== target) {
        this.$router.push(target).catch(() => {})
      }
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (!valid) return
        this.loading = true
        this.saveLoginCookie()
        this.$store.dispatch('Login', this.loginForm).then(() => this.$store.dispatch('GetInfo')).then(() => {
          this.$message.success(authLabels.loginSuccess)
          this.afterAuthSuccess()
        }).catch(() => {
          this.loading = false
          if (this.captchaEnabled) this.getCode()
        })
      })
    },
    handleRegister() {
      this.$refs.registerForm.validate(valid => {
        if (!valid) return
        this.loading = true
        register({
          username: this.registerForm.username,
          password: this.registerForm.password,
          code: this.registerForm.code,
          uuid: this.registerForm.uuid
        }).then(() => {
          this.loading = false
          this.$message.success(authLabels.registerSuccess)
          this.loginForm.username = this.registerForm.username
          this.registerForm.password = ''
          this.registerForm.confirmPassword = ''
          this.registerForm.code = ''
          this.tab = 'login'
          this.getCode()
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
$primary: #0F766E;
$primary-light: #14B8A6;
.auth-subtitle { margin: -8px 0 16px; font-size: 13px; color: #94a3b8; line-height: 1.5; }
.auth-tabs { display: flex; gap: 8px; margin-bottom: 20px; padding: 4px; background: #ECFEFF; border-radius: 10px; }
.auth-tab {
  flex: 1; border: none; background: transparent; padding: 10px 0; font-size: 14px; font-weight: 600;
  color: #64748b; border-radius: 8px; cursor: pointer; transition: all 0.15s;
  &.active { color: #fff; background: linear-gradient(135deg, $primary, $primary-light); box-shadow: 0 2px 8px rgba(15, 118, 110, 0.25); }
}
.auth-form ::v-deep .el-input__inner { height: 42px; border-radius: 10px; }
.code-row { display: flex; gap: 10px; .el-input { flex: 1; } }
.code-img { height: 42px; border-radius: 10px; cursor: pointer; border: 1px solid #E2E8F0; }
.form-options { margin-bottom: 16px; }
.submit-btn { width: 100%; height: 44px; border-radius: 10px; font-size: 15px; background: linear-gradient(135deg, $primary, $primary-light); border-color: $primary; }
.auth-footer {
  display: flex; justify-content: space-between; margin-top: 16px; font-size: 13px;
  a { color: $primary; text-decoration: none; &:hover { color: $primary-light; } }
  .admin-link { color: #94a3b8; }
}
::v-deep .el-dialog__header { padding-bottom: 8px; }
::v-deep .el-dialog__body { padding-top: 8px; }
</style>
