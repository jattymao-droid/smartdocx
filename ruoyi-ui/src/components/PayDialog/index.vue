<template>
  <el-dialog
    :title="title"
    :visible.sync="visible"
    width="440px"
    append-to-body
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-loading="creating || checking" class="pay-dialog-body">
      <div v-if="checkInfo && checkInfo.purchased" class="pay-owned">
        <i class="el-icon-circle-check" />
        <span>{{ L.owned }}</span>
      </div>

      <div v-else-if="checkInfo && !checkInfo.needPay" class="pay-owned pay-owned--free">
        <i class="el-icon-circle-check" />
        <span>{{ L.free }}</span>
      </div>

      <template v-else-if="checkInfo">
        <div class="pay-product">
          <p class="pay-product-name">{{ checkInfo.productName || productLabel }}</p>
          <p class="pay-product-price">
            <span class="pay-price-label">{{ L.amountLabel }}</span>
            <span class="pay-price-value">&yen;{{ formatPrice(checkInfo.price) }}</span>
          </p>
        </div>

        <div v-if="!checkInfo.enabled" class="pay-disabled-hint">
          <i class="el-icon-warning-outline" />
          <span>{{ L.disabled }}</span>
        </div>

        <div class="pay-type-row">
          <span class="pay-type-label">{{ L.payTypeLabel }}</span>
          <el-radio-group v-model="payType" size="small" :disabled="!checkInfo.enabled">
            <el-radio-button label="alipay">{{ L.alipay }}</el-radio-button>
            <el-radio-button label="wxpay">{{ L.wxpay }}</el-radio-button>
          </el-radio-group>
        </div>

        <div v-if="qrcodeUrl" class="pay-qrcode-wrap">
          <img :src="qrcodeUrl" :alt="L.qrcodeAlt" class="pay-qrcode">
          <p class="pay-qrcode-hint">{{ payType === 'wxpay' ? L.scanWx : L.scanAli }}</p>
        </div>

        <div v-if="polling" class="pay-polling">
          <i class="el-icon-loading" />
          <span>{{ L.waiting }}</span>
        </div>
      </template>
    </div>

    <div slot="footer">
      <el-button @click="visible = false">{{ L.cancel }}</el-button>
      <el-button
        v-if="checkInfo && checkInfo.purchased"
        type="primary"
        @click="confirmOwned"
      >{{ L.confirmDownload }}</el-button>
      <template v-else-if="checkInfo && checkInfo.needPay">
        <el-button v-if="payUrl && !qrcodeUrl" type="primary" plain @click="openPayUrl">{{ L.openPayPage }}</el-button>
        <el-button type="primary" :loading="creating" :disabled="!checkInfo.enabled" @click="startPay">
          {{ orderNo ? L.refreshQr : L.goPay }}
        </el-button>
      </template>
    </div>
  </el-dialog>
</template>

<script>
import { checkPayAccess, createPayOrder, getPayOrderStatus } from '@/api/education/pay'

const L = {
  amountLabel: '\u5e94\u4ed8\u91d1\u989d',
  payTypeLabel: '\u652f\u4ed8\u65b9\u5f0f',
  alipay: '\u652f\u4ed8\u5b9d',
  wxpay: '\u5fae\u4fe1',
  qrcodeAlt: '\u652f\u4ed8\u4e8c\u7ef4\u7801',
  scanWx: '\u8bf7\u4f7f\u7528\u5fae\u4fe1\u626b\u7801\u652f\u4ed8',
  scanAli: '\u8bf7\u4f7f\u7528\u652f\u4ed8\u5b9d\u626b\u7801\u652f\u4ed8',
  waiting: '\u7b49\u5f85\u652f\u4ed8\u7ed3\u679c\u2026',
  cancel: '\u53d6\u6d88',
  openPayPage: '\u6253\u5f00\u652f\u4ed8\u9875',
  refreshQr: '\u5237\u65b0\u4e8c\u7ef4\u7801',
  goPay: '\u53bb\u652f\u4ed8',
  paySuccess: '\u652f\u4ed8\u6210\u529f',
  createFail: '\u521b\u5efa\u8ba2\u5355\u5931\u8d25',
  owned: '\u60a8\u5df2\u8d2d\u4e70\u8be5\u5185\u5bb9\uff0c\u53ef\u76f4\u63a5\u4e0b\u8f7d',
  free: '\u8be5\u5185\u5bb9\u514d\u8d39\uff0c\u53ef\u76f4\u63a5\u4e0b\u8f7d',
  confirmDownload: '\u7ee7\u7eed\u4e0b\u8f7d',
  disabled: '\u652f\u4ed8\u529f\u80fd\u672a\u5f00\u542f\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458'
}

