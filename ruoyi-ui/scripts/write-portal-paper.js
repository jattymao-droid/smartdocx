/**
 * Rewrite Paper.vue with smart UI (ASCII-safe). Run: node scripts/write-portal-paper.js
 */
const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/views/portal/Paper.vue')

const L = {
  home: '\u9996\u9875',
  breadcrumb: '\u667a\u80fd\u7ec4\u5377',
  title: '\u667a\u80fd\u7ec4\u5377',
  desc: '\u4ece\u8bd5\u9898\u7bee\u9009\u62e9\u8bd5\u9898\u540e\uff0c\u53ef\u5728\u6b64\u9884\u89c8\u5e76\u5bfc\u51fa\u8bd5\u5377',
  preview: '\u8fdb\u5165\u7ec4\u5377\u9884\u89c8',
  continuePick: '\u7ee7\u7eed\u9009\u9898',
  basketPrefix: '\u5f53\u524d\u8bd5\u9898\u7bee\uff1a',
  basketUnit: '\u9898',
  smartTitle: '\u667a\u80fd\u7ec4\u5377\u5f15\u64ce',
  smartDesc: '\u81ea\u52a8\u7edf\u8ba1\u9898\u578b\u5206\u5e03\u3001\u96be\u5ea6\u5c42\u7ea7\uff0c\u751f\u6210\u89c4\u8303\u8bd5\u5377',
  stepsTitle: '\u4e09\u6b65\u5b8c\u6210\u51fa\u5377',
  emptyBasket: '\u8bd5\u9898\u7bee\u4e3a\u7a7a\uff0c\u5148\u53bb\u9009\u9898\u5427',
  goPick: '\u53bb\u9009\u9898'
}

