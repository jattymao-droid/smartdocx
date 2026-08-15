<template>
  <div class="portal-float-bar">
    <div class="float-item basket" :class="{ active: count > 0 }" @click="onBasketClick">
      <el-badge :value="count" :hidden="!count">
        <i class="el-icon-shopping-cart-2" />
      </el-badge>
      <span>试题篮</span>
    </div>
    <div v-if="count > 0" class="float-item preview" @click="goPreview">
      <i class="el-icon-document-copy" />
      <span>组卷</span>
    </div>
    <div class="float-item" @click="scrollTop">
      <i class="el-icon-top" />
      <span>顶部</span>
    </div>
    <question-basket-drawer v-model="openBasket" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { goPortalLogin } from '@/utils/portalLogin'

export default {
  name: 'PortalFloatingBar',
  components: {
    QuestionBasketDrawer: () => import('@/views/education/question-bank/components/QuestionBasketDrawer')
  },
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
    },
    goPreview() {
      if (!this.token) {
        goPortalLogin(this.$router, '/paper/preview')
        return
      }
      this.$router.push('/paper/preview')
    }
  }
}
</script>

<style scoped lang="scss">
$primary: #0F766E;
$primary-light: #14B8A6;
$cyan: #0E7490;
$accent: #D97706;

.portal-float-bar {
  position: fixed;
  right: 24px;
  bottom: 88px;
  z-index: 1900;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.float-item {
  width: 56px;
  min-height: 56px;
  padding: 8px 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #64748b;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(8px);
  border-radius: 14px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.08);
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s, color 0.2s, background 0.2s;

  i { font-size: 20px; display: block; margin-bottom: 3px; }

  &:hover {
    color: $primary;
    border-color: rgba(15, 118, 110, 0.3);
    background: linear-gradient(160deg, #ECFEFF, #fff);
    transform: translateY(-3px);
    box-shadow: 0 8px 28px rgba(15, 118, 110, 0.15);
  }
}

.float-item.basket {
  color: $cyan;
  background: linear-gradient(160deg, #ECFEFF 0%, #F0FDFA 55%, #fff 100%);
  border-color: rgba(14, 116, 144, 0.25);

  i { color: $cyan; }

  &.active {
    color: #fff;
    background: linear-gradient(145deg, $primary-light 0%, $primary 55%, $cyan 100%);
    border-color: transparent;
    animation: basket-glow 2.5s ease-in-out infinite, portal-float 3s ease-in-out infinite;

    i { color: #fff; }

    ::v-deep .el-badge__content {
      background: #fff;
      color: $primary;
      border: none;
      font-weight: 700;
    }
  }
}

.float-item.preview {
  color: $primary;
  background: linear-gradient(160deg, #F0FDFA, #fff);
  border-color: rgba(15, 118, 110, 0.25);

  i { color: $primary; }

  &:hover {
    color: #fff;
    background: linear-gradient(145deg, $primary, $cyan);
    border-color: transparent;
    i { color: #fff; }
  }
}

@keyframes basket-glow {
  0%, 100% { box-shadow: 0 4px 20px rgba(15, 118, 110, 0.25); }
  50% { box-shadow: 0 8px 32px rgba(15, 118, 110, 0.45), 0 0 0 4px rgba(15, 118, 110, 0.08); }
}
</style>
