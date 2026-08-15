/**
 * Patch preview.vue: save paper to backend when logged in.
 * Run: node scripts/patch-preview-save-mypaper.js
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

if (text.includes('saveMyPaper')) {
  console.log('preview save-mypaper already patched')
  process.exit(0)
}

mustReplace(
  'import saveMyPaper',
  "import { exportAnswerSheetClient } from '@/utils/paperAnswerSheetExport'",
  "import { exportAnswerSheetClient } from '@/utils/paperAnswerSheetExport'\nimport { saveMyPaper } from '@/api/education/paper'"
)

mustReplace(
  'data paperId',
  '      sheetLoading: false\n    }\n  },',
  `      sheetLoading: false,
      savedPaperId: null
    }
  },`
)

mustReplace(
  'draft load paperId',
  `        this.applyDraftSnapshot(draft)
      }
    }
    const shareId = this.$route.query.share`,
  `        this.applyDraftSnapshot(draft)
        if (draft.paperId) this.savedPaperId = draft.paperId
      }
    }
    if (this.$route.query.paperId) {
      this.savedPaperId = Number(this.$route.query.paperId) || this.savedPaperId
    }
    const shareId = this.$route.query.share`
)

mustReplace(
  'confirmSaveDraft async',
  `    confirmSaveDraft() {
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
    },`,
  `    confirmSaveDraft() {
      if (!this.canPreview) {
        this.$modal.msgWarning('\\u8bf7\\u5148\\u52a0\\u5165\\u8bd5\\u9898\\u5e76\\u8bbe\\u7f6e\\u5206\\u503c')
        return
      }
      const savedAt = Date.now()
      this.saveDraft(false, savedAt)
      const finish = () => {
        this.saveDialogInfo = {
          title: this.form.paperTitle || '\\u672a\\u547d\\u540d\\u8bd5\\u5377',
          count: this.basketCount,
          score: this.basketTotalScore,
          savedAtText: new Date(savedAt).toLocaleString()
        }
        this.saveDialogOpen = true
      }
      if (this.$store.getters.token) {
        saveMyPaper(this.buildSavePaperPayload()).then(res => {
          if (res.data) this.savedPaperId = res.data
          finish()
        }).catch(() => finish())
        return
      }
      finish()
    },
    buildSavePaperPayload() {
      const paperId = this.savedPaperId || (this.$route.query.paperId ? Number(this.$route.query.paperId) : null)
      return {
        paperId: paperId || undefined,
        paperTitle: this.form.paperTitle,
        templateCode: this.form.templateCode,
        sortMode: this.form.sortMode,
        exportMode: this.form.exportMode,
        answerLayout: this.form.answerLayout,
        header: this.form.header,
        exportConfig: this.form.exportConfig,
        groupTab: this.groupTab,
        orderRadio: this.orderRadio,
        paperTemplate: this.paperTemplate,
        exportFormat: this.exportFormat,
        pageLayout: this.pageLayout,
        answerAreas: this.answerAreas,
        basketItems: this.questionBasketItems.map(item => ({ ...item })),
        items: this.questionBasketItems.map((item, idx) => ({
          questionId: item.questionId,
          orderNum: item.orderNum || idx + 1,
          scoreValue: item.scoreValue
        }))
      }
    },`
)

mustReplace(
  'saveDraft paperId',
  `        items: this.questionBasketItems.map(item => ({ ...item })),
        savedAt: ts,
        itemCount: this.questionBasketItems.length
      })`,
  `        items: this.questionBasketItems.map(item => ({ ...item })),
        paperId: this.savedPaperId,
        savedAt: ts,
        itemCount: this.questionBasketItems.length
      })`
)

fs.writeFileSync(file, text.replace(/\n/g, '\r\n'), 'utf8')
console.log('preview save-mypaper patched OK')
