<template>
  <div class="qb-basket-float-root">
    <button type="button" class="qb-basket-float" data-qb-basket-target :aria-label="basketLabel" @click="open = true">
      <el-badge :value="count" :hidden="!count" class="qb-basket-float-badge">
        <span class="qb-basket-float-body">
          <i class="el-icon-shopping-cart-2 qb-basket-float-icon" />
          <span class="qb-basket-float-label">{{ basketLabel }}</span>
        </span>
      </el-badge>
    </button>
    <question-basket-drawer v-model="open" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import QuestionBasketDrawer from './QuestionBasketDrawer'

export default {
  name: 'QuestionBasketFloat',
  components: { QuestionBasketDrawer },
  data() {
    return {
      open: false,
      basketLabel: '\u8bd5\u9898\u7bee'
    }
  },
  computed: {
    ...mapGetters(['questionBasketCount']),
    count() {
      return this.questionBasketCount
    }
  }
}
</script>

<style scoped lang="scss">
.qb-basket-float-root {
  pointer-events: none;
}

.qb-basket-float {
  position: fixed;
  right: 0;
  top: 50%;
  z-index: 1900;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 8px 10px 10px;
  border: 1px solid #f0c78a;
  border-right: none;
  border-radius: 10px 0 0 10px;
  background: #fff9e6;
  color: #e6a23c;
  cursor: pointer;
  box-shadow: -2px 2px 12px rgba(230, 162, 60, 0.18);
  transition: background 0.2s, box-shadow 0.2s, transform 0.2s;
  pointer-events: auto;

  &:hover {
    background: #fff3d6;
    box-shadow: -3px 4px 16px rgba(230, 162, 60, 0.28);
    transform: translateY(-50%) translateX(-2px);
  }

  &:focus {
    outline: none;
  }
}

.qb-basket-float-badge {
  line-height: 1;
}

.qb-basket-float-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 28px;
}

.qb-basket-float-icon {
  font-size: 20px;
  line-height: 1;
}

.qb-basket-float-label {
  font-size: 12px;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: 0.5px;
  writing-mode: vertical-rl;
  text-orientation: mixed;
}

::v-deep .qb-basket-float-badge .el-badge__content {
  border: 2px solid #fff9e6;
  font-size: 11px;
  height: 18px;
  line-height: 14px;
  padding: 0 5px;
  right: 2px;
  top: 2px;
}
</style>

<style lang="scss">
.qb-basket-float.qb-basket-float--pulse {
  animation: qb-basket-float-pulse 0.42s ease;
}

@keyframes qb-basket-float-pulse {
  0% { transform: translateY(-50%) scale(1); }
  40% { transform: translateY(-50%) scale(1.08); box-shadow: -4px 4px 18px rgba(230, 162, 60, 0.35); }
  100% { transform: translateY(-50%) scale(1); }
}
</style>
