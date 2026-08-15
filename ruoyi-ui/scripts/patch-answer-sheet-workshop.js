/**
 * Upgrade answer sheet dialog to workshop with live preview.
 * Run: node scripts/patch-answer-sheet-workshop.js
 */
const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/views/education/question-bank/paper/preview.vue')
let text = fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n')

function mustReplace(label, from, to) {
  if (!text.includes(from)) {
    console.error('MISSING:', label)
    process.exit(1)
  }
  text = text.replace(from, to)
}

const workshopDialog = `
    <el-dialog
      :title="answerSheetWorkshopMode ? '\\u5236\\u4f5c\\u7b54\\u9898\\u5361' : '\\u7b54\\u9898\\u5361\\u8bbe\\u7f6e'"
      :visible.sync="answerSheetOpen"
      width="960px"
      append-to-body
      class="answer-sheet-workshop-dialog"
      @open="onAnswerSheetDialogOpen"
    >
      <div class="as-workshop">
        <div class="as-workshop-left">
          <div class="as-stats-bar">
            <span>\u5171 {{ answerSheetStats.total }} \u9898</span>
            <span>\u5ba2\u89c2 {{ answerSheetStats.objective }}</span>
            <span>\u586b\u7a7a {{ answerSheetStats.fill }}</span>
            <span>\u4e3b\u89c2 {{ answerSheetStats.subjective }}</span>
            <span>\u6ee1\u5206 {{ answerSheetStats.totalScore }}</span>
          </div>
          <el-form label-width="96px" size="small">
            <el-form-item label="\\u5361\\u7247\\u7c7b\\u578b">
              <el-radio-group v-model="answerSheetOptions.sheetMode">
                <el-radio label="student">\\u5b66\\u751f\\u586b\\u6d82\\u5361</el-radio>
                <el-radio label="teacher">\\u6559\\u5e08\\u53c2\\u8003\\u7248</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="\\u7248\\u9762\\u98ce\\u683c">
              <el-radio-group v-model="answerSheetOptions.style">
                <el-radio label="standard">\\u6807\\u51c6</el-radio>
                <el-radio label="compact">\\u7d27\\u51d1</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="\\u5305\\u542b\\u533a\\u57df">
              <el-checkbox v-model="answerSheetOptions.includeObjective">\\u5ba2\\u89c2\\u9898\\u586b\\u6d82</el-checkbox>
              <el-checkbox v-model="answerSheetOptions.includeFill">\\u586b\\u7a7a\\u9898</el-checkbox>
              <el-checkbox v-model="answerSheetOptions.includeSubjective">\\u4e3b\\u89c2\\u9898\\u4f5c\\u7b54</el-checkbox>
            </el-form-item>
            <el-form-item label="\\u663e\\u793a\\u5206\\u503c">
              <el-switch v-model="answerSheetOptions.showScore" />
            </el-form-item>
            <el-form-item label="\\u586b\\u6d82\\u5217\\u6570">
              <el-radio-group v-model="answerSheetOptions.choicePerRow">
                <el-radio :label="5">5 \u9898/\u884c</el-radio>
                <el-radio :label="10">10 \u9898/\u884c</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="\\u8003\\u53f7\\u586b\\u6d82">
              <el-switch v-model="answerSheetOptions.showExamNumber" />
            </el-form-item>
          </el-form>
        </div>
        <div v-loading="answerSheetPreviewLoading" class="as-workshop-right">
          <div class="as-preview-toolbar">
            <span>\\u5b9e\\u65f6\\u9884\\u89c8</span>
            <el-button type="text" size="mini" icon="el-icon-refresh" @click="refreshAnswerSheetPreview">\\u5237\\u65b0</el-button>
          </div>
          <div class="as-preview-scroll">
            <div v-if="answerSheetPreviewHtml" class="as-preview-paper" v-html="answerSheetPreviewHtml" />
            <el-empty v-else description="\\u9884\\u89c8\\u52a0\\u8f7d\\u4e2d..." />
          </div>
        </div>
      </div>
      <div slot="footer">
        <el-button size="small" @click="answerSheetOpen = false">\\u53d6\\u6d88</el-button>
        <el-button size="small" icon="el-icon-printer" :loading="sheetLoading" @click="printAnswerSheet">\\u6253\\u5370</el-button>
        <el-button type="primary" size="small" icon="el-icon-download" :loading="sheetLoading" @click="confirmAnswerSheetExport">\\u4e0b\\u8f7d PDF</el-button>
      </div>
    </el-dialog>
`

