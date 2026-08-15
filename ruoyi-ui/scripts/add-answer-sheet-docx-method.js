const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/views/education/question-bank/paper/preview.vue')
let text = fs.readFileSync(file, 'utf8')

if (text.includes('confirmAnswerSheetDocxExport()')) {
  console.log('method already exists')
  process.exit(0)
}

const marker = '    confirmAnswerSheetExport() {'
const idx = text.indexOf(marker)
if (idx < 0) {
  console.error('marker not found')
  process.exit(1)
}

const method = `    confirmAnswerSheetDocxExport() {
      if (!this.canPreview) return
      this.sheetLoading = true
      exportAnswerSheetDocxClient(this, { ...this.answerSheetOptions }).then(() => {
        this.$modal.msgSuccess('${'\u7b54\u9898\u5361 Word \u5bfc\u51fa\u6210\u529f'}')
        this.answerSheetOpen = false
        this.saveDraft(false)
      }).catch(err => {
        console.error('answer sheet docx export failed', err)
        this.$modal.msgError('${'\u7b54\u9898\u5361 Word \u5bfc\u51fa\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5'}')
      }).finally(() => { this.sheetLoading = false })
    },
`

text = text.slice(0, idx) + method + text.slice(idx)
text = text.replace(
  "this.$modal.msgSuccess('\\u7b54\\u9898\\u5361\\u5bfc\\u51fa\\u6210\\u529f')",
  "this.$modal.msgSuccess('\\u7b54\\u9898\\u5361 PDF \\u5bfc\\u51fa\\u6210\\u529f')"
)
text = text.replace(
  "this.$modal.msgError('\\u7b54\\u9898\\u5361\\u5bfc\\u51fa\\u5931\\u8d25",
  "this.$modal.msgError('\\u7b54\\u9898\\u5361 PDF \\u5bfc\\u51fa\\u5931\\u8d25"
)

fs.writeFileSync(file, text, 'utf8')
console.log('added confirmAnswerSheetDocxExport')
