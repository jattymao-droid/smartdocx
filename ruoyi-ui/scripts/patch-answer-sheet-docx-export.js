/**
 * Add answer sheet DOCX export to preview.vue (ASCII-safe).
 * Run: node scripts/patch-answer-sheet-docx-export.js
 */
const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/views/education/question-bank/paper/preview.vue')
let text = fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n')

const importOld = `  exportAnswerSheetClient,
  printAnswerSheetClient,
  buildAnswerSheetPreviewHtml,
  getAnswerSheetStats
} from '@/utils/paperAnswerSheetExport'`

const importNew = `  exportAnswerSheetClient,
  exportAnswerSheetDocxClient,
  printAnswerSheetClient,
  buildAnswerSheetPreviewHtml,
  getAnswerSheetStats
} from '@/utils/paperAnswerSheetExport'`

if (!text.includes(importOld)) {
  console.error('import block not found')
  process.exit(1)
}
text = text.replace(importOld, importNew)

const footerOld = `        <el-button size="small" icon="el-icon-printer" :loading="sheetLoading" @click="printAnswerSheet">${'\u6253\u5370'}</el-button>
        <el-button type="primary" size="small" icon="el-icon-download" :loading="sheetLoading" @click="confirmAnswerSheetExport">${'\u4e0b\u8f7d PDF'}</el-button>`

const footerNew = `        <el-button size="small" icon="el-icon-printer" :loading="sheetLoading" @click="printAnswerSheet">${'\u6253\u5370'}</el-button>
        <el-button size="small" icon="el-icon-document" :loading="sheetLoading" @click="confirmAnswerSheetDocxExport">${'\u4e0b\u8f7d Word'}</el-button>
        <el-button type="primary" size="small" icon="el-icon-download" :loading="sheetLoading" @click="confirmAnswerSheetExport">${'\u4e0b\u8f7d PDF'}</el-button>`

if (!text.includes(footerOld)) {
  console.error('footer buttons not found')
  process.exit(1)
}
text = text.replace(footerOld, footerNew)

const exportOld = `    confirmAnswerSheetExport() {
      if (!this.canPreview) return
      this.sheetLoading = true
      exportAnswerSheetClient(this, { ...this.answerSheetOptions }).then(() => {
        this.$modal.msgSuccess('\\u7b54\\u9898\\u5361\\u5bfc\\u51fa\\u6210\\u529f')
        this.answerSheetOpen = false
        this.saveDraft(false)
      }).catch(err => {
        console.error('answer sheet export failed', err)
        this.$modal.msgError('\\u7b54\\u9898\\u5361\\u5bfc\\u51fa\\u5931\\u8d25\\uff0c\\u8bf7\\u7a0d\\u540e\\u91cd\\u8bd5')
      }).finally(() => { this.sheetLoading = false })
    },`

const exportNew = `    confirmAnswerSheetDocxExport() {
      if (!this.canPreview) return
      this.sheetLoading = true
      exportAnswerSheetDocxClient(this, { ...this.answerSheetOptions }).then(() => {
        this.$modal.msgSuccess('\\u7b54\\u9898\\u5361 Word \\u5bfc\\u51fa\\u6210\\u529f')
        this.answerSheetOpen = false
        this.saveDraft(false)
      }).catch(err => {
        console.error('answer sheet docx export failed', err)
        this.$modal.msgError('\\u7b54\\u9898\\u5361 Word \\u5bfc\\u51fa\\u5931\\u8d25\\uff0c\\u8bf7\\u7a0d\\u540e\\u91cd\\u8bd5')
      }).finally(() => { this.sheetLoading = false })
    },
    confirmAnswerSheetExport() {
      if (!this.canPreview) return
      this.sheetLoading = true
      exportAnswerSheetClient(this, { ...this.answerSheetOptions }).then(() => {
        this.$modal.msgSuccess('\\u7b54\\u9898\\u5361 PDF \\u5bfc\\u51fa\\u6210\\u529f')
        this.answerSheetOpen = false
        this.saveDraft(false)
      }).catch(err => {
        console.error('answer sheet export failed', err)
        this.$modal.msgError('\\u7b54\\u9898\\u5361 PDF \\u5bfc\\u51fa\\u5931\\u8d25\\uff0c\\u8bf7\\u7a0d\\u540e\\u91cd\\u8bd5')
      }).finally(() => { this.sheetLoading = false })
    },`

if (!text.includes('confirmAnswerSheetExport()')) {
  console.error('confirmAnswerSheetExport not found')
  process.exit(1)
}
if (text.includes('confirmAnswerSheetDocxExport')) {
  console.log('already patched')
} else {
  text = text.replace(exportOld, exportNew)
}

fs.writeFileSync(file, text, 'utf8')
console.log('preview.vue patched for answer sheet DOCX export')