// Replace old dialog - match from el-dialog answer sheet to closing el-dialog before paper-question-detail
const dialogStart = text.indexOf('<el-dialog :title="\'\\u7b54\\u9898\\u5361\\u8bbe\\u7f6e\'"')
if (dialogStart < 0) {
  // try actual Chinese title in file
  const alt = text.indexOf("    <el-dialog :title=\"'\u7b54\u9898\u5361\u8bbe\u7f6e'\"")
  if (alt < 0) {
    console.error('MISSING: answer sheet dialog')
    process.exit(1)
  }
}
const detailIdx = text.indexOf('    <paper-question-detail-dialog', dialogStart > 0 ? dialogStart : 0)
if (detailIdx < 0) {
  console.error('MISSING: detail dialog marker')
  process.exit(1)
}
const oldDialogEnd = text.lastIndexOf('    </el-dialog>', detailIdx)
text = text.slice(0, dialogStart) + workshopDialog + '\n' + text.slice(oldDialogEnd + '    </el-dialog>'.length)

mustReplace(
  'import',
  "import { exportAnswerSheetClient } from '@/utils/paperAnswerSheetExport'",
  "import {\n  exportAnswerSheetClient,\n  printAnswerSheetClient,\n  buildAnswerSheetPreviewHtml,\n  getAnswerSheetStats\n} from '@/utils/paperAnswerSheetExport'"
)

mustReplace(
  'data options',
  `      answerSheetOpen: false,
      answerSheetOptions: {
        showScore: true,
        choicePerRow: 5,
        judgePerRow: 10,
        showExamNumber: true
      }`,
  `      answerSheetOpen: false,
      answerSheetWorkshopMode: false,
      answerSheetPreviewHtml: '',
      answerSheetPreviewLoading: false,
      answerSheetOptions: {
        showScore: true,
        choicePerRow: 5,
        judgePerRow: 10,
        showExamNumber: true,
        includeObjective: true,
        includeFill: true,
        includeSubjective: true,
        sheetMode: 'student',
        style: 'standard'
      }`
)

mustReplace(
  'computed stats',
  `    basketQuestionIds() {
      return this.questionBasketItems.map(i => i.questionId)
    }
  },
  watch:`,
  `    answerSheetStats() {
      return getAnswerSheetStats(this)
    },
    basketQuestionIds() {
      return this.questionBasketItems.map(i => i.questionId)
    }
  },
  watch:`
)

mustReplace(
  'watch options',
  `  watch: {
    questionBasketItems:`,
  `  watch: {
    answerSheetOptions: {
      deep: true,
      handler() {
        if (!this.answerSheetOpen) return
        clearTimeout(this._answerSheetPreviewTimer)
        this._answerSheetPreviewTimer = setTimeout(() => this.refreshAnswerSheetPreview(), 280)
      }
    },
    questionBasketItems:`
)

// sideActions - use regex for handlers
text = text.replace(
  /(\{ key: 'sheet',[^}]+)this\.openAnswerSheetDialog\(\)/,
  '$1this.downloadAnswerSheetQuick()'
)
text = text.replace(
  /(\{ key: 'makeSheet',[^}]+)this\.openAnswerSheetDialog\(\)/,
  '$1this.openAnswerSheetWorkshop()'
)

mustReplace(
  'applyDraftSnapshot',
  `      if (draft.pageLayout) this.pageLayout = draft.pageLayout
    },`,
  `      if (draft.pageLayout) this.pageLayout = draft.pageLayout
      if (draft.answerSheetOptions) {
        this.answerSheetOptions = { ...this.answerSheetOptions, ...draft.answerSheetOptions }
      }
    },`
)

