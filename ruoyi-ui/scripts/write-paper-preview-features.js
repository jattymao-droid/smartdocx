/**
 * Patch preview.vue: paper analysis, save, answer sheet, share.
 * Run: node scripts/write-paper-preview-features.js
 */
const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/views/education/question-bank/paper/preview.vue')
let text = fs.readFileSync(file, 'utf8')
text = text.replace(/\r\n/g, '\n')

const L = {
  analysisTitle: '\u8bd5\u5377\u5206\u6790',
  totalQuestions: '\u9898\u91cf',
  totalScore: '\u603b\u5206',
  avgDifficulty: '\u5e73\u5747\u96be\u5ea6',
  duration: '\u8003\u8bd5\u65f6\u957f',
  typeDistribution: '\u9898\u578b\u5206\u5e03',
  typeName: '\u9898\u578b',
  count: '\u9898\u6570',
  score: '\u5206\u503c',
  ratio: '\u5360\u6bd4',
  difficultyDistribution: '\u96be\u5ea6\u5206\u5e03',
  easy: '\u5bb9\u6613',
  medium: '\u4e2d\u7b49',
  hard: '\u8f83\u96be',
  close: '\u5173\u95ed',
  saveTitle: '\u4fdd\u5b58\u6210\u529f',
  saveHint: '\u8bd5\u5377\u5df2\u4fdd\u5b58\u5230\u672c\u5730\u8349\u7a3f\uff0c\u53ef\u5728\u7ec4\u5377\u9875\u9762\u901a\u8fc7\u8349\u7a3f\u7ee7\u7eed\u7f16\u8f91',
  paperTitle: '\u8bd5\u5377\u6807\u9898',
  savedAt: '\u4fdd\u5b58\u65f6\u95f4',
  confirm: '\u786e\u5b9a',
  shareTitle: '\u5206\u4eab\u8bd5\u5377',
  shareHint: '\u590d\u5236\u94fe\u63a5\u540e\u53ef\u5728\u540c\u4e00\u6d4f\u89c8\u5668\u6253\u5f00\uff08\u672c\u5730\u5206\u4eab\uff09',
  copyLink: '\u590d\u5236\u94fe\u63a5',
  analysis: '\u8bd5\u5377\u5206\u6790',
  savePaper: '\u4fdd\u5b58\u8bd5\u5377',
  downloadSheet: '\u4e0b\u8f7d\u7b54\u9898\u5361',
  sharePaper: '\u5206\u4eab\u8bd5\u5377'
}

