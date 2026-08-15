<template>
  <div class="subject-selector">
    <el-popover v-model="open" placement="bottom-start" width="860" trigger="click" popper-class="subject-popover">
      <div class="subject-panel">
        <div class="panel-title">选择学科</div>
        <div class="subject-grid">
          <span
            v-for="item in options"
            :key="item.subjectId"
            class="subject-item"
            :class="{ active: item.subjectId === value }"
            @click="pick(item)"
          >{{ item.subjectName }}</span>
        </div>
      </div>
      <el-button slot="reference" type="primary" size="small" class="subject-trigger">
        <span class="trigger-label">{{ currentLabel }}</span>
        <i class="el-icon-arrow-down el-icon--right" />
      </el-button>
    </el-popover>
  </div>
</template>

<script>
export default {
  name: 'SubjectSelectorBar',
  props: {
    value: { type: [Number, String], default: undefined },
    options: { type: Array, default: () => [] }
  },
  data() {
    return { open: false }
  },
  computed: {
    currentLabel() {
      const item = this.options.find(i => i.subjectId === this.value)
      return item ? item.subjectName : '选择学科'
    }
  },
  methods: {
    pick(item) {
      this.open = false
      if (item.subjectId === this.value) return
      this.$emit('input', item.subjectId)
      this.$emit('change', item.subjectId)
    }
  }
}
</script>

<style scoped lang="scss">
.subject-selector {
  display: inline-flex;
}
.subject-trigger {
  min-width: 88px;
  font-size: 13px;
  font-weight: 600;
  border-radius: 4px;
  padding: 7px 12px;
}
.trigger-label {
  margin-right: 4px;
}
.subject-panel {
  padding: 4px 0;
}
.panel-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}
.subject-grid {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}
.subject-item {
  flex-shrink: 0;
  min-width: auto;
  padding: 8px 14px;
  text-align: center;
  border-radius: 4px;
  background: #f5f7fa;
  color: #606266;
  cursor: pointer;
  font-size: 13px;
  white-space: nowrap;
  transition: all 0.15s;
  &:hover {
    color: #409eff;
    background: #ecf5ff;
  }
  &.active {
    color: #fff;
    background: #409eff;
  }
}
</style>
