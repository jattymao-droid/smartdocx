<template>
  <div class="kms-vip-page">
    <!-- ?????????? -->
    <section class="vip-banner">
      <div class="banner-city" aria-hidden="true" />
      <div class="portal-container banner-inner">
        <div class="banner-text">
          <h1>{{ L.bannerTitle }}</h1>
          <p class="banner-sub">
            <span>{{ L.bannerSub1 }}</span>
            <em>|</em>
            <span>{{ L.bannerSub2 }}</span>
            <em>|</em>
            <span>{{ L.bannerSub3 }}</span>
            <em>|</em>
            <span>{{ L.bannerSub4 }}</span>
          </p>
        </div>
        <div class="banner-crown" aria-hidden="true">
          <div class="crown-pedestal" />
          <i class="el-icon-medal crown-icon" />
        </div>
      </div>
    </section>

    <div class="portal-container vip-main-wrap">
      <nav class="portal-breadcrumb vip-crumb" aria-label="breadcrumb">
        <router-link to="/">{{ L.home }}</router-link>
        <span class="sep">/</span>
        <router-link to="/library">{{ L.library }}</router-link>
        <span class="sep">/</span>
        <span class="current">{{ L.breadcrumb }}</span>
      </nav>

      <div v-loading="loading" class="vip-panel">
        <!-- ?????? -->
        <div v-if="status.active" class="vip-member-active">
          <div class="member-badge"><i class="el-icon-medal" /></div>
          <h2>{{ L.activeTitle }}</h2>
          <p>{{ L.expireLabel }}??{{ formatDate(status.expireTime) }} ?? {{ L.remainLabel.replace('{n}', status.remainDays || 0) }}</p>
          <el-button type="warning" round @click="$router.push('/library')">{{ L.goLibrary }}</el-button>
        </div>

        <template v-else>
          <!-- ?????? -->
          <div class="tier-row">
            <button
              v-for="plan in plans"
              :key="plan.code"
              type="button"
              class="tier-card"
              :class="{ 'tier-card--active': selectedPlanCode === plan.code }"
              @click="selectPlan(plan.code)"
            >
              <span v-if="plan.badge" class="tier-badge">{{ plan.badge }}</span>
              <span class="tier-name">{{ plan.name }}</span>
              <span class="tier-price">&yen;{{ formatPrice(plan.price) }}</span>
              <span class="tier-original">&yen;{{ formatPrice(plan.originalPrice) }}</span>
              <i v-if="selectedPlanCode === plan.code" class="el-icon-check tier-check" />
            </button>
          </div>

          <!-- VIP ??? -->
          <div class="privilege-section">
            <div class="privilege-head">
              <span class="privilege-line" />
              <h3>{{ L.privilegeTitle }}</h3>
              <span class="privilege-line" />
            </div>
            <div class="privilege-grid">
              <div v-for="item in privileges" :key="item.key" class="privilege-item">
                <div class="privilege-icon"><i :class="item.icon" /></div>
                <p class="privilege-main">{{ item.main }}</p>
                <p class="privilege-sub">{{ item.sub }}</p>
              </div>
            </div>
          </div>

          <!-- ??? + ??? -->
          <div class="checkout-row">
            <div class="checkout-pay">
              <div class="pay-methods">
                <button
                  type="button"
                  class="pay-method"
                  :class="{ 'pay-method--active': payType === 'wxpay' }"
                  @click="switchPayType('wxpay')"
                >
                  <i class="el-icon-chat-dot-round" />
                  <span>{{ L.wxpay }}</span>
                  <i v-if="payType === 'wxpay'" class="el-icon-check pay-check" />
                </button>
                <button
                  type="button"
                  class="pay-method"
                  :class="{ 'pay-method--active': payType === 'alipay' }"
                  @click="switchPayType('alipay')"
                >
                  <i class="el-icon-wallet" />
                  <span>{{ L.alipay }}</span>
                  <i v-if="payType === 'alipay'" class="el-icon-check pay-check" />
                </button>
              </div>

              <div class="pay-body">
                <div class="qr-box" @click="handlePay">
                  <img v-if="qrcodeUrl" :src="qrcodeUrl" alt="pay qrcode" class="qr-image">
                  <div v-else class="qr-placeholder">
                    <i v-if="paying" class="el-icon-loading" />
                    <template v-else>
                      <p>{{ qrHint }}</p>
                      <span v-if="!token" class="qr-login">{{ L.loginHint }}</span>
                    </template>
                  </div>
                </div>

                <div class="pay-summary">
                  <p class="pay-amount">
                    {{ L.payAmount }}<strong>&yen;{{ formatPrice(selectedPlan.price) }}</strong>
                  </p>
                  <p v-if="savedAmount > 0" class="pay-saved">{{ L.saved.replace('{n}', formatPrice(savedAmount)) }}</p>
                  <p class="pay-warn">{{ L.giveUpWarn }}</p>
                  <div class="pay-countdown">
                    <span>{{ L.countdownLabel }}</span>
                    <strong>{{ countdownText }}</strong>
                  </div>
                  <p v-if="!status.enabled" class="pay-disabled-tip"><i class="el-icon-info" />{{ L.disabled }}</p>
                  <p v-else-if="!status.payEnabled" class="pay-disabled-tip"><i class="el-icon-warning-outline" />{{ L.payDisabled }}</p>
                </div>
              </div>
            </div>

            <div class="checkout-feed">
              <div class="feed-list">
                <div v-for="(item, idx) in feedList" :key="idx + item.displayName" class="feed-item">
                  <span class="feed-avatar"><i class="el-icon-user-solid" /></span>
                  <div class="feed-main">
                    <p class="feed-user">{{ item.displayName }} <span class="feed-time">{{ item.timeLabel }}</span></p>
                    <p class="feed-action">
                      {{ L.feedAction.replace('{plan}', item.planName) }}
                      <span v-if="Number(item.savedAmount) > 0" class="feed-save">{{ L.feedSaved.replace('{n}', formatPrice(item.savedAmount)) }}</span>
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- ??????????? -->
    <aside class="vip-float-bar" aria-label="quick actions">
      <router-link to="/library" class="float-item" :title="L.floatLibrary">
        <i class="el-icon-reading" />
        <span>{{ L.floatLibrary }}</span>
      </router-link>
      <router-link to="/profile" class="float-item" :title="L.floatProfile">
        <i class="el-icon-user" />
        <span>{{ L.floatProfile }}</span>
      </router-link>
      <button type="button" class="float-item" :title="L.floatShare" @click="copyShare">
        <i class="el-icon-share" />
        <span>{{ L.floatShare }}</span>
      </button>
    </aside>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getVipStatus, getVipRecentOrders } from '@/api/education/vip'
