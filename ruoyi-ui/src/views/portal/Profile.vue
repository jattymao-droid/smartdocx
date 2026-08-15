<template>
  <div class="portal-profile portal-page">
    <div class="portal-container profile-wrap">
      <nav class="portal-breadcrumb" aria-label="breadcrumb">
        <router-link to="/">{{ labels.home }}</router-link>
        <span class="sep">/</span>
        <span class="current">{{ labels.breadcrumb }}</span>
      </nav>

      <div v-if="!token" class="login-hint portal-card portal-login-hint">
        <i class="el-icon-info" />
        <span>{{ labels.loginRequired }}</span>
        <el-button type="primary" size="small" @click="goLogin">{{ labels.goLogin }}</el-button>
      </div>

      <template v-else>
        <header class="profile-hero portal-card portal-page-hero portal-page-hero--violet">
          <div class="hero-text">
            <div class="hero-title-row">
              <span class="hero-icon portal-page-hero__icon portal-page-hero__icon--violet"><i class="el-icon-user" /></span>
              <div>
                <h1>{{ labels.title }}</h1>
                <p>{{ labels.subtitle }}</p>
              </div>
            </div>
          </div>
          <div class="hero-actions">
            <el-button round size="small" type="primary" plain icon="el-icon-search" @click="$router.push('/chapter')">{{ labels.goPick }}</el-button>
            <el-button round size="small" plain icon="el-icon-document-copy" @click="$router.push('/paper')">{{ labels.goPaper }}</el-button>
          </div>
        </header>

        <div v-loading="loading" class="profile-grid">
          <aside class="profile-side portal-card">
            <div class="side-head">{{ labels.cardProfile }}</div>
            <div class="avatar-wrap">
              <user-avatar />
            </div>
            <h2 class="display-name">{{ user.nickName || user.userName || '-' }}</h2>
            <p class="display-account">@{{ user.userName || '-' }}</p>

            <ul class="info-list">
              <li><span class="info-label">{{ labels.phone }}</span><span class="info-value">{{ user.phonenumber || '-' }}</span></li>
              <li><span class="info-label">{{ labels.email }}</span><span class="info-value">{{ user.email || '-' }}</span></li>
              <li><span class="info-label">{{ labels.dept }}</span><span class="info-value">{{ deptText }}</span></li>
              <li><span class="info-label">{{ labels.role }}</span><span class="info-value">{{ roleGroup || '-' }}</span></li>
              <li><span class="info-label">{{ labels.createTime }}</span><span class="info-value">{{ user.createTime || '-' }}</span></li>
            </ul>

            <div class="stat-box">
              <span class="stat-label">{{ labels.basketStat }}</span>
              <b class="stat-num">{{ questionBasketCount }}</b>
              <span class="stat-unit">{{ labels.basketUnit }}</span>
            </div>

            <nav class="profile-quick-nav">
              <router-link to="/my-papers" class="quick-nav-item">
                <i class="el-icon-folder-opened" />
                <span>{{ labels.myPapers }}</span>
                <i class="el-icon-arrow-right quick-nav-arrow" />
              </router-link>
              <router-link to="/my-purchases" class="quick-nav-item quick-nav-item--purchase">
                <i class="el-icon-shopping-bag-1" />
                <span>{{ labels.myPurchases }}</span>
                <i class="el-icon-arrow-right quick-nav-arrow" />
              </router-link>
              <router-link to="/library/vip" class="quick-nav-item quick-nav-item--vip">
                <i class="el-icon-medal" />
                <span>{{ labels.myVip }}</span>
                <i class="el-icon-arrow-right quick-nav-arrow" />
              </router-link>
            </nav>
          </aside>

          <main class="profile-main portal-card">
            <div class="main-head">{{ labels.cardEdit }}</div>
            <el-tabs v-model="activeTab" class="profile-tabs">
              <el-tab-pane :label="labels.tabInfo" name="info">
                <el-form ref="infoForm" :model="infoForm" :rules="infoRules" label-width="88px" class="profile-form">
                  <el-form-item :label="labels.nickName" prop="nickName">
                    <el-input v-model="infoForm.nickName" maxlength="30" />
                  </el-form-item>
                  <el-form-item :label="labels.phone" prop="phonenumber">
                    <el-input v-model="infoForm.phonenumber" maxlength="11" />
                  </el-form-item>
                  <el-form-item :label="labels.email" prop="email">
                    <el-input v-model="infoForm.email" maxlength="50" />
                  </el-form-item>
                  <el-form-item :label="labels.sex">
                    <el-radio-group v-model="infoForm.sex">
                      <el-radio label="0">{{ labels.male }}</el-radio>
                      <el-radio label="1">{{ labels.female }}</el-radio>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" :loading="savingInfo" @click="submitInfo">{{ labels.save }}</el-button>
                    <el-button @click="resetInfo">{{ labels.reset }}</el-button>
                  </el-form-item>
                </el-form>
              </el-tab-pane>
              <el-tab-pane :label="labels.tabPwd" name="pwd">
                <el-form ref="pwdForm" :model="pwdForm" :rules="pwdRules" label-width="88px" class="profile-form">
                  <el-form-item :label="labels.oldPwd" prop="oldPassword">
                    <el-input v-model="pwdForm.oldPassword" type="password" show-password :placeholder="labels.oldPwdPh" />
                  </el-form-item>
                  <el-form-item :label="labels.newPwd" prop="newPassword" :rules="infoPwdValidator">
                    <el-input v-model="pwdForm.newPassword" type="password" show-password :placeholder="labels.newPwdPh" />
                  </el-form-item>
                  <el-form-item :label="labels.confirmPwd" prop="confirmPassword">
                    <el-input v-model="pwdForm.confirmPassword" type="password" show-password :placeholder="labels.confirmPwdPh" />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" :loading="savingPwd" @click="submitPwd">{{ labels.save }}</el-button>
                    <el-button @click="resetPwd">{{ labels.reset }}</el-button>
                  </el-form-item>
                </el-form>
              </el-tab-pane>
            </el-tabs>
          </main>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getUserProfile, updateUserProfile, updateUserPwd } from '@/api/system/user'