const content = `<template>
  <div class="portal-paper portal-page">
    <div class="portal-container paper-wrap">
      <div class="breadcrumb">
        <router-link to="/portal/home">{{ labels.home }}</router-link>
        <span class="sep">&gt;</span>
        <span>{{ labels.breadcrumb }}</span>
      </div>

      <div class="paper-hero portal-card">
        <div class="paper-hero-left">
          <span class="smart-badge"><i class="el-icon-cpu" /> AI</span>
          <h1>{{ labels.title }}</h1>
          <p>{{ labels.desc }}</p>
          <div class="paper-actions">
            <el-button type="primary" round icon="el-icon-view" :disabled="!basketCount" @click="goPreview">{{ labels.preview }}</el-button>
            <el-button round icon="el-icon-search" @click="$router.push('/portal/chapter')">{{ labels.continuePick }}</el-button>
          </div>
        </div>
        <div class="paper-hero-right">
          <div class="basket-ring" :class="{ empty: !basketCount }">
            <svg viewBox="0 0 120 120" class="ring-svg">
              <circle cx="60" cy="60" r="52" class="ring-bg" />
              <circle cx="60" cy="60" r="52" class="ring-fill" :style="ringStyle" />
            </svg>
            <div class="ring-center">
              <b>{{ basketCount }}</b>
              <span>{{ labels.basketUnit }}</span>
            </div>
          </div>
          <p class="basket-label">{{ labels.basketPrefix }}<b>{{ basketCount }}</b> {{ labels.basketUnit }}</p>
        </div>
      </div>

      <div v-if="!basketCount" class="empty-hint portal-card">
        <i class="el-icon-shopping-cart-2" />
        <span>{{ labels.emptyBasket }}</span>
        <el-button type="primary" size="small" round @click="$router.push('/portal/chapter')">{{ labels.goPick }}</el-button>
      </div>

      <div class="smart-engine portal-card">
        <div class="engine-icon"><i class="el-icon-magic-stick" /></div>
        <div class="engine-text">
          <h3>{{ labels.smartTitle }}</h3>
          <p>{{ labels.smartDesc }}</p>
        </div>
      </div>

      <div class="portal-section-title tight steps-head">{{ labels.stepsTitle }}</div>
      <div class="paper-steps portal-card">
        <div v-for="(step, i) in steps" :key="step.title" class="step-item">
          <div class="step-num"><i :class="step.icon" /></div>
          <div class="step-body">
            <h4>{{ step.title }}</h4>
            <p>{{ step.desc }}</p>
          </div>
          <span v-if="i < steps.length - 1" class="step-connector" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { goPortalLogin } from '@/utils/portalLogin'
import { paperLabels } from './portal-paper-labels'

export default {
  name: 'PortalPaper',
  data() {
    return {
      labels: paperLabels,
      steps: [
        { icon: 'el-icon-folder-opened', title: '\u7ae0\u8282 / \u77e5\u8bc6\u70b9\u9009\u9898', desc: '\u6309\u6559\u6750\u7ae0\u8282\u6216\u77e5\u8bc6\u70b9\u7b5b\u9009\u8bd5\u9898\uff0c\u52a0\u5165\u8bd5\u9898\u7bee' },
        { icon: 'el-icon-shopping-cart-2', title: '\u8c03\u6574\u8bd5\u9898\u7bee', desc: '\u53f3\u4fa7\u6d6e\u52a8\u680f\u6253\u5f00\u8bd5\u9898\u7bee\uff0c\u53ef\u6392\u5e8f\u3001\u5220\u9898\u3001\u8bbe\u5206\u503c' },
        { icon: 'el-icon-printer', title: '\u9884\u89c8\u5bfc\u51fa', desc: '\u751f\u6210\u8bd5\u5377\u9884\u89c8\uff0c\u652f\u6301 Word \u5bfc\u51fa\u4e0e\u6253\u5370' }
      ]
    }
  },
  computed: {
    ...mapGetters(['questionBasketCount', 'token']),
    basketCount() {
      return this.questionBasketCount
    },
    ringStyle() {
      const max = 20
      const pct = Math.min(this.basketCount / max, 1)
      const circumference = 2 * Math.PI * 52
      const offset = circumference * (1 - pct)
      return {
        strokeDasharray: circumference,
        strokeDashoffset: offset
      }
    }
  },
  methods: {
    goPreview() {
      if (!this.token) {
        goPortalLogin(this.$router, '/portal/paper')
        return
      }
      this.$router.push('/portal/paper/preview')
    }
  }
}
</script>

<style scoped lang="scss">
$primary: #2563EB;
$primary-light: #3B82F6;
$accent: #3B82F6;

.paper-wrap { padding: 20px 20px 48px; }

.breadcrumb {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 14px;
  a { color: $primary; text-decoration: none; font-weight: 500; }
  .sep { margin: 0 8px; color: #cbd5e1; }
}

.paper-hero {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 32px;
  align-items: center;
  padding: 32px 36px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #fff 0%, #EFF6FF 55%, #fff 100%);
  border: 1px solid rgba(37,99,235,0.2);
  overflow: hidden;
}

.smart-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, $primary-light, $primary);
  margin-bottom: 14px;
  box-shadow: 0 4px 14px rgba(37,99,235,0.3);
}

.paper-hero-left {
  h1 { margin: 0 0 10px; font-size: 28px; font-weight: 800; color: #1E293B; }
  p { margin: 0 0 22px; color: #64748b; font-size: 15px; line-height: 1.6; max-width: 480px; }
}

.paper-actions { display: flex; gap: 12px; flex-wrap: wrap; }

.paper-hero-right { text-align: center; }

.basket-ring {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 0 auto 10px;
  &.empty .ring-fill { stroke: #e2e8f0; }
}

.ring-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: #EEF2F6;
  stroke-width: 8;
}

.ring-fill {
  fill: none;
  stroke: url(#paperGrad);
  stroke: $primary;
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.5s ease;
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  b { font-size: 36px; font-weight: 800; color: $primary; line-height: 1; }
  span { font-size: 13px; color: #94a3b8; margin-top: 4px; }
}

.basket-label {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  b { color: $accent; font-size: 18px; }
}

.empty-hint {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #64748b;
  background: #EFF6FF;
  border: 1px solid #BFDBFE;
  i { font-size: 20px; color: $accent; }
  .el-button { margin-left: auto; }
}

.smart-engine {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 22px 24px;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #FFFFFF, #fff);
  border: 1px solid rgba(37,99,235,0.2);
}

.engine-icon {
  width: 52px; height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, $primary-light, $primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  box-shadow: 0 6px 18px rgba(37,99,235,0.3);
}

.engine-text {
  h3 { margin: 0 0 6px; font-size: 16px; font-weight: 700; color: #1E293B; }
  p { margin: 0; font-size: 13px; color: #64748b; line-height: 1.5; }
}

.steps-head { margin: 8px 0 12px; }

.paper-steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  padding: 8px 16px 20px;
}

.step-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 20px 16px;
}

.step-num {
  width: 48px; height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #DBEAFE, #E2E8F0);
  color: #1D4ED8;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
  i { font-size: 22px; }
}

.step-body {
  h4 { margin: 0 0 6px; font-size: 15px; color: #1E293B; font-weight: 600; }
  p { margin: 0; font-size: 12px; color: #64748b; line-height: 1.55; }
}

.step-connector {
  position: absolute;
  top: 44px;
  right: -8%;
  width: 16%;
  height: 2px;
  background: linear-gradient(90deg, #E2E8F0, $primary-light, #E2E8F0);
}

@media (max-width: 900px) {
  .paper-hero { grid-template-columns: 1fr; text-align: center; }
  .paper-hero-left p { max-width: none; }
  .paper-actions { justify-content: center; }
  .paper-steps { grid-template-columns: 1fr; }
  .step-connector { display: none; }
}
</style>
`

fs.writeFileSync(file, content, 'utf8')
console.log('wrote Paper.vue (smart UI)')