import { checkPayAccess, createPayOrder, getPayOrderStatus, PAY_BIZ } from '@/api/education/pay'
import { goPortalLogin } from '@/utils/portalLogin'

const L = {
  home: '\u9996\u9875',
  library: '\u6587\u5e93',
  breadcrumb: 'VIP \u4f1a\u5458',
  bannerTitle: '\u5f00\u901aVIP \u6d77\u91cf\u7cbe\u54c1\u514d\u8d39\u4e0b\u8f7d',
  bannerSub1: 'VIP\u4e13\u4eab\u6587\u6863\u4e0b\u8f7d',
  bannerSub2: '\u6d77\u91cf\u4f18\u8d28\u6587\u6863\u514d\u8d39\u4e0b\u8f7d',
  bannerSub3: '\u9ad8\u901f\u4e0b\u8f7d\u7279\u6743',
  bannerSub4: '1v1\u4e13\u5c5e\u5ba2\u670d',
  privilegeTitle: 'VIP\u7279\u6743',
  wxpay: '\u5fae\u4fe1\u652f\u4ed8',
  alipay: '\u652f\u4ed8\u5b9d',
  payAmount: '\u652f\u4ed8\u91d1\u989d',
  saved: '\u7701\u4e86{n}\u5143',
  giveUpWarn: '\u653e\u5f03\u6b64\u6b21\u4f18\u60e0\uff0c\u4e0b\u6b21\u5c06\u539f\u4ef7\u8d2d\u4e70',
  countdownLabel: '\u4f18\u60e0\u5269\u4f59\u65f6\u95f4\uff1a',
  disabled: 'VIP \u529f\u80fd\u6682\u672a\u5f00\u653e',
  payDisabled: '\u652f\u4ed8\u529f\u80fd\u672a\u5f00\u542f\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458',
  loginHint: '\u70b9\u51fb\u767b\u5f55\u540e\u626b\u7801\u652f\u4ed8',
  qrReady: '\u70b9\u51fb\u83b7\u53d6\u652f\u4ed8\u4e8c\u7ef4\u7801',
  activeTitle: '\u60a8\u5df2\u662f VIP \u4f1a\u5458',
  expireLabel: '\u5230\u671f\u65f6\u95f4',
  remainLabel: '\u5269\u4f59 {n} \u5929',
  goLibrary: '\u53bb\u6587\u5e93\u4e0b\u8f7d',
  feedAction: '\u5f00\u901a\u4e86{plan}',
  feedSaved: '\u7701\u4e86{n}\u5143',
  floatLibrary: '\u6587\u5e93',
  floatProfile: '\u4e2a\u4eba\u4e2d\u5fc3',
  floatShare: '\u5206\u4eab',
  shareCopied: '\u94fe\u63a5\u5df2\u590d\u5236',
  paySuccess: '\u5f00\u901a\u6210\u529f\uff0c\u6b22\u8fce\u52a0\u5165 VIP\uff01'
}