mustReplace(
  'methods sheet',
  `    openAnswerSheetDialog() {
      if (!this.canPreview) {
        this.$modal.msgWarning('\\u8bf7\\u5148\\u52a0\\u5165\\u8bd5\\u9898\\u5e76\\u8bbe\\u7f6e\\u5206\\u503c')
        return
      }
      this.answerSheetOpen = true
    },
    confirmAnswerSheetExport() {
      if (!this.canPreview) return
      this.sheetLoading = true
      exportAnswerSheetClient(this, { ...this.answerSheetOptions }).then(() => {
        this.$modal.msgSuccess('\\u7b54\\u9898\\u5361\\u5bfc\\u51fa\\u6210\\u529f')
        this.answerSheetOpen = false
        this.saveDraft(false)
      }).catch(err => {
        console.error('answer sheet export failed', err)
        this.$modal.msgError('\\u7b54\\u9898\\u5361\\u5bfc\\u51fa\\u5931\\u8d25\\uFF0C\\u8bf7\\u7a0d\\u540e\\u91cd\\u8bd5')
      }).finally(() => { this.sheetLoading = false })
    },`,
  `    openAnswerSheetWorkshop() {
      if (!this.canPreview) {
        this.$modal.msgWarning('\\u8bf7\\u5148\\u52a0\\u5165\\u8bd5\\u9898\\u5e76\\u8bbe\\u7f6e\\u5206\\u503c')
        return
      }
      this.answerSheetWorkshopMode = true
      this.answerSheetOpen = true
    },
    downloadAnswerSheetQuick() {
      if (!this.canPreview) {
        this.$modal.msgWarning('\\u8bf7\\u5148\\u52a0\\u5165\\u8bd5\\u9898\\u5e76\\u8bbe\\u7f6e\\u5206\\u503c')
        return
      }
      this.sheetLoading = true
      exportAnswerSheetClient(this, { ...this.answerSheetOptions }).then(() => {
        this.$modal.msgSuccess('\\u7b54\\u9898\\u5361\\u5bfc\\u51fa\\u6210\\u529f')
        this.saveDraft(false)
      }).catch(err => {
        console.error('answer sheet export failed', err)
        this.$modal.msgError('\\u7b54\\u9898\\u5361\\u5bfc\\u51fa\\u5931\\u8d25\\uFF0C\\u8bf7\\u7a0d\\u540e\\u91cd\\u8bd5')
      }).finally(() => { this.sheetLoading = false })
    },
    onAnswerSheetDialogOpen() {
      this.refreshAnswerSheetPreview()
    },
    refreshAnswerSheetPreview() {
      if (!this.canPreview) return
      this.answerSheetPreviewLoading = true
      buildAnswerSheetPreviewHtml(this, { ...this.answerSheetOptions }).then(html => {
        this.answerSheetPreviewHtml = html
      }).catch(err => {
        console.error('answer sheet preview failed', err)
        this.answerSheetPreviewHtml = ''
      }).finally(() => { this.answerSheetPreviewLoading = false })
    },
    printAnswerSheet() {
      if (!this.canPreview) return
      this.sheetLoading = true
      printAnswerSheetClient(this, { ...this.answerSheetOptions }).then(() => {
        this.$modal.msgSuccess('\\u5df2\\u6253\\u5f00\\u6253\\u5370\\u7a97\\u53e3')
      }).catch(err => {
        console.error('answer sheet print failed', err)
        this.$modal.msgError('\\u6253\\u5370\\u5931\\u8d25\\uFF0C\\u8bf7\\u5141\\u8bb8\\u5f39\\u7a97\\u6216\\u7a0d\\u540e\\u91cd\\u8bd5')
      }).finally(() => { this.sheetLoading = false })
    },
    confirmAnswerSheetExport() {
      if (!this.canPreview) return
      this.sheetLoading = true
      exportAnswerSheetClient(this, { ...this.answerSheetOptions }).then(() => {
        this.$modal.msgSuccess('\\u7b54\\u9898\\u5361\\u5bfc\\u51fa\\u6210\\u529f')
        this.answerSheetOpen = false
        this.saveDraft(false)
      }).catch(err => {
        console.error('answer sheet export failed', err)
        this.$modal.msgError('\\u7b54\\u9898\\u5361\\u5bfc\\u51fa\\u5931\\u8d25\\uFF0C\\u8bf7\\u7a0d\\u540e\\u91cd\\u8bd5')
      }).finally(() => { this.sheetLoading = false })
    },`
)

mustReplace(
  'saveDraft options',
  `        paperId: this.savedPaperId,
        savedAt: ts,
        itemCount: this.questionBasketItems.length
      })`,
  `        paperId: this.savedPaperId,
        answerSheetOptions: { ...this.answerSheetOptions },
        savedAt: ts,
        itemCount: this.questionBasketItems.length
      })`
)

const styleBlock = `
.answer-sheet-workshop-dialog ::v-deep .el-dialog__body { padding-top: 8px; }
.as-workshop { display: flex; gap: 16px; min-height: 420px; }
.as-workshop-left {
  width: 300px; flex-shrink: 0; padding-right: 12px; border-right: 1px solid #ebeef5;
}
.as-workshop-right { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.as-stats-bar {
  display: flex; flex-wrap: wrap; gap: 8px 12px; margin-bottom: 14px; padding: 10px 12px;
  background: #f5f7fa; border-radius: 8px; font-size: 12px; color: #606266;
}
.as-stats-bar span { white-space: nowrap; }
.as-preview-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 8px; font-size: 13px; color: #606266; font-weight: 600;
}
.as-preview-scroll {
  flex: 1; overflow: auto; border: 1px solid #dcdfe6; border-radius: 6px;
  background: #eef2f8; padding: 12px; max-height: 520px;
}
.as-preview-paper {
  background: #fff; box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  transform-origin: top center; transform: scale(0.72); width: 794px; margin: 0 auto;
}
.as-workshop-left ::v-deep .el-checkbox { display: block; margin-left: 0; margin-bottom: 4px; }

.paper-compose-page--portal {
`

if (text.includes('.answer-sheet-workshop-dialog')) {
  console.log('workshop styles already present')
} else {
  mustReplace('styles', '.paper-compose-page--portal {', styleBlock)
}

fs.writeFileSync(file, text.replace(/\n/g, '\r\n'), 'utf8')
console.log('answer sheet workshop patched OK')
