const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/views/education/question-bank/paper/preview.vue')
let text = fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n')

const markerStart = '    if (!this.questionBasketItems.length) {'
const markerEnd = '    if (!this.form.paperTitle) {'
const startIdx = text.indexOf(markerStart)
const endIdx = text.indexOf(markerEnd)
if (startIdx < 0 || endIdx < 0 || endIdx <= startIdx) {
  console.error('markers not found')
  process.exit(1)
}

const replacement = `    if (this.$route.query.draft === '1') {
      const draft = loadPaperDraft()
      if (draft) {
        if (draft.items && draft.items.length) {
          this.$store.commit('questionBasket/SET_ITEMS', draft.items)
        }
        this.applyDraftSnapshot(draft)
      }
    }
    const shareId = this.$route.query.share
    if (shareId) {
      const snap = loadPaperShare(String(shareId))
      if (snap && snap.items && snap.items.length) {
        this.$store.commit('questionBasket/SET_ITEMS', snap.items)
        this.applyDraftSnapshot(snap)
        this.$modal.msgSuccess('\u5df2\u52a0\u8f7d\u5206\u4eab\u7684\u8bd5\u5377')
      } else {
        this.$modal.msgWarning('\u5206\u4eab\u94fe\u63a5\u65e0\u6548\u6216\u5df2\u8fc7\u671f')
      }
    }
    if (!this.questionBasketItems.length) {
      this.$modal.msgWarning('\u8bd5\u9898\u680f\u4e3a\u7a7a\uff0c\u8bf7\u5148\u52a0\u5165\u8bd5\u9898')
      if (this.isPortal) {
        this.$router.replace('/portal/chapter')
      }
      return
    }
`

text = text.slice(0, startIdx) + replacement + text.slice(endIdx)
text = text.replace(
  "duration: this.form.header.duration || '90 \\u5206\\u949f'",
  "duration: this.form.header.duration || '90 \u5206\u949f'"
)

fs.writeFileSync(file, text.replace(/\n/g, '\r\n'), 'utf8')
console.log('fixed preview share/draft order')