const FALLBACK_PLANS = [
  { code: 'supreme', name: '\u81f3\u5c0a\u4f1a\u5458', price: 388, originalPrice: 598, durationDays: 1095, badge: '\u5b98\u65b9\u63a8\u8350', recommended: true },
  { code: 'diamond', name: '\u94bb\u77f3\u4f1a\u5458', price: 188, originalPrice: 398, durationDays: 365, badge: null, recommended: false },
  { code: 'gold', name: '\u9ec4\u91d1\u4f1a\u5458', price: 128, originalPrice: 288, durationDays: 180, badge: '\u9650\u65f6\u6298\u6263', recommended: false },
  { code: 'test', name: '\u6d4b\u8bd5\u4f1a\u5458', price: 0.1, originalPrice: 128, durationDays: 30, badge: '100\u6b21\u514d\u8d39\u4e0b\u8f7d', recommended: false }
]

export default {
  name: 'PortalLibraryVip',
  data() {
    return {
      L,
      PAY_BIZ,
      loading: false,
      paying: false,
      polling: false,
      pollTimer: null,
      countdownTimer: null,
      countdownSec: 600,
      status: {
        enabled: false,
        active: false,
        expireTime: null,
        remainDays: 0,
        freeDownload: true,
        previewPages: 0,
        payEnabled: false,
        plans: [],
        defaultPlanCode: 'supreme'
      },
      feedList: [],
      selectedPlanCode: 'supreme',
      payType: 'wxpay',
      orderNo: '',
      qrcodeUrl: '',
      payUrl: ''
    }
  },
  computed: {
    ...mapGetters(['token']),
    plans() {
      return (this.status.plans && this.status.plans.length) ? this.status.plans : FALLBACK_PLANS
    },
    selectedPlan() {
      return this.plans.find(p => p.code === this.selectedPlanCode) || this.plans[0] || FALLBACK_PLANS[0]
    },
    savedAmount() {
      const price = Number(this.selectedPlan.price) || 0
      const original = Number(this.selectedPlan.originalPrice) || 0
      return Math.max(0, original - price)
    },
    countdownText() {
      const h = String(Math.floor(this.countdownSec / 3600)).padStart(2, '0')
      const m = String(Math.floor((this.countdownSec % 3600) / 60)).padStart(2, '0')
      const s = String(this.countdownSec % 60).padStart(2, '0')
      return `${h}:${m}:${s}`
    },
    qrHint() {
      if (!this.status.enabled || !this.status.payEnabled) return L.disabled
      return L.qrReady
    },
    privileges() {
      const months = Math.max(1, Math.round((this.selectedPlan.durationDays || 30) / 30))
      const list = [
        { key: 'dl1', icon: 'el-icon-download', main: '\u65e0\u9650\u6b21', sub: 'VIP\u4e13\u4eab\u6587\u6863\u4e0b\u8f7d' },
        { key: 'dl2', icon: 'el-icon-document', main: '\u65e0\u9650\u6b21', sub: 'VIP\u514d\u8d39\u6587\u6863\u4e0b\u8f7d' },
        { key: 'dl3', icon: 'el-icon-folder-opened', main: '\u65e0\u9650\u6b21', sub: '\u514d\u8d39\u6587\u6863\u4e0b\u8f7d' }
      ]
      if (this.status.freeDownload) {
        list.push({ key: 'discount', icon: 'el-icon-price-tag', main: '\u514d\u8d39', sub: '\u4ed8\u8d39\u6587\u6863\u4e13\u4eab\u6298\u6263' })
      }
      list.push(
        { key: 'months', icon: 'el-icon-date', main: months + '\u4e2a\u6708', sub: '\u4f1a\u5458\u6709\u6548\u671f' },
        { key: 'share', icon: 'el-icon-present', main: '\u5206\u9500\u8d5a\u94b1', sub: '\u5206\u4eab\u5e73\u53f0\u8d5a\u53d6\u4f63\u91d1' }
      )
      return list.slice(0, 6)
    },
    canPay() {
      return this.status.enabled && this.status.payEnabled && Number(this.selectedPlan.price) > 0
    }
  },
  watch: {
    token(val, oldVal) {
      if (val && val !== oldVal) {
        this.loadStatus()
        this.resetPayState()
      }
    }
  },
  created() {
    this.loadStatus()
    this.scheduleFeedLoad()
    this.startCountdown()
    if (this.$route.query.payReturn === '1') {
      this.handlePayReturn()
    }
  },
  beforeDestroy() {
    this.stopPoll()
    this.stopCountdown()
  },
  methods: {
    loadStatus() {
      this.loading = true
      getVipStatus().then(res => {
        this.status = Object.assign({}, this.status, res.data || {})
        this.selectedPlanCode = this.status.defaultPlanCode || (this.plans[0] && this.plans[0].code) || 'supreme'
      }).finally(() => { this.loading = false })
    },
    loadFeed() {
      getVipRecentOrders(8).then(res => {
        this.feedList = res.data || []
      }).catch(() => {})
    },
    scheduleFeedLoad() {
      const run = () => this.loadFeed()
      if (typeof window !== 'undefined' && typeof window.requestIdleCallback === 'function') {
        window.requestIdleCallback(run, { timeout: 2000 })
      } else {
        setTimeout(run, 120)
      }
    },
    selectPlan(code) {
      if (this.selectedPlanCode === code) return
      this.selectedPlanCode = code
      this.resetPayState()
    },
    switchPayType(type) {
      if (this.payType === type) return
      this.payType = type
      this.resetPayState()
    },
    resetPayState() {
      this.stopPoll()
      this.orderNo = ''
      this.qrcodeUrl = ''
      this.payUrl = ''
    },
    handlePay() {
      if (!this.token) {
        goPortalLogin(this.$router, this.$route.fullPath)
        return
      }
      if (!this.canPay) return
      if (this.qrcodeUrl && this.orderNo) return
      this.paying = true
      checkPayAccess({
        bizType: PAY_BIZ.LIBRARY_VIP,
        bizId: 0,
        bizRef: this.selectedPlanCode
      }).then(res => {
        const info = res.data || {}
        if (!info.needPay) {
          this.$message.warning(L.disabled)
          return
        }
        if (!info.enabled) {
          this.$message.warning(L.payDisabled)
          return
        }
        return createPayOrder({
          bizType: PAY_BIZ.LIBRARY_VIP,
          bizId: 0,
          bizRef: this.selectedPlanCode,
          payType: this.payType,
          returnUrl: window.location.origin + '/library/vip?payReturn=1&payOrder='
        })
      }).then(res => {
        if (!res) return
        const order = res.data || {}
        this.orderNo = order.orderNo || ''
        this.payUrl = order.payUrl || ''
        this.qrcodeUrl = order.qrcodeUrl || ''
        if (this.payUrl && !this.qrcodeUrl) {
          window.open(this.payUrl, '_blank')
        }
        this.startPoll()
      }).catch(err => {
        this.$message.error((err && err.message) || '\u521b\u5efa\u8ba2\u5355\u5931\u8d25')
      }).finally(() => { this.paying = false })
    },
    handlePayReturn() {
      const orderNo = this.$route.query.payOrder || this.$route.query.out_trade_no
      if (!orderNo) return
      this.orderNo = orderNo
      this.startPoll()
    },
    startPoll() {
      this.stopPoll()
      if (!this.orderNo) return
      this.polling = true
      this.pollTimer = setInterval(() => {
        getPayOrderStatus(this.orderNo).then(res => {
          if ((res.data || {}).status === 'paid') {
            this.onPaidSuccess()
          }
        }).catch(() => {})
      }, 2500)
    },
    stopPoll() {
      this.polling = false
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },
    onPaidSuccess() {
      this.stopPoll()
      this.$message.success(L.paySuccess)
      this.loadStatus()
      this.loadFeed()
      this.resetPayState()
    },
    startCountdown() {
      this.stopCountdown()
      this.countdownTimer = setInterval(() => {
        if (this.countdownSec > 0) {
          this.countdownSec -= 1
        } else {
          this.countdownSec = 600
        }
      }, 1000)
    },
    stopCountdown() {
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer)
        this.countdownTimer = null
      }
    },
    copyShare() {
      const url = window.location.href.split('?')[0]
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(url).then(() => {
          this.$message.success(L.shareCopied)
        }).catch(() => {})
      }
    },
    formatPrice(value) {
      const n = Number(value)
      return Number.isFinite(n) ? n.toFixed(2) : '0.00'
    },
    formatDate(value) {
      if (!value) return '-'
      return String(value).replace('T', ' ').slice(0, 16)
    }
  }
}
</script>

