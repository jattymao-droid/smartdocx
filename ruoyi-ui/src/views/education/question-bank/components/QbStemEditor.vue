<template>
  <div class="qb-stem-editor">
    <div ref="editorWrap" class="qb-stem-quill">
      <div ref="toolbar" class="qb-stem-toolbar">
        <span class="ql-formats qb-toolbar-formula-group">
          <button
            type="button"
            class="qb-formula-tool-btn"
            title="插入公式"
            aria-label="插入公式"
            @mousedown.prevent
            @click="openFormulaDialog"
          >Σ</button>
        </span>
        <span class="ql-formats">
          <button class="ql-bold" />
          <button class="ql-italic" />
          <button class="ql-underline" />
        </span>
        <span class="ql-formats">
          <button class="ql-list" value="ordered" />
          <button class="ql-list" value="bullet" />
        </span>
        <span class="ql-formats">
          <button class="ql-clean" />
        </span>
      </div>
      <div ref="editor" class="qb-stem-editor-area" />
    </div>
    <el-dialog
      :title="dialogTitle"
      :visible.sync="formulaVisible"
      width="820px"
      append-to-body
      :close-on-click-modal="false"
      @opened="onFormulaDialogOpened"
      @closed="onFormulaDialogClosed"
    >
      <div v-loading="!mathReady && !mathFallback" class="formula-dialog-body">
        <div v-if="mathReady" class="formula-quick-bar">
          <formula-quick-insert-bar
            :default-category="formulaCategory"
            @insert="insertQuickFormula"
          />
        </div>
        <math-field
          v-if="mathReady"
          ref="formulaField"
          class="qb-formula-field"
          locale="zh-cn"
          virtual-keyboard-mode="onfocus"
          smart-fence
        />
        <el-input
          v-else-if="mathFallback"
          v-model="formulaDraft"
          type="textarea"
          :rows="3"
          placeholder="LaTeX 公式，如 \frac{a}{b}、\sqrt{x}"
        />
      </div>
      <div slot="footer">
        <el-button @click="formulaVisible = false">取 消</el-button>
        <el-button type="primary" @click="confirmFormula">插入</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Quill from 'quill'
import 'quill/dist/quill.core.css'
import 'quill/dist/quill.snow.css'
import 'mathlive/dist/mathlive-static.css'
import 'mathlive/dist/mathlive-fonts.css'
import { toMathLiveLatex } from '@/utils/questionFormula'
import { ensureKatexForQuill, textToQuillDelta, quillContentsToText } from '@/utils/qbQuillFormula'
import {
  configureMathLiveOnce,
  insertFormulaTemplate,
  applyMathFieldLocale,
  applySubjectFormulaMenu
} from '@/utils/mathliveLocale'
import FormulaQuickInsertBar from './FormulaQuickInsertBar'

