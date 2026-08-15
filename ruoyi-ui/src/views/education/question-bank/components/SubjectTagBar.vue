<template>
  <div class="subject-tag-bar">
    <span class="row-label">{{ labels.subject }}</span>
    <div class="row-tags">
      <span
        v-for="item in options"
        :key="item.subjectId"
        class="tag-item"
        :class="{ active: item.subjectId === value }"
        @click="pick(item.subjectId)"
      >{{ item.subjectName }}</span>
      <span v-if="!options.length" class="tag-empty">{{ labels.empty }}</span>
    </div>
  </div>
</template>

<script>
const LABELS = {
  subject: '\u79d1\u76ee',
  empty: '\u6682\u65e0\u5b66\u79d1\u6570\u636e'
}

export default {
  name: 'SubjectTagBar',
  props: {
    value: { type: [Number, String], default: undefined },
    options: { type: Array, default: () => [] }
  },
  data() {
    return { labels: LABELS }
  },
  methods: {
    pick(id) {
      if (id === this.value) return
      this.$emit('input', id)
      this.$emit('change', id)
    }
  }
}
</script>

<style scoped lang="scss">
.subject-tag-bar {
  display: flex;
  align-items: flex-start;
  padding: 8px 0;
  font-size: 13px;
  &:not(:last-child) {
    border-bottom: 1px dashed #eef2f6;
  }
}
.row-label {
  flex-shrink: 0;
  width: 36px;
  line-height: 28px;
  color: #909399;
  font-weight: 600;
}
.row-tags {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
}
.tag-item {
  flex-shrink: 0;
  display: inline-block;
  padding: 4px 12px;
  line-height: 20px;
  border-radius: 4px;
  color: #606266;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.15s;
  &:hover {
    color: #409eff;
    background: #f0f7ff;
  }
  &.active {
    color: #fff;
    background: #409eff;
    font-weight: 600;
  }
}
.tag-empty {
  color: #c0c4cc;
  line-height: 28px;
  font-size: 12px;
  white-space: nowrap;
}
</style>