<style scoped lang="scss">
$dark: #2c3142;
$gold: #d4a017;
$orange: #ff8c00;
$orange-border: #ff9900;

.kms-vip-page {
  background: #f0f2f5;
  min-height: calc(100vh - 120px);
  padding-bottom: 48px;
  position: relative;
}

/* ===== Banner ===== */
.vip-banner {
  position: relative;
  background: linear-gradient(135deg, #1a1d29 0%, $dark 40%, #3d4459 100%);
  overflow: hidden;
  padding: 36px 0 48px;
}

.banner-city {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(to top, rgba(0, 0, 0, 0.5) 0%, transparent 50%),
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1200 120' preserveAspectRatio='none'%3E%3Cpath fill='%231f2330' d='M0 120 L0 80 L40 80 L40 50 L80 50 L80 70 L120 70 L120 40 L160 40 L160 75 L200 75 L200 55 L240 55 L240 30 L280 30 L280 60 L320 60 L320 45 L360 45 L360 70 L400 70 L400 35 L440 35 L440 65 L480 65 L480 50 L520 50 L520 25 L560 25 L560 55 L600 55 L600 40 L640 40 L640 68 L680 68 L680 48 L720 48 L720 28 L760 28 L760 58 L800 58 L800 42 L840 42 L840 72 L880 72 L880 52 L920 52 L920 32 L960 32 L960 62 L1000 62 L1000 46 L1040 46 L1040 76 L1080 76 L1080 56 L1120 56 L1120 36 L1160 36 L1160 66 L1200 66 L1200 120 Z'/%3E%3C/svg%3E") bottom center / 100% 80px no-repeat;
  opacity: 0.9;
}

.banner-inner {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.banner-text h1 {
  margin: 0 0 14px;
  font-size: 32px;
  font-weight: 700;
  color: $gold;
  letter-spacing: 0.04em;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.banner-sub {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;

  em {
    font-style: normal;
    opacity: 0.4;
  }
}

.banner-crown {
  position: relative;
  width: 120px;
  height: 120px;
  flex-shrink: 0;
}

.crown-pedestal {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 24px;
  border-radius: 50%;
  background: radial-gradient(ellipse, rgba(212, 160, 23, 0.6) 0%, transparent 70%);
}

.crown-icon {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 64px;
  color: $gold;
  filter: drop-shadow(0 4px 12px rgba(212, 160, 23, 0.5));
}

/* ===== Main panel ===== */
.vip-main-wrap {
  margin-top: -28px;
  position: relative;
  z-index: 2;
}

.vip-crumb {
  margin-bottom: 12px;
}

.vip-panel {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  padding: 28px 32px 32px;
}

/* ===== Tier cards ===== */
.tier-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.tier-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  padding: 20px 12px 16px;
  border: 2px solid #e8e8e8;
  border-radius: 6px;
  background: #fafafa;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
  text-align: center;

  &:hover {
    border-color: #ffd591;
  }

  &--active {
    border-color: $orange-border;
    background: #fff;
    box-shadow: 0 0 0 1px $orange-border;
  }
}

.tier-badge {
  position: absolute;
  top: -1px;
  left: 50%;
  transform: translateX(-50%);
  padding: 2px 10px;
  font-size: 11px;
  color: #fff;
  background: linear-gradient(90deg, #ff6b35, $orange);
  border-radius: 0 0 4px 4px;
  white-space: nowrap;
}

.tier-name {
  writing-mode: vertical-rl;
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 13px;
  color: #666;
  letter-spacing: 2px;
}

.tier-price {
  font-size: 26px;
  font-weight: 700;
  color: $gold;
  line-height: 1.2;
}

.tier-original {
  font-size: 12px;
  color: #bbb;
  text-decoration: line-through;
  margin-top: 4px;
}

.tier-check {
  position: absolute;
  right: 8px;
  bottom: 8px;
  font-size: 16px;
  color: $orange;
  font-weight: bold;
}

/* ===== Privileges ===== */
.privilege-section {
  margin-bottom: 32px;
}

.privilege-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #333;
    white-space: nowrap;
  }
}

.privilege-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, #e0e0e0, transparent);
}