import userAvatar from '@/views/system/user/profile/userAvatar'
import passwordRule from '@/utils/passwordRule'
import { goPortalLogin } from '@/utils/portalLogin'
import { profileLabels } from './portal-profile-labels'

export default {
  name: 'PortalProfile',
  components: { userAvatar },
  mixins: [passwordRule],
  data() {
    return {
      labels: profileLabels,
      loading: false,
      savingInfo: false,
      savingPwd: false,
      activeTab: 'info',
      user: {},
      roleGroup: '',
      postGroup: '',
      infoForm: {
        nickName: '',
        phonenumber: '',
        email: '',
        sex: '0'
      },
      pwdForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
    }
  },
  computed: {
    ...mapGetters(['token', 'questionBasketCount']),
    deptText() {
      if (!this.user.dept) return '-'
      const dept = this.user.dept.deptName || ''
      const post = this.postGroup || ''
      return post ? dept + ' / ' + post : dept
    },
    infoRules() {
      const L = this.labels
      return {
        nickName: [{ required: true, message: L.nickRequired, trigger: 'blur' }],
        email: [
          { required: true, message: L.emailRequired, trigger: 'blur' },
          { type: 'email', message: L.emailInvalid, trigger: ['blur', 'change'] }
        ],
        phonenumber: [
          { required: true, message: L.phoneRequired, trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: L.phoneInvalid, trigger: 'blur' }
        ]
      }
    },
    pwdRules() {
      const L = this.labels
      return {
        oldPassword: [{ required: true, message: L.oldPwdRequired, trigger: 'blur' }],
        confirmPassword: [
          { required: true, message: L.confirmRequired, trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              if (this.pwdForm.newPassword !== value) {
                callback(new Error(L.pwdMismatch))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }
        ]
      }
    }
  },
  watch: {
    token: {
      immediate: true,
      handler(val) {
        if (val) this.loadProfile()
      }
    }
  },
  methods: {
    goLogin() {
      goPortalLogin(this.$router, '/profile')
    },
    loadProfile() {
      this.loading = true
      getUserProfile().then(res => {
        this.user = res.data || {}
        this.roleGroup = res.roleGroup || ''
        this.postGroup = res.postGroup || ''
        this.syncInfoForm()
      }).catch(() => {
        this.$message.error(this.labels.loadFail)
      }).finally(() => {
        this.loading = false
      })
    },
    syncInfoForm() {
      this.infoForm = {
        nickName: this.user.nickName || '',
        phonenumber: this.user.phonenumber || '',
        email: this.user.email || '',
        sex: this.user.sex != null ? String(this.user.sex) : '0'
      }
    },
    submitInfo() {
      this.$refs.infoForm.validate(valid => {
        if (!valid) return
        this.savingInfo = true
        updateUserProfile(this.infoForm).then(() => {
          this.$message.success(this.labels.saveOk)
          this.user.nickName = this.infoForm.nickName
          this.user.phonenumber = this.infoForm.phonenumber
          this.user.email = this.infoForm.email
          this.user.sex = this.infoForm.sex
          this.$store.commit('SET_NICK_NAME', this.infoForm.nickName)
        }).finally(() => {
          this.savingInfo = false
        })
      })
    },
    resetInfo() {
      this.syncInfoForm()
      this.$refs.infoForm && this.$refs.infoForm.clearValidate()
    },
    submitPwd() {
      this.$refs.pwdForm.validate(valid => {
        if (!valid) return
        this.savingPwd = true
        updateUserPwd(this.pwdForm.oldPassword, this.pwdForm.newPassword).then(() => {
          this.$message.success(this.labels.pwdOk)
          this.resetPwd()
        }).finally(() => {
          this.savingPwd = false
        })
      })
    },
    resetPwd() {
      this.pwdForm = { oldPassword: '', newPassword: '', confirmPassword: '' }
      this.$refs.pwdForm && this.$refs.pwdForm.clearValidate()
    }
  }
}
</script>

<style scoped lang="scss">
$primary: #2563EB;
$violet: #7C3AED;
$emerald: #059669;
$ink: #0F172A;
$muted: #64748B;
$border: #E2E8F0;

.profile-wrap { padding-bottom: 48px; }

.login-hint {
  i { color: $primary; }
  .el-button { margin-left: auto; }
}

.profile-hero {
  margin-bottom: 16px;
}

.hero-title-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hero-text {
  h1 {
    margin: 0 0 4px;
    font-size: 20px;
    font-weight: 800;
    color: $ink;
    letter-spacing: -0.01em;
  }
  p { margin: 0; font-size: 13px; color: $muted; }
}

.hero-actions { display: flex; gap: 8px; flex-wrap: wrap; }

.profile-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  align-items: start;
}