const dialogsBlock = `
    <el-dialog :title="'${L.analysisTitle}'" :visible.sync="analysisOpen" width="520px" append-to-body class="paper-analysis-dialog">
      <div v-if="paperAnalysisStats" class="analysis-body">
        <div class="analysis-summary">
          <div class="analysis-stat"><label>${L.totalQuestions}</label><b>{{ paperAnalysisStats.count }}</b></div>
          <div class="analysis-stat"><label>${L.totalScore}</label><b>{{ paperAnalysisStats.totalScore }}</b></div>
          <div class="analysis-stat"><label>${L.avgDifficulty}</label><b>{{ paperAnalysisStats.difficultyLabel }}</b></div>
          <div class="analysis-stat"><label>${L.duration}</label><b>{{ paperAnalysisStats.duration }}</b></div>
        </div>
        <div class="analysis-block">
          <div class="analysis-block-title">${L.typeDistribution}</div>
          <el-table :data="paperAnalysisStats.typeRows" size="mini" border>
            <el-table-column prop="label" :label="'${L.typeName}'" />
            <el-table-column prop="count" :label="'${L.count}'" width="72" align="center" />
            <el-table-column prop="score" :label="'${L.score}'" width="72" align="center" />
            <el-table-column prop="ratio" :label="'${L.ratio}'" width="80" align="center">
              <template slot-scope="scope">{{ scope.row.ratio }}%</template>
            </el-table-column>
          </el-table>
        </div>
        <div class="analysis-block">
          <div class="analysis-block-title">${L.difficultyDistribution}</div>
          <div class="diff-bars">
            <div v-for="row in paperAnalysisStats.diffRows" :key="row.key" class="diff-row">
              <span class="diff-label">{{ row.label }}</span>
              <div class="diff-bar-wrap"><div class="diff-bar" :style="{ width: row.percent + '%' }" /></div>
              <span class="diff-count">{{ row.count }}</span>
            </div>
          </div>
        </div>
      </div>
      <div slot="footer"><el-button size="small" @click="analysisOpen = false">${L.close}</el-button></div>
    </el-dialog>

    <el-dialog :title="'${L.saveTitle}'" :visible.sync="saveDialogOpen" width="420px" append-to-body>
      <p class="save-dialog-hint">${L.saveHint}</p>
      <div v-if="saveDialogInfo" class="save-dialog-meta">
        <div><label>${L.paperTitle}</label>{{ saveDialogInfo.title }}</div>
        <div><label>${L.totalQuestions}</label>{{ saveDialogInfo.count }} \u9898</div>
        <div><label>${L.totalScore}</label>{{ saveDialogInfo.score }} \u5206</div>
        <div><label>${L.savedAt}</label>{{ saveDialogInfo.savedAtText }}</div>
      </div>
      <div slot="footer"><el-button type="primary" size="small" @click="saveDialogOpen = false">${L.confirm}</el-button></div>
    </el-dialog>

    <el-dialog :title="'${L.shareTitle}'" :visible.sync="shareDialogOpen" width="480px" append-to-body>
      <p class="share-dialog-hint">${L.shareHint}</p>
      <el-input v-model="shareLink" readonly size="small">
        <el-button slot="append" @click="copyShareLink">${L.copyLink}</el-button>
      </el-input>
      <div slot="footer"><el-button size="small" @click="shareDialogOpen = false">${L.close}</el-button></div>
    </el-dialog>
`

const stylesBlock = `
.analysis-body { font-size: 13px; }
.analysis-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}
.analysis-stat {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px;
  text-align: center;
}
.analysis-stat label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.analysis-stat b { font-size: 18px; color: #303133; }
.analysis-block { margin-bottom: 16px; }
.analysis-block-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}
.diff-bars { display: flex; flex-direction: column; gap: 10px; }
.diff-row { display: flex; align-items: center; gap: 10px; }
.diff-label { width: 40px; font-size: 12px; color: #606266; }
.diff-bar-wrap {
  flex: 1;
  height: 8px;
  background: #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}
.diff-bar { height: 100%; background: #409eff; border-radius: 4px; min-width: 2px; }
.diff-count { width: 28px; text-align: right; font-size: 12px; color: #606266; }
.save-dialog-hint, .share-dialog-hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}
.save-dialog-meta {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 12px 14px;
  font-size: 13px;
  color: #303133;
}
.save-dialog-meta div { margin: 6px 0; }
.save-dialog-meta label {
  display: inline-block;
  width: 72px;
  color: #909399;
}

.paper-compose-page--portal {
`

function mustReplace(label, from, to) {
  if (!text.includes(from)) {
    console.error('MISSING:', label)
    process.exit(1)
  }
  text = text.replace(from, to)
}

// 1. Dialogs before detail dialog
mustReplace(
  'dialogs insert',
  '    <paper-question-detail-dialog v-model="detailOpen" :question-id="detailQuestionId" />',
  dialogsBlock + '    <paper-question-detail-dialog v-model="detailOpen" :question-id="detailQuestionId" />'
)

