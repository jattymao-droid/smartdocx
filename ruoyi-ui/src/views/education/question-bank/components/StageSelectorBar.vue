<template>
  <div class="stage-selector">
    <span class="row-label">学段</span>
    <div class="stage-pills">
      <span
        v-for="item in options"
        :key="item.value"
        class="pill-item"
        :class="{ active: item.value === value }"
        @click="pick(item.value)"
      >{{ item.label }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'StageSelectorBar',
  props: {
    value: { type: String, default: '高中' }
  },
  data() {
    return {
      options: [
        { label: '初中', value: '初中' },
        { label: '高中', value: '高中' }
      ]
    }
  },
  methods: {
    pick(val) {
      if (val === this.value) return
      this.$emit('input', val)
      this.$emit('change', val)
    }
  }
}
</script>

<style scoped lang="scss">
.stage-selector {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.row-label {
  flex-shrink: 0;
  font-size: 13px;
  line-height: 28px;
  color: #909399;
  font-weight: 600;
}
.stage-pills {
  display: inline-flex;
  padding: 2px;
  border-radius: 6px;
  background: #f0f2f5;
}
.pill-item {
  padding: 0 16px;
  line-height: 26px;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
  &:hover {
    color: #409eff;
  }
  &.active {
    color: #409eff;
    background: #fff;
    font-weight: 600;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  }
}
</style>