.privilege-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.privilege-item {
  text-align: center;
}

.privilege-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fff8e6, #ffe4b8);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: $gold;
}

.privilege-main {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.privilege-sub {
  margin: 0;
  font-size: 12px;
  color: #888;
  line-height: 1.4;
}

/* ===== Checkout ===== */
.checkout-row {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
  border-top: 1px solid #f0f0f0;
  padding-top: 28px;
}

.pay-methods {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.pay-method {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: 2px solid #e8e8e8;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  transition: border-color 0.2s;

  i:first-child {
    font-size: 20px;
    color: #07c160;
  }

  &:last-child i:first-child {
    color: #1677ff;
  }

  &--active {
    border-color: #07c160;

    &:last-child {
      border-color: #1677ff;
    }
  }
}

.pay-check {
  position: absolute;
  right: 8px;
  bottom: 8px;
  font-size: 14px;
  color: inherit;
}

.pay-body {
  display: flex;
  gap: 28px;
  align-items: flex-start;
}

.qr-box {
  width: 180px;
  height: 180px;
  flex-shrink: 0;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  background: #fafafa;
}

.qr-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qr-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px;
  text-align: center;

  p {
    margin: 0;
    font-size: 13px;
    color: #999;
    line-height: 1.5;
  }
}

.qr-login {
  margin-top: 8px;
  font-size: 12px;
  color: $orange;
}

.pay-summary {
  flex: 1;
}

.pay-amount {
  margin: 0 0 8px;
  font-size: 14px;
  color: #666;

  strong {
    font-size: 28px;
    color: #e53935;
    font-weight: 700;
    margin-left: 4px;
  }
}

.pay-saved {
  margin: 0 0 6px;
  font-size: 13px;
  color: #888;
}

.pay-warn {
  margin: 0 0 16px;
  font-size: 12px;
  color: #e53935;
}

.pay-countdown {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 4px;
  font-size: 13px;
  color: #666;

  strong {
    color: #e53935;
    font-size: 15px;
    font-family: monospace;
  }
}

.pay-disabled-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #fa8c16;

  i { margin-right: 4px; }
}