.profile-side {
  padding: 0;
  overflow: hidden;
  box-shadow: none;
  background: linear-gradient(180deg, #fff 0%, #FAFBFF 100%);
}

.profile-main {
  padding: 0;
  overflow: hidden;
  box-shadow: none;
  background: linear-gradient(180deg, #fff 0%, #F8FAFF 100%);
}

.side-head,
.main-head {
  padding: 14px 20px;
  font-size: 13px;
  font-weight: 700;
  color: $muted;
  background: linear-gradient(90deg, #FAFBFC, #F8FAFF);
  border-bottom: 1px solid $border;
  letter-spacing: 0.01em;
  display: flex;
  align-items: center;
  gap: 10px;

  &::before {
    content: '';
    width: 3px;
    height: 14px;
    border-radius: 2px;
    background: linear-gradient(180deg, #A78BFA, $violet);
  }
}

.main-head::before {
  background: linear-gradient(180deg, #60A5FA, $primary);
}

.avatar-wrap {
  display: flex;
  justify-content: center;
  padding: 24px 20px 8px;
}

.display-name {
  margin: 0;
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  color: $ink;
}

.display-account {
  margin: 4px 0 16px;
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
}

.info-list {
  list-style: none;
  margin: 0;
  padding: 0 20px 16px;
  li {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding: 10px 0;
    border-bottom: 1px dashed #EEF2F6;
    font-size: 13px;
    &:last-child { border-bottom: none; }
  }
  .info-label { color: #94a3b8; flex-shrink: 0; }
  .info-value { color: #475569; text-align: right; word-break: break-all; }
}

.stat-box {
  margin: 0 20px 20px;
  padding: 14px 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, #FAF5FF 0%, #EFF6FF 100%);
  border: 1px solid rgba(124, 58, 237, 0.18);
  display: flex;
  align-items: baseline;
  gap: 6px;
  .stat-label { font-size: 13px; color: #64748b; flex: 1; }
  .stat-num {
    font-size: 24px;
    font-weight: 800;
    line-height: 1;
    background: linear-gradient(135deg, $violet, $primary);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
  .stat-unit { font-size: 12px; color: #94a3b8; }
}

.profile-quick-nav {
  margin: 0 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  transition: background 0.2s ease, border-color 0.2s ease, transform 0.15s ease;

  i:first-child {
    font-size: 16px;
    color: #7c3aed;
  }

  .quick-nav-arrow {
    margin-left: auto;
    font-size: 12px;
    color: #94a3b8;
  }

  &:hover {
    background: #f1f5f9;
    border-color: #cbd5e1;
    transform: translateY(-1px);
  }

  &.router-link-active {
    background: linear-gradient(135deg, #faf5ff 0%, #eff6ff 100%);
    border-color: rgba(124, 58, 237, 0.25);
  }
}

.quick-nav-item--purchase i:first-child {
  color: #059669;
}

.profile-tabs {
  padding: 8px 20px 24px;
  ::v-deep .el-tabs__item.is-active { color: $primary; }
  ::v-deep .el-tabs__active-bar { background-color: $primary; }
}

.profile-form {
  max-width: 480px;
  padding-top: 12px;

  ::v-deep .el-input__inner {
    border-radius: 10px;
    border-color: $border;
    &:focus { border-color: $primary; }
  }
}

@media (max-width: 960px) {
  .profile-grid { grid-template-columns: 1fr; }
  .profile-hero { flex-direction: column; align-items: flex-start; }
}
</style>
