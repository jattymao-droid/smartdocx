const fs = require('fs')
const path = require('path')

const out = path.join(__dirname, '../src/views/education/exam-paper/ExamPaperMarkWorkspace.vue')

const content = `<template>
  <div class="exam-mark-workspace" :class="{ 'exam-mark-workspace--full': fullPage }">
    <el-form :inline="true" size="small" class="meta-form">
      <el-form-item label="\u6807\u9898" required>
        <el-input v-model="commitForm.paperTitle" placeholder="\u8bd5\u5377\u6807\u9898" style="width:200px" />
      </el-form-item>
      <el-form-item label="\u5206\u7c7b" required>
        <el-select v-model="commitForm.examCategory" style="width:130px">
          <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="\u5e74\u4efd">
        <el-input v-model="commitForm.examYear" placeholder="2024" style="width:90px" />
      </el-form-item>
      <el-form-item label="\u5730\u533a">
        <el-input v-model="commitForm.region" placeholder="\u7701\u5e02" style="width:110px" />
      </el-form-item>
    </el-form>

    <div class="mark-summary">
      \u5df2\u8bc6\u522b <b>{{ questionMarkCount }}</b> \u9053\u9898\u76ee \u00b7 \u70b9\u51fb\u8bd5\u5377\u9898\u76ee\u81ea\u52a8\u6846\u9009\u6574\u9898 \u00b7 \u5728\u9009\u6846\u8bbe\u7f6e\u5206\u7c7b\u540e\u6dfb\u52a0\u9898\u76ee
    </div>

    <el-row :gutter="12" class="mark-body">
      <el-col :span="15">
        <el-card shadow="never" class="preview-card">
          <el-tabs v-model="previewMode" class="preview-tabs">
            <el-tab-pane label="\u8bd5\u5377\u539f\u6837" name="visual">
              <docx-visual-canvas
                ref="visualCanvas"
                :key="visualKey"
                :local-file="localFile"
                :file-path="sourceFile"
                expanded
                enable-box-catalog
                :default-subject-id="subjectId"
                :subject-options="subjectOptions"
                subject-locked
                @capture="onVisualCapture"
                @rendered="scheduleVisualOverlays"
              />
            </el-tab-pane>
            <el-tab-pane label="\u6bb5\u843d\u6807\u8bb0" name="blocks" lazy>
              <docx-preview-canvas
                v-if="previewMode === 'blocks'"
                :preview-html="previewHtml"
                :marked-block-ids="includedBlockIds"
                :excluded-block-ids="excludedBlockIds"
                :interactive-marks="true"
                expanded
                @toggle-block="toggleItemByBlockId"
                @capture="onBlockCapture"
              />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
      <el-col :span="9">
        <el-card shadow="never" class="list-card">
          <div slot="header" class="list-card-head">
            <span>\u9898\u76ee\u5217\u8868</span>
            <el-button type="text" size="mini" @click="selectAllQuestions">\u5168\u9009</el-button>
            <el-button type="text" size="mini" @click="clearAllQuestions">\u5168\u4e0d\u9009</el-button>
          </div>
          <el-table
            ref="markTable"
            :data="questionItems"
            border
            size="mini"
            :max-height="tableMaxHeight"
            highlight-current-row
            @row-click="onRowClick"
          >
            <el-table-column label="#" width="42" align="center" prop="orderNum" />
            <el-table-column label="\u7c7b\u578b" width="92">
              <template slot-scope="scope">
                <el-select v-model="scope.row.questionType" size="mini" style="width:86px">
                  <el-option
                    v-for="opt in questionTypeOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="\u5206\u7c7b" width="88" align="center">
              <template slot-scope="scope">
                <el-popover placement="left" width="300" trigger="click" @show="prepareCatalogEdit(scope.row)">
                  <question-catalog-picker
                    v-if="catalogEditRow && catalogEditRow.blockId === scope.row.blockId"
                    v-model="catalogEditMeta"
                    :subject-id="subjectId"
                    :subject-options="subjectOptions"
                    subject-locked
                    @input="onCatalogEditInput(scope.row)"
                  />
                  <el-button slot="reference" type="text" size="mini">{{ catalogSummary(scope.row) }}</el-button>
                </el-popover>
              </template>
            </el-table-column>
            <el-table-column label="\u5206" width="58" align="center">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.scoreValue" :min="1" :max="30" size="mini" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="\u5bfc\u5165" width="46" align="center">
              <template slot-scope="scope">
                <el-checkbox v-model="scope.row.included" />
              </template>
            </el-table-column>
            <el-table-column label="\u7b54\u6848" width="52" align="center">
              <template slot-scope="scope">
                <span class="answer-cell">{{ formatAnswer(scope.row.correctAnswer, scope.row.questionType) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="\u89e3\u6790" width="56" align="center">
              <template slot-scope="scope">
                <el-tooltip v-if="scope.row.analysis" :content="scope.row.analysis" placement="left" :open-delay="300">
                  <span class="analysis-cell">\u6709</span>
                </el-tooltip>
                <span v-else class="muted-cell">-</span>
              </template>
            </el-table-column>
            <el-table-column label="\u9898\u5e72" min-width="100" show-overflow-tooltip prop="content" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import DocxVisualCanvas from '@/views/education/question-bank/import/DocxVisualCanvas'
import DocxPreviewCanvas from '@/views/education/question-bank/import/DocxPreviewCanvas'
import QuestionCatalogPicker from '@/views/education/question-bank/components/QuestionCatalogPicker'
import { applyParsedToMarkItem } from '@/utils/examPaperParse'
import { QUESTION_TYPE_OPTIONS } from '@/utils/questionTypes'
import { formatChoiceAnswer } from '@/utils/questionAnswer'

export default {
  name: 'ExamPaperMarkWorkspace',
  components: { DocxVisualCanvas, DocxPreviewCanvas, QuestionCatalogPicker },
  props: {
    localFile: { type: File, default: null },
    sourceFile: { type: String, default: '' },
    previewHtml: { type: String, default: '' },
    markedItems: { type: Array, default: () => [] },
    commitForm: { type: Object, default: () => ({}) },
    categoryOptions: { type: Array, default: () => [] },
    visualKey: { type: [String, Number], default: 0 },
    subjectId: { type: [Number, String], default: undefined },
    subjectOptions: { type: Array, default: () => [] },
    fullPage: { type: Boolean, default: false }
  },
  data() {
    return {
      previewMode: 'visual',
      overlayTimer: null,
      catalogEditRow: null,
      catalogEditMeta: null,
      manualBlockSeq: -1
    }
  },
  computed: {
    questionTypeOptions() {
      return QUESTION_TYPE_OPTIONS
    },
    questionItems() {
      return (this.markedItems || []).filter(i => i && i.question)
    },
    questionMarkCount() {
      return this.questionItems.filter(i => i.included).length
    },
    includedBlockIds() {
      return this.questionItems.filter(i => i.included).map(i => i.blockId).filter(id => id != null)
    },
    excludedBlockIds() {
      return this.questionItems.filter(i => !i.included).map(i => i.blockId).filter(id => id != null)
    },
    tableMaxHeight() {
      return this.fullPage ? 'calc(100vh - 280px)' : 520
    }
  },
  watch: {
    markedItems: {
      deep: true,
      handler() {
        this.scheduleVisualOverlays()
      }
    },
    previewMode(val) {
      if (val === 'visual') this.scheduleVisualOverlays()
    }
  },
  beforeDestroy() {
    if (this.overlayTimer) clearTimeout(this.overlayTimer)
  },
  methods: {
    parseKnowledgeTags(raw) {
      if (!raw) return []
      if (Array.isArray(raw)) return raw
      try {
        const arr = JSON.parse(raw)
        return Array.isArray(arr) ? arr : []
      } catch (e) {
        return []
      }
    },
    buildCatalogMeta(row) {
      return {
        subjectId: this.subjectId,
        versionId: row.versionId,
        textbookId: row.textbookId,
        chapterPath: row.chapterPath || [],
        chapterId: row.chapterId,
        chapterText: row.chapterText || '',
        knowledgeTags: this.parseKnowledgeTags(row.knowledgePoints)
      }
    },
    applyCatalogMeta(item, meta) {
      if (!item || !meta) return
      if (meta.textbookId != null) item.textbookId = meta.textbookId
      if (meta.chapterId != null) item.chapterId = meta.chapterId
      if (meta.chapterText != null) item.chapterText = meta.chapterText
      if (meta.chapterPath) item.chapterPath = meta.chapterPath
      if (meta.knowledgeTags && meta.knowledgeTags.length) {
        item.knowledgePoints = JSON.stringify(meta.knowledgeTags)
      }
    },
    catalogSummary(row) {
      if (row.chapterText) {
        return row.chapterText.length > 10 ? row.chapterText.slice(0, 10) + '\\u2026' : row.chapterText
      }
      const tags = this.parseKnowledgeTags(row.knowledgePoints)
      if (tags.length) return tags[0]
      return '\\u8bbe\\u7f6e'
    },
    prepareCatalogEdit(row) {
      this.catalogEditRow = row
      this.catalogEditMeta = this.buildCatalogMeta(row)
    },
    onCatalogEditInput(row) {
      this.applyCatalogMeta(row, this.catalogEditMeta)
      this.$emit('update:markedItems', [...this.markedItems])
    },
    scheduleVisualOverlays() {
      if (this.overlayTimer) clearTimeout(this.overlayTimer)
      this.overlayTimer = setTimeout(() => this.buildVisualOverlays(), 300)
    },
    buildVisualOverlays() {
      if (this.previewMode !== 'visual') return
      const visual = this.$refs.visualCanvas
      if (!visual || !visual.$refs || !visual.$refs.docxHost || !visual.$refs.canvasInner) {
        return
      }
      const host = visual.$refs.docxHost
      const canvas = visual.$refs.canvasInner
      const canvasRect = canvas.getBoundingClientRect()
      canvas.querySelectorAll('.exam-visual-pin').forEach(el => el.remove())
      const usedAnchors = []
      this.questionItems.forEach(item => {
        const anchor = this.findTextAnchor(host, item.content)
        if (!anchor) return
        const rect = anchor.getBoundingClientRect()
        let top = rect.top - canvasRect.top + canvas.scrollTop - 4
        let left = rect.left - canvasRect.left + canvas.scrollLeft - 4
        usedAnchors.forEach(prev => {
          if (Math.abs(prev.top - top) < 18 && Math.abs(prev.left - left) < 18) {
            left += 18
          }
        })
        usedAnchors.push({ top, left })
        const btn = document.createElement('button')
        btn.type = 'button'
        btn.className = 'exam-visual-pin visual-mark-pin' + (item.included !== false ? ' active' : ' dim')
        btn.style.top = top + 'px'
        btn.style.left = left + 'px'
        btn.textContent = String(item.orderNum || '')
        btn.setAttribute('data-block-id', String(item.blockId))
        btn.title = (item.content || '').slice(0, 80)
        btn.addEventListener('click', (event) => {
          event.stopPropagation()
          if (visual.smartFrameByContent) {
            visual.smartFrameByContent(item.content)
          }
        })
        canvas.appendChild(btn)
      })
    },
    findTextAnchor(root, content) {
      const lines = String(content || '').split(/\\r?\\n/).map(s => s.trim()).filter(Boolean)
      const needle = lines[0] || ''
      if (needle.length < 4) return null
      const shortNeedle = needle.length > 36 ? needle.slice(0, 36) : needle
      const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT)
      let node
      while ((node = walker.nextNode())) {
        const text = (node.textContent || '').replace(/\\s+/g, ' ')
        if (text.includes(shortNeedle)) {
          let el = node.parentElement
          while (el && el !== root && el.getBoundingClientRect().height < 8) {
            el = el.parentElement
          }
          return el || node.parentElement
        }
      }
      return null
    },
    toggleItemByBlockId(blockId) {
      const item = (this.markedItems || []).find(i => i && i.blockId === blockId)
      if (!item || !item.question) return
      item.included = !item.included
      this.$emit('update:markedItems', [...this.markedItems])
      this.scheduleVisualOverlays()
    },
    onBlockCapture(blockIds) {
      if (!Array.isArray(blockIds) || !blockIds.length) return
      blockIds.forEach(id => {
        const item = (this.markedItems || []).find(i => i && i.blockId === id)
        if (item && item.question) item.included = true
      })
      this.$emit('update:markedItems', [...this.markedItems])
    },
    onVisualCapture(payload) {
      const content = (payload && payload.content) || ''
      if (!content.trim() && !(payload && payload.imageUrls && payload.imageUrls.length)) return
      const nextOrder = this.questionItems.length + 1
      const item = applyParsedToMarkItem({
        blockId: this.manualBlockSeq--,
        orderNum: nextOrder,
        question: true,
        included: true,
        sectionName: '',
        questionType: 'short',
        scoreValue: 5,
        content: content.trim(),
        options: null,
        correctAnswer: null,
        analysis: null,
        images: payload.imageUrls && payload.imageUrls.length ? JSON.stringify(payload.imageUrls) : null,
        matchStatus: 'new'
      }, content.trim())
      this.applyCatalogMeta(item, payload && payload.meta)
      this.$emit('update:markedItems', [...this.markedItems, item])
      this.$message.success('\\u5df2\\u6dfb\\u52a0\\u624b\\u52a8\\u6807\\u8bb0\\u9898\\u76ee')
    },
    onRowClick(row) {
      this.previewMode = 'visual'
      this.$nextTick(() => {
        const visual = this.$refs.visualCanvas
        if (visual && visual.smartFrameByContent) {
          visual.smartFrameByContent(row.content)
        }
        const canvas = visual && visual.$refs.canvasInner
        if (!canvas) return
        const pin = canvas.querySelector('.exam-visual-pin[data-block-id="' + row.blockId + '"]')
        if (pin) {
          canvas.scrollTo({ top: Math.max(0, pin.offsetTop - 80), behavior: 'smooth' })
        }
      })
    },
    selectAllQuestions() {
      this.questionItems.forEach(i => { i.included = true })
      this.$emit('update:markedItems', [...this.markedItems])
    },
    clearAllQuestions() {
      this.questionItems.forEach(i => { i.included = false })
      this.$emit('update:markedItems', [...this.markedItems])
    },
    formatAnswer(raw, questionType) {
      if (!raw) return '-'
      const text = formatChoiceAnswer(questionType || 'single', raw)
      return text || '-'
    }
  }
}
</script>

<style scoped lang="scss">
.exam-mark-workspace {
  .meta-form { margin-bottom: 4px; }
  .mark-summary {
    margin: 0 0 10px;
    font-size: 13px;
    color: #475569;
    b { color: #2563eb; }
  }
  &.exam-mark-workspace--full {
    .mark-body { min-height: calc(100vh - 260px); }
    .preview-card ::v-deep .canvas-inner { min-height: calc(100vh - 320px); }
  }
}
.preview-card, .list-card { height: 100%; }
.preview-tabs ::v-deep .el-tabs__content { padding-top: 8px; }
.preview-tabs ::v-deep .canvas-inner .exam-visual-pin {
  position: absolute;
  z-index: 5;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 11px;
  border: 2px solid #fff;
  background: #409eff;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.45);
}
.preview-tabs ::v-deep .canvas-inner .exam-visual-pin.dim {
  background: #c0c4cc;
  box-shadow: none;
}
.preview-tabs ::v-deep .qb-block-image {
  max-width: 100%;
  max-height: 420px;
  object-fit: contain;
}
.list-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  span { flex: 1; font-weight: 600; }
}
.answer-cell { color: #2563eb; font-weight: 600; }
.analysis-cell { color: #16a34a; font-weight: 600; cursor: help; }
.muted-cell { color: #c0c4cc; }
</style>
`

fs.writeFileSync(out, content, 'utf8')
console.log('Wrote', out)
