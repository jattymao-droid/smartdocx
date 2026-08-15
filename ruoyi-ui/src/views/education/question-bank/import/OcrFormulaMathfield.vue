<template>
  <div class="ocr-formula-mathfield-wrap" @focusin="onFocus">
    <span v-if="prefix" class="option-prefix">{{ prefix }}</span>
    <math-field
      v-if="mathReady"
      ref="mathField"
      class="ocr-math-field"
      locale="zh-cn"
      virtual-keyboard-mode="onfocus"
      smart-fence
    />
    <el-input
      v-else-if="fallback"
      :value="value"
      type="textarea"
      :autosize="{ minRows: 1, maxRows: 6 }"
      placeholder="\u516c\u5f0f\u7f16\u8f91\u5668\u52a0\u8f7d\u5931\u8d25\uff0c\u53ef\u76f4\u63a5\u7f16\u8f91\u6587\u672c"
      @input="$emit('input', $event)"
    />
    <span v-else class="math-loading">{{ loadingText }}</span>
  </div>
</template>

<script>
import 'mathlive/dist/mathlive-static.css'
import 'mathlive/dist/mathlive-fonts.css'
import { mergeOcrLine, parseOcrLine, toMathLiveLatex } from '@/utils/questionFormula'
import {
  configureMathLiveOnce,
  applyMathFieldLocale,
  applySubjectFormulaMenu,
  insertFormulaTemplate
} from '@/utils/mathliveLocale'

export default {
  name: 'OcrFormulaMathfield',
  props: {
    value: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      prefix: '',
      syncing: false,
      mathReady: false,
      fallback: false,
      loadingText: '\u516c\u5f0f\u7f16\u8f91\u5668\u52a0\u8f7d\u4e2d\u2026'
    }
  },
  watch: {
    value(val) {
      if (!this.syncing && this.mathReady) {
        this.applyValue(val)
      }
    }
  },
  mounted() {
    this.initMathLive()
  },
  beforeDestroy() {
    if (this.mathFieldEl && this.onInputHandler) {
      this.mathFieldEl.removeEventListener('input', this.onInputHandler)
    }
  },
  methods: {
    async initMathLive() {
      try {
        await configureMathLiveOnce()
        if (window.customElements && window.customElements.whenDefined) {
          await window.customElements.whenDefined('math-field')
        }
        this.mathReady = true
        await this.$nextTick()
        applyMathFieldLocale(this.$refs.mathField)
        this.bindMathField()
        this.applyValue(this.value)
      } catch (e) {
        console.error('MathLive init failed', e)
        this.fallback = true
      }
    },
    bindMathField() {
      this.mathFieldEl = this.$refs.mathField
      if (!this.mathFieldEl || typeof this.mathFieldEl.getValue !== 'function') return
      applySubjectFormulaMenu(this.mathFieldEl)
      this.onInputHandler = () => {
        const math = this.mathFieldEl.getValue('latex') || ''
        const full = mergeOcrLine(this.prefix, math)
        this.syncing = true
        this.$emit('input', full)
        this.$emit('change', full)
        this.$nextTick(() => {
          this.syncing = false
        })
      }
      this.mathFieldEl.addEventListener('input', this.onInputHandler)
    },
    applyValue(full) {
      const parsed = parseOcrLine(full || '')
      this.prefix = parsed.prefix
      const latex = toMathLiveLatex(parsed.body)
      if (!this.mathFieldEl || typeof this.mathFieldEl.setValue !== 'function') return
      const current = this.mathFieldEl.getValue('latex') || ''
      if (current !== latex) {
        this.mathFieldEl.setValue(latex, { silenceNotifications: true })
      }
    },
    onFocus() {
      this.$emit('focus', this)
    },
    insertTemplate(latex) {
      if (!this.mathFieldEl) return false
      const ok = insertFormulaTemplate(this.mathFieldEl, latex)
      if (ok && typeof this.mathFieldEl.focus === 'function') {
        this.mathFieldEl.focus()
      }
      return ok
    },
    focusField() {
      if (this.mathFieldEl && typeof this.mathFieldEl.focus === 'function') {
        this.mathFieldEl.focus()
      }
    }
  }
}
</script>

<style scoped lang="scss">
.ocr-formula-mathfield-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-height: 40px;
  padding: 4px 2px;
}

.option-prefix {
  flex-shrink: 0;
  font-weight: 600;
  color: #303133;
  line-height: 1.75;
}

.math-loading {
  color: #909399;
  font-size: 13px;
}

.ocr-math-field {
  flex: 1;
  min-width: 0;
  font-size: 1.05em;
  border: 1px solid transparent;
  border-radius: 6px;
  padding: 4px 6px;
  transition: border-color 0.2s, background 0.2s;

  &:hover {
    background: #f8fbff;
    border-color: #e4e9f2;
  }

  &:focus-within {
    background: #fff;
    border-color: #409eff;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.12);
  }
}
</style>
