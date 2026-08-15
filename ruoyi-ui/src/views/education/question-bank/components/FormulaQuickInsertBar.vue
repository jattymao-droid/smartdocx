<template>
  <div class="formula-quick-bar">
    <div class="formula-quick-title">{{ title }}</div>
    <el-tabs v-model="activeCategory" class="formula-tabs">
      <el-tab-pane
        v-for="group in formulaTemplateGroups"
        :key="group.key"
        :label="group.label"
        :name="group.key"
      >
        <div class="formula-quick-items">
          <el-tooltip
            v-for="item in group.templates"
            :key="item.key"
            :content="item.label"
            placement="top"
            :open-delay="300"
          >
            <el-button
              size="mini"
              plain
              type="primary"
              class="formula-quick-btn"
              @click="onInsert(item.latex)"
            >
              <span class="formula-icon" v-html="renderFormulaIcon(item.preview)" />
            </el-button>
          </el-tooltip>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { renderFormulaPreview } from '@/utils/questionFormula'
import { FORMULA_TEMPLATE_GROUPS } from '@/utils/mathliveLocale'

export default {
  name: 'FormulaQuickInsertBar',
  props: {
    defaultCategory: {
      type: String,
      default: 'math'
    },
    title: {
      type: String,
      default: '\u5feb\u6377\u63d2\u5165\u516c\u5f0f'
    }
  },
  data() {
    return {
      activeCategory: this.defaultCategory || 'math',
      formulaTemplateGroups: FORMULA_TEMPLATE_GROUPS
    }
  },
  watch: {
    defaultCategory(val) {
      if (val && this.formulaTemplateGroups.some(g => g.key === val)) {
        this.activeCategory = val
      }
    }
  },
  methods: {
    onInsert(latex) {
      this.$emit('insert', latex)
    },
    renderFormulaIcon(preview) {
      return renderFormulaPreview(preview || '')
    }
  }
}
</script>

<style scoped lang="scss">
.formula-quick-bar {
  margin-bottom: 8px;
  padding: 6px 8px 8px;
  background: #f5f9ff;
  border: 1px solid #e4ecf7;
  border-radius: 6px;
}

.formula-quick-title {
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
}

.formula-tabs ::v-deep .el-tabs__header {
  margin-bottom: 8px;
}

.formula-tabs ::v-deep .el-tabs__item {
  height: 30px;
  line-height: 30px;
  font-size: 12px;
  padding: 0 12px;
}

.formula-quick-items {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 120px;
  overflow-y: auto;
}

.formula-quick-btn {
  margin: 0 !important;
  min-width: 36px;
  height: 32px;
  padding: 2px 5px !important;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.formula-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  pointer-events: none;
}

.formula-icon ::v-deep .katex {
  font-size: 0.9em;
}

.formula-icon ::v-deep .katex .mord,
.formula-icon ::v-deep .katex .mop,
.formula-icon ::v-deep .katex .mbin,
.formula-icon ::v-deep .katex .mrel {
  color: #409eff;
}
</style>
