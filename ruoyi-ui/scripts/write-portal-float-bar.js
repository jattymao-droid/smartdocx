/**
 * Rewrite PortalFloatingBar with smart pulse when basket has items.
 */
const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/layout-portal/components/PortalFloatingBar.vue')

const content = `<template>
  <div class="portal-float-bar">
    <div class="float-item basket" :class="{ active: count > 0 }" @click="onBasketClick">
      <el-badge :value="count" :hidden="!count">
        <i class="el-icon-shopping-cart-2" />
      </el-badge>
      <span>\u8bd5\u9898\u7bee</span>
    </div>
    <div class="float-item" @click="scrollTop">
      <i class="el-icon-top" />
      <span>\u9876\u90e8</span>
    </div>
    <question-basket-drawer v-model="openBasket" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import QuestionBasketDrawer from '@/views/education/question-bank/components/QuestionBasketDrawer'
import { goPortalLogin } from '@/utils/portalLogin'

export default {
  name: 'PortalFloatingBar',
  components: { QuestionBasketDrawer },
  data() {
    return { openBasket: false }
  },
  computed: {
    ...mapGetters(['questionBasketCount', 'token']),
    count() {
      return this.questionBasketCount
    }
  },
  methods: {
    onBasketClick() {
      if (!this.token) {
        goPortalLogin(this.$router, this.$route.fullPath)
        return
      }
      this.openBasket = true
    },
    scrollTop() {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }
}
</script>

<style scoped lang="scss">
$primary: #2563EB;
$accent: #3B82F6;

.portal-float-bar {
  position: fixed;
  right: 20px;
  bottom: 80px;
  z-index: 1900;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.float-item {
  width: 58px;
  height: 58px;
  padding: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 10px;
  color: #64748b;
  cursor: pointer;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.1);
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
  i { font-size: 22px; display: block; margin-bottom: 2px; }
  &:hover {
    color: $primary;
    border-color: #DBEAFE;
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(37, 99, 235, 0.18);
  }
}

.float-item.basket {
  color: $accent;
  background: linear-gradient(135deg, #EFF6FF, #fff);
  border-color: #BFDBFE;
  i { color: $accent; }
  &.active {
    animation: basket-glow 2.5s ease-in-out infinite;
    border-color: $accent;
  }
}

@keyframes basket-glow {
  0%, 100% { box-shadow: 0 4px 16px rgba(59, 130, 246, 0.2); }
  50% { box-shadow: 0 6px 24px rgba(59, 130, 246, 0.45); }
}
</style>
`

fs.writeFileSync(file, content, 'utf8')
console.log('wrote PortalFloatingBar.vue')