/* ===== Activity feed ===== */
.checkout-feed {
  border-left: 1px solid #f0f0f0;
  padding-left: 24px;
}

.feed-list {
  max-height: 280px;
  overflow-y: auto;
}

.feed-item {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;

  &:last-child { border-bottom: none; }
}

.feed-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bbb;
  flex-shrink: 0;
}

.feed-user {
  margin: 0 0 4px;
  font-size: 13px;
  color: #333;
}

.feed-time {
  color: #bbb;
  font-size: 12px;
  margin-left: 6px;
}

.feed-action {
  margin: 0;
  font-size: 12px;
  color: #666;
}

.feed-save {
  color: #e53935;
  margin-left: 4px;
}

/* ===== Active member ===== */
.vip-member-active {
  text-align: center;
  padding: 48px 24px;

  .member-badge {
    width: 72px;
    height: 72px;
    margin: 0 auto 16px;
    border-radius: 50%;
    background: linear-gradient(135deg, #fbbf24, $gold);
    color: #fff;
    font-size: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  h2 { margin: 0 0 8px; font-size: 22px; color: #333; }
  p { margin: 0 0 20px; color: #888; font-size: 14px; }
}

/* ===== Float bar ===== */
.vip-float-bar {
  position: fixed;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.12);
  padding: 8px 0;
  border: 1px solid #eee;
}

.float-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 14px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 11px;
  color: #666;
  text-decoration: none;
  transition: color 0.2s;

  i { font-size: 20px; color: $orange; }

  &:hover {
    color: $orange;
  }
}

@media (max-width: 1100px) {
  .tier-row { grid-template-columns: repeat(2, 1fr); }
  .privilege-grid { grid-template-columns: repeat(3, 1fr); }
  .checkout-row { grid-template-columns: 1fr; }
  .checkout-feed {
    border-left: none;
    padding-left: 0;
    border-top: 1px solid #f0f0f0;
    padding-top: 20px;
  }
}

@media (max-width: 640px) {
  .banner-text h1 { font-size: 22px; }
  .banner-crown { display: none; }
  .vip-panel { padding: 20px 16px; }
  .tier-row { grid-template-columns: 1fr; }
  .tier-name { writing-mode: horizontal-tb; position: static; transform: none; margin-bottom: 8px; }
  .privilege-grid { grid-template-columns: repeat(2, 1fr); }
  .pay-body { flex-direction: column; }
  .vip-float-bar { display: none; }
}
</style>