// 2. Imports
mustReplace(
  'import prefs',
  "import { loadPaperDraft, savePaperDraft } from '@/utils/questionBasketPrefs'",
  "import { loadPaperDraft, savePaperDraft, savePaperShare, loadPaperShare, generateShareId } from '@/utils/questionBasketPrefs'"
)
mustReplace(
  'import export client',
  "import { exportPaperClient } from '@/utils/paperExportClient'",
  "import { exportPaperClient } from '@/utils/paperExportClient'\nimport { exportAnswerSheetClient } from '@/utils/paperAnswerSheetExport'"
)

// 3. data fields
mustReplace(
  'data fields',
  '      sideActions: []\n    }\n  },',
  `      sideActions: [],
      analysisOpen: false,
      saveDialogOpen: false,
      saveDialogInfo: null,
      shareDialogOpen: false,
      shareLink: '',
      sheetLoading: false
    }
  },`
)

// 4. computed paperAnalysisStats
mustReplace(
  'computed stats',
  '    basketQuestionIds() {\n      return this.questionBasketItems.map(i => i.questionId)\n    }',
  `    paperAnalysisStats() {
      const items = this.sortedItems
      if (!items.length) return null
      const totalScore = this.basketTotalScore
      const typeMap = {}
      const diffMap = { easy: 0, medium: 0, hard: 0 }
      items.forEach(item => {
        const type = item.questionType || 'short'
        const label = this.typeLabel(type)
        if (!typeMap[type]) typeMap[type] = { label, count: 0, score: 0 }
        typeMap[type].count += 1
        typeMap[type].score += Number(item.scoreValue) || 0
        const d = Number(item.difficulty) || 0
        if (d <= 0.35) diffMap.easy += 1
        else if (d <= 0.65) diffMap.medium += 1
        else diffMap.hard += 1
      })
      const typeRows = Object.values(typeMap)
        .map(row => ({
          ...row,
          ratio: totalScore > 0 ? Math.round((row.score / totalScore) * 100) : 0
        }))
        .sort((a, b) => b.count - a.count)
      const total = items.length
      const diffRows = [
        { key: 'easy', label: '${L.easy}', count: diffMap.easy, percent: total ? Math.round((diffMap.easy / total) * 100) : 0 },
        { key: 'medium', label: '${L.medium}', count: diffMap.medium, percent: total ? Math.round((diffMap.medium / total) * 100) : 0 },
        { key: 'hard', label: '${L.hard}', count: diffMap.hard, percent: total ? Math.round((diffMap.hard / total) * 100) : 0 }
      ]
      return {
        count: items.length,
        totalScore,
        difficultyLabel: this.difficultyLabel,
        duration: this.form.header.duration || '90 \\u5206\\u949f',
        typeRows,
        diffRows
      }
    },
    basketQuestionIds() {
      return this.questionBasketItems.map(i => i.questionId)
    }`
)

// 5. sideActions handlers (regex �� ASCII-safe)
const sideActionRegex = [
  [/(\{ key: 'analysis',[^}]+)this\.stubAction\([^)]+\)/, '$1this.openAnalysis()'],
  [/(\{ key: 'save',[^}]+)this\.saveDraft\(\)/, '$1this.confirmSaveDraft()'],
  [/(\{ key: 'sheet',[^}]+)this\.stubAction\([^)]+\)/, '$1this.downloadAnswerSheet()'],
  [/(\{ key: 'share',[^}]+)this\.stubAction\([^)]+\)/, '$1this.sharePaper()']
]
sideActionRegex.forEach(([re, rep], i) => {
  if (!re.test(text)) {
    console.error('MISSING side handler regex:', i)
    process.exit(1)
  }
  text = text.replace(re, rep)
})