export default {
  name: 'QbStemEditor',
  components: { FormulaQuickInsertBar },
  props: {
    value: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: '\u8bf7\u8f93\u5165\u9898\u5e72\uff0c\u53ef\u4f7f\u7528\u5de5\u5177\u680f\u63d2\u5165\u516c\u5f0f'
    },
    minHeight: {
      type: Number,
      default: 160
    },
    formulaCategory: {
      type: String,
      default: 'math'
    }
  },
  data() {
    return {
      quill: null,
      innerValue: '',
      formulaVisible: false,
      formulaDraft: '',
      mathReady: false,
      mathFallback: false,
      dialogTitle: '\u63d2\u5165\u516c\u5f0f'
    }
  },
  watch: {
    value(val) {
      const next = val == null ? '' : String(val)
      if (next === this.innerValue) return
      this.innerValue = next
      if (this.quill) {
        this.setQuillText(next)
      }
    }
  },
  mounted() {
    this.initQuill()
  },
  beforeDestroy() {
    this.quill = null
  },
  methods: {
    initQuill() {
      ensureKatexForQuill()
      this.quill = new Quill(this.$refs.editor, {
        theme: 'snow',
        placeholder: this.placeholder,
        modules: {
          toolbar: this.$refs.toolbar
        }
      })
      this.setQuillText(this.value || '')
      this.quill.on('text-change', () => {
        const text = quillContentsToText(this.quill.getContents())
        if (text === this.innerValue) return
        this.innerValue = text
        this.$emit('input', text)
        this.$emit('change', text)
      })
      this.applyEditorMinHeight()
    },
    applyEditorMinHeight() {
      const editor = this.$refs.editorWrap && this.$refs.editorWrap.querySelector('.ql-editor')
      if (editor) {
        editor.style.minHeight = `${this.minHeight}px`
      }
    },
    setQuillText(text) {
      if (!this.quill) return
      const current = quillContentsToText(this.quill.getContents())
      if (current === text) return
      this.quill.setContents(textToQuillDelta(text || ''), 'silent')
    },
    openFormulaDialog() {
      this.formulaDraft = ''
      this.formulaVisible = true
    },
    async onFormulaDialogOpened() {
      this.mathReady = false
      this.mathFallback = false
      try {
        await configureMathLiveOnce()
        if (window.customElements && window.customElements.whenDefined) {
          await window.customElements.whenDefined('math-field')
        }
        this.mathReady = true
        await this.$nextTick()
        const el = this.$refs.formulaField
        applyMathFieldLocale(el)
        applySubjectFormulaMenu(el)
        if (el && typeof el.setValue === 'function') {
          el.setValue('', { silenceNotifications: true })
          el.focus()
        }
      } catch (e) {
        console.error('MathLive init failed', e)
        this.mathFallback = true
      }
    },
    onFormulaDialogClosed() {
      this.mathReady = false
      this.mathFallback = false
      this.formulaDraft = ''
    },
    readFormulaLatex() {
      if (this.mathReady && this.$refs.formulaField) {
        return (this.$refs.formulaField.getValue('latex') || '').trim()
      }
      return (this.formulaDraft || '').trim()
    },
    insertQuickFormula(latex) {
      const el = this.$refs.formulaField
      if (!el) return
      insertFormulaTemplate(el, latex)
      if (typeof el.focus === 'function') el.focus()
    },
    confirmFormula() {
      const raw = this.readFormulaLatex()
      if (!raw) {
        this.$message.warning('\u8bf7\u8f93\u5165\u516c\u5f0f')
        return
      }
      const latex = toMathLiveLatex(raw)
      const range = this.quill.getSelection(true)
      const index = range ? range.index : Math.max(0, this.quill.getLength() - 1)
      ensureKatexForQuill()
      this.quill.insertEmbed(index, 'formula', latex, 'user')
      this.quill.setSelection(index + 1, 0, 'user')
      this.formulaVisible = false
    }
  }
}
</script>

<style scoped lang="scss">
.qb-stem-editor {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

.qb-stem-quill ::v-deep .ql-toolbar,
.qb-stem-toolbar {
  border: none;
  border-bottom: 1px solid #ebeef5;
  background: #fafbfc;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  padding: 4px 8px;
  line-height: 1;
}

.qb-stem-quill ::v-deep .ql-toolbar .ql-formats,
.qb-stem-toolbar .ql-formats {
  display: inline-flex;
  align-items: center;
  margin-right: 0;
  padding-right: 8px;
  margin-bottom: 0;
  border-right: 1px solid #e4e7ed;
}

.qb-stem-quill ::v-deep .ql-toolbar .ql-formats:last-child,
.qb-stem-toolbar .ql-formats:last-child {
  border-right: none;
  padding-right: 0;
}

.qb-stem-quill ::v-deep .ql-toolbar .qb-toolbar-formula-group,
.qb-stem-toolbar .qb-toolbar-formula-group {
  padding-right: 10px;
  margin-right: 2px;
}

.qb-stem-quill ::v-deep .ql-toolbar button,
.qb-stem-toolbar button {
  width: 28px;
  height: 28px;
  padding: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  float: none;
}

.qb-formula-tool-btn {
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  background: transparent;
  font-size: 16px;
  font-weight: 700;
  font-family: 'Times New Roman', serif;
  color: #409eff;
  cursor: pointer;
  line-height: 28px;
  text-align: center;
}

.qb-formula-tool-btn:hover,
.qb-formula-tool-btn:focus {
  color: #409eff;
  background: #ecf5ff;
  border-radius: 4px;
  outline: none;
}

.qb-stem-editor-area {
  min-height: 120px;
}

.qb-stem-quill ::v-deep .ql-container {
  border: none;
  font-size: 14px;
}

.qb-stem-quill ::v-deep .ql-editor {
  line-height: 1.75;
  padding: 12px 14px;
}

.qb-stem-quill ::v-deep .ql-editor .ql-formula {
  display: inline-block;
  vertical-align: middle;
  margin: 0 1px;
  padding: 0 2px;
  cursor: default;
  user-select: none;
}

.qb-stem-quill ::v-deep .ql-editor .ql-formula .katex {
  font-size: 1.05em;
}

.formula-dialog-body {
  min-height: 72px;
}

.formula-quick-bar {
  margin-bottom: 10px;
}

.qb-formula-field {
  display: block;
  width: 100%;
  min-height: 56px;
  font-size: 1.15em;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 8px 10px;
}
</style>