export default {
  name: 'PayDialog',
  data() {
    return {
      L,
      visible: false,
      checking: false,
      creating: false,
      polling: false,
      pollTimer: null,
      bizType: '',
      bizId: null,
      bizRef: '',
      title: '\u5728\u7ebf\u652f\u4ed8',
      productLabel: '\u4ed8\u8d39\u5185\u5bb9',
      checkInfo: null,
      payType: 'alipay',
      orderNo: '',
      payUrl: '',
      qrcodeUrl: '',
      resolveFn: null,
      rejectFn: null,
      settled: false
    }
  },
  beforeDestroy() {
    this.stopPoll()
  },
  methods: {
    open(options = {}) {
      this.bizType = options.bizType || ''
      this.bizId = options.bizId || null
      this.bizRef = options.bizRef || ''
      this.title = options.title || '\u5728\u7ebf\u652f\u4ed8'
      this.productLabel = options.productLabel || '\u4ed8\u8d39\u5185\u5bb9'
      this.payType = options.payType || 'alipay'
      this.orderNo = options.orderNo || ''
      this.payUrl = ''
      this.qrcodeUrl = ''
      this.checkInfo = null
      this.settled = false
      this.visible = true
      return new Promise((resolve, reject) => {
        this.resolveFn = resolve
        this.rejectFn = reject
        this.loadCheck().then(info => {
          if (!info.needPay || info.purchased) {
            this.settle({ purchased: true, skipped: true, status: info })
          } else if (this.orderNo) {
            this.startPoll()
          }
        }).catch(err => {
          this.visible = false
          reject(err)
        })
      })
    },
    loadCheck() {
      this.checking = true
      return checkPayAccess({
        bizType: this.bizType,
        bizId: this.bizId,
        bizRef: this.bizRef || undefined
      }).then(res => {
        this.checkInfo = res.data || {}
        return this.checkInfo
      }).finally(() => {
        this.checking = false
      })
    },
    startPay() {
      if (!this.bizType || !this.checkInfo || !this.checkInfo.enabled) return
      this.creating = true
      const returnUrl = this.buildReturnUrl()
      createPayOrder({
        bizType: this.bizType,
        bizId: this.bizId,
        bizRef: this.bizRef || undefined,
        payType: this.payType,
        returnUrl
      }).then(res => {
        const order = res.data || {}
        this.orderNo = order.orderNo || ''
        this.payUrl = order.payUrl || ''
        this.qrcodeUrl = order.qrcodeUrl || ''
        this.syncReturnQuery()
        if (this.payUrl && !this.qrcodeUrl) {
          window.open(this.payUrl, '_blank')
        }
        this.startPoll()
      }).catch(err => {
        this.$modal.msgError((err && err.message) || L.createFail)
      }).finally(() => {
        this.creating = false
      })
    },
    buildReturnUrl() {
      const url = new URL(window.location.href)
      url.searchParams.set('payReturn', '1')
      if (this.bizType) url.searchParams.set('payBiz', this.bizType)
      if (this.bizId) url.searchParams.set('payBizId', String(this.bizId))
      return url.toString()
    },
    syncReturnQuery() {
      if (!this.orderNo || !this.$router) return
      const query = { ...this.$route.query, payReturn: '1', payOrder: this.orderNo }
      this.$router.replace({ path: this.$route.path, query }).catch(() => {})
    },
    openPayUrl() {
      if (this.payUrl) window.open(this.payUrl, '_blank')
    },
    startPoll() {
      this.stopPoll()
      if (!this.orderNo) return
      this.polling = true
      this.pollTimer = setInterval(() => {
        getPayOrderStatus(this.orderNo).then(res => {
          const order = res.data || {}
          if (order.status === 'paid') {
            this.onPaidSuccess()
          }
        }).catch(() => {})
      }, 2500)
    },
    onPaidSuccess() {
      this.stopPoll()
      this.$modal.msgSuccess(L.paySuccess)
      if (this.checkInfo) this.checkInfo.purchased = true
      this.settle({ purchased: true, orderNo: this.orderNo })
    },
    confirmOwned() {
      this.settle({ purchased: true, skipped: true })
    },
    settle(result) {
      if (this.settled) return
      this.settled = true
      this.visible = false
      this.clearReturnQuery()
      const resolve = this.resolveFn
      this.resolveFn = null
      this.rejectFn = null
      if (resolve) resolve(result)
    },
    clearReturnQuery() {
      if (!this.$router) return
      const query = { ...this.$route.query }
      let changed = false
      ;['payReturn', 'payOrder', 'payBiz', 'payBizId', 'out_trade_no'].forEach(key => {
        if (query[key] !== undefined) {
          delete query[key]
          changed = true
        }
      })
      if (changed) {
        this.$router.replace({ path: this.$route.path, query }).catch(() => {})
      }
    },
    stopPoll() {
      this.polling = false
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },
    handleClose() {
      this.stopPoll()
      if (this.settled || !this.rejectFn) return
      const reject = this.rejectFn
      this.rejectFn = null
      this.resolveFn = null
      reject(new Error('cancelled'))
    },
    formatPrice(value) {
      const n = Number(value)
      if (!Number.isFinite(n)) return '0.00'
      return n.toFixed(2)
    }
  }
}
</script>

<style scoped lang="scss">
.pay-dialog-body {
  min-height: 120px;
}
.pay-owned {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border-radius: 8px;
  background: #f0f9eb;
  color: #67c23a;
  font-size: 14px;
  i { font-size: 22px; }
}
.pay-owned--free {
  background: #f5f8fc;
  color: #409eff;
}
.pay-disabled-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fdf6ec;
  color: #e6a23c;
  font-size: 13px;
}
.pay-product {
  margin-bottom: 16px;
  padding: 12px 14px;
  background: #f5f8fc;
  border-radius: 8px;
}
.pay-product-name {
  margin: 0 0 8px;
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
}
.pay-product-price {
  margin: 0;
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.pay-price-label {
  font-size: 13px;
  color: #909399;
}
.pay-price-value {
  font-size: 22px;
  font-weight: 700;
  color: #e6a23c;
}
.pay-type-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.pay-type-label {
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
}
.pay-qrcode-wrap {
  text-align: center;
  margin-top: 8px;
}
.pay-qrcode {
  width: 200px;
  height: 200px;
  object-fit: contain;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}
.pay-qrcode-hint {
  margin: 10px 0 0;
  font-size: 13px;
  color: #909399;
}
.pay-polling {
  margin-top: 12px;
  text-align: center;
  font-size: 13px;
  color: #409eff;
  i { margin-right: 6px; }
}
</style>