// 6. Share load in created
mustReplace(
  'share load',
  `    if (this.$route.query.draft === '1') {
      const draft = loadPaperDraft()
      if (draft && draft.form) {
        this.form = { ...this.form, ...draft.form, header: { ...this.form.header, ...(draft.form.header || {}) } }
        if (draft.groupTab) this.groupTab = draft.groupTab
        if (draft.orderRadio) this.orderRadio = draft.orderRadio
        if (draft.paperTemplate) this.paperTemplate = draft.paperTemplate
        if (draft.answerAreas) this.answerAreas = { ...draft.answerAreas }
        if (draft.exportFormat) this.exportFormat = draft.exportFormat
        if (draft.pageLayout) this.pageLayout = draft.pageLayout
      }
    }`,
  `    if (this.$route.query.draft === '1') {
      const draft = loadPaperDraft()
      if (draft && draft.form) {
        this.applyDraftSnapshot(draft)
      }
    }
    const shareId = this.$route.query.share
    if (shareId) {
      const snap = loadPaperShare(String(shareId))
      if (snap && snap.items && snap.items.length) {
        this.$store.commit('questionBasket/SET_ITEMS', snap.items)
        this.applyDraftSnapshot(snap)
        this.$modal.msgSuccess('\\u5df2\\u52a0\\u8f7d\\u5206\\u4eab\\u7684\\u8bd5\\u5377')
      } else {
        this.$modal.msgWarning('\\u5206\\u4eab\\u94fe\\u63a5\\u65e0\\u6548\\u6216\\u5df2\\u8fc7\\u671f')
      }
    }`
)

// 7. Methods after stubAction
mustReplace(
  'methods',
  `    stubAction(name) {
      this.$modal.msgInfo(\`\${name}\u529f\u80fd\u5f00\u53d1\u4e2d\`)
    },`,
  `    stubAction(name) {
      this.$modal.msgInfo(\`\${name}\u529f\u80fd\u5f00\u53d1\u4e2d\`)
    },
    applyDraftSnapshot(draft) {
      if (!draft) return
      if (draft.form) {
        this.form = { ...this.form, ...draft.form, header: { ...this.form.header, ...(draft.form.header || {}) } }
      }
      if (draft.groupTab) this.groupTab = draft.groupTab
      if (draft.orderRadio) this.orderRadio = draft.orderRadio
      if (draft.paperTemplate) this.paperTemplate = draft.paperTemplate
      if (draft.answerAreas) this.answerAreas = { ...draft.answerAreas }
      if (draft.exportFormat) this.exportFormat = draft.exportFormat
      if (draft.pageLayout) this.pageLayout = draft.pageLayout
    },
    openAnalysis() {
      if (!this.canPreview) {
        this.$modal.msgWarning('\\u8bf7\\u5148\\u52a0\\u5165\\u8bd5\\u9898\\u5e76\\u8bbe\\u7f6e\\u5206\\u503c')
        return
      }
      this.analysisOpen = true
    },
    confirmSaveDraft() {
      if (!this.canPreview) {
        this.$modal.msgWarning('\\u8bf7\\u5148\\u52a0\\u5165\\u8bd5\\u9898\\u5e76\\u8bbe\\u7f6e\\u5206\\u503c')
        return
      }
      const savedAt = Date.now()
      this.saveDraft(false, savedAt)
      this.saveDialogInfo = {
        title: this.form.paperTitle || '\\u672a\\u547d\\u540d\\u8bd5\\u5377',
        count: this.basketCount,
        score: this.basketTotalScore,
        savedAtText: new Date(savedAt).toLocaleString()
      }
      this.saveDialogOpen = true
    },
    downloadAnswerSheet() {
      if (!this.canPreview) {
        this.$modal.msgWarning('\\u8bf7\\u5148\\u52a0\\u5165\\u8bd5\\u9898\\u5e76\\u8bbe\\u7f6e\\u5206\\u503c')
        return
      }
      this.sheetLoading = true
      exportAnswerSheetClient(this).then(() => {
        this.$modal.msgSuccess('\\u7b54\\u9898\\u5361\\u5bfc\\u51fa\\u6210\\u529f')
        this.saveDraft(false)
      }).catch(err => {
        console.error('answer sheet export failed', err)
        this.$modal.msgError('\\u7b54\\u9898\\u5361\\u5bfc\\u51fa\\u5931\\u8d25\\uFF0C\\u8bf7\\u7a0d\\u540e\\u91cd\\u8bd5')
      }).finally(() => { this.sheetLoading = false })
    },
    sharePaper() {
      if (!this.canPreview) {
        this.$modal.msgWarning('\\u8bf7\\u5148\\u52a0\\u5165\\u8bd5\\u9898\\u5e76\\u8bbe\\u7f6e\\u5206\\u503c')
        return
      }
      const id = generateShareId()
      const snapshot = this.buildShareSnapshot()
      savePaperShare(id, snapshot)
      const base = window.location.origin
      const path = this.isPortal
        ? '/portal/paper/preview'
        : '/question-bank-center/question-bank/paper/preview'
      this.shareLink = base + path + '?share=' + encodeURIComponent(id)
      this.shareDialogOpen = true
    },
    buildShareSnapshot() {
      return {
        form: this.form,
        groupTab: this.groupTab,
        orderRadio: this.orderRadio,
        paperTemplate: this.paperTemplate,
        exportFormat: this.exportFormat,
        pageLayout: this.pageLayout,
        answerAreas: this.answerAreas,
        items: this.questionBasketItems.map(item => ({ ...item })),
        savedAt: Date.now()
      }
    },
    copyShareLink() {
      const text = this.shareLink
      if (!text) return
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
          this.$modal.msgSuccess('\\u94fe\\u63a5\\u5df2\\u590d\\u5236')
        }).catch(() => this.fallbackCopyShareLink(text))
        return
      }
      this.fallbackCopyShareLink(text)
    },
    fallbackCopyShareLink(text) {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.left = '-9999px'
      document.body.appendChild(ta)
      ta.select()
      try {
        document.execCommand('copy')
        this.$modal.msgSuccess('\\u94fe\\u63a5\\u5df2\\u590d\\u5236')
      } catch (e) {
        this.$modal.msgWarning('\\u590d\\u5236\\u5931\\u8d25\\uFF0C\\u8bf7\\u624b\\u52a8\\u590d\\u5236\\u94fe\\u63a5')
      }
      document.body.removeChild(ta)
    },`
)

