<template>
  <div class="ocr-editable-formula" :class="{ empty: !hasContent }">
    <formula-quick-insert-bar
      v-if="showQuickBar"
      :default-category="formulaCategory"
      :title="quickBarTitle"
      @insert="onQuickInsert"
    />
    <div v-if="!hasContent" class="empty-hint">{{ placeholder }}</div>
    <ocr-formula-mathfield
      v-for="(line, idx) in lineItems"
      :key="idx"
      ref="mathFields"
      :value="line"
      @input="onLineInput(idx, $event)"
      @focus="onFieldFocus"
    />
    <div class="formula-edit-tip">{{ tipText }}</div>
  </div>
</template>

<script>
import OcrFormulaMathfield from './OcrFormulaMathfield'
import FormulaQuickInsertBar from '../components/FormulaQuickInsertBar'

export default {
  name: 'OcrEditableFormula',
  components: { OcrFormulaMathfield, FormulaQuickInsertBar },
  props: {
    value: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: '\u70b9\u51fb\u6b64\u5904\u7f16\u8f91\u516c\u5f0f'
    },
    tipText: {
      type: String,
      default: '\u76f4\u63a5\u5728\u516c\u5f0f\u4e0a\u70b9\u51fb\u4fee\u6539\uff1b\u53f3\u952e\u300c\u63d2\u5165\u300d\u83dc\u5355\u53ef\u9009\u5404\u5b66\u79d1\u516c\u5f0f'
    },
    minRows: {
      type: Number,
      default: 1
    },
    formulaCategory: {
      type: String,
      default: 'math'
    },
    showQuickBar: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      activeField: null,
      quickBarTitle: '\u5feb\u6377\u63d2\u5165\u516c\u5f0f\uff08\u70b9\u51fb\u540e\u63d2\u5165\u5230\u5f53\u524d\u5149\u6807\u4f4d\u7f6e\uff09'
    }
  },
  computed: {
    lineItems() {
      const raw = this.value || ''
      if (!raw.trim()) {
        return new Array(Math.max(1, this.minRows)).fill('')
      }
      const lines = raw.split('\n')
      while (lines.length < this.minRows) {
        lines.push('')
      }
      return lines
    },
    hasContent() {
      return !!(this.value && this.value.trim())
    }
  },
  methods: {
    onLineInput(idx, nextLine) {
      const lines = this.lineItems.slice()
      lines[idx] = nextLine || ''
      while (lines.length > 1 && !lines[lines.length - 1].trim()) {
        lines.pop()
      }
      const joined = lines.join('\n')
      this.$emit('input', joined)
      this.$emit('change', joined)
    },
    onFieldFocus(field) {
      this.activeField = field
      this.$emit('focus', field)
    },
    onQuickInsert(latex) {
      const target = this.activeField || this.getFirstField()
      if (!target) return
      if (!target.insertTemplate(latex)) {
        this.$message && this.$message.warning('\u8bf7\u5148\u70b9\u51fb\u516c\u5f0f\u7f16\u8f91\u533a\u57df')
      }
    },
    getFirstField() {
      const refs = this.$refs.mathFields
      if (Array.isArray(refs)) return refs[0] || null
      return refs || null
    }
  }
}
</script>

<style scoped lang="scss">
.ocr-editable-formula {
  border: 1px solid #e4e9f2;
  border-radius: 8px;
  background: #fff;
  padding: 8px 10px;

  &.empty {
    min-height: 48px;
  }
}

.empty-hint {
  color: #c0c4cc;
  font-size: 13px;
  padding: 8px 4px 4px;
}

.formula-edit-tip {
  margin-top: 6px;
  font-size: 11px;
  color: #909399;
  text-align: right;
}
</style>