// 8. Enhance saveDraft
mustReplace(
  'saveDraft',
  `    saveDraft(showMsg = true) {
      savePaperDraft({
        form: this.form,
        groupTab: this.groupTab,
        orderRadio: this.orderRadio,
        paperTemplate: this.paperTemplate,
        exportFormat: this.exportFormat,
        pageLayout: this.pageLayout,
        answerAreas: this.answerAreas,
        savedAt: Date.now(),
        itemCount: this.questionBasketItems.length
      })
      if (showMsg) {
        this.$modal.msgSuccess('\u8bd5\u5377\u5df2\u4fdd\u5b58\u8349\u7a3f')
      }
    },`,
  `    saveDraft(showMsg = true, savedAt) {
      const ts = savedAt || Date.now()
      savePaperDraft({
        form: this.form,
        groupTab: this.groupTab,
        orderRadio: this.orderRadio,
        paperTemplate: this.paperTemplate,
        exportFormat: this.exportFormat,
        pageLayout: this.pageLayout,
        answerAreas: this.answerAreas,
        items: this.questionBasketItems.map(item => ({ ...item })),
        savedAt: ts,
        itemCount: this.questionBasketItems.length
      })
      if (showMsg) {
        this.$modal.msgSuccess('\u8bd5\u5377\u5df2\u4fdd\u5b58\u8349\u7a3f')
      }
    },`
)

// 9. Styles
mustReplace('styles', '.paper-compose-page--portal {', stylesBlock)

fs.writeFileSync(file, text.replace(/\n/g, '\r\n'), 'utf8')
console.log('preview.vue features patched OK')
